package com.cloud.baseai.adapter.web.auth;

import com.cloud.baseai.application.auth.command.LoginCommand;
import com.cloud.baseai.application.auth.command.RefreshTokenCommand;
import com.cloud.baseai.application.auth.command.TokenValidationCommand;
import com.cloud.baseai.application.auth.dto.LoginResponseDTO;
import com.cloud.baseai.application.auth.dto.TokenResponseDTO;
import com.cloud.baseai.application.auth.dto.TokenValidationResponseDTO;
import com.cloud.baseai.application.auth.dto.UserInfoDTO;
import com.cloud.baseai.application.auth.service.AuthAppService;
import com.cloud.baseai.infrastructure.exception.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <h1>认证控制器</h1>
 *
 * <p>AuthController负责处理用户的登录、登出、身份验证等各种服务。
 * 作为适配器层的一部分，它将HTTP请求转换为应用服务的调用。</p>
 *
 * <p><b>现代认证系统的复杂性：</b></p>
 * <p>在移动互联网时代，用户认证不再是简单的用户名密码验证。我们需要考虑：</p>
 * <ul>
 * <li>多种登录方式：用户名、邮箱、手机号、第三方登录</li>
 * <li>安全性要求：防暴力破解、防撞库、防钓鱼</li>
 * <li>用户体验：记住登录状态、无感刷新令牌</li>
 * <li>多端同步：Web、移动App、小程序等多平台统一认证</li>
 * </ul>
 *
 * <p><b>JWT令牌管理策略：</b></p>
 * <p>我们采用了双令牌（Access Token + Refresh Token）策略：</p>
 * <ul>
 * <li>Access Token：短期有效（如1小时），用于API访问</li>
 * <li>Refresh Token：长期有效（如7天），用于刷新Access Token</li>
 * </ul>
 * <p>这种设计在安全性和用户体验之间找到了很好的平衡点。</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@Validated
@Tag(name = "用户认证", description = "Authentication APIs - 提供用户登录、注册、令牌管理等认证服务")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /**
     * 认证应用服务 - 负责编排认证相关的业务流程
     */
    private final AuthAppService authAppService;

    /**
     * 构造函数，注入认证应用服务
     */
    public AuthController(AuthAppService authAppService) {
        this.authAppService = authAppService;
    }

    /**
     * 用户登录
     *
     * <p>这是认证系统的核心接口。用户提供用户名和密码，系统验证身份后
     * 返回JWT令牌。整个过程委托给应用服务层进行业务逻辑处理。</p>
     *
     * <p><b>登录安全机制：</b></p>
     * <ul>
     * <li>密码强度验证：确保密码符合安全要求</li>
     * <li>登录尝试限制：防止暴力破解攻击</li>
     * <li>异地登录检测：发现异常登录行为</li>
     * <li>设备指纹记录：跟踪登录设备信息</li>
     * </ul>
     */
    @PostMapping("/login")
    @Operation(
            summary = "用户登录",
            description = "使用用户名/邮箱和密码进行登录认证，成功后返回JWT访问令牌和刷新令牌。支持设备指纹验证和记住我功能。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误"),
            @ApiResponse(responseCode = "423", description = "账户被锁定"),
            @ApiResponse(responseCode = "429", description = "登录尝试过于频繁")
    })
    public ResponseEntity<ApiResult<LoginResponseDTO>> login(
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
            ) LoginCommand loginCommand,
            HttpServletRequest request) {

        log.info("用户登录请求: username={}", loginCommand.username());

        try {
            LoginResponseDTO response = authAppService.login(loginCommand, request);
            return ResponseEntity.ok(ApiResult.success(response, "登录成功"));

        } catch (Exception e) {
            log.error("用户登录处理异常: username={}", loginCommand.username(), e);
            throw e; // 由全局异常处理器处理
        }
    }

    /**
     * 刷新访问令牌
     *
     * <p>当Access Token即将过期时，前端可以使用Refresh Token来获取新的
     * Access Token，而无需用户重新登录。</p>
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "刷新访问令牌",
            description = "使用刷新令牌获取新的访问令牌，延长用户登录状态而无需重新输入密码。支持设备指纹验证。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "令牌刷新成功"),
            @ApiResponse(responseCode = "401", description = "刷新令牌无效或已过期"),
            @ApiResponse(responseCode = "403", description = "刷新令牌已被撤销")
    })
    public ResponseEntity<ApiResult<TokenResponseDTO>> refreshToken(
            @Valid @RequestBody RefreshTokenCommand command,
            HttpServletRequest request) {

        log.debug("收到令牌刷新请求");

        try {
            TokenResponseDTO response = authAppService.refreshToken(command, request);
            return ResponseEntity.ok(ApiResult.success(response, "令牌刷新成功"));

        } catch (Exception e) {
            log.error("令牌刷新处理异常", e);
            throw e; // 由全局异常处理器处理
        }
    }

    /**
     * 用户登出
     *
     * <p>登出操作会使当前的令牌失效。委托给应用服务层处理具体的登出逻辑。</p>
     */
    @PostMapping("/logout")
    @Operation(
            summary = "用户登出",
            description = "注销当前用户会话，使访问令牌失效。建议同时清除客户端存储的所有令牌。"
    )
    public ResponseEntity<ApiResult<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.debug("收到用户登出请求");

        try {
            authAppService.logout(authHeader, request);
            return ResponseEntity.ok(ApiResult.success(null, "登出成功"));

        } catch (Exception e) {
            log.error("登出处理异常", e);
            // 即使出现异常也返回成功，避免暴露系统内部信息
            return ResponseEntity.ok(ApiResult.success(null, "登出成功"));
        }
    }

    /**
     * 登出所有设备
     *
     * <p>撤销用户在所有设备上的登录状态，用于安全场景。</p>
     */
    @PostMapping("/logout-all")
    @Operation(
            summary = "登出所有设备",
            description = "撤销当前用户在所有设备上的登录状态，使所有令牌失效。"
    )
    public ResponseEntity<ApiResult<Void>> logoutAll() {

        log.debug("收到用户全设备登出请求");

        try {
            authAppService.logoutAll();
            return ResponseEntity.ok(ApiResult.success(null, "所有设备登出成功"));

        } catch (Exception e) {
            log.error("登出所有设备异常", e);
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
    public ResponseEntity<ApiResult<UserInfoDTO>> getCurrentUser() {

        log.debug("收到获取当前用户信息请求");

        try {
            UserInfoDTO userInfo = authAppService.getCurrentUser();
            return ResponseEntity.ok(ApiResult.success(userInfo));

        } catch (Exception e) {
            log.error("获取当前用户信息失败", e);
            throw e; // 由全局异常处理器处理
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
            description = "验证JWT令牌的有效性，返回令牌状态和用户信息。支持设备指纹验证。"
    )
    public ResponseEntity<ApiResult<TokenValidationResponseDTO>> validateToken(
            @Valid @RequestBody TokenValidationCommand command,
            HttpServletRequest request) {

        log.debug("收到令牌验证请求");

        try {
            TokenValidationResponseDTO response = authAppService.validateToken(command, request);
            return ResponseEntity.ok(ApiResult.success(response));

        } catch (Exception e) {
            log.error("令牌验证失败", e);
            throw e; // 由全局异常处理器处理
        }
    }
}