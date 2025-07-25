package com.cloud.baseai.infrastructure.constants;

/**
 * <h2>智能对话常量定义</h2>
 *
 * <p>集中定义智能对话模块使用的所有常量，确保常量的一致性和可维护性。</p>
 */
public final class ChatConstants {

    private ChatConstants() {
        throw new UnsupportedOperationException("常量类不能被实例化");
    }

    // =================== 消息类型 ===================

    /**
     * 消息内容类型常量
     *
     * <p>现代对话系统支持多种内容类型，从纯文本到富媒体内容。
     * 不同的内容类型需要不同的处理逻辑和渲染方式。</p>
     */
    public static final class ContentTypes {
        /**
         * 纯文本内容
         */
        public static final String TEXT = "TEXT";
        /**
         * Markdown格式内容
         */
        public static final String MARKDOWN = "MARKDOWN";
        /**
         * HTML格式内容
         */
        public static final String HTML = "HTML";
        /**
         * JSON格式内容
         */
        public static final String JSON = "JSON";
        /**
         * 代码片段
         */
        public static final String CODE = "CODE";
        /**
         * 图片内容
         */
        public static final String IMAGE = "IMAGE";
        /**
         * 文件附件
         */
        public static final String FILE = "FILE";
        /**
         * 音频内容
         */
        public static final String AUDIO = "AUDIO";
        /**
         * 视频内容
         */
        public static final String VIDEO = "VIDEO";
    }

    /**
     * 消息处理状态常量
     */
    public static final class MessageStatus {
        /**
         * 等待处理
         */
        public static final String PENDING = "PENDING";
        /**
         * 正在处理
         */
        public static final String PROCESSING = "PROCESSING";
        /**
         * 处理完成
         */
        public static final String COMPLETED = "COMPLETED";
        /**
         * 处理失败
         */
        public static final String FAILED = "FAILED";
        /**
         * 已取消
         */
        public static final String CANCELLED = "CANCELLED";
    }

    // =================== 流式处理常量 ===================

    /**
     * 流式事件类型常量
     *
     * <p>流式处理是现代AI对话的核心特性，它让用户能够实时看到AI的思考过程。
     * 通过标准化的事件类型，我们可以为用户提供丰富的交互反馈。</p>
     */
    public static final class StreamEvents {
        /**
         * 处理开始事件
         */
        public static final String START = "start";
        /**
         * 处理步骤事件
         */
        public static final String STEP = "step";
        /**
         * 文本块事件
         */
        public static final String CHUNK = "chunk";
        /**
         * 处理完成事件
         */
        public static final String COMPLETE = "complete";
        /**
         * 错误事件
         */
        public static final String ERROR = "error";
        /**
         * 进度更新事件
         */
        public static final String PROGRESS = "progress";
        /**
         * 工具调用事件
         */
        public static final String TOOL_CALL = "tool_call";
        /**
         * 思考过程事件
         */
        public static final String THINKING = "thinking";
        /**
         * 引用事件
         */
        public static final String CITATION = "citation";
    }

    /**
     * 处理步骤类型常量
     *
     * <p>AI处理用户请求时经历的各个阶段。每个步骤都有特定的目的和输出，
     * 向用户展示这些步骤能够增强AI的可解释性和可信度。</p>
     */
    public static final class ProcessingSteps {
        /**
         * 内容理解和分析
         */
        public static final String UNDERSTANDING = "understanding";
        /**
         * 知识库检索
         */
        public static final String KNOWLEDGE_RETRIEVAL = "knowledge_retrieval";
        /**
         * 工具调用
         */
        public static final String TOOL_CALLING = "tool_calling";
        /**
         * 流程编排
         */
        public static final String FLOW_ORCHESTRATION = "flow_orchestration";
        /**
         * 内容生成
         */
        public static final String GENERATING = "generating";
        /**
         * 后处理
         */
        public static final String POST_PROCESSING = "post_processing";
        /**
         * 质量检查
         */
        public static final String QUALITY_CHECK = "quality_check";
    }

    // =================== AI模型相关常量 ===================

