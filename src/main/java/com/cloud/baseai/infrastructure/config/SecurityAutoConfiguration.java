package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.config.properties.SecurityProperties;
import com.cloud.baseai.infrastructure.security.jwt.JwtAuthenticationEntryPoint;
import com.cloud.baseai.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.cloud.baseai.infrastructure.security.permission.CustomPermissionEvaluator;
import com.cloud.baseai.infrastructure.security.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;

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
 *
 * <p><b>配置特点：</b></p>
 * <ul>
 * <li>配置驱动：所有安全参数均可通过配置文件调整</li>
 * <li>环境适配：开发、测试、生产环境的差异化配置</li>
 * <li>监控友好：提供详细的安全事件日志和指标</li>
 * <li>扩展性强：易于集成新的认证方式和安全特性</li>
 * </ul>
 *
 * <p><b>JWT无状态认证的优势：</b></p>
 * <p>传统的Session认证在分布式环境中存在状态同步问题，而JWT令牌是自包含的，
 * 包含了用户的身份信息和权限数据，非常适合微服务架构和云原生部署。</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        prePostEnabled = true,        // 启用 @PreAuthorize 和 @PostAuthorize
        securedEnabled = true,        // 启用 @Secured
        jsr250Enabled = true          // 启用 @RolesAllowed
)
@RequiredArgsConstructor
public class SecurityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityAutoConfiguration.class);

    private final SecurityProperties securityProps;
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomPermissionEvaluator customPermissionEvaluator;

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
        log.info("正在配置Spring Security过滤器链...");

        // 验证安全配置的有效性
        validateSecurityConfiguration();

        return http
                // ========== 基础安全配置 ==========
                .csrf(AbstractHttpConfigurer::disable)  // 禁用CSRF，因为使用JWT无状态认证
                .cors(cors -> {
                    // 配置CORS
                    if (securityProps.getCors().isEnabled()) {
                        cors.configurationSource(corsConfigurationSource());
                    } else {
                        cors.disable();
                    }
                })

                // ========== 会话管理配置 ==========
                .sessionManagement(session -> {
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);  // 无状态会话
                    // 配置会话并发控制（如果需要）
                    if (!securityProps.getAuth().isEnableMultiDevice()) {
                        session.maximumSessions(1)
                                .maxSessionsPreventsLogin(false);
                    }
                })

                // ========== 异常处理配置 ==========
                .exceptionHandling(exceptions -> {
                    exceptions.authenticationEntryPoint(jwtAuthenticationEntryPoint);
                    // 添加访问拒绝处理器
                    // exceptions.accessDeniedHandler(customAccessDeniedHandler);
                })

                // ========== URL访问权限配置 ==========
                .authorizeHttpRequests(authz -> authz
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
                        .anyRequest().permitAll()
                )

                // ========== 添加自定义过滤器（JWT） ==========
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // ========== 安全头配置 ==========
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)   // 防止点击劫持
                        .contentTypeOptions(Customizer.withDefaults())              // 防止MIME类型嗅探
                        .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                .maxAgeInSeconds(31536000)        // HSTS一年有效期
                                .includeSubDomains(true)          // 包含子域名
                        )
                )

                .build();
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
        // Argon2参数：saltLength=16, hashLength=32, parallelism=1, memory=4096KB, iterations=3
        return new Argon2PasswordEncoder(16, 32, 1, 4096, 3);
    }

    /**
     * 配置认证管理器
     *
     * <p>认证管理器是Spring Security的核心组件，负责协调各种认证提供者。
     * 在当前的系统中，主要使用DAO认证提供者进行数据库用户认证。</p>
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 配置认证提供者
     *
     * <p>DAO认证提供者负责从数据库加载用户信息并验证密码。
     * 配置了密码编码器和用户详情服务。</p>
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        // 安全配置：隐藏用户不存在的异常，防止用户枚举攻击
        authProvider.setHideUserNotFoundExceptions(true);

        log.info("DAO认证提供者配置完成");
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
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(this.customPermissionEvaluator);
        log.info("方法安全表达式处理器配置完成");
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
        log.info("配置CORS跨域策略...");

        CorsConfiguration configuration = new CorsConfiguration();
        SecurityProperties.CorsProperties corsProps = securityProps.getCors();

        // 解析允许的源地址
        List<String> allowedOrigins = Arrays.asList(corsProps.getAllowedOrigins().split(","));
        configuration.setAllowedOriginPatterns(allowedOrigins);

        // 解析允许的HTTP方法
        List<String> allowedMethods = Arrays.asList(corsProps.getAllowedMethods().split(","));
        configuration.setAllowedMethods(allowedMethods);

        // 解析允许的请求头
        List<String> allowedHeaders = Arrays.asList(corsProps.getAllowedHeaders().split(","));
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

        log.info("CORS配置完成: origins={}, methods={}, credentials={}",
                allowedOrigins, allowedMethods, corsProps.isAllowCredentials());

        return source;
    }

    // =================== 私有辅助方法 ===================

    /**
     * 验证安全配置的有效性
     */
    private void validateSecurityConfiguration() {
        log.info("验证安全配置有效性...");

        // 验证JWT配置
        if (securityProps.getJwt().isUseRsa()) {
            if (!securityProps.isRsaConfigurationValid()) {
                throw new IllegalStateException(
                        "RSA模式下必须配置有效的RSA公钥和私钥：baseai.security.jwt.rsa-private-key 和 baseai.security.jwt.rsa-public-key");
            }
            log.info("✓ RSA密钥配置验证通过");
        } else {
            if (!securityProps.isHmacConfigurationValid()) {
                throw new IllegalStateException(
                        "HMAC模式下必须配置长度至少32字符的密钥：baseai.security.jwt.secret");
            }
            log.info("✓ HMAC密钥配置验证通过");
        }

        // 验证令牌过期时间配置
        if (securityProps.getJwt().getAccessTokenExpiration() <= 0) {
            throw new IllegalStateException("访问令牌过期时间必须大于0");
        }

        if (securityProps.getJwt().getRefreshTokenExpiration() <=
                securityProps.getJwt().getAccessTokenExpiration()) {
            throw new IllegalStateException("刷新令牌过期时间必须大于访问令牌过期时间");
        }

        log.info("✓ 令牌过期时间配置验证通过");

        // 验证密码策略配置
        SecurityProperties.PasswordProperties passwordProps = securityProps.getPassword();
        if (passwordProps.getMinLength() > passwordProps.getMaxLength()) {
            throw new IllegalStateException("密码最小长度不能大于最大长度");
        }

        log.info("✓ 密码策略配置验证通过");

        log.info("所有安全配置验证通过，安全特性状态：");
        log.info("  - JWT加密方式: {}", securityProps.getJwt().isUseRsa() ? "RSA" : "HMAC");
        log.info("  - 令牌黑名单: {}", securityProps.getJwt().isEnableBlacklist() ? "启用" : "禁用");
        log.info("  - 设备指纹验证: {}", securityProps.getJwt().isEnableDeviceFingerprint() ? "启用" : "禁用");
        log.info("  - CORS跨域: {}", securityProps.getCors().isEnabled() ? "启用" : "禁用");
        log.info("  - 增强安全功能: {}", securityProps.isEnhancedSecurityEnabled() ? "启用" : "禁用");
    }
}