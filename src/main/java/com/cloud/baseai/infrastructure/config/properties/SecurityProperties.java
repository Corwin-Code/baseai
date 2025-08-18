package com.cloud.baseai.infrastructure.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * <h2>安全相关配置属性类</h2>
 *
 * <p>该类负责管理应用的安全配置，包括JWT令牌设置、CORS跨域配置、
 * 以及密码安全策略等关键安全功能的配置参数。</p>
 *
 * <p><b>配置验证：</b></p>
 * <p>使用JSR-303验证注解确保配置的有效性，在应用启动时就能发现配置问题。</p>
 */
@Data
@Validated
@ConfigurationProperties(prefix = "baseai.security")
public class SecurityProperties {

    /**
     * JWT令牌相关配置
     */
    @Valid
    @NotNull
    private JwtProperties jwt = new JwtProperties();

    /**
     * CORS跨域资源共享配置
     */
    @Valid
    @NotNull
    private CorsProperties cors = new CorsProperties();

    /**
     * 密码安全策略配置
     */
    @Valid
    @NotNull
    private PasswordProperties password = new PasswordProperties();

    /**
     * 认证相关配置
     */
    @Valid
    @NotNull
    private AuthProperties auth = new AuthProperties();

    /**
     * JWT令牌配置内部类
     */
    @Data
    public static class JwtProperties {
        /**
         * 是否使用RSA加密（true=RSA，false=HMAC）
         */
        private boolean useRsa = true;

        /**
         * HMAC签名密钥（当useRsa=false时使用）
         */
        @NotBlank(message = "JWT HMAC密钥不能为空")
        private String secret = "very-long-secret-key-at-least-256-bits-for-production-environment";

        /**
         * RSA私钥（Base64编码，当useRsa=true时使用）
         */
        private String rsaPrivateKey;

        /**
         * RSA公钥（Base64编码，当useRsa=true时使用）
         */
        private String rsaPublicKey;

        /**
         * 访问令牌有效期（毫秒），默认1小时
         */
        @Min(value = 300000, message = "访问令牌有效期不能少于5分钟")
        private Long accessTokenExpiration = 3600000L;

        /**
         * 刷新令牌有效期（毫秒），默认7天
         */
        @Min(value = 3600000, message = "刷新令牌有效期不能少于1小时")
        private Long refreshTokenExpiration = 604800000L;

        /**
         * JWT发行者标识
         */
        @NotBlank(message = "JWT发行者不能为空")
        private String issuer = "baseai-system";

        /**
         * 是否启用令牌黑名单
         */
        private boolean enableBlacklist = true;

        /**
         * 是否启用设备指纹验证
         */
        private boolean enableDeviceFingerprint = true;

        /**
         * 令牌自动刷新阈值（秒），当令牌剩余时间少于此值时自动刷新
         */
        @Min(value = 60, message = "令牌自动刷新阈值不能少于60秒")
        private Long autoRefreshThreshold = 1800L; // 30分钟
    }

    /**
     * CORS配置内部类
     */
    @Data
    public static class CorsProperties {
        /**
         * 是否启用CORS
         */
        private boolean enabled = true;

        /**
         * 允许的源地址列表
         */
        @NotBlank(message = "CORS允许的源地址不能为空")
        private String allowedOrigins = "http://localhost:3000,http://localhost:8080,https://*.baseai.com";

        /**
         * 允许的HTTP方法
         */
        @NotBlank(message = "CORS允许的方法不能为空")
        private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS,PATCH";

        /**
         * 允许的请求头
         */
        @NotBlank(message = "CORS允许的请求头不能为空")
        private String allowedHeaders = "*";

        /**
         * 暴露给前端的响应头
         */
        private String exposedHeaders = "Authorization,X-Total-Count,X-Page-Number,X-Page-Size,X-Request-Id";

        /**
         * 是否允许发送Cookie
         */
        private boolean allowCredentials = true;

        /**
         * 预检请求缓存时间（秒）
         */
        @Min(value = 0, message = "预检请求缓存时间不能为负数")
        private Long maxAge = 3600L;
    }

    /**
     * 密码策略配置内部类
     */
    @Data
    public static class PasswordProperties {
        /**
         * 密码最小长度
         */
        @Min(value = 8, message = "密码最小长度不能少于8位")
        private Integer minLength = 12;

        /**
         * 密码最大长度
         */
        @Min(value = 8, message = "密码最大长度不能少于8位")
        private Integer maxLength = 128;

        /**
         * 是否要求包含大写字母
         */
        private boolean requireUppercase = true;

        /**
         * 是否要求包含小写字母
         */
        private boolean requireLowercase = true;

        /**
         * 是否要求包含数字
         */
        private boolean requireNumbers = true;

