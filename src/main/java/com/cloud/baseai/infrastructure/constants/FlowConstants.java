package com.cloud.baseai.infrastructure.constants;

/**
 * <h2>流程编排常量定义</h2>
 *
 * <p>集中定义流程编排模块使用的所有常量，确保常量的一致性和可维护性。</p>
 */
public final class FlowConstants {

    private FlowConstants() {
        throw new UnsupportedOperationException("常量类不能被实例化");
    }

    // =================== 字段长度限制 ===================

    /**
     * 字段长度限制常量
     *
     * <p>合理的字段长度限制既能满足业务需求，又能保证系统性能。
     * 这些限制是基于实际业务场景和技术约束综合确定的。</p>
     */
    public static final class FieldLengths {
        /**
         * 项目名称最大长度
         */
        public static final int MAX_PROJECT_NAME_LENGTH = 128;
        /**
         * 项目名称最小长度
         */
        public static final int MIN_PROJECT_NAME_LENGTH = 2;

        /**
         * 流程名称最大长度
         */
        public static final int MAX_FLOW_NAME_LENGTH = 128;
        /**
         * 流程名称最小长度
         */
        public static final int MIN_FLOW_NAME_LENGTH = 2;

        /**
         * 流程描述最大长度
         */
        public static final int MAX_FLOW_DESCRIPTION_LENGTH = 500;

        /**
         * 节点名称最大长度
         */
        public static final int MAX_NODE_NAME_LENGTH = 64;
        /**
         * 节点键最大长度
         */
        public static final int MAX_NODE_KEY_LENGTH = 32;

        /**
         * 节点配置JSON最大长度
         */
        public static final int MAX_NODE_CONFIG_LENGTH = 8192;
        /**
         * 重试策略JSON最大长度
         */
        public static final int MAX_RETRY_POLICY_LENGTH = 1024;

        /**
         * 边配置JSON最大长度
         */
        public static final int MAX_EDGE_CONFIG_LENGTH = 2048;

        /**
         * 图表JSON最大长度
         */
        public static final int MAX_DIAGRAM_JSON_LENGTH = 1048576; // 1MB

        /**
         * 快照JSON最大长度
         */
        public static final int MAX_SNAPSHOT_JSON_LENGTH = 2097152; // 2MB

        /**
         * 执行结果JSON最大长度
         */
        public static final int MAX_RESULT_JSON_LENGTH = 1048576; // 1MB

        /**
         * 输入输出JSON最大长度
         */
        public static final int MAX_IO_JSON_LENGTH = 524288; // 512KB
    }

    // =================== 业务限制常量 ===================

    /**
     * 业务限制常量
     *
     * <p>这些限制确保系统在合理的资源消耗下稳定运行，
     * 防止异常情况导致的资源耗尽或性能问题。</p>
     */
    public static final class BusinessLimits {
        /**
         * 每个租户最大项目数
         */
        public static final int MAX_PROJECTS_PER_TENANT = 100;
        /**
         * 每个项目最大流程数
         */
        public static final int MAX_FLOWS_PER_PROJECT = 500;
        /**
         * 每个流程最大节点数
         */
        public static final int MAX_NODES_PER_FLOW = 100;
        /**
         * 每个流程最大边数
         */
        public static final int MAX_EDGES_PER_FLOW = 200;

        /**
         * 流程最大版本数
         */
        public static final int MAX_VERSIONS_PER_FLOW = 50;
        /**
         * 每个快照最大运行实例数
         */
        public static final int MAX_RUNS_PER_SNAPSHOT = 10000;

        /**
         * 并行网关最大分支数
         */
        public static final int MAX_PARALLEL_BRANCHES = 10;
        /**
         * 子流程最大嵌套层级
         */
        public static final int MAX_SUB_PROCESS_DEPTH = 5;

        /**
         * 流程最大执行时间（分钟）
         */
        public static final int MAX_EXECUTION_TIMEOUT_MINUTES = 720; // 12小时
        /**
         * 默认执行超时时间（分钟）
         */
        public static final int DEFAULT_EXECUTION_TIMEOUT_MINUTES = 60;

        /**
         * 节点最大重试次数
         */
        public static final int MAX_NODE_RETRY_COUNT = 10;
        /**
         * 默认节点重试次数
         */
        public static final int DEFAULT_NODE_RETRY_COUNT = 3;

        /**
         * 流程运行历史保留天数
         */
        public static final int RUN_HISTORY_RETENTION_DAYS = 90;
        /**
         * 流程日志保留天数
         */
        public static final int LOG_RETENTION_DAYS = 30;
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
         * 搜索结果最大数量
         */
        public static final int MAX_SEARCH_RESULTS = 1000;
    }

