package com.cloud.baseai.infrastructure.config.properties;

import com.cloud.baseai.infrastructure.constants.KbConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * <h2>知识库模块配置属性类</h2>
 *
 * <p>该类管理知识库相关的所有配置，包括文档处理、分块策略、
 * 向量嵌入、搜索配置以及性能优化等核心功能设置。</p>
 */
@Data
@ConfigurationProperties(prefix = "baseai.knowledge-base")
public class KnowledgeBaseProperties {

    /**
     * 文档处理配置
     */
    private DocumentProperties document = new DocumentProperties();

    /**
     * 文档分块配置
     */
    private ChunkingProperties chunking = new ChunkingProperties();

    /**
     * 向量嵌入配置
     */
    private EmbeddingProperties embedding = new EmbeddingProperties();

    /**
     * 向量搜索配置
     */
    private SearchProperties search = new SearchProperties();

    /**
     * 性能优化配置
     */
    private PerformanceProperties performance = new PerformanceProperties();

    /**
     * 文档处理配置内部类
     */
    @Data
    public static class DocumentProperties {
        /**
         * 单个文档最大大小（字节），默认10MB
         */
        private Long maxSizeBytes = 10485760L;

        /**
         * 批量处理最大数量
         */
        private Integer maxBatchSize = 100;

        /**
         * 批量处理超时时间（秒）
         */
        private Integer batchTimeoutSeconds = 300;

        /**
         * 处理失败重试次数
         */
        private Integer retryAttempts = 3;

        /**
         * 支持的文件格式
         */
        private List<String> supportedFormats = List.of(
                KbConstants.SourceTypes.PDF,
                KbConstants.SourceTypes.TEXT,
                KbConstants.SourceTypes.MARKDOWN,
                KbConstants.SourceTypes.WORD,
                KbConstants.SourceTypes.WORDX,
                KbConstants.SourceTypes.RTF
        );

        /**
         * 支持的MIME类型
         */
        private List<String> supportedMimeTypes = List.of(
                KbConstants.MimeTypes.TEXT_PLAIN,
                KbConstants.MimeTypes.TEXT_MARKDOWN,
                KbConstants.MimeTypes.APPLICATION_PDF,
                KbConstants.MimeTypes.APPLICATION_MSWORD,
                KbConstants.MimeTypes.APPLICATION_DOCX
        );
    }

    /**
     * 文档分块配置内部类
     */
    @Data
    public static class ChunkingProperties {
        /**
         * 默认分块大小（字符数）
         */
        private Integer defaultChunkSize = 1000;

        /**
         * 分块重叠大小（字符数），保证语义连续性
         */
        private Integer chunkOverlap = 200;

        /**
         * 最小分块大小，避免过小的分块
         */
        private Integer minChunkSize = 100;

        /**
         * 最大分块大小，避免过大的分块影响检索精度
         */
        private Integer maxChunkSize = 4000;

        /**
         * 是否启用自适应分块，根据内容类型调整策略
         */
        private Boolean adaptiveChunking = true;

        /**
         * 按内容类型的分块策略
         */
        private Map<String, ContentTypeStrategy> contentTypeStrategies = Map.of(
                "markdown", new ContentTypeStrategy(800, 150),
                "pdf", new ContentTypeStrategy(1200, 250),
                "text", new ContentTypeStrategy(1000, 200)
        );
    }

    /**
     * 内容类型策略内部类
     */
    @Data
    public static class ContentTypeStrategy {
        private Integer chunkSize;
        private Integer overlap;

        public ContentTypeStrategy() {
        }

        public ContentTypeStrategy(Integer chunkSize, Integer overlap) {
            this.chunkSize = chunkSize;
            this.overlap = overlap;
        }
    }

    /**
     * 向量嵌入配置内部类
     */
    @Data
    public static class EmbeddingProperties {
        /**
         * 默认嵌入模型
         */
        private String defaultModel = "text-embedding-3-small";

        /**
         * 向量维度
         */
        private Integer dimension = 1536;

        /**
         * 批量处理大小
         */
        private Integer batchSize = 50;

        /**
         * API调用超时时间（秒）
         */
        private Integer timeoutSeconds = 30;

        /**
         * 是否标准化向量
         */
        private Boolean normalize = true;

        /**
         * 备用模型列表，当主模型不可用时使用
         */
        private List<String> fallbackModels = List.of("text-embedding-ada-002");

        /**
         * 模型能力配置，用于智能选择模型
         */
        private Map<String, ModelCapability> modelCapabilities = Map.of(
                "text-embedding-3-small", new ModelCapability(8191, 1536, 0.00002),
                "text-embedding-ada-002", new ModelCapability(8191, 1536, 0.0001)
        );
    }

    /**
     * 模型能力配置内部类
     */
    @Data
    public static class ModelCapability {
        private Integer maxTokens;
        private Integer dimension;
        private Double costPerToken;

        public ModelCapability() {
        }

        public ModelCapability(Integer maxTokens, Integer dimension, Double costPerToken) {
            this.maxTokens = maxTokens;
            this.dimension = dimension;
            this.costPerToken = costPerToken;
        }
    }

    /**
     * 向量搜索配置内部类
     */
    @Data
    public static class SearchProperties {
        /**
         * 默认返回结果数量
         */
        private Integer defaultTopK = 10;

        /**
         * 最大返回结果数量
         */
        private Integer maxTopK = 100;

        /**
         * 默认相似度阈值
         */
        private Double defaultThreshold = 0.7;

        /**
         * 最小相似度阈值
         */
        private Double minThreshold = 0.0;

        /**
         * 是否启用重排序，提高搜索质量
         */
        private Boolean enableRerank = true;

        /**
         * 重排序候选数量
         */
        private Integer rerankTopK = 20;

        /**
         * 搜索结果缓存时间（秒）
         */
        private Integer resultCacheTtlSeconds = 300;

        /**
         * 高亮显示配置
         */
        private HighlightProperties highlight = new HighlightProperties();
    }

    /**
     * 高亮显示配置内部类
     */
    @Data
    public static class HighlightProperties {
        /**
         * 是否启用高亮显示
         */
        private Boolean enable = true;

        /**
         * 最大高亮数量
         */
        private Integer maxHighlights = 3;

        /**
         * 高亮长度
         */
        private Integer highlightLength = 100;

        /**
         * 片段大小
         */
        private Integer fragmentSize = 150;
    }

    /**
     * 性能优化配置内部类
     */
    @Data
    public static class PerformanceProperties {
        /**
         * 数据库连接池大小
         */
        private Integer dbPoolSize = 20;

        /**
         * 异步处理线程池大小
         */
        private Integer asyncPoolSize = 10;

        /**
         * 批量处理大小
         */
        private Integer batchProcessSize = 100;

        /**
         * 是否启用查询缓存
         */
        private Boolean enableQueryCache = true;

        /**
         * 查询缓存大小
         */
        private Integer queryCacheSize = 1000;

        /**
         * 是否启用性能指标收集
         */
        private Boolean enableMetrics = true;

        /**
         * 慢查询阈值（毫秒）
         */
        private Integer slowQueryThresholdMs = 1000;
    }
}