package com.cloud.baseai.infrastructure.constants;

/**
 * <h2>MCP工具协议常量定义</h2>
 *
 * <p>集中定义MCP（Model Context Protocol）工具系统使用的所有常量，
 * 确保工具管理生态系统的一致性和可维护性。</p>
 */
public final class McpConstants {

    private McpConstants() {
        throw new UnsupportedOperationException("常量类不能被实例化");
    }

    // =================== 工具类型和分类 ===================

    /**
     * 工具类型常量
     *
     * <p>不同类型的工具代表了AI系统与外部世界交互的不同方式。
     * 每种工具类型都有其特定的能力边界和适用场景，就像人类使用
     * 不同的工具来完成不同类型的任务。</p>
     */
    public static final class ToolTypes {
        /**
         * HTTP工具 - 通过HTTP协议调用外部API服务
         */
        public static final String HTTP = "HTTP";
        /**
         * 数据库工具 - 直接操作数据库进行查询和更新
         */
        public static final String DATABASE = "DATABASE";
        /**
         * 文件系统工具 - 处理文件的读取、写入和管理
         */
        public static final String FILE_SYSTEM = "FILE_SYSTEM";
        /**
         * 脚本工具 - 执行Python、JavaScript等脚本代码
         */
        public static final String SCRIPT = "SCRIPT";
        /**
         * 系统命令工具 - 执行操作系统级别的命令
         */
        public static final String SYSTEM_COMMAND = "SYSTEM_COMMAND";
        /**
         * 消息工具 - 发送邮件、短信、推送通知等
         */
        public static final String MESSAGING = "MESSAGING";
        /**
         * 计算工具 - 执行数学计算、数据分析等
         */
        public static final String COMPUTATION = "COMPUTATION";
        /**
         * 图像处理工具 - 处理图片、生成图表等
         */
        public static final String IMAGE_PROCESSING = "IMAGE_PROCESSING";
        /**
         * 文档处理工具 - 处理PDF、Word、Excel等文档
         */
        public static final String DOCUMENT_PROCESSING = "DOCUMENT_PROCESSING";
        /**
         * 网络爬虫工具 - 抓取和解析网页内容
         */
        public static final String WEB_SCRAPING = "WEB_SCRAPING";
        /**
         * 第三方集成工具 - 集成Slack、GitHub、Jira等第三方服务
         */
        public static final String THIRD_PARTY_INTEGRATION = "THIRD_PARTY_INTEGRATION";
        /**
         * 自定义工具 - 用户自定义的专用工具
         */
        public static final String CUSTOM = "CUSTOM";
    }

    /**
     * 工具分类标签常量
     *
     * <p>工具分类标签帮助用户快速理解工具的功能领域，
     * 类似于应用商店中的应用分类。这种分类方式便于
     * 工具的发现、管理和推荐。</p>
     */
    public static final class ToolCategories {
        /**
         * 数据处理类工具
         */
        public static final String DATA_PROCESSING = "DATA_PROCESSING";
        /**
         * 通讯协作类工具
         */
        public static final String COMMUNICATION = "COMMUNICATION";
        /**
         * 开发运维类工具
         */
        public static final String DEVELOPMENT = "DEVELOPMENT";
        /**
         * 业务流程类工具
         */
        public static final String BUSINESS_PROCESS = "BUSINESS_PROCESS";
        /**
         * 内容创作类工具
         */
        public static final String CONTENT_CREATION = "CONTENT_CREATION";
        /**
         * 分析报告类工具
         */
        public static final String ANALYTICS = "ANALYTICS";
        /**
         * 安全管理类工具
         */
        public static final String SECURITY = "SECURITY";
        /**
         * 系统监控类工具
         */
        public static final String MONITORING = "MONITORING";
    }

    // =================== 工具状态和执行状态 ===================