    /**
     * AI模型类型常量
     *
     * <p>不同的AI模型有不同的特性和适用场景。通过模型类型的分类，
     * 系统可以智能选择最适合的模型来处理特定类型的请求。</p>
     */
    public static final class ModelTypes {
        /**
         * 通用对话模型
         */
        public static final String GENERAL_CHAT = "GENERAL_CHAT";
        /**
         * 代码生成模型
         */
        public static final String CODE_GENERATION = "CODE_GENERATION";
        /**
         * 文本分析模型
         */
        public static final String TEXT_ANALYSIS = "TEXT_ANALYSIS";
        /**
         * 创意写作模型
         */
        public static final String CREATIVE_WRITING = "CREATIVE_WRITING";
        /**
         * 专业领域模型
         */
        public static final String DOMAIN_SPECIFIC = "DOMAIN_SPECIFIC";
        /**
         * 多模态模型
         */
        public static final String MULTIMODAL = "MULTIMODAL";
    }

    /**
     * 模型能力标签常量
     */
    public static final class ModelCapabilities {
        /**
         * 支持工具调用
         */
        public static final String TOOL_CALLING = "TOOL_CALLING";
        /**
         * 支持函数调用
         */
        public static final String FUNCTION_CALLING = "FUNCTION_CALLING";
        /**
         * 支持视觉理解
         */
        public static final String VISION = "VISION";
        /**
         * 支持代码理解
         */
        public static final String CODE_UNDERSTANDING = "CODE_UNDERSTANDING";
        /**
         * 支持多语言
         */
        public static final String MULTILINGUAL = "MULTILINGUAL";
        /**
         * 支持长上下文
         */
        public static final String LONG_CONTEXT = "LONG_CONTEXT";
    }

    // =================== 字段长度限制 ===================

    /**
     * 字段长度限制常量
     *
     * <p>合理的字段长度限制既能保证系统性能，又能满足用户的实际需求。
     * 这些限制是基于大量实际使用数据分析得出的最佳实践值。</p>
     */
    public static final class FieldLengths {
        /**
         * 对话线程标题最大长度
         */
        public static final int MAX_THREAD_TITLE_LENGTH = 256;
        /**
         * 对话线程标题最小长度
         */
        public static final int MIN_THREAD_TITLE_LENGTH = 1;

        /**
         * 消息内容最大长度 - 单条消息的合理上限
         */
        public static final int MAX_MESSAGE_CONTENT_LENGTH = 50000;
        /**
         * 消息内容最小长度
         */
        public static final int MIN_MESSAGE_CONTENT_LENGTH = 1;

        /**
         * 系统提示词最大长度
         */
        public static final int MAX_SYSTEM_PROMPT_LENGTH = 10000;

        /**
         * 工具调用JSON最大长度
         */
        public static final int MAX_TOOL_CALL_JSON_LENGTH = 8192;

        /**
         * 反馈评论最大长度
         */
        public static final int MAX_FEEDBACK_COMMENT_LENGTH = 1000;

        /**
         * 模型名称最大长度
         */
        public static final int MAX_MODEL_NAME_LENGTH = 128;

        /**
         * 建议问题最大长度
         */
        public static final int MAX_SUGGESTION_LENGTH = 256;

        /**
         * 引用内容最大长度
         */
        public static final int MAX_CITATION_CONTENT_LENGTH = 2048;
    }

    // =================== 业务限制常量 ===================

    /**
     * 业务限制常量
     *
     * <p>这些限制确保系统能够在合理的资源消耗下稳定运行，
     * 同时为用户提供良好的服务体验。每个限制都是基于系统容量、
     * 用户体验和成本效益的综合考虑。</p>
     */
    public static final class BusinessLimits {
        /**
         * 每个租户最大对话线程数
         */
        public static final int MAX_THREADS_PER_TENANT = 10000;
        /**
         * 每个用户最大对话线程数
         */
        public static final int MAX_THREADS_PER_USER = 500;
        /**
         * 每个线程最大消息数
         */
        public static final int MAX_MESSAGES_PER_THREAD = 1000;

        /**
         * 每分钟最大消息发送数（用户级别）
         */
        public static final int MAX_MESSAGES_PER_MINUTE_USER = 30;
        /**
         * 每小时最大消息发送数（用户级别）
         */
        public static final int MAX_MESSAGES_PER_HOUR_USER = 300;
        /**
         * 每天最大消息发送数（用户级别）
         */
        public static final int MAX_MESSAGES_PER_DAY_USER = 1000;

        /**
         * 每分钟最大消息发送数（租户级别）
         */
        public static final int MAX_MESSAGES_PER_MINUTE_TENANT = 1000;
        /**
         * 每小时最大消息发送数（租户级别）
         */
        public static final int MAX_MESSAGES_PER_HOUR_TENANT = 10000;

