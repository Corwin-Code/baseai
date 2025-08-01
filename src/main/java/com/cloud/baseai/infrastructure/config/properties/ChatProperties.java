package com.cloud.baseai.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * <h2>对话系统配置属性类</h2>
 *
 * <p>该类负责管理聊天机器人的核心配置，包括消息处理规则、
 * 知识检索策略以及AI功能设置，确保对话系统的高效运行。</p>
 */
@Data
@ConfigurationProperties(prefix = "baseai.chat")
public class ChatProperties {

    /**
     * 消息约束配置
     */
    private MessageProperties message = new MessageProperties();

    /**
     * 知识检索配置
     */
    private KnowledgeRetrievalProperties knowledgeRetrieval = new KnowledgeRetrievalProperties();

    /**
     * AI功能配置
     */
    private AiFeaturesProperties aiFeatures = new AiFeaturesProperties();

    /**
     * 消息配置内部类
     */
    @Data
    public static class MessageProperties {
        /**
         * 单条消息最大长度（字符数）
         */
        private Integer maxLength = 32000;

        /**
         * 上下文中最大消息数量
         */
        private Integer maxContextMessages = 50;

        /**
         * 上下文最大令牌数
         */
        private Integer maxContextTokens = 8000;

        /**
         * 是否启用消息内容验证
         */
        private Boolean enableValidation = true;

        /**
         * 是否自动截断过长消息
         */
        private Boolean autoTruncate = true;

        /**
         * 被阻止的内容模式列表
         */
        private List<String> blockedPatterns = List.of("spam", "abuse", "advertisement");
    }

    /**
     * 知识检索配置内部类
     */
    @Data
    public static class KnowledgeRetrievalProperties {
        /**
         * 检索结果数量
         */
        private Integer topK = 5;

        /**
         * 相似度阈值
         */
        private Double threshold = 0.7;

        /**
         * 是否启用上下文增强
         */
        private Boolean enableContextEnrichment = true;

        /**
         * 最大检索字符数
         */
        private Integer maxRetrievedChars = 4000;

        /**
         * 检索超时时间（毫秒）
         */
        private Integer retrievalTimeoutMs = 5000;

        /**
         * 是否启用引用功能
         */
        private Boolean enableCitation = true;

        /**
         * 引用格式
         */
        private String citationFormat = "markdown";
    }

    /**
     * AI功能配置内部类
     */
    @Data
    public static class AiFeaturesProperties {
        /**
         * 是否启用语义分析
         */
        private Boolean enableSemanticAnalysis = true;

        /**
         * 是否启用情感检测
         */
        private Boolean enableSentimentDetection = false;

        /**
         * 是否启用自动摘要
         */
        private Boolean enableAutoSummary = true;

        /**
         * 回复风格：conservative（保守）、balanced（平衡）、creative（创意）
         */
        private String responseStyle = "balanced";

        /**
         * 创造性水平（0.0-1.0）
         */
        private Double creativityLevel = 0.7;

        /**
         * 安全等级：low、medium、high
         */
        private String safetyLevel = "high";

        /**
         * 支持的语言列表
         */
        private List<String> supportedLanguages = List.of("zh", "en", "ja", "ko");

        /**
         * 默认语言
         */
        private String defaultLanguage = "zh";
    }
}