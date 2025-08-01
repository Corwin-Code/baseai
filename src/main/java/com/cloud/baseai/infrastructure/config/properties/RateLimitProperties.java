package com.cloud.baseai.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <h2>速率限制配置属性类</h2>
 *
 * <p>该类用于配置API调用的速率限制策略，支持全局限制和用户级别限制，
 * 有效防止系统过载和恶意攻击。</p>
 */
@Data
@ConfigurationProperties(prefix = "baseai.rate-limit")
public class RateLimitProperties {

    /**
     * 全局API限制配置
     */
    private GlobalLimitProperties global = new GlobalLimitProperties();

    /**
     * 用户级别限制配置
     */
    private UserLimitProperties user = new UserLimitProperties();

    /**
     * 全局限制配置内部类
     */
    @Data
    public static class GlobalLimitProperties {
        /**
         * 每分钟最大请求数
         */
        private Integer requestsPerMinute = 100;

        /**
         * 每小时最大请求数
         */
        private Integer requestsPerHour = 1000;

        /**
         * 每天最大请求数
         */
        private Integer requestsPerDay = 10000;

        /**
         * 最大并发请求数
         */
        private Integer concurrentRequests = 20;

        /**
         * 是否启用突发保护
         */
        private Boolean enableBurstProtection = true;

        /**
         * 突发倍数，允许短时间内超过正常限制
         */
        private Double burstMultiplier = 2.0;
    }

    /**
     * 用户级别限制配置内部类
     */
    @Data
    public static class UserLimitProperties {
        /**
         * 免费用户限制
         */
        private TierLimitProperties freeTier = new TierLimitProperties(100, 20, 10);

        /**
         * 高级用户限制
         */
        private TierLimitProperties premiumTier = new TierLimitProperties(5000, 200, 100);

        /**
         * 企业用户限制
         */
        private TierLimitProperties enterpriseTier = new TierLimitProperties(50000, 2000, 1000);
    }

    /**
     * 用户等级限制配置内部类
     */
    @Data
    public static class TierLimitProperties {
        /**
         * 每天请求数限制
         */
        private Integer requestsPerDay;

        /**
         * 每小时聊天消息数限制
         */
        private Integer chatMessagesPerHour;

        /**
         * 每天文档上传数限制
         */
        private Integer documentUploadsPerDay;

        public TierLimitProperties() {
        }

        public TierLimitProperties(Integer requestsPerDay, Integer chatMessagesPerHour, Integer documentUploadsPerDay) {
            this.requestsPerDay = requestsPerDay;
            this.chatMessagesPerHour = chatMessagesPerHour;
            this.documentUploadsPerDay = documentUploadsPerDay;
        }
    }
}