        /**
         * 并发对话处理数量限制
         */
        public static final int MAX_CONCURRENT_CONVERSATIONS = 1000;
        /**
         * 最大上下文长度（消息数）
         */
        public static final int MAX_CONTEXT_MESSAGES = 50;
        /**
         * 最大知识检索结果数
         */
        public static final int MAX_KNOWLEDGE_RETRIEVAL_RESULTS = 20;

        /**
         * 消息生成超时时间（秒）
         */
        public static final int MESSAGE_GENERATION_TIMEOUT_SECONDS = 120;
        /**
         * 流式响应超时时间（秒）
         */
        public static final int STREAM_RESPONSE_TIMEOUT_SECONDS = 300;

        /**
         * 建议问题数量限制
         */
        public static final int MAX_SUGGESTIONS_COUNT = 10;
        /**
         * 默认建议问题数量
         */
        public static final int DEFAULT_SUGGESTIONS_COUNT = 3;

        /**
         * 对话历史保留天数
         */
        public static final int CONVERSATION_RETENTION_DAYS = 365;
        /**
         * 消息详细日志保留天数
         */
        public static final int MESSAGE_LOG_RETENTION_DAYS = 90;
    }

    /**
     * 分页参数常量
     */
    public static final class Pagination {
        /**
         * 默认页面大小
         */
        public static final int DEFAULT_PAGE_SIZE = 20;
        /**
         * 最大页面大小
         */
        public static final int MAX_PAGE_SIZE = 100;
        /**
         * 最小页面大小
         */
        public static final int MIN_PAGE_SIZE = 1;
        /**
         * 对话线程列表默认大小
         */
        public static final int DEFAULT_THREAD_LIST_SIZE = 15;
        /**
         * 消息列表默认大小
         */
        public static final int DEFAULT_MESSAGE_LIST_SIZE = 50;
        /**
         * 搜索结果最大数量
         */
        public static final int MAX_SEARCH_RESULTS = 500;
    }

    // =================== 缓存键常量 ===================

    /**
     * 缓存键前缀常量
     *
     * <p>良好的缓存策略是高性能对话系统的关键。通过标准化的缓存键命名，
     * 我们可以实现精确的缓存控制和高效的数据访问。</p>
     */
    public static final class CacheKeys {
        /**
         * 缓存键根前缀
         */
        public static final String PREFIX = "chat:";

        /**
         * 对话线程缓存键前缀
         */
        public static final String THREAD = PREFIX + "thread:";
        /**
         * 消息缓存键前缀
         */
        public static final String MESSAGE = PREFIX + "message:";
        /**
         * 引用信息缓存键前缀
         */
        public static final String CITATION = PREFIX + "citation:";
        /**
         * 使用统计缓存键前缀
         */
        public static final String USAGE = PREFIX + "usage:";

        /**
         * 用户会话缓存键前缀
         */
        public static final String USER_SESSION = PREFIX + "session:";
        /**
         * 速率限制缓存键前缀
         */
        public static final String RATE_LIMIT = PREFIX + "rate:";
        /**
         * 模型状态缓存键前缀
         */
        public static final String MODEL_STATUS = PREFIX + "model:";

        /**
         * 统计信息缓存键前缀
         */
        public static final String STATISTICS = PREFIX + "stats:";
        /**
         * 热门话题缓存键前缀
         */
        public static final String HOT_TOPICS = PREFIX + "topics:";
        /**
         * 建议问题缓存键前缀
         */
        public static final String SUGGESTIONS = PREFIX + "suggest:";
    }

    /**
     * 缓存配置常量
     */
    public static final class Cache {
        /**
         * 默认缓存过期时间（分钟）
         */
        public static final int DEFAULT_TTL_MINUTES = 60;
        /**
         * 对话线程缓存过期时间（分钟）
         */
        public static final int THREAD_TTL_MINUTES = 120;
        /**
         * 消息内容缓存过期时间（分钟）
         */
        public static final int MESSAGE_TTL_MINUTES = 30;
        /**
         * 用户会话缓存过期时间（分钟）
         */
        public static final int SESSION_TTL_MINUTES = 480; // 8小时
        /**
         * 速率限制缓存过期时间（分钟）
         */
        public static final int RATE_LIMIT_TTL_MINUTES = 60;
        /**
         * 模型状态缓存过期时间（分钟）
         */
        public static final int MODEL_STATUS_TTL_MINUTES = 15;
        /**
         * 统计信息缓存过期时间（分钟）
         */
        public static final int STATISTICS_TTL_MINUTES = 30;
        /**
         * 建议问题缓存过期时间（分钟）
         */
        public static final int SUGGESTIONS_TTL_MINUTES = 60;
    }