    // =================== 缓存键常量 ===================

    /**
     * 缓存键前缀常量
     *
     * <p>统一的缓存键命名规范便于缓存管理和问题排查。
     * 采用层级结构，从通用到具体，便于批量操作。</p>
     */
    public static final class CacheKeys {
        /**
         * 缓存键根前缀
         */
        public static final String PREFIX = "flow:";

        /**
         * 项目缓存键前缀
         */
        public static final String PROJECT = PREFIX + "project:";
        /**
         * 流程定义缓存键前缀
         */
        public static final String DEFINITION = PREFIX + "definition:";
        /**
         * 节点缓存键前缀
         */
        public static final String NODE = PREFIX + "node:";
        /**
         * 边缓存键前缀
         */
        public static final String EDGE = PREFIX + "edge:";
        /**
         * 快照缓存键前缀
         */
        public static final String SNAPSHOT = PREFIX + "snapshot:";
        /**
         * 运行实例缓存键前缀
         */
        public static final String RUN = PREFIX + "run:";
        /**
         * 运行日志缓存键前缀
         */
        public static final String RUN_LOG = PREFIX + "log:";
        /**
         * 统计信息缓存键前缀
         */
        public static final String STATISTICS = PREFIX + "stats:";
        /**
         * 用户信息缓存键前缀
         */
        public static final String USER_INFO = PREFIX + "user:";
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
         * 项目信息缓存过期时间（分钟）
         */
        public static final int PROJECT_TTL_MINUTES = 120;
        /**
         * 流程定义缓存过期时间（分钟）
         */
        public static final int DEFINITION_TTL_MINUTES = 60;
        /**
         * 快照缓存过期时间（分钟）
         */
        public static final int SNAPSHOT_TTL_MINUTES = 240;
        /**
         * 统计信息缓存过期时间（分钟）
         */
        public static final int STATISTICS_TTL_MINUTES = 30;
        /**
         * 用户信息缓存过期时间（分钟）
         */
        public static final int USER_INFO_TTL_MINUTES = 120;
    }

    // =================== 审计操作类型 ===================

    /**
     * 审计操作类型常量
     *
     * <p>完整的审计日志是系统可追溯性的重要保障。
     * 每个关键操作都应该有对应的审计记录。</p>
     */
    public static final class AuditActions {
        // 项目操作
        public static final String PROJECT_CREATED = "PROJECT_CREATED";
        public static final String PROJECT_UPDATED = "PROJECT_UPDATED";
        public static final String PROJECT_DELETED = "PROJECT_DELETED";

        // 流程定义操作
        public static final String FLOW_CREATED = "FLOW_CREATED";
        public static final String FLOW_UPDATED = "FLOW_UPDATED";
        public static final String FLOW_PUBLISHED = "FLOW_PUBLISHED";
        public static final String FLOW_ARCHIVED = "FLOW_ARCHIVED";
        public static final String FLOW_DELETED = "FLOW_DELETED";

        // 流程结构操作
        public static final String STRUCTURE_UPDATED = "STRUCTURE_UPDATED";
        public static final String NODE_ADDED = "NODE_ADDED";
        public static final String NODE_UPDATED = "NODE_UPDATED";
        public static final String NODE_DELETED = "NODE_DELETED";
        public static final String EDGE_ADDED = "EDGE_ADDED";
        public static final String EDGE_UPDATED = "EDGE_UPDATED";
        public static final String EDGE_DELETED = "EDGE_DELETED";

        // 版本管理操作
        public static final String VERSION_CREATED = "VERSION_CREATED";
        public static final String SNAPSHOT_CREATED = "SNAPSHOT_CREATED";

        // 流程执行操作
        public static final String FLOW_EXECUTED = "FLOW_EXECUTED";
        public static final String EXECUTION_STARTED = "EXECUTION_STARTED";
        public static final String EXECUTION_COMPLETED = "EXECUTION_COMPLETED";
        public static final String EXECUTION_FAILED = "EXECUTION_FAILED";
        public static final String EXECUTION_CANCELLED = "EXECUTION_CANCELLED";
        public static final String EXECUTION_TIMEOUT = "EXECUTION_TIMEOUT";

        // 节点执行操作
        public static final String NODE_STARTED = "NODE_STARTED";
        public static final String NODE_COMPLETED = "NODE_COMPLETED";
        public static final String NODE_FAILED = "NODE_FAILED";
        public static final String NODE_RETRIED = "NODE_RETRIED";
        public static final String NODE_SKIPPED = "NODE_SKIPPED";
    }