    /**
     * 工具调用状态常量
     *
     * <p>工具调用的生命周期管理是MCP系统可靠性的关键。
     * 每个状态都代表调用过程中的一个关键节点，帮助我们
     * 精确追踪和管理工具的执行过程。</p>
     */
    public static final class CallStatus {
        /**
         * 等待执行 - 调用请求已接收，等待资源分配
         */
        public static final String PENDING = "PENDING";
        /**
         * 正在执行 - 工具正在运行中
         */
        public static final String RUNNING = "RUNNING";
        /**
         * 执行成功 - 工具成功完成并返回结果
         */
        public static final String SUCCESS = "SUCCESS";
        /**
         * 执行失败 - 工具执行过程中遇到错误
         */
        public static final String FAILED = "FAILED";
        /**
         * 执行超时 - 工具执行时间超过预设限制
         */
        public static final String TIMEOUT = "TIMEOUT";
        /**
         * 已取消 - 用户或系统主动取消执行
         */
        public static final String CANCELLED = "CANCELLED";
        /**
         * 重试中 - 失败后正在进行重试
         */
        public static final String RETRYING = "RETRYING";
    }

    /**
     * 工具启用状态常量
     */
    public static final class EnableStatus {
        /**
         * 已启用 - 工具可以正常使用
         */
        public static final String ENABLED = "ENABLED";
        /**
         * 已禁用 - 工具暂时不可用
         */
        public static final String DISABLED = "DISABLED";
        /**
         * 维护中 - 工具正在维护，暂时不可用
         */
        public static final String MAINTENANCE = "MAINTENANCE";
        /**
         * 已弃用 - 工具不再推荐使用，但向后兼容
         */
        public static final String DEPRECATED = "DEPRECATED";
    }

    /**
     * 工具优先级常量
     */
    public static final class Priority {
        /**
         * 低优先级 - 非紧急的后台任务
         */
        public static final String LOW = "LOW";
        /**
         * 普通优先级 - 常规业务操作
         */
        public static final String NORMAL = "NORMAL";
        /**
         * 高优先级 - 重要业务操作
         */
        public static final String HIGH = "HIGH";
        /**
         * 紧急优先级 - 关键业务操作
         */
        public static final String URGENT = "URGENT";
    }

    // =================== 认证和安全常量 ===================

    /**
     * 认证类型常量
     *
     * <p>不同的认证类型代表了不同的安全级别和适用场景。
     * 选择合适的认证方式是确保工具调用安全性的重要基础。</p>
     */
    public static final class AuthTypes {
        /**
         * 无认证 - 适用于公开的、无敏感数据的工具
         */
        public static final String NONE = "NONE";
        /**
         * API密钥认证 - 最常见的API认证方式
         */
        public static final String API_KEY = "API_KEY";
        /**
         * Bearer Token认证 - 基于令牌的认证
         */
        public static final String BEARER_TOKEN = "BEARER_TOKEN";
        /**
         * OAuth2认证 - 标准的OAuth2.0认证流程
         */
        public static final String OAUTH2 = "OAUTH2";
        /**
         * 基础认证 - HTTP Basic认证
         */
        public static final String BASIC_AUTH = "BASIC_AUTH";
        /**
         * JWT认证 - 基于JSON Web Token的认证
         */
        public static final String JWT = "JWT";
        /**
         * 证书认证 - 基于数字证书的双向认证
         */
        public static final String CERTIFICATE = "CERTIFICATE";
        /**
         * 自定义认证 - 特殊的认证方式
         */
        public static final String CUSTOM = "CUSTOM";
    }

    /**
     * 权限级别常量
     */
    public static final class PermissionLevels {
        /**
         * 只读权限 - 只能查询和读取数据
         */
        public static final String READ_ONLY = "READ_ONLY";
        /**
         * 读写权限 - 可以查询和修改数据
         */
        public static final String READ_WRITE = "READ_WRITE";
        /**
         * 管理权限 - 可以执行管理操作
         */
        public static final String ADMIN = "ADMIN";
        /**
         * 超级权限 - 拥有所有权限
         */
        public static final String SUPER = "SUPER";
    }

    // =================== 字段长度限制 ===================