    // =================== 审计操作类型 ===================

    /**
     * 审计操作类型常量
     *
     * <p>全面的审计日志是AI系统可信度和合规性的重要保障。
     * 通过记录每个关键操作，我们可以实现完整的操作追溯和行为分析。</p>
     */
    public static final class AuditActions {
        // 对话线程操作
        public static final String THREAD_CREATED = "THREAD_CREATED";
        public static final String THREAD_UPDATED = "THREAD_UPDATED";
        public static final String THREAD_DELETED = "THREAD_DELETED";
        public static final String THREAD_ACCESSED = "THREAD_ACCESSED";

        // 消息操作
        public static final String MESSAGE_SENT = "MESSAGE_SENT";
        public static final String MESSAGE_RECEIVED = "MESSAGE_RECEIVED";
        public static final String MESSAGE_REGENERATED = "MESSAGE_REGENERATED";
        public static final String MESSAGE_DELETED = "MESSAGE_DELETED";

        // AI交互操作
        public static final String AI_REQUEST_SENT = "AI_REQUEST_SENT";
        public static final String AI_RESPONSE_RECEIVED = "AI_RESPONSE_RECEIVED";
        public static final String MODEL_SWITCHED = "MODEL_SWITCHED";
        public static final String TEMPERATURE_ADJUSTED = "TEMPERATURE_ADJUSTED";

        // 流式处理操作
        public static final String STREAM_STARTED = "STREAM_STARTED";
        public static final String STREAM_COMPLETED = "STREAM_COMPLETED";
        public static final String STREAM_INTERRUPTED = "STREAM_INTERRUPTED";
        public static final String STREAM_ERROR = "STREAM_ERROR";

        // 功能集成操作
        public static final String KNOWLEDGE_RETRIEVED = "KNOWLEDGE_RETRIEVED";
        public static final String TOOL_CALLED = "TOOL_CALLED";
        public static final String FLOW_EXECUTED = "FLOW_EXECUTED";

        // 用户交互操作
        public static final String FEEDBACK_SUBMITTED = "FEEDBACK_SUBMITTED";
        public static final String SUGGESTION_REQUESTED = "SUGGESTION_REQUESTED";
        public static final String SEARCH_PERFORMED = "SEARCH_PERFORMED";

        // 系统管理操作
        public static final String RATE_LIMIT_TRIGGERED = "RATE_LIMIT_TRIGGERED";
        public static final String QUOTA_EXCEEDED = "QUOTA_EXCEEDED";
        public static final String SERVICE_HEALTH_CHECKED = "SERVICE_HEALTH_CHECKED";

        // 安全相关操作
        public static final String UNAUTHORIZED_ACCESS_ATTEMPT = "UNAUTHORIZED_ACCESS_ATTEMPT";
        public static final String SUSPICIOUS_ACTIVITY_DETECTED = "SUSPICIOUS_ACTIVITY_DETECTED";
        public static final String CONTENT_FILTERED = "CONTENT_FILTERED";
    }

    // =================== 默认配置值 ===================

    /**
     * 默认配置值常量
     *
     * <p>精心设计的默认值能够让系统在大多数场景下开箱即用，
     * 同时为高级用户提供充分的定制空间。这些默认值是基于
     * 大量用户研究和性能测试得出的最佳实践。</p>
     */
    public static final class Defaults {
        /**
         * 默认AI模型
         */
        public static final String DEFAULT_MODEL = "gpt-3.5-turbo";
        /**
         * 默认温度参数
         */
        public static final double DEFAULT_TEMPERATURE = 0.7;
        /**
         * 默认最大输出Token数
         */
        public static final int DEFAULT_MAX_TOKENS = 2048;
        /**
         * 默认最大输入Token数
         */
        public static final int DEFAULT_MAX_INPUT_TOKENS = 8192;