    // =================== 默认配置值 ===================

    /**
     * 默认配置值常量
     *
     * <p>合理的默认值能够降低系统配置的复杂度，
     * 让大部分场景下系统能够开箱即用。</p>
     */
    public static final class Defaults {
        /**
         * 默认流程版本号
         */
        public static final int DEFAULT_VERSION = 1;
        /**
         * 默认异步执行线程池大小
         */
        public static final int DEFAULT_ASYNC_POOL_SIZE = 10;
        /**
         * 默认最大并发执行数
         */
        public static final int DEFAULT_MAX_CONCURRENT_EXECUTIONS = 100;

        /**
         * 默认节点超时时间（分钟）
         */
        public static final int DEFAULT_NODE_TIMEOUT_MINUTES = 30;
        /**
         * 默认重试间隔（秒）
         */
        public static final int DEFAULT_RETRY_INTERVAL_SECONDS = 60;
        /**
         * 默认重试退避倍数
         */
        public static final double DEFAULT_RETRY_BACKOFF_MULTIPLIER = 2.0;

        /**
         * 默认批处理大小
         */
        public static final int DEFAULT_BATCH_SIZE = 100;
        /**
         * 默认缓冲区大小
         */
        public static final int DEFAULT_BUFFER_SIZE = 1000;

        /**
         * 默认健康检查间隔（秒）
         */
        public static final int DEFAULT_HEALTH_CHECK_INTERVAL_SECONDS = 30;
        /**
         * 默认指标收集间隔（秒）
         */
        public static final int DEFAULT_METRICS_COLLECTION_INTERVAL_SECONDS = 60;

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
     * <p>统一的验证模式确保数据的一致性和有效性。
     * 这些模式基于业务需求和安全考虑设计。</p>
     */
    public static final class Patterns {
        /**
         * 项目名称模式 - 支持中英文、数字、下划线、连字符
         */
        public static final String PROJECT_NAME_PATTERN = "^[\\u4e00-\\u9fa5a-zA-Z0-9_-]{2,128}$";
        /**
         * 流程名称模式 - 支持中英文、数字、下划线、连字符、空格
         */
        public static final String FLOW_NAME_PATTERN = "^[\\u4e00-\\u9fa5a-zA-Z0-9_\\s-]{2,128}$";
        /**
         * 节点键模式 - 只允许字母、数字、下划线
         */
        public static final String NODE_KEY_PATTERN = "^[a-zA-Z][a-zA-Z0-9_]{1,31}$";
        /**
         * 版本号模式 - 正整数
         */
        public static final String VERSION_PATTERN = "^[1-9]\\d*$";
        /**
         * 时间范围模式 - 支持1h、1d、7d、30d等格式
         */
        public static final String TIME_RANGE_PATTERN = "^\\d+[hdwmy]$";
        /**
         * JSON格式验证模式 - 简单的JSON格式检查
         */
        public static final String JSON_PATTERN = "^\\s*[{\\[].*[}\\]]\\s*$";
    }

    // =================== 系统限制常量 ===================

    /**
     * 系统限制常量
     *
     * <p>这些限制是基于系统性能和稳定性考虑设定的，
     * 防止异常情况导致系统资源耗尽。</p>
     */
    public static final class SystemLimits {
        /**
         * 最大并发执行流程数
         */
        public static final int MAX_CONCURRENT_EXECUTIONS = 500;
        /**
         * 最大并发构建流程数
         */
        public static final int MAX_CONCURRENT_BUILDS = 20;
        /**
         * 最大异步任务队列大小
         */
        public static final int MAX_ASYNC_QUEUE_SIZE = 10000;

        /**
         * 慢查询阈值（毫秒）
         */
        public static final long SLOW_QUERY_THRESHOLD_MS = 2000;
        /**
         * 流程执行超时阈值（毫秒）
         */
        public static final long EXECUTION_TIMEOUT_THRESHOLD_MS = 3600000; // 1小时
        /**
         * 节点执行超时阈值（毫秒）
         */
        public static final long NODE_TIMEOUT_THRESHOLD_MS = 1800000; // 30分钟

        /**
         * 最大重试次数限制
         */
        public static final int MAX_RETRY_LIMIT = 10;
        /**
         * 最大批处理大小限制
         */
        public static final int MAX_BATCH_SIZE_LIMIT = 10000;

