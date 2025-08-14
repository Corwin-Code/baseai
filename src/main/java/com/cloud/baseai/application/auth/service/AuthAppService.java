package com.cloud.baseai.application.auth.service;

import com.cloud.baseai.application.auth.command.LoginCommand;
import com.cloud.baseai.application.auth.command.RefreshTokenCommand;
import com.cloud.baseai.application.auth.command.TokenValidationCommand;
import com.cloud.baseai.application.auth.dto.LoginResponseDTO;
import com.cloud.baseai.application.auth.dto.TokenResponseDTO;
import com.cloud.baseai.application.auth.dto.TokenValidationResponseDTO;
import com.cloud.baseai.application.auth.dto.UserInfoDTO;
import com.cloud.baseai.domain.audit.service.AuditService;
import com.cloud.baseai.domain.user.model.User;
import com.cloud.baseai.domain.user.repository.UserRepository;
import com.cloud.baseai.domain.user.service.UserDomainService;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.exception.UserException;
import com.cloud.baseai.infrastructure.security.UserPrincipal;
import com.cloud.baseai.infrastructure.security.jwt.JwtTokenService;
import com.cloud.baseai.infrastructure.security.service.CustomUserDetailsService;
import com.cloud.baseai.infrastructure.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>认证应用服务</h2>
 *
 * <p>这是认证系统的应用服务层，负责编排认证相关的业务流程。
 * 它作为Controller和Domain层之间的桥梁，确保业务逻辑的正确执行。</p>
 *
 * <p><b>主要职责：</b></p>
 * <ul>
 * <li><b>业务流程编排：</b>协调用户登录、登出、令牌管理等流程</li>
 * <li><b>事务管理：</b>确保认证操作的数据一致性</li>
 * <li><b>安全策略执行：</b>实施密码策略、设备验证等安全措施</li>
 * <li><b>异常处理：</b>将技术异常转换为业务友好的错误信息</li>
 * <li><b>审计记录：</b>记录认证相关的安全事件</li>
 * </ul>
 */
@Service
public class AuthAppService {