    /**
     * 字段长度限制常量
     *
     * <p>合理的字段长度限制是系统性能和用户体验的重要保障。
     * 这些限制基于实际使用场景和技术约束精心设计，既能满足
     * 复杂工具的配置需求，又能确保系统的稳定性。</p>
     */
    public static final class FieldLengths {
        /**
         * 工具代码最大长度 - 保持简洁但具有表达性
         */
        public static final int MAX_TOOL_CODE_LENGTH = 64;
        /**
         * 工具代码最小长度
         */
        public static final int MIN_TOOL_CODE_LENGTH = 2;

        /**
         * 工具名称最大长度
         */
        public static final int MAX_TOOL_NAME_LENGTH = 128;
        /**
         * 工具名称最小长度
         */
        public static final int MIN_TOOL_NAME_LENGTH = 2;

        /**
         * 工具描述最大长度
         */
        public static final int MAX_TOOL_DESCRIPTION_LENGTH = 1000;

        /**
         * 图标URL最大长度
         */
        public static final int MAX_ICON_URL_LENGTH = 512;

        /**
         * 端点URL最大长度
         */
        public static final int MAX_ENDPOINT_URL_LENGTH = 1024;

        /**
         * 参数Schema最大长度 - 支持复杂的参数定义
         */
        public static final int MAX_PARAM_SCHEMA_LENGTH = 10240;
        /**
         * 结果Schema最大长度
         */
        public static final int MAX_RESULT_SCHEMA_LENGTH = 10240;

        /**
         * API密钥最大长度
         */
        public static final int MAX_API_KEY_LENGTH = 256;

        /**
         * 工具标签最大长度
         */
        public static final int MAX_TAG_LENGTH = 32;
        /**
         * 单个工具最大标签数
         */
        public static final int MAX_TAGS_PER_TOOL = 10;

        /**
         * 错误消息最大长度
         */
        public static final int MAX_ERROR_MESSAGE_LENGTH = 2048;

        /**
         * 执行结果最大长度
         */
        public static final int MAX_EXECUTION_RESULT_LENGTH = 1048576; // 1MB

        /**
         * 调用参数最大长度
         */
        public static final int MAX_CALL_PARAMS_LENGTH = 102400; // 100KB
    }

    // =================== 业务限制常量 ===================

    /**
     * 业务限制常量
     *
     * <p>这些限制是基于系统容量、安全考虑和用户体验的综合平衡。
     * 它们就像交通规则一样，既保证了系统的有序运行，
     * 又为用户提供了充分的使用空间。</p>
     */
    public static final class BusinessLimits {
        /**
         * 系统最大工具数量
         */
        public static final int MAX_TOOLS_TOTAL = 10000;
        /**
         * 每个租户最大授权工具数
         */
        public static final int MAX_TOOLS_PER_TENANT = 100;
        /**
         * 每个租户最大自定义工具数
         */
        public static final int MAX_CUSTOM_TOOLS_PER_TENANT = 20;

        /**
         * 默认工具调用配额（每天）
         */
        public static final int DEFAULT_DAILY_QUOTA = 1000;
        /**
         * 最大工具调用配额（每天）
         */
        public static final int MAX_DAILY_QUOTA = 100000;

        /**
         * 每分钟最大工具调用次数（单用户）
         */
        public static final int MAX_CALLS_PER_MINUTE_USER = 60;
        /**
         * 每小时最大工具调用次数（单用户）
         */
        public static final int MAX_CALLS_PER_HOUR_USER = 1000;
        /**
         * 每天最大工具调用次数（单用户）
         */
        public static final int MAX_CALLS_PER_DAY_USER = 10000;

        /**
         * 每分钟最大工具调用次数（单租户）
         */
        public static final int MAX_CALLS_PER_MINUTE_TENANT = 1000;
        /**
         * 每小时最大工具调用次数（单租户）
         */
        public static final int MAX_CALLS_PER_HOUR_TENANT = 50000;