        /**
         * 默认异步执行线程池大小
         */
        public static final int DEFAULT_ASYNC_POOL_SIZE = 10;
        /**
         * 默认流式响应缓冲区大小
         */
        public static final int DEFAULT_STREAM_BUFFER_SIZE = 1024;

        /**
         * 默认知识检索阈值
         */
        public static final double DEFAULT_KNOWLEDGE_THRESHOLD = 0.7;
        /**
         * 默认知识检索数量
         */
        public static final int DEFAULT_KNOWLEDGE_TOP_K = 5;
        /**
         * 默认向量模型
         */
        public static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small";

        /**
         * 默认速率限制窗口（分钟）
         */
        public static final int DEFAULT_RATE_LIMIT_WINDOW = 1;
        /**
         * 默认速率限制最大次数
         */
        public static final int DEFAULT_RATE_LIMIT_MAX = 30;

        /**
         * 默认会话超时时间（分钟）
         */
        public static final int DEFAULT_SESSION_TIMEOUT_MINUTES = 480; // 8小时
        /**
         * 默认连接超时时间（秒）
         */
        public static final int DEFAULT_CONNECTION_TIMEOUT_SECONDS = 30;
        /**
         * 默认读取超时时间（秒）
         */
        public static final int DEFAULT_READ_TIMEOUT_SECONDS = 120;

        /**
         * 默认重试次数
         */
        public static final int DEFAULT_RETRY_COUNT = 3;
        /**
         * 默认重试间隔（秒）
         */
        public static final int DEFAULT_RETRY_INTERVAL_SECONDS = 5;
        /**
         * 默认重试退避倍数
         */
        public static final double DEFAULT_RETRY_BACKOFF_MULTIPLIER = 2.0;

        /**
         * 默认时区
         */
        public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
        /**
         * 默认语言
         */
        public static final String DEFAULT_LANGUAGE = "zh-CN";
        /**
         * 默认编码
         */
        public static final String DEFAULT_CHARSET = "UTF-8";

        /**
         * 默认线程标题
         */
        public static final String DEFAULT_THREAD_TITLE = "新对话";
        /**
         * 默认系统提示词
         */
        public static final String DEFAULT_SYSTEM_PROMPT = "你是一个有用的AI助手，请为用户提供准确、有帮助的回答。";
    }

    // =================== 正则表达式模式 ===================

    /**
     * 正则表达式模式常量
     *
     * <p>标准化的验证模式确保数据的一致性和安全性。
     * 这些模式基于实际业务需求和安全最佳实践设计。</p>
     */
    public static final class Patterns {
        /**
         * 对话线程标题模式 - 支持中英文、数字、标点符号
         */
        public static final String THREAD_TITLE_PATTERN = "^[\\u4e00-\\u9fa5a-zA-Z0-9\\s\\p{P}]{1,256}$";
        /**
         * 模型名称模式 - 只允许字母、数字、连字符、下划线
         */
        public static final String MODEL_NAME_PATTERN = "^[a-zA-Z0-9_-]{1,128}$";
        /**
         * 时间范围模式 - 支持1h、1d、7d、30d等格式
         */
        public static final String TIME_RANGE_PATTERN = "^\\d+[hdwmy]$";
        /**
         * 温度参数模式 - 0.0到2.0之间的小数
         */
        public static final String TEMPERATURE_PATTERN = "^([01]?(\\.[0-9]+)?|2(\\.0+)?)$";
        /**
         * Token数量模式 - 正整数
         */
        public static final String TOKEN_COUNT_PATTERN = "^[1-9]\\d*$";
        /**
         * 评分模式 - 1到5的整数
         */
        public static final String RATING_PATTERN = "^[1-5]$";
        /**
         * 用户ID模式 - 正整数
         */
        public static final String USER_ID_PATTERN = "^[1-9]\\d*$";
        /**
         * 租户ID模式 - 正整数
         */
        public static final String TENANT_ID_PATTERN = "^[1-9]\\d*$";
        /**
         * JSON格式验证模式 - 简单的JSON格式检查
         */
        public static final String JSON_PATTERN = "^\\s*[{\\[].*[}\\]]\\s*$";
    }

    // =================== 系统限制常量 ===================

