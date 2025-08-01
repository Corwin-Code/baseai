package com.cloud.baseai.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * <h2>邮件服务配置属性类</h2>
 *
 * <p>该类管理邮件服务的完整配置，包括发送设置、投递策略、
 * 模板管理等功能，支持高并发邮件发送和模板化邮件处理。</p>
 */
@Data
@ConfigurationProperties(prefix = "baseai.email")
public class EmailProperties {

    /**
     * 基本发送配置
     */
    private SenderProperties sender = new SenderProperties();

    /**
     * 投递设置
     */
    private DeliveryProperties delivery = new DeliveryProperties();

    /**
     * 模板配置
     */
    private TemplateProperties template = new TemplateProperties();

    /**
     * 发送者配置内部类
     */
    @Data
    public static class SenderProperties {
        /**
         * 默认发件人地址
         */
        private String fromAddress = "noreply@baseai.com";

        /**
         * 默认发件人名称
         */
        private String fromName = "BaseAI系统";

        /**
         * 回复地址
         */
        private String replyTo = "support@baseai.com";

        /**
         * 退信地址
         */
        private String bounceAddress = "bounce@baseai.com";

        /**
         * 字符编码
         */
        private String charset = "UTF-8";

        /**
         * 是否启用HTML格式
         */
        private Boolean enableHtml = true;

        /**
         * 是否启用邮件跟踪
         */
        private Boolean enableTracking = false;
    }

    /**
     * 投递配置内部类
     */
    @Data
    public static class DeliveryProperties {
        /**
         * 是否启用异步发送
         */
        private Boolean enableAsync = true;

        /**
         * 异步队列大小
         */
        private Integer asyncQueueSize = 1000;

        /**
         * 最大重试次数
         */
        private Integer maxRetryAttempts = 3;

        /**
         * 重试延迟时间（秒）
         */
        private List<Integer> retryDelays = List.of(60, 300, 900);

        /**
         * 批量发送大小
         */
        private Integer batchSize = 50;

        /**
         * 批量发送间隔（毫秒）
         */
        private Integer batchDelayMs = 100;

        /**
         * 投递超时时间（秒）
         */
        private Integer deliveryTimeoutSeconds = 30;

        /**
         * 是否启用死信队列
         */
        private Boolean enableDeadLetterQueue = true;
    }

    /**
     * 模板配置内部类
     */
    @Data
    public static class TemplateProperties {
        /**
         * 是否启用模板功能
         */
        private Boolean enabled = true;

        /**
         * 模板缓存时间（秒）
         */
        private Integer cacheTtlSeconds = 3600;

        /**
         * 默认模板
         */
        private String defaultTemplate = "system-notification";

        /**
         * 模板文件位置
         */
        private String location = "classpath:/email-templates/";

        /**
         * 是否启用模板缓存
         */
        private Boolean cacheEnabled = true;

        /**
         * 支持的模板格式
         */
        private List<String> supportedFormats = List.of("html", "text");

        /**
         * 是否自动生成纯文本版本
         */
        private Boolean autoGenerateText = true;

        /**
         * 全局模板变量
         */
        private Map<String, String> globalVariables = Map.of(
                "system-name", "BaseAI",
                "support-email", "support@baseai.com",
                "website-url", "https://baseai.com"
        );
    }
}