        /**
         * 并发工具调用数量限制
         */
        public static final int MAX_CONCURRENT_CALLS = 1000;
        /**
         * 单个工具最大并发调用数
         */
        public static final int MAX_CONCURRENT_CALLS_PER_TOOL = 50;

        /**
         * 工具执行超时时间（秒） - 默认值
         */
        public static final int DEFAULT_EXECUTION_TIMEOUT_SECONDS = 300; // 5分钟
        /**
         * 工具执行超时时间（秒） - 最大值
         */
        public static final int MAX_EXECUTION_TIMEOUT_SECONDS = 3600; // 1小时
        /**
         * 工具执行超时时间（秒） - 最小值
         */
        public static final int MIN_EXECUTION_TIMEOUT_SECONDS = 5;

        /**
         * 异步调用结果保留时间（小时）
         */
        public static final int ASYNC_RESULT_RETENTION_HOURS = 72;
        /**
         * 调用日志保留天数
         */
        public static final int CALL_LOG_RETENTION_DAYS = 90;
        /**
         * 工具版本历史保留数量
         */
        public static final int TOOL_VERSION_RETENTION_COUNT = 10;

        /**
         * 最大重试次数
         */
        public static final int MAX_RETRY_COUNT = 5;
        /**
         * 默认重试次数
         */
        public static final int DEFAULT_RETRY_COUNT = 3;

        /**
         * 工具热度计算窗口（天）
         */
        public static final int TOOL_POPULARITY_WINDOW_DAYS = 30;
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
         * 工具列表默认大小
         */
        public static final int DEFAULT_TOOL_LIST_SIZE = 15;
        /**
         * 调用历史默认大小
         */
        public static final int DEFAULT_CALL_HISTORY_SIZE = 50;
        /**
         * 搜索结果最大数量
         */
        public static final int MAX_SEARCH_RESULTS = 500;
    }

    // =================== 缓存键常量 ===================

    /**
     * 缓存键前缀常量
     *
     * <p>精心设计的缓存策略是高性能MCP系统的核心。这些缓存键
     * 采用层级结构设计，便于批量操作和精确控制缓存失效。</p>
     */
    public static final class CacheKeys {
        /**
         * 缓存键根前缀
         */
        public static final String PREFIX = "mcp:";

        /**
         * 工具信息缓存键前缀
         */
        public static final String TOOL = PREFIX + "tool:";
        /**
         * 工具配置缓存键前缀
         */
        public static final String TOOL_CONFIG = PREFIX + "tool:config:";
        /**
         * 工具状态缓存键前缀
         */
        public static final String TOOL_STATUS = PREFIX + "tool:status:";

        /**
         * 租户授权缓存键前缀
         */
        public static final String AUTH = PREFIX + "auth:";
        /**
         * 租户工具列表缓存键前缀
         */
        public static final String TENANT_TOOLS = PREFIX + "tenant:tools:";

        /**
         * 调用历史缓存键前缀
         */
        public static final String CALL_LOG = PREFIX + "call:";
        /**
         * 调用结果缓存键前缀
         */
        public static final String CALL_RESULT = PREFIX + "result:";

        /**
         * 配额使用缓存键前缀
         */
        public static final String QUOTA = PREFIX + "quota:";
        /**
         * 速率限制缓存键前缀
         */
        public static final String RATE_LIMIT = PREFIX + "rate:";

        /**
         * 统计信息缓存键前缀
         */
        public static final String STATISTICS = PREFIX + "stats:";
        /**
         * 热门工具缓存键前缀
         */
        public static final String HOT_TOOLS = PREFIX + "hot:";

