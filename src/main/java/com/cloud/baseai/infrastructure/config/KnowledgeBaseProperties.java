package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.utils.KbConstants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>知识库配置类</h2>
 *
 * <p>统一管理知识库模块的所有配置参数，支持从配置文件中读取并提供合理的默认值。</p>
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "baseai.kb")
public class KnowledgeBaseProperties {

    /**
     * 文档处理相关配置
     */
    private DocumentConfig document = new DocumentConfig();

    /**
     * 向量处理相关配置
     */
    private VectorConfig vector = new VectorConfig();

    /**
     * 搜索相关配置
     */
    private SearchConfig search = new SearchConfig();

    /**
     * 性能相关配置
     */
    private PerformanceConfig performance = new PerformanceConfig();

    /**
     * <h3>文档处理配置</h3>
     */
    @Setter
    @Getter
    public static class DocumentConfig {

        /**
         * 单个文档最大大小（字节）
         */
        private long maxSizeBytes = 10 * 1024 * 1024; // 10MB

        /**
         * 批量上传最大文档数量
         */
        private int maxBatchSize = 100;

        /**
         * 默认分块大小（字符数）
         */
        private int defaultChunkSize = 1000;

        /**
         * 分块重叠大小（字符数）
         */
        private int chunkOverlap = 200;

        /**
         * 最小分块大小（字符数）
         */
        private int minChunkSize = 100;

        /**
         * 支持的文件类型
         */
        private String[] supportedMimeTypes = {
                KbConstants.MimeTypes.TEXT_PLAIN,
                KbConstants.MimeTypes.TEXT_MARKDOWN,
                KbConstants.MimeTypes.TEXT_HTML,
                KbConstants.MimeTypes.APPLICATION_PDF,
                KbConstants.MimeTypes.APPLICATION_MSWORD,
                KbConstants.MimeTypes.APPLICATION_DOCX,
                KbConstants.MimeTypes.APPLICATION_JSON,
                KbConstants.MimeTypes.TEXT_CSV,
        };

        /**
         * 默认语言代码
         */
        private String defaultLangCode = KbConstants.LanguageCodes.AUTO;

    }

    /**
     * <h3>向量处理配置</h3>
     */
    @Setter
    @Getter
    public static class VectorConfig {

        /**
         * 默认向量模型
         */
        private String defaultModel = KbConstants.VectorModels.TEXT_EMBEDDING_3_SMALL;

        /**
         * 向量维度
         */
        private int dimension = KbConstants.VectorDimensions.TEXT_EMBEDDING_3_SMALL;

        /**
         * 批量向量生成大小
         */
        private int batchSize = 50;

        /**
         * 向量生成超时时间（秒）
         */
        private int timeoutSeconds = 30;

        /**
         * 向量归一化
         */
        private boolean normalize = true;

        /**
         * 向量缓存TTL（秒）
         */
        private int cacheTtlSeconds = 3600;

    }

    /**
     * <h3>搜索配置</h3>
     */
    @Setter
    @Getter
    public static class SearchConfig {

        /**
         * 默认返回结果数量
         */
        private int defaultTopK = 10;

        /**
         * 最大返回结果数量
         */
        private int maxTopK = 100;

        /**
         * 默认相似度阈值
         */
        private float defaultThreshold = KbConstants.SimilarityThresholds.LOW;

        /**
         * 最小相似度阈值
         */
        private float minThreshold = KbConstants.SimilarityThresholds.MIN;

        /**
         * 搜索结果缓存TTL（秒）
         */
        private int resultCacheTtlSeconds = 300;

        /**
         * 高亮片段最大数量
         */
        private int maxHighlights = 3;

        /**
         * 高亮片段长度
         */
        private int highlightLength = 100;

    }

    /**
     * <h3>性能配置</h3>
     */
    @Setter
    @Getter
    public static class PerformanceConfig {

        /**
         * 数据库连接池大小
         */
        private int dbPoolSize = 20;

        /**
         * 异步任务线程池大小
         */
        private int asyncPoolSize = 10;

        /**
         * 批处理大小
         */
        private int batchProcessSize = 100;

        /**
         * 是否启用查询缓存
         */
        private boolean enableQueryCache = true;

        /**
         * 查询缓存大小
         */
        private int queryCacheSize = 1000;

        /**
         * 是否启用统计信息
         */
        private boolean enableMetrics = true;

        /**
         * 慢查询阈值（毫秒）
         */
        private long slowQueryThresholdMs = 1000;

    }
}