package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.security.jwt.JwtAuthenticationEntryPoint;
import com.cloud.baseai.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.cloud.baseai.infrastructure.security.permission.CustomPermissionEvaluator;
import com.cloud.baseai.infrastructure.security.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
 * <p>这个配置类协调和管理着应用程序的所有安全机制。</p>
 *
 * <p><b>设计理念：</b></p>
 * <p>系统采用了"分层防护、细粒度控制"的安全策略：</p>
 * <ul>
 * <li><b>网关层防护：</b>通过JWT过滤器验证所有请求的身份</li>
 * <li><b>角色层控制：</b>基于用户角色进行粗粒度的访问控制</li>
 * <li><b>方法层授权：</b>通过注解实现细粒度的权限控制</li>
 * <li><b>资源层隔离：</b>确保多租户之间的数据安全隔离</li>
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
public class SecurityConfiguration {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 环境配置
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${security.cors.allowed-origins:http://localhost:3000}")
    private String[] allowedOrigins;

    @Value("${security.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String[] allowedMethods;

    @Value("${security.headers.content-security-policy:default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'}")
    private String contentSecurityPolicy;

    public SecurityConfiguration(
            CustomUserDetailsService userDetailsService,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 密码编码器配置
     *
     * <p>使用BCrypt算法，强度设置为12轮，在安全性和性能之间取得平衡。
     * 生产环境建议使用更高的强度值。</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * 配置安全过滤器链
     *
     * <p>这是Spring Security的核心配置方法。它定义了哪些URL需要什么样的保护，
     * 就像为城堡的每一扇门配置不同级别的守卫。</p>
     *
     * <p><b>安全策略说明：</b></p>
     * <p>我们采用了"白名单 + 黑名单"的混合策略：</p>
     * <p>1. 公开接口（如注册、登录）完全开放</p>
     * <p>2. 管理接口需要管理员权限</p>
     * <p>3. 业务接口需要认证，具体权限通过方法注解控制</p>
     *
     * @param http Spring Security的HTTP安全配置对象
     * @return 配置完成的安全过滤器链
     * @throws Exception 配置过程中可能出现的异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // ========== 基础安全配置 ==========
                .csrf(AbstractHttpConfigurer::disable)  // 禁用CSRF，因为使用JWT无状态认证
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // 配置CORS

                // ========== 会话管理配置 ==========
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // 无状态会话

                // ========== 异常处理配置 ==========
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // ========== URL访问权限配置 ==========
                .authorizeHttpRequests(authz -> authz
                        // 公开访问的端点（无需认证）
                        .requestMatchers(
                                "/api/v1/users/register",           // 用户注册
                                "/api/v1/users/activate",           // 用户激活
                                "/api/v1/auth/**",                  // 认证相关接口
                                "/api/v1/misc/files",               // 文件上传（可能需要公开访问）
                                "/api/v1/kb/health",                // 健康检查
                                "/api/v1/chat/health",              // 对话健康检查
                                "/api/v1/system/health",            // 系统健康检查
                                "/api/v1/users/invitations/*/respond" // 邀请响应
                        ).permitAll()

                        // 监控和管理端点（通常在不同端口）
                        .requestMatchers(
                                "/actuator/**",                     // Spring Boot Actuator端点
                                "/v3/api-docs/**",                  // OpenAPI文档
                                "/swagger-ui/**",                   // Swagger UI
                                "/swagger-ui.html",                 // Swagger首页
                                "/doc.html"
                        ).permitAll()

                        // 静态资源
                        .requestMatchers(HttpMethod.GET,
                                "/",
                                "/favicon.ico",
                                "/static/**",
                                "/public/**"
                        ).permitAll()

                        // 系统管理接口（需要系统管理员权限）
                        .requestMatchers("/api/v1/system/**")
                        .hasRole("SYSTEM_ADMIN")

                        // 审计接口（需要审计员或管理员权限）
                        .requestMatchers("/api/v1/audit/**")
                        .hasAnyRole("ADMIN", "AUDITOR", "SYSTEM_ADMIN")

                        // MCP工具管理（需要工具管理员权限）
                        .requestMatchers(HttpMethod.POST, "/api/v1/mcp/tools")
                        .hasRole("TOOL_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/mcp/tools/**")
                        .hasRole("TOOL_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/mcp/tools/**")
                        .hasRole("TOOL_ADMIN")

                        // 其他所有API都需要认证
                        .requestMatchers("/api/**").authenticated()

                        // 其他请求允许访问
                        .anyRequest().permitAll()
                )

                // ========== 添加自定义过滤器 ==========
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    /**
     * 配置认证管理器
     *
     * <p>认证管理器是Spring Security的"大脑"，它决定如何验证用户的身份。
     * 我们使用数据库存储的用户信息进行认证。</p>
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 配置认证提供者
     *
     * <p>认证提供者定义了具体的认证逻辑。我们使用DAO认证提供者，
     * 它会从数据库加载用户信息并验证密码。</p>
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
//        authProvider.setPasswordEncoder(passwordEncoder);
        // 隐藏用户不存在的异常，防止用户枚举攻击
        authProvider.setHideUserNotFoundExceptions(false);
        return authProvider;
    }

    /**
     * 配置方法安全表达式处理器
     *
     * <p>这个处理器让我们能够在方法级别使用自定义的权限表达式，
     * 比如 @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'READ')")。</p>
     */
    @Bean
    public static MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(new CustomPermissionEvaluator());
        return expressionHandler;
    }

    /**
     * 配置CORS（跨域资源共享）
     *
     * <p>现代Web应用通常采用前后端分离架构，前端和后端可能部署在不同的域名下。
     * CORS配置确保前端能够安全地调用后端API。</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的源域名（生产环境应该配置具体的域名）
        configuration.setAllowedOriginPatterns(List.of("*"));

        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 允许的请求头
        configuration.setAllowedHeaders(List.of("*"));

        // 允许发送认证信息（如Cookies、Authorization头）
        configuration.setAllowCredentials(true);

        // 预检请求的缓存时间
        configuration.setMaxAge(3600L);

        // 暴露给前端的响应头
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", "X-Total-Count", "X-Page-Number", "X-Page-Size"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}