        /**
         * 健康状态缓存键前缀
         */
        public static final String HEALTH = PREFIX + "health:";
        /**
         * 性能指标缓存键前缀
         */
        public static final String METRICS = PREFIX + "metrics:";
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
         * 工具信息缓存过期时间（分钟）
         */
        public static final int TOOL_TTL_MINUTES = 240; // 4小时
        /**
         * 工具配置缓存过期时间（分钟）
         */
        public static final int TOOL_CONFIG_TTL_MINUTES = 120; // 2小时
        /**
         * 授权信息缓存过期时间（分钟）
         */
        public static final int AUTH_TTL_MINUTES = 60;
        /**
         * 调用结果缓存过期时间（分钟）
         */
        public static final int CALL_RESULT_TTL_MINUTES = 30;
        /**
         * 配额使用缓存过期时间（分钟）
         */
        public static final int QUOTA_TTL_MINUTES = 15;
        /**
         * 速率限制缓存过期时间（分钟）
         */
        public static final int RATE_LIMIT_TTL_MINUTES = 60;
        /**
         * 统计信息缓存过期时间（分钟）
         */
        public static final int STATISTICS_TTL_MINUTES = 30;
        /**
         * 热门工具缓存过期时间（分钟）
         */
        public static final int HOT_TOOLS_TTL_MINUTES = 60;
        /**
         * 健康状态缓存过期时间（分钟）
         */
        public static final int HEALTH_TTL_MINUTES = 5;
    }

    // =================== 审计操作类型 ===================

    /**
     * 审计操作类型常量
     *
     * <p>全面的审计体系是企业级工具管理系统的重要特征。
     * 通过记录每个关键操作，我们可以实现完整的可追溯性
     * 和合规性要求。</p>
     */
    public static final class AuditActions {
        // 工具管理操作
        public static final String TOOL_REGISTERED = "TOOL_REGISTERED";
        public static final String TOOL_UPDATED = "TOOL_UPDATED";
        public static final String TOOL_DELETED = "TOOL_DELETED";
        public static final String TOOL_ENABLED = "TOOL_ENABLED";
        public static final String TOOL_DISABLED = "TOOL_DISABLED";
        public static final String TOOL_VERSION_CREATED = "TOOL_VERSION_CREATED";

        // 租户授权操作
        public static final String TENANT_AUTHORIZED = "TENANT_AUTHORIZED";
        public static final String AUTHORIZATION_REVOKED = "AUTHORIZATION_REVOKED";
        public static final String AUTHORIZATION_UPDATED = "AUTHORIZATION_UPDATED";
        public static final String QUOTA_UPDATED = "QUOTA_UPDATED";

        // 工具调用操作
        public static final String TOOL_CALLED = "TOOL_CALLED";
        public static final String TOOL_CALL_SUCCESS = "TOOL_CALL_SUCCESS";
        public static final String TOOL_CALL_FAILED = "TOOL_CALL_FAILED";
        public static final String TOOL_CALL_TIMEOUT = "TOOL_CALL_TIMEOUT";
        public static final String TOOL_CALL_CANCELLED = "TOOL_CALL_CANCELLED";
        public static final String TOOL_CALL_RETRIED = "TOOL_CALL_RETRIED";

        // 认证相关操作
        public static final String AUTH_SUCCESS = "AUTH_SUCCESS";
        public static final String AUTH_FAILED = "AUTH_FAILED";
        public static final String API_KEY_UPDATED = "API_KEY_UPDATED";
        public static final String TOKEN_REFRESHED = "TOKEN_REFRESHED";

        // 配额和限制操作
        public static final String QUOTA_EXCEEDED = "QUOTA_EXCEEDED";
        public static final String RATE_LIMIT_TRIGGERED = "RATE_LIMIT_TRIGGERED";
        public static final String QUOTA_RESET = "QUOTA_RESET";

        // 系统管理操作
        public static final String SYSTEM_HEALTH_CHECKED = "SYSTEM_HEALTH_CHECKED";
        public static final String MAINTENANCE_MODE_ENABLED = "MAINTENANCE_MODE_ENABLED";
        public static final String MAINTENANCE_MODE_DISABLED = "MAINTENANCE_MODE_DISABLED";

