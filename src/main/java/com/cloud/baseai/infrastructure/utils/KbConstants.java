package com.cloud.baseai.infrastructure.utils;

/**
 * <h2>知识库常量定义</h2>
 *
 * <p>集中定义知识库模块使用的所有常量，确保常量的一致性和可维护性。</p>
 */
public final class KbConstants {

    private KbConstants() {
        // 常量类不允许实例化
    }

    // =================== 文档相关常量 ===================

    /**
     * 支持的文档来源类型
     */
    public static final class SourceTypes {
        public static final String PDF = "PDF";
        public static final String WORD = "WORD";
        public static final String MARKDOWN = "MARKDOWN";
        public static final String TEXT = "TEXT";
        public static final String URL = "URL";
        public static final String HTML = "HTML";
        public static final String CSV = "CSV";
        public static final String JSON = "JSON";
    }

    /**
     * MIME类型常量
     */
    public static final class MimeTypes {
        public static final String TEXT_PLAIN = "text/plain";
        public static final String TEXT_MARKDOWN = "text/markdown";
        public static final String TEXT_HTML = "text/html";
        public static final String APPLICATION_PDF = "application/pdf";
        public static final String APPLICATION_MSWORD = "application/msword";
        public static final String APPLICATION_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        public static final String APPLICATION_JSON = "application/json";
        public static final String TEXT_CSV = "text/csv";
    }

    /**
     * 语言代码常量
     */
    public static final class LanguageCodes {
        public static final String AUTO = "auto";
        public static final String CHINESE_SIMPLIFIED = "zh-CN";
        public static final String CHINESE_TRADITIONAL = "zh-TW";
        public static final String ENGLISH = "en";
        public static final String JAPANESE = "ja";
        public static final String KOREAN = "ko";
        public static final String FRENCH = "fr";
        public static final String GERMAN = "de";
        public static final String SPANISH = "es";
        public static final String RUSSIAN = "ru";
    }

    // =================== 向量相关常量 ===================

    /**
     * 向量模型常量
     */
    public static final class VectorModels {
        public static final String TEXT_EMBEDDING_3_SMALL = "text-embedding-3-small";
        public static final String TEXT_EMBEDDING_3_LARGE = "text-embedding-3-large";
        public static final String TEXT_EMBEDDING_ADA_002 = "text-embedding-ada-002";
    }

    /**
     * 向量维度常量
     */
    public static final class VectorDimensions {
        public static final int TEXT_EMBEDDING_3_SMALL = 1536;
        public static final int TEXT_EMBEDDING_3_LARGE = 3072;
        public static final int TEXT_EMBEDDING_ADA_002 = 1536;
    }

    /**
     * 相似度阈值常量
     */
    public static final class SimilarityThresholds {
        public static final float HIGH = 0.9f;
        public static final float MEDIUM = 0.8f;
        public static final float LOW = 0.7f;
        public static final float VERY_LOW = 0.6f;
        public static final float MIN = 0.0f;
        public static final float MAX = 1.0f;
    }

    // =================== 搜索相关常量 ===================

    /**
     * 搜索类型常量
     */
    public static final class SearchTypes {
        public static final String VECTOR = "vector";
        public static final String TEXT = "text";
        public static final String HYBRID = "hybrid";
    }

    /**
     * 置信度等级常量
     */
    public static final class ConfidenceLevels {
        public static final String HIGH = "HIGH";
        public static final String MEDIUM = "MEDIUM";
        public static final String LOW = "LOW";
        public static final String VERY_LOW = "VERY_LOW";
    }

    /**
     * 搜索限制常量
     */
    public static final class SearchLimits {
        public static final int DEFAULT_TOP_K = 10;
        public static final int MAX_TOP_K = 100;
        public static final int MIN_TOP_K = 1;
        public static final int DEFAULT_PAGE_SIZE = 20;
        public static final int MAX_PAGE_SIZE = 100;
        public static final int MAX_HIGHLIGHTS = 5;
        public static final int DEFAULT_HIGHLIGHT_LENGTH = 100;
    }

    // =================== 文本处理常量 ===================

