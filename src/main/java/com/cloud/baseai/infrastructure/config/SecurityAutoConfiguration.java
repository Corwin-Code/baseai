package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.config.base.BaseAutoConfiguration;
import com.cloud.baseai.infrastructure.config.properties.SecurityProperties;
import com.cloud.baseai.infrastructure.security.jwt.JwtAuthenticationEntryPoint;
import com.cloud.baseai.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.cloud.baseai.infrastructure.security.permission.CustomPermissionEvaluator;
import com.cloud.baseai.infrastructure.security.service.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h1>Spring Security 核心配置类</h1>
 *
 * <p>这个配置类协调和管理着应用程序的所有安全机制。采用了现代化的安全架构设计，
 * 支持JWT无状态认证、细粒度权限控制、多租户数据隔离等企业级安全特性。</p>
 *
 * <p><b>安全架构设计：</b></p>
 * <ul>
 * <li><b>分层防护：</b>网关层→认证层→授权层→业务层的多重安全检查</li>
 * <li><b>无状态认证：</b>基于JWT的无状态认证，支持水平扩展</li>
 * <li><b>细粒度控制：</b>支持基于资源、租户、角色的精细权限控制</li>
 * <li><b>安全增强：</b>密码策略、令牌黑名单、设备指纹等安全特性</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        prePostEnabled = true,        // 启用 @PreAuthorize 和 @PostAuthorize
        securedEnabled = true,        // 启用 @Secured
        jsr250Enabled = true          // 启用 @RolesAllowed
)
@ConditionalOnClass(SecurityFilterChain.class)
public class SecurityAutoConfiguration extends BaseAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityAutoConfiguration.class);

    private final SecurityProperties securityProps;
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomPermissionEvaluator customPermissionEvaluator;

    public SecurityAutoConfiguration(SecurityProperties securityProps,
                                     CustomUserDetailsService userDetailsService,
                                     JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                                     JwtAuthenticationFilter jwtAuthenticationFilter,
                                     CustomPermissionEvaluator customPermissionEvaluator) {
        this.securityProps = securityProps;
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customPermissionEvaluator = customPermissionEvaluator;

        // 统一初始化
        initializeConfiguration();
    }

    @Override
    protected String getConfigurationName() {
        return "Spring Security 安全框架";
    }

    @Override
    protected String getModuleName() {
        return "SECURITY";
    }

    @Override
    protected void validateConfiguration() {
        logInfo("开始验证Spring Security配置...");

        // JWT配置验证
        validateJwtConfiguration();

        // CORS配置验证
        validateCorsConfiguration();

        // 密码策略验证
        validatePasswordConfiguration();

        // 认证配置验证
        validateAuthConfiguration();

        // 依赖组件验证
        validateSecurityComponents();

        logSuccess("Spring Security配置验证通过");
    }

    /**
     * 验证JWT配置
     */
    private void validateJwtConfiguration() {
        logInfo("验证JWT配置...");

        SecurityProperties.JwtProperties jwt = securityProps.getJwt();

        validateNotNull(jwt, "JWT配置");
        validateNotBlank(jwt.getIssuer(), "JWT发行者");

        // 验证加密方式配置
        if (jwt.isUseRsa()) {
            validateNotBlank(jwt.getRsaPrivateKey(), "RSA私钥");
            validateNotBlank(jwt.getRsaPublicKey(), "RSA公钥");
            logInfo("使用RSA加密方式");
        } else {
            validateNotBlank(jwt.getSecret(), "HMAC密钥");
            if (jwt.getSecret().length() < 32) {
                logWarning("HMAC密钥长度过短 (%d字符)，建议至少32字符", jwt.getSecret().length());
            }
            logInfo("使用HMAC加密方式");
        }

        // 验证令牌过期时间
        validatePositive(jwt.getAccessTokenExpiration(), "访问令牌过期时间");
        validatePositive(jwt.getRefreshTokenExpiration(), "刷新令牌过期时间");

        if (jwt.getRefreshTokenExpiration() <= jwt.getAccessTokenExpiration()) {
            logWarning("刷新令牌过期时间应大于访问令牌过期时间");
        }

        // 验证自动刷新阈值
        validatePositive(jwt.getAutoRefreshThreshold(), "自动刷新阈值");
        if (jwt.getAutoRefreshThreshold() > jwt.getAccessTokenExpiration() / 1000 / 2) {
            logWarning("自动刷新阈值过大，可能导致频繁刷新");
        }

        // 安全特性状态
        logInfo("JWT安全特性 - 黑名单: %s, 设备指纹: %s",
                jwt.isEnableBlacklist() ? "启用" : "禁用",
                jwt.isEnableDeviceFingerprint() ? "启用" : "禁用");
    }

    /**
     * 验证CORS配置
     */
    private void validateCorsConfiguration() {
        SecurityProperties.CorsProperties cors = securityProps.getCors();

        if (!cors.isEnabled()) {
            logInfo("CORS已禁用");
            return;
        }

        logInfo("验证CORS配置...");

        validateNotBlank(cors.getAllowedOrigins(), "允许的源地址");
        validateNotBlank(cors.getAllowedMethods(), "允许的HTTP方法");
        validateNotBlank(cors.getAllowedHeaders(), "允许的请求头");
        validateNonNegative(cors.getMaxAge(), "预检请求缓存时间");

        // 验证源地址安全性
        String[] origins = cors.getAllowedOrigins().split(",");
        for (String origin : origins) {
            origin = origin.trim();
            if ("*".equals(origin)) {
                logWarning("CORS配置允许所有源地址(*)，存在安全风险");
            } else if (origin.startsWith("http://") && !origin.contains("localhost")) {
                logWarning("CORS配置包含HTTP源地址，建议使用HTTPS: %s", origin);
            }
        }

        logInfo("CORS配置验证完成 - 源地址数: %d, 凭据支持: %s",
                origins.length, cors.isAllowCredentials() ? "启用" : "禁用");
    }

    /**
     * 验证密码策略配置
     */
    private void validatePasswordConfiguration() {
        logInfo("验证密码策略配置...");

        SecurityProperties.PasswordProperties password = securityProps.getPassword();

        validateRange(password.getMinLength(), 6, 128, "密码最小长度");
        validateRange(password.getMaxLength(), 8, 512, "密码最大长度");

        if (password.getMinLength() > password.getMaxLength()) {
            throw new IllegalArgumentException("密码最小长度不能大于最大长度");
        }

        validateNotBlank(password.getSpecialChars(), "特殊字符集合");
        validateNonNegative(password.getHistoryCount(), "密码历史记录数量");
        validateRange(password.getMaxAgeDays(), 1, 365, "密码最大使用天数");
        validateRange(password.getLockoutThreshold(), 1, 20, "登录失败锁定阈值");
        validateRange(password.getLockoutDurationMinutes(), 1, 1440, "账户锁定时间");

        // 密码复杂度要求检查
        int complexityScore = 0;
        if (password.isRequireUppercase()) complexityScore++;
        if (password.isRequireLowercase()) complexityScore++;
        if (password.isRequireNumbers()) complexityScore++;
        if (password.isRequireSpecialChars()) complexityScore++;

        if (complexityScore < 3) {
            logWarning("密码复杂度要求较低，建议至少启用3种字符类型要求");
        }

        logInfo("密码策略 - 长度: %d-%d, 复杂度要求: %d/4, 锁定阈值: %d次",
                password.getMinLength(), password.getMaxLength(), complexityScore, password.getLockoutThreshold());
    }

    /**
     * 验证认证配置
     */
    private void validateAuthConfiguration() {
        logInfo("验证认证配置...");

        SecurityProperties.AuthProperties auth = securityProps.getAuth();

        validateRange(auth.getMaxDevicesPerUser(), 1, 50, "最大同时登录设备数");
        validateRange(auth.getSessionTimeoutMinutes(), 5, 1440, "会话超时时间");
        validateRange(auth.getCaptchaThreshold(), 1, 10, "验证码触发阈值");
        validateRange(auth.getTwoFactorCodeExpiration(), 30, 600, "二次验证代码有效期");

        if (auth.isEnableRememberMe()) {
            validateRange(auth.getRememberMeDays(), 1, 365, "记住我有效期");
        }

        if (auth.isEnableLoginHistory()) {
            validateRange(auth.getLoginHistoryRetentionDays(), 7, 180, "登录历史有效期");
        }

        // IP验证配置检查
        if (auth.isEnableIpValidation() && !auth.getAllowedIps().isEmpty()) {
            logInfo("IP地址验证已启用，允许的IP数量: %d",
                    auth.getAllowedIps().split(",").length);
        }

        logInfo("认证配置 - 多设备: %s, 记住我: %s, 验证码: %s, 2FA: %s",
                auth.isEnableMultiDevice() ? "启用" : "禁用",
                auth.isEnableRememberMe() ? "启用" : "禁用",
                auth.isEnableCaptcha() ? "启用" : "禁用",
                auth.isEnableTwoFactorAuth() ? "启用" : "禁用",
                auth.isEnableAnomalyDetection() ? "启用" : "禁用",
                auth.isEnableLoginHistory() ? "启用" : "禁用");
    }

    /**
     * 验证安全组件
     */
    private void validateSecurityComponents() {
        logInfo("验证安全组件依赖...");

        validateNotNull(userDetailsService, "用户详情服务");
        validateNotNull(jwtAuthenticationEntryPoint, "JWT认证入口点");
        validateNotNull(jwtAuthenticationFilter, "JWT认证过滤器");
        validateNotNull(customPermissionEvaluator, "自定义权限评估器");

        logInfo("所有安全组件验证通过");
    }

    @Override
    protected Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();

        // JWT配置摘要
        SecurityProperties.JwtProperties jwt = securityProps.getJwt();
        summary.put("JWT加密方式", jwt.isUseRsa() ? "RSA" : "HMAC");
        summary.put("访问令牌有效期(小时)", jwt.getAccessTokenExpiration() / 1000 / 3600);
        summary.put("刷新令牌有效期(天)", jwt.getRefreshTokenExpiration() / 1000 / 3600 / 24);
        summary.put("令牌黑名单", jwt.isEnableBlacklist() ? "启用" : "禁用");
        summary.put("设备指纹验证", jwt.isEnableDeviceFingerprint() ? "启用" : "禁用");

        // CORS配置摘要
        SecurityProperties.CorsProperties cors = securityProps.getCors();
        summary.put("CORS跨域", cors.isEnabled() ? "启用" : "禁用");
        if (cors.isEnabled()) {
            summary.put("CORS源地址数量", cors.getAllowedOrigins().split(",").length);
            summary.put("CORS凭据支持", cors.isAllowCredentials() ? "启用" : "禁用");
        }

        // 密码策略摘要
        SecurityProperties.PasswordProperties password = securityProps.getPassword();
        summary.put("密码长度范围", password.getMinLength() + "-" + password.getMaxLength());
        summary.put("密码复杂度检查", password.isEnableComplexityCheck() ? "启用" : "禁用");
        summary.put("弱密码检测", password.isEnableWeakPasswordCheck() ? "启用" : "禁用");
        summary.put("登录失败锁定阈值", password.getLockoutThreshold() + "次");

        // 认证配置摘要
        SecurityProperties.AuthProperties auth = securityProps.getAuth();
        summary.put("多设备登录", auth.isEnableMultiDevice() ? "启用" : "禁用");
        summary.put("最大设备数", auth.getMaxDevicesPerUser());
        summary.put("会话超时(分钟)", auth.getSessionTimeoutMinutes());
        summary.put("二次验证", auth.isEnableTwoFactorAuth() ? "启用" : "禁用");

        // 增强安全特性
        summary.put("增强安全功能", securityProps.isEnhancedSecurityEnabled() ? "启用" : "禁用");

        return summary;
    }

    /**
     * 配置安全过滤器链
     *
     * <p>这是Spring Security的核心配置方法，定义了完整的安全策略。
     * 采用声明式配置方式，清晰地表达了每个URL的安全要求。</p>
     *
     * <p><b>安全策略说明：</b></p>
     * <ul>
     * <li><b>公开端点：</b>认证、注册、健康检查等无需认证</li>
     * <li><b>管理端点：</b>需要管理员权限的系统管理接口</li>
     * <li><b>业务端点：</b>需要认证，具体权限通过方法注解控制</li>
     * <li><b>静态资源：</b>公开访问，但可以配置CDN缓存策略</li>
     * </ul>
     *
     * @param http Spring Security的HTTP安全配置对象
     * @return 配置完成的安全过滤器链
     * @throws Exception 配置过程中可能出现的异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        logBeanCreation("SecurityFilterChain", "安全过滤器链");

        try {
            SecurityFilterChain chain = http
                    // ========== 基础安全配置 ==========
                    .csrf(AbstractHttpConfigurer::disable)  // 禁用CSRF，因为使用JWT无状态认证
                    .cors(cors -> {
                        // 配置CORS
                        if (securityProps.getCors().isEnabled()) {
                            cors.configurationSource(corsConfigurationSource());
                            logInfo("CORS跨域支持已启用");
                        } else {
                            cors.disable();
                            logInfo("CORS跨域支持已禁用");
                        }
                    })

                    // ========== 会话管理配置 ==========
                    .sessionManagement(session -> {
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);  // 无状态会话
                        // 配置会话并发控制（如果需要）
                        if (!securityProps.getAuth().isEnableMultiDevice()) {
                            session.maximumSessions(1).maxSessionsPreventsLogin(false);
                            logInfo("单设备登录模式已启用");
                        } else {
                            logInfo("多设备登录模式已启用，最大设备数: %d",
                                    securityProps.getAuth().getMaxDevicesPerUser());
                        }
                    })

                    // ========== 异常处理配置 ==========
                    .exceptionHandling(exceptions -> {
                        exceptions.authenticationEntryPoint(jwtAuthenticationEntryPoint);
                        // 添加访问拒绝处理器
                        // exceptions.accessDeniedHandler(customAccessDeniedHandler);
                        logInfo("JWT认证异常处理已配置");
                    })

                    // ========== URL访问权限配置 ==========
                    .authorizeHttpRequests(this::configureAuthorization)

                    // ========== 添加自定义过滤器（JWT） ==========
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                    // ========== 安全头配置 ==========
                    .headers(this::configureSecurityHeaders)

                    .build();

            logBeanSuccess("SecurityFilterChain");
            logSuccess("Spring Security配置完成，安全防护已就绪");

            return chain;

        } catch (Exception e) {
            String errorMsg = String.format("创建SecurityFilterChain失败: %s", e.getMessage());
            log.error("❌ [{}] {}", getModuleName(), errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    /**
     * 配置URL授权规则
     */
    private void configureAuthorization(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {

        logInfo("配置URL访问权限规则...");

        authz
                // 完全公开的端点（无需任何认证）
                .requestMatchers(
                        "/api/v1/auth/**",                      // 认证相关接口
                        "/api/v1/users/register",               // 用户注册
                        "/api/v1/users/activate",               // 用户激活
                        "/api/v1/users/invitations/*/respond",  // 邀请响应
                        "/api/v1/misc/files",                   // 公开文件访问
                        "/api/v1/system/health",                // 系统健康检查
                        "/api/v1/*/health",                     // 各模块健康检查
                        "/error"                                // 错误页面
                ).permitAll()

                // 监控和管理端点（通常在不同端口或内网访问）
                .requestMatchers(
                        "/actuator/health",                     // 健康检查
                        "/actuator/info",                       // 应用信息
                        "/actuator/prometheus"                  // 监控指标
                ).permitAll()

                // 管理端点（需要特殊权限）
                .requestMatchers("/actuator/**")
                .hasRole("ACTUATOR_ADMIN")

                // API文档（可以根据环境配置是否开放）
                .requestMatchers(
                        "/v3/api-docs/**",                      // OpenAPI文档
                        "/swagger-ui/**",                       // Swagger UI
                        "/swagger-ui.html",                     // Swagger首页
                        "/doc.html",                            // 文档首页
                        "/webjars/**"                           // Web资源
                ).permitAll()

                // 静态资源（公开访问）
                .requestMatchers(HttpMethod.GET,
                        "/",
                        "/favicon.ico",
                        "/static/**",
                        "/public/**",
                        "/assets/**"
                ).permitAll()

                // 系统管理接口（需要系统管理员权限）
                .requestMatchers("/api/v1/system/**")
                .hasAnyRole("SYSTEM_ADMIN", "SUPER_ADMIN")

                // 审计接口（需要审计权限）
                .requestMatchers("/api/v1/audit/**")
                .hasAnyRole("ADMIN", "AUDITOR", "SYSTEM_ADMIN", "SUPER_ADMIN")

                // 租户管理接口（需要租户管理权限）
                .requestMatchers(HttpMethod.POST, "/api/v1/users/tenants")
                .hasAnyRole("TENANT_CREATOR", "ADMIN", "SUPER_ADMIN")

                .requestMatchers(HttpMethod.DELETE, "/api/v1/users/tenants/**")
                .hasAnyRole("TENANT_OWNER", "SUPER_ADMIN")

                // 用户管理接口（需要用户管理权限）
                .requestMatchers(HttpMethod.POST, "/api/v1/users/*/roles")
                .hasAnyRole("USER_ADMIN", "SUPER_ADMIN")

                // 知识库管理接口（需要相应权限）
                .requestMatchers(HttpMethod.DELETE, "/api/v1/kb/**")
                .hasAnyRole("KB_ADMIN", "TENANT_ADMIN", "ADMIN")

                // 文件上传接口（需要认证）
                .requestMatchers(HttpMethod.POST, "/api/v1/misc/files")
                .authenticated()

                // MCP工具管理（需要工具管理员权限）
                .requestMatchers(HttpMethod.POST, "/api/v1/mcp/tools")
                .hasRole("TOOL_ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/mcp/tools/**")
                .hasRole("TOOL_ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/mcp/tools/**")
                .hasRole("TOOL_ADMIN")

                // 其他所有API都需要认证
                .requestMatchers("/api/**").authenticated()

                // 其他请求（如前端路由）允许访问
                .anyRequest().permitAll();

        logInfo("URL访问权限规则配置完成");
    }

    /**
     * 配置安全头
     */
    private void configureSecurityHeaders(HeadersConfigurer<HttpSecurity> headers) {

        logInfo("配置安全响应头...");

        headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)   // 防止点击劫持
                .contentTypeOptions(Customizer.withDefaults())              // 防止MIME类型嗅探
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                        .maxAgeInSeconds(31536000)        // HSTS一年有效期
                        .includeSubDomains(true)          // 包含子域名
                );

        logInfo("安全响应头配置完成 - HSTS: 1年, 防点击劫持: 启用");
    }

    /**
     * 密码编码器配置
     *
     * <p>使用Argon2算法，这是目前最安全的密码哈希算法：</p>
     * <ul>
     * <li><b>内存硬化：</b>抵抗专用硬件攻击</li>
     * <li><b>时间成本：</b>可调节的计算复杂度</li>
     * <li><b>并行化抵抗：</b>限制并行计算的优势</li>
     * <li><b>现代标准：</b>2015年密码哈希竞赛获胜者</li>
     * </ul>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        logBeanCreation("PasswordEncoder", "Argon2密码编码器");

        // Argon2参数：saltLength=16, hashLength=32, parallelism=1, memory=4096KB, iterations=3
        PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 4096, 3);

        logInfo("密码编码器配置 - 算法: Argon2, 内存: 4MB, 迭代: 3次");
        logBeanSuccess("PasswordEncoder");

        return encoder;
    }

    /**
     * 配置认证管理器
     *
     * <p>认证管理器是Spring Security的核心组件，负责协调各种认证提供者。
     * 在当前的系统中，主要使用DAO认证提供者进行数据库用户认证。</p>
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        logBeanCreation("AuthenticationManager", "认证管理器");

        AuthenticationManager manager = config.getAuthenticationManager();
        logBeanSuccess("AuthenticationManager");

        return manager;
    }

    /**
     * 配置认证提供者
     *
     * <p>DAO认证提供者负责从数据库加载用户信息并验证密码。
     * 配置了密码编码器和用户详情服务。</p>
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        logBeanCreation("AuthenticationProvider", "DAO认证提供者");

        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        // 安全配置：隐藏用户不存在的异常，防止用户枚举攻击
        authProvider.setHideUserNotFoundExceptions(true);

        logInfo("DAO认证提供者配置 - 用户枚举攻击防护: 启用");
        logBeanSuccess("AuthenticationProvider");

        return authProvider;
    }

    /**
     * 配置方法安全表达式处理器
     *
     * <p>这个处理器让我们能够在方法级别使用自定义的权限表达式，
     * 实现细粒度的权限控制。</p>
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        logBeanCreation("MethodSecurityExpressionHandler", "方法安全表达式处理器");

        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(this.customPermissionEvaluator);

        logInfo("方法级安全控制已启用 - @PreAuthorize, @PostAuthorize, @Secured");
        logBeanSuccess("MethodSecurityExpressionHandler");

        return expressionHandler;
    }

    /**
     * 配置CORS（跨域资源共享）
     *
     * <p>现代Web应用通常采用前后端分离架构，CORS配置确保前端能够
     * 安全地调用后端API，同时防止恶意站点的跨域攻击。</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        logBeanCreation("CorsConfigurationSource", "CORS跨域配置源");

        CorsConfiguration configuration = new CorsConfiguration();
        SecurityProperties.CorsProperties corsProps = securityProps.getCors();

        // 解析配置
        // 解析允许的源地址
        List<String> allowedOrigins = Arrays.asList(corsProps.getAllowedOrigins().split(","));
        // 解析允许的HTTP方法
        List<String> allowedMethods = Arrays.asList(corsProps.getAllowedMethods().split(","));
        // 解析允许的请求头
        List<String> allowedHeaders = Arrays.asList(corsProps.getAllowedHeaders().split(","));

        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(allowedMethods);
        configuration.setAllowedHeaders(allowedHeaders);

        // 解析暴露的响应头
        if (corsProps.getExposedHeaders() != null && !corsProps.getExposedHeaders().isEmpty()) {
            List<String> exposedHeaders = Arrays.asList(corsProps.getExposedHeaders().split(","));
            configuration.setExposedHeaders(exposedHeaders);
        }

        // 允许发送认证信息（如Cookies、Authorization头）
        configuration.setAllowCredentials(corsProps.isAllowCredentials());
        // 预检请求的缓存时间
        configuration.setMaxAge(corsProps.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        logInfo("CORS配置 - 源地址: %d个, 方法: %s, 凭据: %s",
                allowedOrigins.size(), allowedMethods, corsProps.isAllowCredentials() ? "允许" : "禁止");

        logBeanSuccess("CorsConfigurationSource");
        return source;
    }
}