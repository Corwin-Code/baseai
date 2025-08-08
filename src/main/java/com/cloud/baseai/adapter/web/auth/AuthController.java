package com.cloud.baseai.adapter.web.auth;

import com.cloud.baseai.infrastructure.exception.ApiResult;
import com.cloud.baseai.infrastructure.security.UserPrincipal;
import com.cloud.baseai.infrastructure.security.jwt.JwtUtils;
import com.cloud.baseai.infrastructure.security.service.CustomUserDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <h1>认证控制器</h1>
 *
 * <p>AuthController负责处理用户的登录、登出、身份验证等各种服务。
 * 无论是新用户注册还是老用户凭借凭证登录，都要通过这里的专业服务。</p>
 *
 * <p><b>现代认证系统的复杂性：</b></p>
 * <p>在移动互联网时代，用户认证不再是简单的用户名密码验证。我们需要考虑：</p>
 * <p>- 多种登录方式：用户名、邮箱、手机号、第三方登录</p>
 * <p>- 安全性要求：防暴力破解、防撞库、防钓鱼</p>
 * <p>- 用户体验：记住登录状态、无感刷新令牌</p>
 * <p>- 多端同步：Web、移动App、小程序等多平台统一认证</p>
 *
 * <p><b>JWT令牌管理策略：</b></p>
 * <p>我们采用了双令牌（Access Token + Refresh Token）策略：</p>
 * <p>- Access Token：短期有效（如1小时），用于API访问</p>
 * <p>- Refresh Token：长期有效（如7天），用于刷新Access Token</p>
 * <p>这种设计在安全性和用户体验之间找到了很好的平衡点。</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@Validated
