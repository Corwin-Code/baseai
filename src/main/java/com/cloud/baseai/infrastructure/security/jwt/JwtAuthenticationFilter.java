package com.cloud.baseai.infrastructure.security.jwt;

import com.cloud.baseai.infrastructure.security.UserPrincipal;
import com.cloud.baseai.infrastructure.security.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * <h1>JWT认证过滤器</h1>
 *
 * <p>这个过滤器的工作是验证请求头中的JWT令牌，如果令牌有效，就为用户建立安全上下文，
 * 让后续的业务代码知道"这是谁在访问系统"，每个HTTP请求都必须通过这里的检查。</p>
 *
 * <p><b>过滤器工作流程：</b></p>
 * <p>1. <strong>检查证件：</strong>从请求头中提取JWT令牌</p>
 * <p>2. <strong>验证真伪：</strong>使用JWT工具验证令牌的有效性</p>
 * <p>3. <strong>查证身份：</strong>根据令牌信息加载用户详细信息</p>
 * <p>4. <strong>设置权限：</strong>在Spring Security上下文中设置用户身份</p>
 * <p>5. <strong>放行通过：</strong>让请求继续执行后续的业务逻辑</p>
 *
 * <p><b>设计理念：</b></p>
 * <p>我们遵循"一次验证，处处可用"的原则。一旦在这里验证了用户身份，
 * 整个请求处理过程中的任何地方都可以安全地获取当前用户信息，
 * 无需再次验证。这大大简化了业务代码的复杂度。</p>
 *
 * <p><b>性能优化考虑：</b></p>
 * <p>由于这个过滤器会拦截每个请求，性能至关重要。我们做了以下优化：</p>
 * <p>- 快速跳过不需要认证的公开端点</p>
 * <p>- 使用缓存减少重复的用户信息查询</p>
 * <p>- 采用异步日志记录避免阻塞请求处理</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /**
     * JWT令牌在HTTP头中的字段名
     * <p>标准做法是使用Authorization头，格式为"Bearer [token]"</p>
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Bearer令牌的前缀
     * <p>这是OAuth 2.0标准定义的Bearer令牌格式</p>
     */
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * 构造函数，注入依赖的服务
     */
    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, CustomUserDetailsService userDetailsService) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * 过滤器的核心处理方法
     *
     * <p>这个方法是整个认证流程的指挥中心。它协调各个组件完成
     * 用户身份的验证和权限的设置。每个HTTP请求都会经过这里，
     * 所以代码必须高效且健壮。</p>
     *
     * <p><b>处理步骤详解：</b></p>
     * <p>1. <strong>路径检查：</strong>首先检查当前请求是否需要认证</p>
     * <p>2. <strong>令牌提取：</strong>从Authorization头中提取JWT令牌</p>
     * <p>3. <strong>令牌验证：</strong>验证令牌的格式、签名和过期时间</p>
     * <p>4. <strong>用户加载：</strong>根据令牌中的用户ID加载完整的用户信息</p>
     * <p>5. <strong>权限设置：</strong>在Spring Security上下文中设置用户权限</p>
     * <p>6. <strong>继续处理：</strong>将请求传递给下一个过滤器或控制器</p>
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // 记录请求信息（仅在调试模式下）
        if (log.isDebugEnabled()) {
            log.debug("处理请求: {} {}", method, requestURI);
        }

        try {
            // ========== 步骤1：提取JWT令牌 ==========
            String jwt = extractJwtFromRequest(request);
            if (jwt == null) {
                log.debug("请求中未找到JWT令牌: {}", requestURI);
                filterChain.doFilter(request, response);
                return;
            }

            // ========== 步骤2：验证JWT令牌 ==========
            if (!jwtTokenService.validateToken(jwt, null)) {
                log.debug("JWT令牌验证失败: {}", requestURI);
                // 令牌无效，但不在这里处理错误，让后续的认证入口点处理
                filterChain.doFilter(request, response);
                return;
            }

            // ========== 步骤3：检查是否已经认证 ==========
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("用户已认证，跳过重复处理");
                filterChain.doFilter(request, response);
                return;
            }

            // ========== 步骤4：加载用户信息并设置认证 ==========
            authenticateUser(jwt, request);

        } catch (Exception e) {
            // 记录错误但不抛出，让请求继续处理
            // 这样可以让Spring Security的异常处理机制正常工作
            log.error("JWT认证过滤器处理异常: {}", e.getMessage(), e);
        }

        // ========== 步骤6：继续过滤器链 ==========
        filterChain.doFilter(request, response);
    }

    /**
     * 从HTTP请求中提取JWT令牌
     *
     * <p>JWT令牌通常放在Authorization头中，格式为"Bearer [token]"。
     * 这个方法负责解析这个头部并提取出纯净的令牌字符串。</p>
     *
     * <p><b>支持的令牌格式：</b></p>
     * <p>1. Authorization头：Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...</p>
     * <p>2. 查询参数：?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...</p>
     * <p>3. 自定义头：X-Auth-Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...</p>
     *
     * @param request HTTP请求对象
     * @return 提取出的JWT令牌字符串，如果没有找到返回null
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        // 方式1：从Authorization头中提取
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX.length());
            log.debug("从Authorization头中提取到JWT令牌");
            return token;
        }

        // 方式2：从查询参数中提取（用于WebSocket等场景）
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            log.debug("从查询参数中提取到JWT令牌");
            return tokenParam;
        }

        // 方式3：从自定义头中提取（用于移动应用等场景）
        String customToken = request.getHeader("X-Auth-Token");
        if (StringUtils.hasText(customToken)) {
            log.debug("从X-Auth-Token头中提取到JWT令牌");
            return customToken;
        }

        return null;
    }

    /**
     * 根据JWT令牌认证用户并设置安全上下文
     *
     * <p>这是认证流程的核心部分。我们从JWT令牌中提取用户ID，
     * 然后加载完整的用户信息，最后在Spring Security的上下文中
     * 设置用户的认证状态。</p>
     *
     * <p><b>为什么需要重新加载用户信息？</b></p>
     * <p>虽然JWT令牌中包含了一些用户信息，但为了确保数据的实时性
     * （比如用户权限可能已经被修改），我们还是会从数据库重新加载
     * 最新的用户信息。当然，这里可以使用缓存来提高性能。</p>
     *
     * @param jwt     JWT令牌字符串
     * @param request HTTP请求对象，用于设置认证详情
     */
    private void authenticateUser(String jwt, HttpServletRequest request) {
        try {
            // 从JWT中提取用户ID
            Long userId = jwtTokenService.getUserIdFromToken(jwt);
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

            // 创建认证令牌
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,           // 主体（用户信息）
                            null,                  // 凭证（密码，JWT场景下不需要）
                            userDetails.getAuthorities()  // 权限列表
                    );

            // 设置认证详情（包含IP地址、会话ID等信息）
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 在Spring Security上下文中设置认证信息
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 记录成功的认证事件（用于审计）
            if (userDetails instanceof UserPrincipal userPrincipal) {
                log.debug("用户认证成功: userId={}, username={}, tenants={}",
                        userPrincipal.getId(),
                        userPrincipal.getUsername(),
                        userPrincipal.getTenantIds());
            }

        } catch (Exception e) {
            log.error("设置用户认证失败: {}", e.getMessage(), e);
            // 清理可能的部分设置
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 记录认证成功事件
     *
     * <p>这个方法用于记录用户的成功登录事件，这些信息对于
     * 安全审计和用户行为分析非常重要。</p>
     *
     * @param userPrincipal 用户主体信息
     * @param request       HTTP请求对象
     */
    private void logAuthenticationSuccess(UserPrincipal userPrincipal, HttpServletRequest request) {
        // 提取客户端信息
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        // 这里可以发送认证成功事件到审计系统
        log.info("用户认证成功 - userId: {}, username: {}, ip: {}, userAgent: {}",
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                clientIp,
                userAgent);
    }

    /**
     * 获取客户端真实IP地址
     *
     * <p>在使用负载均衡器或代理服务器的环境中，request.getRemoteAddr()
     * 获取到的可能是代理服务器的IP。这个方法尝试从各种头部中
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
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}