    /**
     * 系统限制常量
     *
     * <p>这些限制是基于系统性能、稳定性和成本控制的综合考虑。
     * 它们确保系统在各种负载情况下都能稳定运行。</p>
     */
    public static final class SystemLimits {
        /**
         * 最大并发对话处理数
         */
        public static final int MAX_CONCURRENT_CONVERSATIONS = 1000;
        /**
         * 最大并发流式连接数
         */
        public static final int MAX_CONCURRENT_STREAMS = 500;
        /**
         * 最大异步任务队列大小
         */
        public static final int MAX_ASYNC_QUEUE_SIZE = 10000;

        /**
         * 单次批处理最大消息数
         */
        public static final int MAX_BATCH_MESSAGE_COUNT = 100;
        /**
         * 最大上下文窗口大小（Token数）
         */
        public static final int MAX_CONTEXT_WINDOW_TOKENS = 128000;
        /**
         * 最大单次响应Token数
         */
        public static final int MAX_RESPONSE_TOKENS = 4096;

        /**
         * 慢查询阈值（毫秒）
         */
        public static final long SLOW_QUERY_THRESHOLD_MS = 1000;
        /**
         * 慢响应阈值（毫秒）
         */
        public static final long SLOW_RESPONSE_THRESHOLD_MS = 5000;
        /**
         * AI模型调用超时阈值（毫秒）
         */
        public static final long MODEL_CALL_TIMEOUT_MS = 60000;

        /**
         * 内存使用警告阈值（百分比）
         */
        public static final int MEMORY_WARNING_THRESHOLD = 80;
        /**
         * CPU使用警告阈值（百分比）
         */
        public static final int CPU_WARNING_THRESHOLD = 85;
        /**
         * 磁盘使用警告阈值（百分比）
         */
        public static final int DISK_WARNING_THRESHOLD = 90;

        /**
         * 每秒最大API调用次数
         */
        public static final int MAX_API_CALLS_PER_SECOND = 100;
        /**
         * 每分钟最大AI模型调用次数
         */
        public static final int MAX_MODEL_CALLS_PER_MINUTE = 1000;

        /**
         * 流式响应最大持续时间（秒）
         */
        public static final int MAX_STREAM_DURATION_SECONDS = 300;
        /**
         * 会话最大空闲时间（分钟）
         */
        public static final int MAX_SESSION_IDLE_MINUTES = 30;
    }

    // =================== 度量指标名称 ===================

    /**
     * 度量指标名称常量
     *
     * <p>标准化的指标名称体系是构建可观测性系统的基础。
     * 通过这些指标，我们可以全面监控系统的健康状况和性能表现。</p>
     */
    public static final class MetricNames {
        /**
         * 指标名称前缀
         */
        public static final String PREFIX = "chat.";

        // 对话线程相关指标
        public static final String THREAD_CREATE_COUNT = PREFIX + "thread.create.count";
        public static final String THREAD_CREATE_TIME = PREFIX + "thread.create.time";
        public static final String THREAD_UPDATE_COUNT = PREFIX + "thread.update.count";
        public static final String THREAD_DELETE_COUNT = PREFIX + "thread.delete.count";
        public static final String ACTIVE_THREADS_COUNT = PREFIX + "thread.active.count";

        // 消息处理相关指标
        public static final String MESSAGE_SEND_COUNT = PREFIX + "message.send.count";
        public static final String MESSAGE_SEND_TIME = PREFIX + "message.send.time";
        public static final String MESSAGE_SUCCESS_COUNT = PREFIX + "message.success.count";
        public static final String MESSAGE_FAILURE_COUNT = PREFIX + "message.failure.count";
        public static final String MESSAGE_PROCESSING_TIME = PREFIX + "message.processing.time";

        // AI模型相关指标
        public static final String MODEL_REQUEST_COUNT = PREFIX + "model.request.count";
        public static final String MODEL_REQUEST_TIME = PREFIX + "model.request.time";
        public static final String MODEL_SUCCESS_RATE = PREFIX + "model.success.rate";
        public static final String MODEL_ERROR_COUNT = PREFIX + "model.error.count";
        public static final String MODEL_TOKEN_USAGE = PREFIX + "model.token.usage";
        public static final String MODEL_COST = PREFIX + "model.cost";

        // 流式处理相关指标
        public static final String STREAM_CONNECTION_COUNT = PREFIX + "stream.connection.count";
        public static final String STREAM_DURATION = PREFIX + "stream.duration";
        public static final String STREAM_DISCONNECT_COUNT = PREFIX + "stream.disconnect.count";
        public static final String STREAM_ERROR_COUNT = PREFIX + "stream.error.count";