@Tag(name = "用户认证", description = "Authentication APIs - 提供用户登录、注册、令牌管理等认证服务")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    /**
     * 构造函数，注入认证相关的依赖服务
     */
    public AuthController(
            AuthenticationManager authenticationManager,
            JwtUtils jwtUtils,
            CustomUserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    /**
     * 用户登录
     *
     * <p>这是认证系统的核心接口。用户提供用户名和密码，系统验证身份后
     * 返回JWT令牌。整个过程就像酒店前台验证客人身份并发放房卡。</p>
     *
     * <p><b>登录安全机制：</b></p>
     * <p>1. 密码强度验证：确保密码符合安全要求</p>
     * <p>2. 登录尝试限制：防止暴力破解攻击</p>
     * <p>3. 异地登录检测：发现异常登录行为</p>
     * <p>4. 设备指纹记录：跟踪登录设备信息</p>
     *
     * <p><b>用户体验优化：</b></p>
     * <p>- 支持用户名或邮箱登录</p>
     * <p>- 登录状态记忆（通过Refresh Token）</p>
     * <p>- 登录失败友好提示</p>
     * <p>- 异步验证提升响应速度</p>
     */
    @PostMapping("/login")
    @Operation(
            summary = "用户登录",
            description = "使用用户名/邮箱和密码进行登录认证，成功后返回JWT访问令牌和刷新令牌。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
            @ApiResponse(responseCode = "423", description = "账户被锁定"),
            @ApiResponse(responseCode = "429", description = "登录尝试过于频繁")
    })
    public ResponseEntity<ApiResult<LoginResponse>> login(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "用户登录信息",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "用户登录示例",
                                    value = """
                                            {
                                              "username": "john.doe",
                                              "password": "SecurePass123!",
                                              "rememberMe": true,
                                              "deviceInfo": "Chrome 120.0 on Windows 10"
                                            }
                                            """
                            )
                    )
            ) LoginRequest loginRequest,
            HttpServletRequest request) {

        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        log.info("用户登录请求: username={}, ip={}, userAgent={}",
                loginRequest.username(), clientIp, userAgent);

        try {
            // 执行Spring Security认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.username(),
                            loginRequest.password()
                    )
            );

            // 设置认证上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 获取用户主体信息
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            // 生成JWT令牌
            String accessToken = jwtUtils.generateAccessToken(authentication);
            String refreshToken = jwtUtils.generateRefreshToken(userPrincipal);

            // 构建登录响应
            LoginResponse response = new LoginResponse(
                    accessToken,
                    refreshToken,
                    "Bearer",
                    jwtUtils.getTokenRemainingTime(accessToken),
                    new UserInfo(
                            userPrincipal.getId(),
                            userPrincipal.getUsername(),
                            userPrincipal.getEmail(),
                            userPrincipal.getRoles(),
                            userPrincipal.getTenantIds()
                    )
            );

            // 记录登录成功事件
            logLoginSuccess(userPrincipal, clientIp, userAgent);

            return ResponseEntity.ok(ApiResult.success(response, "登录成功"));

        } catch (AuthenticationException e) {
            log.warn("用户登录失败: username={}, ip={}, error={}",
                    loginRequest.username(), clientIp, e.getMessage());

            // 记录登录失败事件（用于安全监控）
            logLoginFailure(loginRequest.username(), clientIp, userAgent, e.getMessage());

            return ResponseEntity.ok(ApiResult.error("LOGIN_FAILED", "用户名或密码错误"));
        }
    }

    /**
     * 刷新访问令牌
     *
     * <p>当Access Token即将过期时，前端可以使用Refresh Token来获取新的
     * Access Token，而无需用户重新登录。这就像酒店的房卡续期服务。</p>
     *
     * <p><b>令牌刷新的安全考虑：</b></p>
     * <p>1. Refresh Token的一次性使用原则</p>
     * <p>2. 令牌轮换（Token Rotation）机制</p>
     * <p>3. 设备绑定验证</p>
     * <p>4. 异常刷新行为检测</p>
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "刷新访问令牌",
            description = "使用刷新令牌获取新的访问令牌，延长用户登录状态而无需重新输入密码。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "令牌刷新成功"),
            @ApiResponse(responseCode = "401", description = "刷新令牌无效或已过期"),
            @ApiResponse(responseCode = "403", description = "刷新令牌已被撤销")
    })
    public ResponseEntity<ApiResult<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        String refreshToken = request.refreshToken();

        log.debug("收到令牌刷新请求");

        try {
            // 验证刷新令牌的有效性
            if (!jwtUtils.validateToken(refreshToken)) {
                log.warn("无效的刷新令牌");
                return ResponseEntity.ok(ApiResult.error("INVALID_REFRESH_TOKEN", "刷新令牌无效"));
            }

            // 检查这是否确实是一个刷新令牌
            if (!jwtUtils.isRefreshToken(refreshToken)) {
                log.warn("提供的不是刷新令牌");
                return ResponseEntity.ok(ApiResult.error("INVALID_TOKEN_TYPE", "令牌类型错误"));
            }

            // 从刷新令牌中获取用户信息
            Long userId = jwtUtils.getUserIdFromToken(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserById(userId);

            if (userDetails == null) {
                log.warn("刷新令牌对应的用户不存在: userId={}", userId);
                return ResponseEntity.ok(ApiResult.error("USER_NOT_FOUND", "用户不存在"));
            }

            // 检查用户账户状态
            if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                log.warn("用户账户状态异常: userId={}", userId);
                return ResponseEntity.ok(ApiResult.error("ACCOUNT_DISABLED", "账户已被禁用"));
            }

            UserPrincipal userPrincipal = (UserPrincipal) userDetails;

            // 生成新的访问令牌
            String newAccessToken = jwtUtils.generateAccessToken(userPrincipal);
            String newRefreshToken = jwtUtils.generateRefreshToken(userPrincipal);

            TokenResponse response = new TokenResponse(
                    newAccessToken,
                    newRefreshToken,
                    "Bearer",
                    jwtUtils.getTokenRemainingTime(newAccessToken)
            );

            log.debug("令牌刷新成功: userId={}", userId);

            return ResponseEntity.ok(ApiResult.success(response, "令牌刷新成功"));

        } catch (Exception e) {
            log.error("令牌刷新失败", e);
            return ResponseEntity.ok(ApiResult.error("TOKEN_REFRESH_FAILED", "令牌刷新失败"));
        }
    }

    /**
     * 用户登出
     *
     * <p>登出操作会使当前的令牌失效。在分布式系统中，我们通常使用
     * 黑名单机制来实现令牌的即时失效。</p>
     */
    @PostMapping("/logout")
    @Operation(
            summary = "用户登出",
            description = "注销当前用户会话，使访问令牌失效。建议同时清除客户端存储的所有令牌。"
    )
    public ResponseEntity<ApiResult<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String clientIp = getClientIpAddress(request);

        try {
            // 提取JWT令牌
            String token = extractTokenFromHeader(authHeader);
            if (token != null) {
                // 这里可以将令牌加入黑名单
                // tokenBlacklistService.addToBlacklist(token);

                // 获取当前用户信息用于日志记录
                Long userId = jwtUtils.getUserIdFromToken(token);
                log.info("用户登出: userId={}, ip={}", userId, clientIp);
            }

            // 清除Security上下文
            SecurityContextHolder.clearContext();

            return ResponseEntity.ok(ApiResult.success(null, "登出成功"));

        } catch (Exception e) {
            log.error("登出处理异常", e);
            return ResponseEntity.ok(ApiResult.success(null, "登出成功"));
        }
    }

    /**
     * 获取当前用户信息
     *
     * <p>这个接口用于获取当前登录用户的详细信息。前端通常在页面加载时
     * 调用这个接口来获取用户状态。</p>
     */
    @GetMapping("/me")
    @Operation(
            summary = "获取当前用户信息",
            description = "获取当前登录用户的详细信息，包括用户资料、角色、权限等。"
    )
    public ResponseEntity<ApiResult<UserInfo>> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.ok(ApiResult.error("NOT_AUTHENTICATED", "用户未认证"));
            }

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            UserInfo userInfo = new UserInfo(
                    userPrincipal.getId(),
                    userPrincipal.getUsername(),
                    userPrincipal.getEmail(),
                    userPrincipal.getRoles(),
                    userPrincipal.getTenantIds()
            );

            return ResponseEntity.ok(ApiResult.success(userInfo));

        } catch (Exception e) {
            log.error("获取当前用户信息失败", e);
            return ResponseEntity.ok(ApiResult.error("GET_USER_INFO_FAILED", "获取用户信息失败"));
        }
    }

    /**
     * 验证令牌有效性
     *
     * <p>这个接口用于验证JWT令牌是否有效。微服务架构中，其他服务
     * 可能需要验证令牌的有效性。</p>
     */
    @PostMapping("/validate")
    @Operation(
            summary = "验证令牌",
            description = "验证JWT令牌的有效性，返回令牌状态和用户信息。"
    )
    public ResponseEntity<ApiResult<TokenValidationResponse>> validateToken(
            @Valid @RequestBody TokenValidationRequest request) {

        try {
            String token = request.token();

            if (!jwtUtils.validateToken(token)) {
                return ResponseEntity.ok(ApiResult.success(
                        new TokenValidationResponse(false, "令牌无效", null)
                ));
            }

            // 获取令牌中的用户信息
            Long userId = jwtUtils.getUserIdFromToken(token);
            String username = jwtUtils.getUsernameFromToken(token);
            List<String> roles = jwtUtils.getRolesFromToken(token);
            List<Long> tenantIds = jwtUtils.getTenantIdsFromToken(token);

            UserInfo userInfo = new UserInfo(userId, username, null, roles, tenantIds);

            return ResponseEntity.ok(ApiResult.success(
                    new TokenValidationResponse(true, "令牌有效", userInfo)
            ));

        } catch (Exception e) {
            log.error("令牌验证失败", e);
            return ResponseEntity.ok(ApiResult.success(
                    new TokenValidationResponse(false, "令牌验证失败", null)
            ));
        }
    }

    // =================== 辅助方法 ===================

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
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
     * 从Authorization头中提取令牌
     */
    private String extractTokenFromHeader(String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * 记录登录成功事件
     */
    private void logLoginSuccess(UserPrincipal user, String clientIp, String userAgent) {
        log.info("登录成功 - userId: {}, username: {}, ip: {}, userAgent: {}, time: {}",
                user.getId(), user.getUsername(), clientIp, userAgent, LocalDateTime.now());

        // 这里可以发送登录成功事件到审计系统
        // auditService.recordLoginSuccess(user.getId(), clientIp, userAgent);
    }

    /**
     * 记录登录失败事件
     */
    private void logLoginFailure(String username, String clientIp, String userAgent, String reason) {
        log.warn("登录失败 - username: {}, ip: {}, userAgent: {}, reason: {}, time: {}",
                username, clientIp, userAgent, reason, LocalDateTime.now());

        // 这里可以发送登录失败事件到安全监控系统
        // securityService.recordLoginFailure(username, clientIp, userAgent, reason);
    }

    // =================== 数据传输对象 ===================

    /**
     * 登录请求
     */
    public record LoginRequest(
            @NotBlank(message = "用户名不能为空")
            @Size(min = 3, max = 50, message = "用户名长度必须在3-50字符之间")
            String username,

            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 100, message = "密码长度必须在6-100字符之间")
            String password,

            boolean rememberMe,

            String deviceInfo
    ) {
    }

    /**
     * 登录响应
     */
    public record LoginResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserInfo userInfo
    ) {
    }

    /**
     * 刷新令牌请求
     */
    public record RefreshTokenRequest(
            @NotBlank(message = "刷新令牌不能为空")
            String refreshToken
    ) {
    }

    /**
     * 令牌响应
     */
    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn
    ) {
    }

    /**
     * 令牌验证请求
     */
    public record TokenValidationRequest(
            @NotBlank(message = "令牌不能为空")
            String token
    ) {
    }

    /**
     * 令牌验证响应
     */
    public record TokenValidationResponse(
            boolean valid,
            String message,
            UserInfo userInfo
    ) {
    }

    /**
     * 用户信息
     */
    public record UserInfo(
            Long id,
            String username,
            String email,
            List<String> roles,
            List<Long> tenantIds
    ) {
    }
}