        // 安全相关操作
        public static final String SUSPICIOUS_ACTIVITY_DETECTED = "SUSPICIOUS_ACTIVITY_DETECTED";
        public static final String SECURITY_BREACH_ATTEMPT = "SECURITY_BREACH_ATTEMPT";
        public static final String UNAUTHORIZED_ACCESS_BLOCKED = "UNAUTHORIZED_ACCESS_BLOCKED";
    }

    // =================== 默认配置值 ===================

    /**
     * 默认配置值常量
     *
     * <p>合理的默认值让系统能够开箱即用，同时为高级用户
     * 提供充分的定制空间。这些默认值基于最佳实践和
     * 大量的生产环境经验总结。</p>
     */
    public static final class Defaults {
        /**
         * 默认工具类型
         */
        public static final String DEFAULT_TOOL_TYPE = "HTTP";
        /**
         * 默认认证类型
         */
        public static final String DEFAULT_AUTH_TYPE = "API_KEY";
        /**
         * 默认权限级别
         */
        public static final String DEFAULT_PERMISSION_LEVEL = "READ_ONLY";

        /**
         * 默认执行超时时间（秒）
         */
        public static final int DEFAULT_TIMEOUT_SECONDS = 300;
        /**
         * 默认连接超时时间（秒）
         */
        public static final int DEFAULT_CONNECTION_TIMEOUT_SECONDS = 30;
        /**
         * 默认读取超时时间（秒）
         */
        public static final int DEFAULT_READ_TIMEOUT_SECONDS = 60;

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
         * 默认每日配额
         */
        public static final int DEFAULT_DAILY_QUOTA = 1000;
        /**
         * 默认缓存时间（分钟）
         */
        public static final int DEFAULT_CACHE_TTL_MINUTES = 60;

        /**
         * 默认异步执行线程池大小
         */
        public static final int DEFAULT_ASYNC_POOL_SIZE = 20;
        /**
         * 默认连接池大小
         */
        public static final int DEFAULT_CONNECTION_POOL_SIZE = 50;

        /**
         * 默认工具图标URL
         */
        public static final String DEFAULT_TOOL_ICON = "/icons/default-tool.svg";
        /**
         * 默认工具分类
         */
        public static final String DEFAULT_TOOL_CATEGORY = "CUSTOM";

        /**
         * 默认优先级
         */
        public static final String DEFAULT_PRIORITY = "NORMAL";
        /**
         * 默认启用状态
         */
        public static final boolean DEFAULT_ENABLED_STATUS = true;

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
    }

    // =================== 正则表达式模式 ===================

    /**
     * 正则表达式模式常量
     *
     * <p>标准化的验证模式确保数据的一致性和有效性。
     * 这些模式基于业务需求和安全考虑精心设计。</p>
     */
    public static final class Patterns {
        /**
         * 工具代码模式 - 只允许字母、数字、下划线、连字符
         */
        public static final String TOOL_CODE_PATTERN = "^[a-zA-Z][a-zA-Z0-9_-]{1,63}$";
        /**
         * 工具名称模式 - 支持中英文、数字、空格、常用标点
         */
        public static final String TOOL_NAME_PATTERN = "^[\\u4e00-\\u9fa5a-zA-Z0-9\\s\\p{P}]{2,128}$";
        /**
         * URL模式 - 验证HTTP/HTTPS URL格式
         */
        public static final String URL_PATTERN = "^https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$";
        /**
         * API密钥模式 - 字母数字组合，支持特殊字符
         */
        public static final String API_KEY_PATTERN = "^[a-zA-Z0-9._-]{8,256}$";
        /**
         * 版本号模式 - 语义化版本号
         */
        public static final String VERSION_PATTERN = "^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.-]+)?$";
        /**
         * 时间范围模式 - 支持1h、1d、7d、30d等格式
         */
        public static final String TIME_RANGE_PATTERN = "^\\d+[hdwmy]$";
        /**
         * 配额值模式 - 正整数
         */
        public static final String QUOTA_PATTERN = "^[1-9]\\d{0,9}$";
        /**
         * 超时时间模式 - 5到3600秒
         */
        public static final String TIMEOUT_PATTERN = "^([5-9]|[1-9]\\d|[1-9]\\d{2}|[1-2]\\d{3}|3[0-5]\\d{2}|3600)$";
        /**
         * JSON Schema模式 - 简单的JSON格式检查
         */
        public static final String JSON_SCHEMA_PATTERN = "^\\s*\\{.*\"type\".*\\}\\s*$";
        /**
         * 租户ID模式 - 正整数
         */
        public static final String TENANT_ID_PATTERN = "^[1-9]\\d*$";
    }