        /**
         * 是否要求包含特殊字符
         */
        private boolean requireSpecialChars = true;

        /**
         * 允许的特殊字符集合
         */
        @NotBlank(message = "特殊字符集合不能为空")
        private String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";

        /**
         * 密码历史记录数量，防止重复使用近期密码
         */
        @Min(value = 0, message = "密码历史记录数量不能为负数")
        private Integer historyCount = 5;

        /**
         * 密码最大使用天数，超过后强制修改
         */
        @Min(value = 1, message = "密码最大使用天数不能少于1天")
        private Integer maxAgeDays = 90;

        /**
         * 登录失败锁定阈值
         */
        @Min(value = 3, message = "登录失败锁定阈值不能少于3次")
        private Integer lockoutThreshold = 5;

        /**
         * 账户锁定时间（分钟）
         */
        @Min(value = 5, message = "账户锁定时间不能少于5分钟")
        private Integer lockoutDurationMinutes = 30;

        /**
         * 是否启用密码复杂度验证
         */
        private boolean enableComplexityCheck = true;

        /**
         * 是否启用弱密码检测
         */
        private boolean enableWeakPasswordCheck = true;

        /**
         * 是否启用密码泄露检查
         */
        private boolean enableBreachCheck = true;
    }

    /**
     * 认证相关配置
     */
    @Data
    public static class AuthProperties {
        /**
         * 是否启用多设备登录
         */
        private boolean enableMultiDevice = true;

        /**
         * 单用户最大同时登录设备数
         */
        @Min(value = 1, message = "最大同时登录设备数不能少于1")
        private Integer maxDevicesPerUser = 5;

        /**
         * 是否启用IP地址验证
         */
        private boolean enableIpValidation = false;

        /**
         * 允许的IP地址列表，支持CIDR格式（空表示不限制）
         */
        private String allowedIps = "";

        /**
         * 登录会话超时时间（分钟）
         */
        @Min(value = 5, message = "会话超时时间不能少于5分钟")
        private Integer sessionTimeoutMinutes = 120;

        /**
         * 是否启用记住我功能
         */
        private boolean enableRememberMe = true;

        /**
         * 记住我功能的有效期（天）
         */
        @Min(value = 1, message = "记住我有效期不能少于1天")
        private Integer rememberMeDays = 30;

        /**
         * 是否启用验证码
         */
        private boolean enableCaptcha = true;

        /**
         * 登录失败多少次后启用验证码
         */
        @Min(value = 1, message = "验证码触发阈值不能少于1次")
        private Integer captchaThreshold = 3;

        /**
         * 是否启用二次验证
         */
        private boolean enableTwoFactorAuth = false;

        /**
         * 二次验证代码有效期（秒）
         */
        @Min(value = 30, message = "二次验证代码有效期不能少于30秒")
        private Integer twoFactorCodeExpiration = 300;

        /**
         * 是否启用登录异常检测
         * 检测异常登录行为（如异地登录）
         */
        private boolean enableAnomalyDetection = true;

        /**
         * 是否记录登录历史
         */
        private boolean enableLoginHistory = true;

        /**
         * 登录历史保留天数
         */
        @Min(value = 7, message = "登录历史保留天数不能少于7天")
        private Integer loginHistoryRetentionDays = 90;
    }

    // =================== 配置验证方法 ===================

    /**
     * 验证RSA密钥配置
     */
    public boolean isRsaConfigurationValid() {
        if (!jwt.isUseRsa()) {
            return true; // 不使用RSA时不需要验证
        }

        return jwt.getRsaPrivateKey() != null && !jwt.getRsaPrivateKey().trim().isEmpty() &&
                jwt.getRsaPublicKey() != null && !jwt.getRsaPublicKey().trim().isEmpty();
    }

    /**
     * 验证HMAC密钥配置
     */
    public boolean isHmacConfigurationValid() {
        if (jwt.isUseRsa()) {
            return true; // 使用RSA时不需要验证HMAC
        }

        return jwt.getSecret() != null && jwt.getSecret().length() >= 32;
    }

    /**
     * 获取有效的令牌过期时间
     */
    public long getEffectiveTokenExpiration() {
        return Math.max(jwt.getAccessTokenExpiration(), 300000L); // 最少5分钟
    }

    /**
     * 获取有效的刷新令牌过期时间
     */
    public long getEffectiveRefreshTokenExpiration() {
        return Math.max(jwt.getRefreshTokenExpiration(), 3600000L); // 最少1小时
    }

    /**
     * 检查是否启用了增强安全功能
     */
    public boolean isEnhancedSecurityEnabled() {
        return jwt.isEnableBlacklist() &&
                jwt.isEnableDeviceFingerprint() &&
                password.isEnableComplexityCheck();
    }
}