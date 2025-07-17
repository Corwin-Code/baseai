package com.cloud.baseai.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>对话模块配置</h2>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "baseai.chat")
public class ChatProperties {

    /**
     * 最大消息长度
     */
    private int maxMessageLength = 32000;

    /**
     * 默认嵌入模型
     */
    private String defaultEmbeddingModel = "text-embedding-3-small";

    /**
     * 知识检索配置
     */
    private int knowledgeRetrievalTopK = 5;
    private float knowledgeRetrievalThreshold = 0.7f;

    /**
     * 速率限制配置
     */
    private boolean rateLimitEnabled = true;
    private int rateLimitMax = 100;
    private int rateLimitWindow = 60; // 分钟

    /**
     * 语义分析配置
     */
    private boolean semanticAnalysisEnabled = true;

    /**
     * 性能配置
     */
    private Performance performance = new Performance();

    @Data
    public static class Performance {
        private int asyncPoolSize = 10;
        private int maxContextTokens = 8000;
        private boolean enableCaching = true;
    }
}