    // =================== 系统限制常量 ===================

    /**
     * 系统限制常量
     *
     * <p>这些限制基于系统性能、安全性和稳定性的综合考虑。
     * 它们就像是系统的"护栏"，确保在各种极端情况下
     * 系统都能保持稳定运行。</p>
     */
    public static final class SystemLimits {
        /**
         * 最大并发工具调用数
         */
        public static final int MAX_CONCURRENT_CALLS = 1000;
        /**
         * 最大异步任务队列大小
         */
        public static final int MAX_ASYNC_QUEUE_SIZE = 10000;
        /**
         * 最大HTTP连接池大小
         */
        public static final int MAX_HTTP_POOL_SIZE = 200;

        /**
         * 最大工具配置大小（字节）
         */
        public static final int MAX_TOOL_CONFIG_SIZE_BYTES = 1048576; // 1MB
        /**
         * 最大调用参数大小（字节）
         */
        public static final int MAX_CALL_PARAMS_SIZE_BYTES = 102400; // 100KB
        /**
         * 最大执行结果大小（字节）
         */
        public static final int MAX_RESULT_SIZE_BYTES = 10485760; // 10MB

        /**
         * 慢调用阈值（毫秒）
         */
        public static final long SLOW_CALL_THRESHOLD_MS = 5000;
        /**
         * 超长调用阈值（毫秒）
         */
        public static final long VERY_SLOW_CALL_THRESHOLD_MS = 30000;

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
        public static final int MAX_API_CALLS_PER_SECOND = 1000;
        /**
         * 每分钟最大工具注册次数
         */
        public static final int MAX_TOOL_REGISTRATIONS_PER_MINUTE = 10;

        /**
         * 调用历史最大保留数量（单个工具）
         */
        public static final int MAX_CALL_HISTORY_PER_TOOL = 100000;
        /**
         * 错误日志最大保留数量
         */
        public static final int MAX_ERROR_LOGS = 50000;

        /**
         * 工具健康检查间隔（秒）
         */
        public static final int TOOL_HEALTH_CHECK_INTERVAL_SECONDS = 300;
        /**
         * 配额重置检查间隔（秒）
         */
        public static final int QUOTA_RESET_CHECK_INTERVAL_SECONDS = 3600;
    }

    // =================== 度量指标名称 ===================

    /**
     * 度量指标名称常量
     *
     * <p>全面的监控指标体系是构建可观测系统的基础。
     * 这些指标涵盖了工具调用的各个维度，帮助我们
     * 深入理解系统的运行状况和性能特征。</p>
     */
    public static final class MetricNames {
        /**
         * 指标名称前缀
         */
        public static final String PREFIX = "mcp.";

        // 工具管理相关指标
        public static final String TOOL_REGISTER_COUNT = PREFIX + "tool.register.count";
        public static final String TOOL_REGISTER_TIME = PREFIX + "tool.register.time";
        public static final String TOOL_UPDATE_COUNT = PREFIX + "tool.update.count";
        public static final String TOOL_DELETE_COUNT = PREFIX + "tool.delete.count";
        public static final String ACTIVE_TOOLS_COUNT = PREFIX + "tool.active.count";
        public static final String DISABLED_TOOLS_COUNT = PREFIX + "tool.disabled.count";

