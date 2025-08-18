package com.cloud.baseai.infrastructure.security.jwt;

import com.cloud.baseai.infrastructure.config.properties.SecurityProperties;
import com.cloud.baseai.infrastructure.security.UserPrincipal;
import com.cloud.baseai.infrastructure.security.service.CustomUserDetailsService;
import com.cloud.baseai.infrastructure.security.service.LoginProtectionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <h1>JWT认证过滤器</h1>
 *
 * <p>这个过滤器是整个JWT认证流程的核心组件，负责从HTTP请求中提取JWT令牌，
 * 验证其有效性，并在Spring Security上下文中建立用户的安全身份。</p>
 *
 * <p><b>过滤器工作流程：</b></p>
 * <ol>
 * <li><b>令牌提取：</b>从Authorization头、查询参数或自定义头中提取JWT</li>
 * <li><b>令牌验证：</b>验证签名、过期时间、黑名单状态等</li>
 * <li><b>用户加载：</b>根据令牌信息加载完整的用户详情</li>
 * <li><b>权限设置：</b>在Security上下文中设置用户认证状态</li>
 * <li><b>请求传递：</b>将请求传递给下一个过滤器或控制器</li>
 * </ol>
 *
 * <p><b>安全特性：</b></p>
 * <ul>
 * <li>多种令牌提取方式适配不同客户端</li>
 * <li>设备指纹验证防止令牌盗用</li>
 * <li>详细的安全事件日志记录</li>
 * <li>优雅的异常处理不影响业务流程</li>
 * </ul>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /**
     * JWT令牌在HTTP头中的字段名
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_PARAM = "token";
    private static final String CUSTOM_TOKEN_HEADER = "X-Auth-Token";
    private static final String DEVICE_FINGERPRINT_HEADER = "X-Device-Fingerprint";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    // 白名单路径（不需要认证的路径）
    private static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/users/register",
            "/api/v1/users/activate",
            "/api/v1/health",
            "/actuator/health"
    ));

    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService userDetailsService;
    private final SecurityProperties securityProperties;
    private final LoginProtectionService loginProtectionService;

    /**
     * 构造函数，注入依赖的服务
     */
    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   CustomUserDetailsService userDetailsService,
                                   SecurityProperties securityProperties,
                                   LoginProtectionService loginProtectionService) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
        this.securityProperties = securityProperties;
        this.loginProtectionService = loginProtectionService;
    }

    /**
     * 过滤器的核心处理方法
     *
     * <p>这个方法处理每个HTTP请求，执行JWT认证逻辑。为了保证性能，
     * 对于不需要认证的请求会快速跳过，对于已认证的请求避免重复处理。</p>
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String requestId = getOrGenerateRequestId(request);

        // 设置MDC用于日志追踪
        MDC.put("requestId", requestId);
        MDC.put("requestUri", requestURI);

        // 记录请求信息（仅在调试模式下）
        if (log.isDebugEnabled()) {
            log.debug("处理JWT认证: {} {}", method, requestURI);
        }

        try {
            // 快速跳过公开路径
            if (isPublicPath(requestURI)) {
                log.debug("公开路径，跳过JWT认证: {}", requestURI);
                filterChain.doFilter(request, response);
                return;
            }

            // 检查是否已经认证（避免重复处理）
            if (SecurityContextHolder.getContext().getAuthentication() != null &&
                    SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
                log.debug("用户已认证，跳过JWT处理");
                filterChain.doFilter(request, response);
                return;
            }

            // ========== 步骤1：提取JWT令牌 ==========
            String jwt = extractJwtFromRequest(request);
            if (jwt == null) {
                log.debug("请求中未找到JWT令牌: {} {}", method, requestURI);
                filterChain.doFilter(request, response);
                return;
            }

            // ========== 步骤2：提取设备指纹 ==========
            String deviceFingerprint = extractDeviceFingerprint(request);
            if (securityProperties.getJwt().isEnableDeviceFingerprint() &&
                    (deviceFingerprint == null || deviceFingerprint.trim().isEmpty())) {
                log.warn("缺少必需的设备指纹: {} {}", method, requestURI);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "设备指纹验证失败");
                return;
            }

            // ========== 步骤3：获取客户端IP（用于安全检查） ==========
            String clientIp = extractClientIpAddress(request);

            // ========== 步骤4：检查IP是否被封禁（防暴力破解） ==========
            if (loginProtectionService.isIpBlocked(clientIp)) {
                log.warn("IP地址被封禁: ip={}, uri={}", clientIp, requestURI);
                response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE, "请求过于频繁，请稍后再试");
                return;
            }

            // ========== 步骤5：验证JWT令牌 ==========
            if (!jwtTokenService.validateToken(jwt, deviceFingerprint)) {
                log.debug("JWT令牌验证失败: {} {}", method, requestURI);
                // 记录失败尝试
                loginProtectionService.recordFailedAttempt(clientIp);
                filterChain.doFilter(request, response);
                return;
            }

            // ========== 步骤6：提取用户ID并加载用户信息 ==========
            Long userId = jwtTokenService.getUserIdFromToken(jwt);
            if (userId == null) {
                log.warn("无法从JWT中提取用户ID");
                filterChain.doFilter(request, response);
                return;
            }

            // 在MDC中添加用户信息用于日志
            MDC.put("userId", userId.toString());

            // ========== 步骤7：加载用户详细信息 ==========
            UserDetails userDetails = userDetailsService.loadUserById(userId);
            if (userDetails == null) {
                log.warn("无法加载用户信息: userId={}", userId);
                filterChain.doFilter(request, response);
                return;
            }

            // ========== 步骤8：检查用户账户状态 ==========
            if (!validateUserAccountStatus(userDetails)) {
                log.warn("用户账户状态异常: userId={}", userId);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "账户状态异常");
                return;
            }

            // ========== 步骤9：验证IP地址（如果启用） ==========
            if (securityProperties.getAuth().isEnableIpValidation()) {
                if (!validateIpAddress(clientIp, jwt)) {
                    log.warn("IP地址验证失败: userId={}, ip={}", userId, clientIp);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "IP地址验证失败");
                    return;
                }
            }

            // ========== 步骤10：设置Spring Security认证上下文 ==========
            setAuthenticationContext(userDetails, request);

            // ========== 步骤11：记录成功的认证 ==========
            logAuthenticationSuccess(userDetails, request, deviceFingerprint);

            // ========== 步骤12：重置失败计数 ==========
            loginProtectionService.resetFailedAttempts(clientIp);

        } catch (Exception e) {
            // 记录错误但不抛出，让请求继续处理
            // 这样可以让Spring Security的异常处理机制正常工作
            log.error("JWT认证过滤器处理异常: uri={}, error={}", requestURI, e.getMessage(), e);

            // 清理可能的部分认证状态
            SecurityContextHolder.clearContext();
        } finally {
            // 传递请求到下一个过滤器
            filterChain.doFilter(request, response);
            // 清理MDC
            MDC.clear();
        }
    }

    /**
     * 从HTTP请求中提取JWT令牌
     *
     * <p>支持多种令牌传递方式，适配不同类型的客户端：</p>
     * <ul>
     * <li><b>Authorization头：</b>标准的Bearer令牌方式：Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...</li>
     * <li><b>查询参数：</b>适用于WebSocket等无法设置头部的场景：?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...</li>
     * <li><b>自定义头：</b>适用于移动应用等特殊场景：X-Auth-Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...</li>
     * </ul>
     *
     * @param request HTTP请求对象
     * @return 提取出的JWT令牌字符串，如果没有找到返回null
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        // 方式1：从Authorization头中提取（优先级最高）
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty()) {
                log.debug("从Authorization头中提取到JWT令牌");
                return token;
            }
        }

        // 方式2：从查询参数中提取（用于WebSocket等场景）
        String tokenParam = request.getParameter(TOKEN_PARAM);
        if (StringUtils.hasText(tokenParam)) {
            log.debug("从查询参数中提取到JWT令牌");
            return tokenParam.trim();
        }

        // 方式3：从自定义头中提取（用于移动应用等场景）
        String customToken = request.getHeader(CUSTOM_TOKEN_HEADER);
        if (StringUtils.hasText(customToken)) {
            log.debug("从{}头中提取到JWT令牌", CUSTOM_TOKEN_HEADER);
            return customToken.trim();
        }

        return null;
    }

    /**
     * 提取设备指纹
     *
     * <p>设备指纹用于增强安全性，防止令牌被盗用。通过收集客户端的
     * 各种特征信息生成唯一标识。</p>
     */
    private String extractDeviceFingerprint(HttpServletRequest request) {
        // 1. 从专用头部获取设备指纹
        String fingerprint = request.getHeader(DEVICE_FINGERPRINT_HEADER);

        if (StringUtils.hasText(fingerprint)) {
            return fingerprint;
        }

        // 2. 如果配置要求设备指纹但未提供，生成基础指纹
        if (securityProperties.getJwt().isEnableDeviceFingerprint()) {
            // 基于请求特征生成指纹
            String userAgent = request.getHeader("User-Agent");
            String acceptLanguage = request.getHeader("Accept-Language");
            String acceptEncoding = request.getHeader("Accept-Encoding");
            String remoteAddr = getClientIpAddress(request);

            if (userAgent != null) {
                StringBuilder fpBuilder = new StringBuilder();
                fpBuilder.append(userAgent.hashCode());
                if (acceptLanguage != null) {
                    fpBuilder.append(":").append(acceptLanguage.hashCode());
                }
                if (acceptEncoding != null) {
                    fpBuilder.append(":").append(acceptEncoding.hashCode());
                }
                if (remoteAddr != null) {
                    fpBuilder.append(":").append(remoteAddr.hashCode());
                }

                fingerprint = String.valueOf(fpBuilder.toString().hashCode());
                log.debug("生成设备指纹: {}", fingerprint);
                return fingerprint;
            }
        }

        return null;
    }

    /**
     * 提取客户端真实IP地址
     *
     * <p>在使用代理或负载均衡器的环境中，需要从特定头部获取真实IP。</p>
     */
    private String extractClientIpAddress(HttpServletRequest request) {
        // 按优先级检查各种头部
        String[] headers = {
                "X-Real-IP",
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // 处理多个IP的情况（取第一个）
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                // 验证IP格式
                if (isValidIpAddress(ip)) {
                    log.trace("从{}头部获取IP: {}", header, ip);
                    return ip;
                }
            }
        }

        // 如果都没有，使用直接连接的IP
        String remoteAddr = request.getRemoteAddr();
        return isValidIpAddress(remoteAddr) ? remoteAddr : "unknown";
    }

    /**
     * 验证用户账户状态
     */
    private boolean validateUserAccountStatus(UserDetails userDetails) {
        if (!userDetails.isEnabled()) {
            log.warn("用户账户已禁用: username={}", userDetails.getUsername());
            return false;
        }

        if (!userDetails.isAccountNonLocked()) {
            log.warn("用户账户已锁定: username={}", userDetails.getUsername());
            return false;
        }

        if (!userDetails.isAccountNonExpired()) {
            log.warn("用户账户已过期: username={}", userDetails.getUsername());
            return false;
        }

        if (!userDetails.isCredentialsNonExpired()) {
            log.warn("用户凭证已过期: username={}", userDetails.getUsername());
            return false;
        }

        return true;
    }

    /**
     * 验证IP地址是否允许
     */
    private boolean validateIpAddress(String clientIp, String jwt) {
        // 从JWT中获取原始IP
        String tokenIp = jwtTokenService.getIpFromToken(jwt);

        if (tokenIp != null && !tokenIp.equals(clientIp)) {
            // 检查是否在允许的IP范围内
            String allowedIps = securityProperties.getAuth().getAllowedIps();
            if (StringUtils.hasText(allowedIps)) {
                return Arrays.stream(allowedIps.split(","))
                        .anyMatch(ip -> ip.trim().equals(clientIp));
            }

            // 如果IP变化且不在白名单，认为不安全
            return false;
        }

        return true;
    }

    /**
     * 设置Spring Security认证上下文
     */
    private void setAuthenticationContext(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("认证上下文设置成功: username={}", userDetails.getUsername());
    }

    /**
     * 根据JWT令牌认证用户并设置安全上下文
     *
     * <p>这个方法是认证流程的核心，它从JWT令牌中提取用户信息，
     * 加载完整的用户详情，并在Spring Security上下文中设置认证状态。</p>
     *
     * @param jwt               JWT令牌字符串
     * @param request           HTTP请求对象，用于设置认证详情
     * @param deviceFingerprint 设备指纹
     */
    private void authenticateUser(String jwt, HttpServletRequest request, String deviceFingerprint) {
        try {
            // 从JWT中提取用户ID
            Long userId = jwtTokenService.getUserIdFromToken(jwt);
            if (userId == null) {
                log.warn("无法从JWT令牌中提取用户ID");
                return;
            }

            log.debug("从JWT中提取到用户ID: {}", userId);

            // 加载用户详细信息
            UserDetails userDetails = userDetailsService.loadUserById(userId);
            if (userDetails == null) {
                log.warn("无法加载用户信息: userId={}", userId);
                return;
            }

            // 检查用户账户状态
            if (!userDetails.isEnabled()) {
                log.warn("用户账户已禁用: userId={}", userId);
                return;
            }

            if (!userDetails.isAccountNonLocked()) {
                log.warn("用户账户已锁定: userId={}", userId);
                return;
            }

            if (!userDetails.isAccountNonExpired()) {
                log.warn("用户账户已过期: userId={}", userId);
                return;
            }

            if (!userDetails.isCredentialsNonExpired()) {
                log.warn("用户凭证已过期: userId={}", userId);
                return;
            }

            // 创建认证令牌
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,                    // 主体（用户信息）
                            null,                          // 凭证（JWT场景下不需要密码）
                            userDetails.getAuthorities()   // 权限列表
                    );

            // 设置认证详情（包含IP地址、会话ID等信息）
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 在Spring Security上下文中设置认证信息
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 记录成功的认证事件
            logAuthenticationSuccess(userDetails, request, deviceFingerprint);

        } catch (Exception e) {
            log.error("设置用户认证失败: {}", e.getMessage(), e);
            // 清理可能的部分设置
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 记录认证成功事件
     *
     * <p>记录用户的成功认证事件，这些信息对于安全审计和用户行为分析非常重要。</p>
     *
     * @param userDetails       用户主体信息
     * @param request           HTTP请求对象
     * @param deviceFingerprint 设备指纹
     */
    private void logAuthenticationSuccess(UserDetails userDetails,
                                          HttpServletRequest request,
                                          String deviceFingerprint) {
        if (userDetails instanceof UserPrincipal userPrincipal) {
            String clientIp = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String requestURI = request.getRequestURI();

            log.info("JWT认证成功 - userId: {}, username: {}, ip: {}, uri: {}, fingerprint: {}, time: {}",
                    userPrincipal.getId(),
                    userPrincipal.getUsername(),
                    clientIp,
                    requestURI,
                    deviceFingerprint != null ? "present" : "absent",
                    LocalDateTime.now());

            // 这里可以发送认证成功事件到审计系统
            // auditService.recordJwtAuthenticationSuccess(userPrincipal, clientIp, userAgent, requestURI);
        }
    }

    /**
     * 获取或生成请求ID（用于日志追踪）
     */
    private String getOrGenerateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (!StringUtils.hasText(requestId)) {
            requestId = java.util.UUID.randomUUID().toString();
        }
        return requestId;
    }

    /**
     * 检查是否为公开路径
     */
    private boolean isPublicPath(String path) {
        // 精确匹配
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }

        // 前缀匹配（静态资源等）
        return path.startsWith("/static/") ||
                path.startsWith("/public/") ||
                path.startsWith("/assets/") ||
                path.equals("/favicon.ico") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/");
    }

    /**
     * 获取客户端真实IP地址
     *
     * <p>在使用负载均衡器或代理服务器的环境中，需要从特定的头部中
     * 获取客户端的真实IP地址。</p>
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // 处理多个IP的情况（用逗号分隔）
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                // 验证IP格式
                if (isValidIpAddress(ip)) {
                    return ip;
                }
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return isValidIpAddress(remoteAddr) ? remoteAddr : "unknown";
    }

    /**
     * 验证IP地址格式
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // IPv4格式验证
        String ipv4Pattern = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

        // IPv6格式验证（简化版）
        String ipv6Pattern = "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$";

        return ip.matches(ipv4Pattern) ||
                ip.matches(ipv6Pattern) ||
                "localhost".equals(ip) ||
                "127.0.0.1".equals(ip) ||
                "::1".equals(ip);
    }

    /**
     * 检查请求是否需要跳过JWT认证
     *
     * <p>某些请求（如静态资源、健康检查等）不需要进行JWT认证，
     * 可以直接跳过以提高性能。</p>
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // OPTIONS请求直接通过（CORS预检）
        if ("OPTIONS".equals(method)) {
            return true;
        }

        // 公开路径跳过
        return isPublicPath(path);
    }
}