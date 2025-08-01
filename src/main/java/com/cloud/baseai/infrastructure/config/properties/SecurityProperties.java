package com.cloud.baseai.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <h2>安全相关配置属性类</h2>
 *
 * <p>该类负责管理应用的安全配置，包括JWT令牌设置、CORS跨域配置、
 * 以及密码安全策略等关键安全功能的配置参数。</p>
 */
@Data
@ConfigurationProperties(prefix = "baseai.security")
public class SecurityProperties {

    /**
     * JWT令牌相关配置
     */
    private JwtProperties jwt = new JwtProperties();

    /**
     * CORS跨域资源共享配置
     */
    private CorsProperties cors = new CorsProperties();

    /**
     * 密码安全策略配置
     */
    private PasswordProperties password = new PasswordProperties();

    /**
     * JWT令牌配置内部类
     */
    @Data
    public static class JwtProperties {
        /**
         * JWT签名密钥，生产环境必须使用高强度密钥
         */
        private String secret = "very-long-secret-key-at-least-256-bits-for-production";

        /**
         * 访问令牌有效期（毫秒），默认24小时
         */
        private Long accessTokenExpiration = 86400000L;

        /**
         * 刷新令牌有效期（毫秒），默认7天
         */
        private Long refreshTokenExpiration = 604800000L;

        /**
         * JWT发行者标识
         */
        private String issuer = "baseai-system";
    }

    /**
     * CORS配置内部类
     */
    @Data
    public static class CorsProperties {
        /**
         * 允许的源地址列表
         */
        private String allowedOrigins = "http://localhost:3000,http://localhost:8080";

        /**
         * 允许的HTTP方法
         */
        private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS,PATCH";

        /**
         * 允许的请求头
         */
        private String allowedHeaders = "*";

        /**
         * 是否允许发送Cookie
         */
        private Boolean allowCredentials = true;

        /**
         * 预检请求缓存时间（秒）
         */
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
        private Integer minLength = 8;

        /**
         * 密码最大长度
         */
        private Integer maxLength = 128;

        /**
         * 是否要求包含大写字母
         */
        private Boolean requireUppercase = true;

        /**
         * 是否要求包含小写字母
         */
        private Boolean requireLowercase = true;

        /**
         * 是否要求包含数字
         */
        private Boolean requireNumbers = true;

        /**
         * 是否要求包含特殊字符
         */
        private Boolean requireSpecialChars = true;

        /**
         * 允许的特殊字符集合
         */
        private String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";

        /**
         * 密码历史记录数量，防止重复使用近期密码
         */
        private Integer historyCount = 5;

        /**
         * 密码最大使用天数，超过后强制修改
         */
        private Integer maxAgeDays = 90;

        /**
         * 登录失败锁定阈值
         */
        private Integer lockoutThreshold = 5;

        /**
         * 账户锁定时间（分钟）
         */
        private Integer lockoutDurationMinutes = 30;
    }
}