    private static final Logger log = LoggerFactory.getLogger(AuthAppService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final UserDomainService userDomainService;

    @Autowired(required = false)
    private AuditService auditService;

    public AuthAppService(
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            CustomUserDetailsService userDetailsService,
            UserRepository userRepository,
            UserDomainService userDomainService) {

        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.userDomainService = userDomainService;
    }

    /**
     * 用户登录
     *
     * <p>执行完整的用户登录流程，包括身份验证、令牌生成、安全检查等。</p>
     *
     * @param command 登录命令
     * @param request HTTP请求对象，用于获取客户端信息
     * @return 登录结果，包含令牌和用户信息
     * @throws UserException 当登录失败时抛出
     */
    public LoginResponseDTO login(LoginCommand command, HttpServletRequest request) {
        String clientIp = extractClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String deviceFingerprint = extractDeviceFingerprint(request);

        log.info("用户登录请求: username={}, ip={}, hasFingerprint={}",
                command.username(), clientIp, deviceFingerprint != null);

        try {
            // 步骤1：执行Spring Security认证
            Authentication authentication = performAuthentication(command);

            // 步骤2：获取用户主体信息
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            // 步骤3：更新用户最后登录时间
            updateLastLoginTime(userPrincipal.getId());

            // 步骤4：生成JWT令牌对
            JwtTokenService.TokenPair tokenPair = jwtTokenService.generateTokenPair(
                    userPrincipal, deviceFingerprint, clientIp);

            // 步骤5：构建登录响应
            LoginResponseDTO response = buildLoginResponse(
                    tokenPair, userPrincipal, deviceFingerprint, command.rememberMe());

            // 步骤6：记录登录成功事件
            recordLoginSuccessEvent(userPrincipal, clientIp, userAgent, deviceFingerprint);

            return response;

        } catch (AuthenticationException e) {
            log.warn("用户登录失败: username={}, ip={}, error={}",
                    command.username(), clientIp, e.getMessage());

            // 记录登录失败事件
            recordLoginFailureEvent(command.username(), clientIp, userAgent, e.getMessage());

            throw new UserException(ErrorCode.BIZ_USER_004, command.username());
        }
    }

    /**
     * 刷新访问令牌
     *
     * <p>使用刷新令牌获取新的访问令牌，延长用户登录状态。</p>
     *
     * @param command 刷新令牌命令
     * @param request HTTP请求对象
     * @return 新的令牌信息
     */
    public TokenResponseDTO refreshToken(RefreshTokenCommand command, HttpServletRequest request) {
        String deviceFingerprint = extractDeviceFingerprint(request);

        log.debug("收到令牌刷新请求, hasFingerprint={}", deviceFingerprint != null);

        try {
            String newAccessToken = jwtTokenService.refreshAccessToken(
                    command.refreshToken(), deviceFingerprint);

            if (newAccessToken == null) {
                log.warn("令牌刷新失败：无法生成新的访问令牌");
                throw new UserException(ErrorCode.BIZ_USER_025);
            }

            // 构建响应
            TokenResponseDTO response = new TokenResponseDTO(
                    newAccessToken,
                    command.refreshToken(), // 简化：复用原刷新令牌
                    "Bearer",
                    jwtTokenService.getTokenRemainingTime(newAccessToken)
            );

            log.debug("令牌刷新成功");
            return response;

        } catch (SecurityException e) {
            log.warn("令牌刷新安全验证失败: {}", e.getMessage());
            throw new UserException(ErrorCode.BIZ_USER_029);
        } catch (IllegalArgumentException e) {
            log.warn("令牌刷新参数错误: {}", e.getMessage());
            throw new UserException(ErrorCode.BIZ_USER_030);
        }
    }

    /**
     * 用户登出
     *
     * <p>处理用户登出，撤销令牌并清理会话信息。</p>
     *
     * @param authHeader Authorization头部信息
     * @param request    HTTP请求对象
     */
    public void logout(String authHeader, HttpServletRequest request) {
        String clientIp = extractClientIpAddress(request);

        try {
            // 提取JWT令牌
            String token = extractTokenFromHeader(authHeader);
            if (token != null) {
                // 撤销令牌
                jwtTokenService.revokeToken(token);

                // 获取当前用户信息用于日志记录
                Long userId = jwtTokenService.getUserIdFromToken(token);
                log.info("用户登出: userId={}, ip={}", userId, clientIp);

                // 记录登出事件
                if (userId != null) {
                    recordLogoutEvent(userId, clientIp);
                }
            }

            // 清除Security上下文
            SecurityContextHolder.clearContext();

        } catch (Exception e) {
            log.error("登出处理异常", e);
            // 即使出现异常也清除上下文
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 登出所有设备
     *
     * <p>撤销用户在所有设备上的登录状态。</p>
     */
    public void logoutAll() {
        try {
            Optional<Long> userIdOpt = SecurityUtils.getCurrentUserId();
            if (userIdOpt.isPresent()) {
                Long userId = userIdOpt.get();
                jwtTokenService.revokeAllUserTokens(userId);
                log.info("用户所有设备登出: userId={}", userId);

                // 记录全设备登出事件
                recordLogoutAllEvent(userId);
            }

            SecurityContextHolder.clearContext();

        } catch (Exception e) {
            log.error("登出所有设备异常", e);
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 获取当前用户信息
     *
     * <p>获取当前认证用户的详细信息。</p>
     *
     * @return 用户信息
     */
    public UserInfoDTO getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                throw new UserException(ErrorCode.BIZ_USER_003);
            }

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            return new UserInfoDTO(
                    userPrincipal.getId(),
                    userPrincipal.getUsername(),
                    userPrincipal.getEmail(),
                    userPrincipal.getRoles(),
                    userPrincipal.getTenantIds()
            );

        } catch (Exception e) {
            log.error("获取当前用户信息失败", e);
            throw new UserException(ErrorCode.BIZ_USER_009);
        }
    }

    /**
     * 验证令牌有效性
     *
     * <p>验证JWT令牌是否有效，并返回相关的用户信息。</p>
     *
     * @param command 令牌验证命令
     * @param request HTTP请求对象
     * @return 验证结果
     */
    public TokenValidationResponseDTO validateToken(TokenValidationCommand command,
                                                    HttpServletRequest request) {
        try {
            String token = command.token();
            String deviceFingerprint = extractDeviceFingerprint(request);

            if (!jwtTokenService.validateToken(token, deviceFingerprint)) {
                return new TokenValidationResponseDTO(false, "令牌无效", null);
            }

            // 获取令牌中的用户信息
            Long userId = jwtTokenService.getUserIdFromToken(token);
            String username = jwtTokenService.getUsernameFromToken(token);
            String email = jwtTokenService.getEmailFromToken(token);
            List<String> roles = jwtTokenService.getRolesFromToken(token);
            List<Long> tenantIds = jwtTokenService.getTenantIdsFromToken(token);

            UserInfoDTO userInfo = new UserInfoDTO(userId, username, email, roles, tenantIds);

            return new TokenValidationResponseDTO(true, "令牌有效", userInfo);

        } catch (Exception e) {
            log.error("令牌验证失败", e);
            return new TokenValidationResponseDTO(false, "令牌验证失败", null);
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 执行Spring Security认证
     */
    private Authentication performAuthentication(LoginCommand command) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        command.username(),
                        command.password()
                )
        );
    }

    /**
     * 更新用户最后登录时间
     */
    private void updateLastLoginTime(Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                User updatedUser = user.updateLastLogin();
                userRepository.save(updatedUser);
            }
        } catch (Exception e) {
            log.warn("更新用户最后登录时间失败: userId={}", userId, e);
        }
    }

    /**
     * 构建登录响应
     */
    private LoginResponseDTO buildLoginResponse(JwtTokenService.TokenPair tokenPair,
                                                UserPrincipal userPrincipal,
                                                String deviceFingerprint,
                                                boolean rememberMe) {
        return new LoginResponseDTO(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.getTokenType(),
                jwtTokenService.getTokenRemainingTime(tokenPair.accessToken()),
                new UserInfoDTO(
                        userPrincipal.getId(),
                        userPrincipal.getUsername(),
                        userPrincipal.getEmail(),
                        userPrincipal.getRoles(),
                        userPrincipal.getTenantIds()
                ),
                deviceFingerprint,
                rememberMe
        );
    }

    /**
     * 从Authorization头中提取令牌
     */
    private String extractTokenFromHeader(String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return null;
    }

    /**
     * 获取客户端真实IP地址
     */
    private String extractClientIpAddress(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 提取设备指纹
     */
    private String extractDeviceFingerprint(HttpServletRequest request) {
        // 优先从自定义头中获取
        String fingerprint = request.getHeader("X-Device-Fingerprint");

        if (!StringUtils.hasText(fingerprint)) {
            // 基于请求信息生成简单指纹
            String userAgent = request.getHeader("User-Agent");
            String acceptLanguage = request.getHeader("Accept-Language");
            String acceptEncoding = request.getHeader("Accept-Encoding");

            if (userAgent != null) {
                StringBuilder fpBuilder = new StringBuilder();
                fpBuilder.append(userAgent.hashCode());
                if (acceptLanguage != null) {
                    fpBuilder.append(":").append(acceptLanguage.hashCode());
                }
                if (acceptEncoding != null) {
                    fpBuilder.append(":").append(acceptEncoding.hashCode());
                }
                fingerprint = String.valueOf(fpBuilder.toString().hashCode());
            }
        }

        return fingerprint;
    }

    /**
     * 记录登录成功事件
     */
    private void recordLoginSuccessEvent(UserPrincipal user, String clientIp,
                                         String userAgent, String deviceFingerprint) {
        log.info("登录成功 - userId: {}, username: {}, ip: {}, userAgent: {}, fingerprint: {}, time: {}",
                user.getId(), user.getUsername(), clientIp, userAgent,
                deviceFingerprint != null ? "present" : "absent", LocalDateTime.now());

        if (auditService != null) {
            try {
                auditService.recordUserAction("USER_LOGIN_SUCCESS", user.getId(),
                        String.format("用户登录成功，IP: %s", clientIp));
            } catch (Exception e) {
                log.warn("记录登录成功审计日志失败", e);
            }
        }
    }

    /**
     * 记录登录失败事件
     */
    private void recordLoginFailureEvent(String username, String clientIp,
                                         String userAgent, String reason) {
        log.warn("登录失败 - username: {}, ip: {}, userAgent: {}, reason: {}, time: {}",
                username, clientIp, userAgent, reason, LocalDateTime.now());

        if (auditService != null) {
            try {
                auditService.recordUserAction("USER_LOGIN_FAILURE", null,
                        String.format("用户 %s 登录失败，IP: %s，原因: %s", username, clientIp, reason));
            } catch (Exception e) {
                log.warn("记录登录失败审计日志失败", e);
            }
        }
    }

    /**
     * 记录登出事件
     */
    private void recordLogoutEvent(Long userId, String clientIp) {
        if (auditService != null) {
            try {
                auditService.recordUserAction("USER_LOGOUT", userId,
                        String.format("用户登出，IP: %s", clientIp));
            } catch (Exception e) {
                log.warn("记录登出审计日志失败", e);
            }
        }
    }

    /**
     * 记录全设备登出事件
     */
    private void recordLogoutAllEvent(Long userId) {
        if (auditService != null) {
            try {
                auditService.recordUserAction("USER_LOGOUT_ALL", userId, "用户登出所有设备");
            } catch (Exception e) {
                log.warn("记录全设备登出审计日志失败", e);
            }
        }
    }
}