        /**
         * 内存使用警告阈值（百分比）
         */
        public static final int MEMORY_WARNING_THRESHOLD = 80;
        /**
         * CPU使用警告阈值（百分比）
         */
        public static final int CPU_WARNING_THRESHOLD = 85;

        /**
         * 每分钟最大API调用次数
         */
        public static final int MAX_API_CALLS_PER_MINUTE = 1000;
        /**
         * 每小时最大流程执行次数
         */
        public static final int MAX_EXECUTIONS_PER_HOUR = 10000;
    }

    // =================== 度量指标名称 ===================

    /**
     * 度量指标名称常量
     *
     * <p>标准化的指标名称便于监控系统的集成和管理。
     * 采用层级命名方式，便于指标的组织和聚合。</p>
     */
    public static final class MetricNames {
        /**
         * 指标名称前缀
         */
        public static final String PREFIX = "flow.";

        // 项目相关指标
        public static final String PROJECT_CREATE_COUNT = PREFIX + "project.create.count";
        public static final String PROJECT_CREATE_TIME = PREFIX + "project.create.time";
        public static final String PROJECT_UPDATE_COUNT = PREFIX + "project.update.count";
        public static final String PROJECT_DELETE_COUNT = PREFIX + "project.delete.count";

        // 流程定义相关指标
        public static final String FLOW_CREATE_COUNT = PREFIX + "definition.create.count";
        public static final String FLOW_CREATE_TIME = PREFIX + "definition.create.time";
        public static final String FLOW_PUBLISH_COUNT = PREFIX + "definition.publish.count";
        public static final String FLOW_PUBLISH_TIME = PREFIX + "definition.publish.time";

        // 流程执行相关指标
        public static final String EXECUTION_COUNT = PREFIX + "execution.count";
        public static final String EXECUTION_TIME = PREFIX + "execution.time";
        public static final String EXECUTION_SUCCESS_COUNT = PREFIX + "execution.success.count";
        public static final String EXECUTION_FAILURE_COUNT = PREFIX + "execution.failure.count";
        public static final String EXECUTION_TIMEOUT_COUNT = PREFIX + "execution.timeout.count";

        // 节点执行相关指标
        public static final String NODE_EXECUTION_COUNT = PREFIX + "node.execution.count";
        public static final String NODE_EXECUTION_TIME = PREFIX + "node.execution.time";
        public static final String NODE_RETRY_COUNT = PREFIX + "node.retry.count";

        // 系统性能指标
        public static final String SYSTEM_MEMORY_USAGE = PREFIX + "system.memory.usage";
        public static final String SYSTEM_CPU_USAGE = PREFIX + "system.cpu.usage";
        public static final String ACTIVE_EXECUTIONS = PREFIX + "system.active.executions";
        public static final String QUEUE_SIZE = PREFIX + "system.queue.size";

        // API调用指标
        public static final String API_REQUEST_COUNT = PREFIX + "api.request.count";
        public static final String API_RESPONSE_TIME = PREFIX + "api.response.time";
        public static final String API_ERROR_COUNT = PREFIX + "api.error.count";
    }

    // =================== HTTP状态相关 ===================

    /**
     * HTTP状态码常量
     *
     * <p>标准的HTTP状态码用于API响应，确保与客户端的良好交互。</p>
     */
    public static final class HttpStatus {
        /**
         * 成功
         */
        public static final int SUCCESS = 200;
        /**
         * 创建成功
         */
        public static final int CREATED = 201;
        /**
         * 接受请求（异步处理）
         */
        public static final int ACCEPTED = 202;
        /**
         * 无内容
         */
        public static final int NO_CONTENT = 204;

        /**
         * 请求错误
         */
        public static final int BAD_REQUEST = 400;
        /**
         * 未授权
         */
        public static final int UNAUTHORIZED = 401;
        /**
         * 禁止访问
         */
        public static final int FORBIDDEN = 403;
        /**
         * 资源不存在
         */
        public static final int NOT_FOUND = 404;
        /**
         * 方法不允许
         */
        public static final int METHOD_NOT_ALLOWED = 405;
        /**
         * 冲突
         */
        public static final int CONFLICT = 409;
        /**
         * 请求过于频繁
         */
        public static final int TOO_MANY_REQUESTS = 429;

        /**
         * 服务器内部错误
         */
        public static final int INTERNAL_SERVER_ERROR = 500;
        /**
         * 服务不可用
         */
        public static final int SERVICE_UNAVAILABLE = 503;
        /**
         * 网关超时
         */
        public static final int GATEWAY_TIMEOUT = 504;
    }
}