    /**
     * 分块相关常量
     */
    public static final class ChunkLimits {
        public static final int DEFAULT_SIZE = 1000;
        public static final int MIN_SIZE = 100;
        public static final int MAX_SIZE = 4000;
        public static final int DEFAULT_OVERLAP = 200;
        public static final int MIN_OVERLAP = 0;
        public static final int MAX_OVERLAP = 500;
    }

    /**
     * 文本长度限制
     */
    public static final class TextLimits {
        public static final int TITLE_MAX_LENGTH = 256;
        public static final int TAG_NAME_MAX_LENGTH = 64;
        public static final int REMARK_MAX_LENGTH = 500;
        public static final int QUERY_MAX_LENGTH = 1000;
        public static final int DOCUMENT_MAX_SIZE = 10 * 1024 * 1024; // 10MB
    }

    // =================== 错误消息常量 ===================

    /**
     * 错误消息常量
     */
    public static final class ErrorMessages {
        public static final String DOCUMENT_NOT_FOUND = "文档不存在或已被删除";
        public static final String CHUNK_NOT_FOUND = "知识块不存在或已被删除";
        public static final String TAG_NOT_FOUND = "标签不存在或已被删除";
        public static final String EMBEDDING_NOT_FOUND = "向量数据不存在";

        public static final String DUPLICATE_DOCUMENT_TITLE = "文档标题已存在";
        public static final String DUPLICATE_DOCUMENT_CONTENT = "文档内容已存在";
        public static final String DUPLICATE_TAG_NAME = "标签名称已存在";

        public static final String INVALID_VECTOR_DIMENSION = "向量维度不匹配";
        public static final String INVALID_SIMILARITY_THRESHOLD = "相似度阈值必须在0-1之间";
        public static final String INVALID_PAGE_PARAMETERS = "分页参数无效";

        public static final String DOCUMENT_TOO_LARGE = "文档大小超出限制";
        public static final String BATCH_SIZE_EXCEEDED = "批量操作大小超出限制";
        public static final String UNSUPPORTED_FILE_TYPE = "不支持的文件类型";

        public static final String PARSING_FAILED = "文档解析失败";
        public static final String VECTOR_GENERATION_FAILED = "向量生成失败";
        public static final String SEARCH_FAILED = "搜索执行失败";

        public static final String UNAUTHORIZED_OPERATION = "无权限执行此操作";
        public static final String TENANT_MISMATCH = "租户数据不匹配";

        public static final String DOCUMENT_TOO_COMPLEX = "文档内容过于复杂";
    }

    // =================== 缓存键常量 ===================

    /**
     * 缓存键前缀
     */
    public static final class CacheKeys {
        public static final String PREFIX = "kb:";
        public static final String DOCUMENT = PREFIX + "doc:";
        public static final String CHUNK = PREFIX + "chunk:";
        public static final String EMBEDDING = PREFIX + "emb:";
        public static final String TAG = PREFIX + "tag:";
        public static final String SEARCH_RESULT = PREFIX + "search:";
        public static final String USER_INFO = PREFIX + "user:";
    }

    // =================== 统计和监控常量 ===================

    /**
     * 度量指标名称
     */
    public static final class MetricNames {
        public static final String DOCUMENT_UPLOAD_COUNT = "kb.document.upload.count";
        public static final String DOCUMENT_PARSE_TIME = "kb.document.parse.time";
        public static final String VECTOR_GENERATION_TIME = "kb.vector.generation.time";
        public static final String SEARCH_REQUEST_COUNT = "kb.search.request.count";
        public static final String SEARCH_RESPONSE_TIME = "kb.search.response.time";
        public static final String SEARCH_RESULT_COUNT = "kb.search.result.count";
    }

    /**
     * 系统限制常量
     */
    public static final class SystemLimits {
        public static final int MAX_CONCURRENT_UPLOADS = 10;
        public static final int MAX_CONCURRENT_VECTOR_GENERATIONS = 5;
        public static final int MAX_SEARCH_REQUESTS_PER_MINUTE = 100;
        public static final long SLOW_QUERY_THRESHOLD_MS = 1000;
        public static final long VECTOR_GENERATION_TIMEOUT_MS = 30000;
        public static final long MAX_TOKENS_PER_DOCUMENT = 100000;
    }
}