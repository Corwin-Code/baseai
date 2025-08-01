package com.cloud.baseai.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * <h2>短信服务配置属性类</h2>
 *
 * <p>该类管理短信服务的多提供商配置，支持阿里云、腾讯云等多个服务商，
 * 并提供完整的发送限制和内容管理功能。</p>
 */
@Data
@ConfigurationProperties(prefix = "baseai.sms")
public class SmsProperties {

    /**
     * 服务提供商配置
     */
    private ProviderProperties provider = new ProviderProperties();

    /**
     * 阿里云短信配置
     */
    private AliyunProperties aliyun = new AliyunProperties();

    /**
     * 腾讯云短信配置
     */
    private TencentProperties tencent = new TencentProperties();

    /**
     * 发送限制配置
     */
    private RateLimitsProperties rateLimits = new RateLimitsProperties();

    /**
     * 内容配置
     */
    private ContentProperties content = new ContentProperties();

    /**
     * 服务提供商配置内部类
     */
    @Data
    public static class ProviderProperties {
        /**
         * 主要服务商
         */
        private String primary = "aliyun";

        /**
         * 备用服务商列表
         */
        private List<String> fallbackProviders = List.of("tencent", "huawei");

        /**
         * 是否启用故障转移
         */
        private Boolean enableFallback = true;

        /**
         * 故障转移阈值（连续失败次数）
         */
        private Integer failoverThreshold = 3;

        /**
         * 服务商权重配置（用于负载均衡）
         */
        private Map<String, Integer> weights = Map.of(
                "aliyun", 70,
                "tencent", 20,
                "huawei", 10
        );
    }

    /**
     * 阿里云短信配置内部类
     */
    @Data
    public static class AliyunProperties {
        private String accessKey = "";
        private String accessSecret = "";
        private String signature = "BaseAI";
        private String region = "cn-hangzhou";
    }

    /**
     * 腾讯云短信配置内部类
     */
    @Data
    public static class TencentProperties {
        private String secretId = "";
        private String secretKey = "";
        private String signature = "BaseAI";
        private String region = "ap-guangzhou";
    }

    /**
     * 发送限制配置内部类
     */
    @Data
    public static class RateLimitsProperties {
        /**
         * 全局限制
         */
        private GlobalLimitProperties global = new GlobalLimitProperties();

        /**
         * 用户级别限制
         */
        private UserLimitProperties user = new UserLimitProperties();

        /**
         * 手机号级别限制
         */
        private PhoneLimitProperties phone = new PhoneLimitProperties();

        @Data
        public static class GlobalLimitProperties {
            private Integer perMinute = 100;
            private Integer perHour = 1000;
            private Integer perDay = 5000;
        }

        @Data
        public static class UserLimitProperties {
            private Integer perMinute = 5;
            private Integer perHour = 20;
            private Integer perDay = 100;
        }

        @Data
        public static class PhoneLimitProperties {
            private Integer perMinute = 1;
            private Integer perHour = 10;
            private Integer perDay = 20;
        }
    }

    /**
     * 内容配置内部类
     */
    @Data
    public static class ContentProperties {
        /**
         * 重复检测时间（分钟），防止重复发送
         */
        private Integer duplicateCheckMinutes = 2;

        /**
         * 失败后启用验证码的次数
         */
        private Integer enableCaptchaAfterFailures = 3;

        /**
         * 短信模板配置
         */
        private Map<String, SmsTemplate> templates = Map.of(
                "verification-code", new SmsTemplate("SMS_123456", "您的验证码是{code}，有效期{expire}分钟，请勿泄露。", List.of("code", "expire")),
                "security-alert", new SmsTemplate("SMS_789012", "您的账户在{time}发生{action}操作，如非本人操作请及时联系客服。", List.of("time", "action")),
                "notification", new SmsTemplate("SMS_345678", "系统通知：{message}", List.of("message"))
        );
    }

    /**
     * 短信模板内部类
     */
    @Data
    public static class SmsTemplate {
        private String templateId;
        private String content;
        private List<String> params;

        public SmsTemplate() {
        }

        public SmsTemplate(String templateId, String content, List<String> params) {
            this.templateId = templateId;
            this.content = content;
            this.params = params;
        }
    }
}