        // 功能集成相关指标
        public static final String KNOWLEDGE_RETRIEVAL_COUNT = PREFIX + "knowledge.retrieval.count";
        public static final String KNOWLEDGE_RETRIEVAL_TIME = PREFIX + "knowledge.retrieval.time";
        public static final String TOOL_CALL_COUNT = PREFIX + "tool.call.count";
        public static final String TOOL_CALL_TIME = PREFIX + "tool.call.time";

        // 速率限制相关指标
        public static final String RATE_LIMIT_HIT_COUNT = PREFIX + "rate.limit.hit.count";
        public static final String RATE_LIMIT_BLOCK_COUNT = PREFIX + "rate.limit.block.count";

        // 用户体验相关指标
        public static final String USER_SATISFACTION_SCORE = PREFIX + "user.satisfaction.score";
        public static final String RESPONSE_QUALITY_SCORE = PREFIX + "response.quality.score";
        public static final String REGENERATION_COUNT = PREFIX + "regeneration.count";

        // 系统性能指标
        public static final String SYSTEM_MEMORY_USAGE = PREFIX + "system.memory.usage";
        public static final String SYSTEM_CPU_USAGE = PREFIX + "system.cpu.usage";
        public static final String CONCURRENT_USERS = PREFIX + "system.concurrent.users";
        public static final String QUEUE_SIZE = PREFIX + "system.queue.size";

        // API性能指标
        public static final String API_REQUEST_COUNT = PREFIX + "api.request.count";
        public static final String API_RESPONSE_TIME = PREFIX + "api.response.time";
        public static final String API_ERROR_RATE = PREFIX + "api.error.rate";
    }

    // =================== 时间范围常量 ===================

    /**
     * 时间范围常量
     *
     * <p>标准化的时间范围定义便于统计分析和数据聚合。
     * 这些范围涵盖了从实时监控到长期趋势分析的各种需求。</p>
     */
    public static final class TimeRanges {
        /**
         * 最近1小时
         */
        public static final String LAST_HOUR = "1h";
        /**
         * 最近1天
         */
        public static final String LAST_DAY = "1d";
        /**
         * 最近3天
         */
        public static final String LAST_3_DAYS = "3d";
        /**
         * 最近1周
         */
        public static final String LAST_WEEK = "7d";
        /**
         * 最近2周
         */
        public static final String LAST_2_WEEKS = "14d";
        /**
         * 最近1个月
         */
        public static final String LAST_MONTH = "30d";
        /**
         * 最近3个月
         */
        public static final String LAST_QUARTER = "90d";
        /**
         * 最近6个月
         */
        public static final String LAST_HALF_YEAR = "180d";
        /**
         * 最近1年
         */
        public static final String LAST_YEAR = "365d";
    }

    // =================== 质量评估常量 ===================

    /**
     * 质量评估常量
     *
     * <p>AI对话系统的质量评估是一个多维度的复杂过程。
     * 这些常量为质量评估提供了标准化的框架。</p>
     */
    public static final class QualityMetrics {
        /**
         * 响应相关性评分阈值
         */
        public static final double RELEVANCE_THRESHOLD = 0.8;
        /**
         * 响应准确性评分阈值
         */
        public static final double ACCURACY_THRESHOLD = 0.85;
        /**
         * 响应完整性评分阈值
         */
        public static final double COMPLETENESS_THRESHOLD = 0.75;
        /**
         * 响应流畅性评分阈值
         */
        public static final double FLUENCY_THRESHOLD = 0.9;

        /**
         * 用户满意度优秀阈值
         */
        public static final double USER_SATISFACTION_EXCELLENT = 4.5;
        /**
         * 用户满意度良好阈值
         */
        public static final double USER_SATISFACTION_GOOD = 3.5;
        /**
         * 用户满意度及格阈值
         */
        public static final double USER_SATISFACTION_ACCEPTABLE = 2.5;

        /**
         * 响应时间优秀阈值（毫秒）
         */
        public static final long RESPONSE_TIME_EXCELLENT_MS = 1000;
        /**
         * 响应时间良好阈值（毫秒）
         */
        public static final long RESPONSE_TIME_GOOD_MS = 3000;
        /**
         * 响应时间可接受阈值（毫秒）
         */
        public static final long RESPONSE_TIME_ACCEPTABLE_MS = 10000;
    }
}