        // 工具调用相关指标
        public static final String TOOL_CALL_COUNT = PREFIX + "call.count";
        public static final String TOOL_CALL_SUCCESS_COUNT = PREFIX + "call.success.count";
        public static final String TOOL_CALL_FAILURE_COUNT = PREFIX + "call.failure.count";
        public static final String TOOL_CALL_TIMEOUT_COUNT = PREFIX + "call.timeout.count";
        public static final String TOOL_CALL_DURATION = PREFIX + "call.duration";
        public static final String TOOL_CALL_QUEUE_SIZE = PREFIX + "call.queue.size";

        // 租户授权相关指标
        public static final String AUTHORIZATION_COUNT = PREFIX + "auth.count";
        public static final String AUTHORIZATION_SUCCESS_RATE = PREFIX + "auth.success.rate";
        public static final String REVOCATION_COUNT = PREFIX + "auth.revocation.count";

        // 配额和限制相关指标
        public static final String QUOTA_USAGE = PREFIX + "quota.usage";
        public static final String QUOTA_REMAINING = PREFIX + "quota.remaining";
        public static final String RATE_LIMIT_HIT_COUNT = PREFIX + "rate.limit.hit.count";
        public static final String QUOTA_EXCEEDED_COUNT = PREFIX + "quota.exceeded.count";

        // 性能相关指标
        public static final String CONCURRENT_CALLS = PREFIX + "concurrent.calls";
        public static final String AVERAGE_RESPONSE_TIME = PREFIX + "response.time.avg";
        public static final String P95_RESPONSE_TIME = PREFIX + "response.time.p95";
        public static final String P99_RESPONSE_TIME = PREFIX + "response.time.p99";

        // 错误和异常指标
        public static final String ERROR_RATE = PREFIX + "error.rate";
        public static final String TIMEOUT_RATE = PREFIX + "timeout.rate";
        public static final String RETRY_COUNT = PREFIX + "retry.count";
        public static final String CIRCUIT_BREAKER_TRIPS = PREFIX + "circuit.breaker.trips";

        // 资源使用指标
        public static final String MEMORY_USAGE = PREFIX + "memory.usage";
        public static final String CPU_USAGE = PREFIX + "cpu.usage";
        public static final String THREAD_POOL_USAGE = PREFIX + "thread.pool.usage";
        public static final String CONNECTION_POOL_USAGE = PREFIX + "connection.pool.usage";

        // 业务指标
        public static final String POPULAR_TOOLS = PREFIX + "tools.popular";
        public static final String TOOL_USAGE_TREND = PREFIX + "usage.trend";
        public static final String TENANT_ACTIVITY = PREFIX + "tenant.activity";
        public static final String DAILY_CALL_VOLUME = PREFIX + "daily.call.volume";
    }

    // =================== 工具质量评估常量 ===================

    /**
     * 工具质量评估常量
     *
     * <p>工具质量评估帮助我们持续改进工具生态系统，
     * 为用户推荐高质量的工具。</p>
     */
    public static final class QualityMetrics {
        /**
         * 工具可用性优秀阈值
         */
        public static final double AVAILABILITY_EXCELLENT = 0.999;
        /**
         * 工具可用性良好阈值
         */
        public static final double AVAILABILITY_GOOD = 0.99;
        /**
         * 工具可用性及格阈值
         */
        public static final double AVAILABILITY_ACCEPTABLE = 0.95;

        /**
         * 响应时间优秀阈值（毫秒）
         */
        public static final long RESPONSE_TIME_EXCELLENT_MS = 1000;
        /**
         * 响应时间良好阈值（毫秒）
         */
        public static final long RESPONSE_TIME_GOOD_MS = 5000;
        /**
         * 响应时间可接受阈值（毫秒）
         */
        public static final long RESPONSE_TIME_ACCEPTABLE_MS = 30000;

        /**
         * 成功率优秀阈值
         */
        public static final double SUCCESS_RATE_EXCELLENT = 0.98;
        /**
         * 成功率良好阈值
         */
        public static final double SUCCESS_RATE_GOOD = 0.95;
        /**
         * 成功率及格阈值
         */
        public static final double SUCCESS_RATE_ACCEPTABLE = 0.90;

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
    }
}