package com.cloud.baseai.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <h2>向量服务配置</h2>
 *
 * <p>这个配置类管理与外部AI服务相关的所有配置参数。</p>
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "kb.embedding")
public class EmbeddingProperties {

    /**
     * 服务提供商类型 (openai, baidu, aliyun, mock)
     */
    private String provider = "openai";

    /**
     * API端点URL
     */
    private String apiUrl;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * 请求超时时间（毫秒）
     */
    private int timeoutMs = 30000;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 批量处理大小
     */
    private int batchSize = 100;

    /**
     * 是否启用缓存
     */
    private boolean cacheEnabled = true;

    /**
     * 缓存TTL（秒）
     */
    private int cacheTtlSeconds = 3600;

    /**
     * OpenAI 特定配置
     */
    private OpenAIConfig openai = new OpenAIConfig();

    /**
     * 千问特定配置
     */
    private QianWenConfig qianwen = new QianWenConfig();

    /**
     * 限流配置
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    /**
     * 重试配置
     */
    private RetryConfig retry = new RetryConfig();

    @Getter
    @Setter
    public static class OpenAIConfig {
        private String baseUrl = "https://api.openai.com/v1";
        private String defaultModel = "text-embedding-3-small";
        private int maxTokensPerRequest = 8192;
        private double requestsPerSecond = 3.0; // OpenAI的典型限制
        private boolean useExponentialBackoff = true;
    }

    @Getter
    @Setter
    public static class QianWenConfig {
        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
        private String defaultModel = "text-embedding-v1";
        private int maxTokensPerRequest = 6144;
        private double requestsPerSecond = 5.0; // 千问相对宽松一些
        private boolean useExponentialBackoff = true;
    }

    @Getter
    @Setter
    public static class RateLimitConfig {
        /**
         * 是否启用限流
         */
        private boolean enabled = true;
        /**
         * 获取许可的最大等待时间（毫秒）
         */
        private long maxWaitTimeMs = 5000;
        /**
         * 是否在限流时记录警告日志
         */
        private boolean logWarnings = true;
    }

    @Getter
    @Setter
    public static class RetryConfig {
        /**
         * 初始延迟时间（毫秒）
         */
        private long initialDelayMs = 1000;
        /**
         * 最大延迟时间（毫秒）
         */
        private long maxDelayMs = 30000;
        /**
         * 延迟倍增因子
         */
        private double multiplier = 2.0;
        /**
         * 是否对所有异常都重试
         */
        private boolean retryOnAllExceptions = false;
    }
}