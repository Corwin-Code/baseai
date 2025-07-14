package com.clinflash.baseai.infrastructure.utils;

/**
 * <h1>系统管理模块常量定义类</h1>
 *
 * <p>这个常量类定义了模块运行的基本规则和标准。</p>
 *
 * <p><b>为什么系统管理需要这么多常量？</b></p>
 * <p>系统管理涉及很多关键的数值和配置，比如任务重试次数、健康检查间隔、
 * 数据保留期限等。这些值如果散布在代码各处，维护起来就像在迷宫中找路一样困难。
 * 集中管理这些常量，就像把所有的钥匙放在一个钥匙箱里，使用和管理都更加方便。</p>
 *
 * <p><b>常量设计的智慧：</b></p>
 * <p>每个常量的值都不是随意设定的，而是基于系统运行的实际需要和最佳实践。
 * 比如重试次数设为3次，是因为大多数临时性问题在3次重试内都能解决，
 * 而超过3次通常意味着存在更深层的问题，需要人工介入。</p>
 */
public final class SystemConstants {

    // 防止意外实例化：系统常量类应该是"纯静态"的
    private SystemConstants() {
        throw new UnsupportedOperationException("系统常量类不允许实例化，请直接使用静态常量");
    }

    /**
     * <h2>任务管理相关常量</h2>
     * <p>这些常量定义了异步任务系统的运行规则，就像工厂的生产线
     * 需要明确的操作标准一样，任务系统也需要清晰的参数设定。</p>
     */
    public static final class Task {

        /**
         * 任务最大重试次数：3次
         *
         * <p>这个数字来自于实际运维经验的总结：</p>
         * <p>1. 第一次失败：可能是网络抖动或临时资源不足</p>
         * <p>2. 第二次失败：可能是短暂的服务异常</p>
         * <p>3. 第三次失败：如果还失败，很可能是系统性问题，需要人工介入</p>
         * <p>继续重试不仅浪费资源，还可能加重系统负担。</p>
         */
        public static final int MAX_RETRY_COUNT = 3;

        /**
         * 任务执行超时时间：5分钟（300秒）
         *
         * <p>这个时间设定考虑了不同类型任务的需求：</p>
         * <p>- 快速任务（如发送邮件）：几秒钟就能完成</p>
         * <p>- 中等任务（如生成报告）：可能需要1-2分钟</p>
         * <p>- 复杂任务（如数据迁移）：可能需要几分钟</p>
         * <p>5分钟是一个平衡点，既不会让快速任务等太久，也给复杂任务足够时间。</p>
         */
        public static final int TASK_TIMEOUT_SECONDS = 300;

        /**
         * 重试间隔时间：5分钟
         *
         * <p>重试不是立即进行的，需要等待一段时间：</p>
         * <p>1. 给系统恢复的时间：如果是系统负载问题，需要时间缓解</p>
         * <p>2. 避免雪崩效应：大量任务同时重试会加重系统压力</p>
         * <p>3. 减少无效重试：某些问题需要时间才能解决</p>
         */
        public static final int RETRY_INTERVAL_MINUTES = 5;

        /**
         * 批量处理大小：100个任务
         *
         * <p>批量处理是提高效率的重要手段，但批量大小需要仔细平衡：</p>
         * <p>- 太小：频繁的批次切换会降低效率</p>
         * <p>- 太大：单个批次失败影响范围大，内存占用也会增加</p>
         * <p>100是一个经过验证的经验值，适合大多数场景。</p>
         */
        public static final int BATCH_SIZE = 100;

        /**
         * 已完成任务保留期：7天
         *
         * <p>已完成的任务记录需要保留一段时间，用于：</p>
         * <p>1. 问题排查：当用户报告问题时，可以查看历史执行记录</p>
         * <p>2. 性能分析：统计任务执行时间和成功率</p>
         * <p>3. 审计需要：某些业务可能需要保留操作记录</p>
         * <p>7天的保留期既满足了日常需求，又不会占用过多存储空间。</p>
         */
        public static final int COMPLETED_TASK_RETENTION_DAYS = 7;

        /**
         * 任务优先级级别
         */
        public static final int PRIORITY_LOW = 1;
        public static final int PRIORITY_MEDIUM = 2;
        public static final int PRIORITY_HIGH = 3;
        public static final int PRIORITY_URGENT = 4;

        /**
         * 任务状态常量
         */
        public static final int STATUS_PENDING = 0;      // 待执行
        public static final int STATUS_PROCESSING = 1;   // 处理中
        public static final int STATUS_SUCCESS = 2;      // 成功
        public static final int STATUS_FAILED = 3;       // 失败
    }

    /**
     * <h2>健康检查相关常量</h2>
     * <p>系统健康检查就像医生的定期体检，需要合适的检查频率和标准。
     * 太频繁会增加系统负担，太稀少又可能错过问题。</p>
     */
    public static final class HealthCheck {

        /**
         * 健康检查间隔：30秒
         *
         * <p>30秒的间隔是经过权衡的结果：</p>
         * <p>- 足够频繁：能及时发现系统问题</p>
         * <p>- 不会过载：不会对系统性能造成明显影响</p>
         * <p>- 符合惯例：大多数监控系统都使用类似的间隔</p>
         */
        public static final int INTERVAL_SECONDS = 30;

        /**
         * 健康检查超时时间：5秒
         *
         * <p>健康检查本身不应该成为系统的负担，5秒的超时确保：</p>
         * <p>1. 正常情况下足够完成检查</p>
         * <p>2. 异常情况下不会无限等待</p>
         * <p>3. 不会因为检查本身而影响系统响应</p>
         */
        public static final int TIMEOUT_MILLISECONDS = 5000;

        /**
         * 失败阈值：连续3次失败才认为不健康
         *
         * <p>单次失败可能是偶然的网络抖动或瞬时负载高峰，
         * 连续3次失败才能说明确实存在问题。这避免了
         * 因为偶然因素而误报系统异常。</p>
         */
        public static final int FAILURE_THRESHOLD = 3;

        /**
         * 恢复阈值：连续2次成功才认为已恢复
         *
         * <p>系统从不健康状态恢复时，也需要确认稳定性。
         * 2次连续成功说明问题已经解决，避免状态频繁切换。</p>
         */
        public static final int RECOVERY_THRESHOLD = 2;

        /**
         * 健康状态常量
         */
        public static final String STATUS_HEALTHY = "healthy";
        public static final String STATUS_UNHEALTHY = "unhealthy";
        public static final String STATUS_WARNING = "warning";
        public static final String STATUS_UNKNOWN = "unknown";
    }

    /**
     * <h2>系统设置相关常量</h2>
     * <p>系统设置的管理需要严格的规范，就像银行的保险箱需要
     * 多重安全机制一样，系统配置也需要谨慎处理。</p>
     */
    public static final class Setting {

        /**
         * 设置键的最大长度：64字符
         *
         * <p>设置键需要具有描述性，但也不能过长：</p>
         * <p>- 64字符足够表达清晰的含义</p>
         * <p>- 不会在数据库索引时造成性能问题</p>
         * <p>- 符合大多数配置系统的约定</p>
         */
        public static final int MAX_KEY_LENGTH = 64;

        /**
         * 设置值的最大长度：65535字符（64KB）
         *
         * <p>这个长度可以容纳：</p>
         * <p>- 简单的字符串配置</p>
         * <p>- 复杂的JSON配置对象</p>
         * <p>- 甚至小型的配置文件内容</p>
         */
        public static final int MAX_VALUE_LENGTH = 65535;

        /**
         * 敏感配置的掩码字符
         *
         * <p>敏感配置（如密码、密钥）在日志和API响应中需要掩码处理，
         * 使用星号是通用的做法，既保护了安全又保留了长度信息。</p>
         */
        public static final String SENSITIVE_MASK = "***";

        /**
         * 批量更新的最大数量：50个设置
         *
         * <p>限制批量操作的规模，避免：</p>
         * <p>1. 单次事务过大影响性能</p>
         * <p>2. 操作失败时影响范围过广</p>
         * <p>3. 锁定资源时间过长</p>
         */
        public static final int MAX_BATCH_UPDATE_SIZE = 50;

        /**
         * 配置值类型常量
         */
        public static final String VALUE_TYPE_STRING = "STRING";
        public static final String VALUE_TYPE_INTEGER = "INTEGER";
        public static final String VALUE_TYPE_BOOLEAN = "BOOLEAN";
        public static final String VALUE_TYPE_JSON = "JSON";
    }

    /**
     * <h2>监控和统计相关常量</h2>
     * <p>系统监控就像汽车的仪表盘，需要显示关键指标但不能过于复杂。
     * 这些常量定义了监控数据的收集和展示规则。</p>
     */
    public static final class Monitoring {

        /**
         * 指标收集间隔：5分钟
         *
         * <p>5分钟的间隔适合大多数指标：</p>
         * <p>- 足够频繁：能捕捉到系统状态的变化趋势</p>
         * <p>- 不会过载：不会因为收集本身消耗太多资源</p>
         * <p>- 便于存储：生成的数据量可控</p>
         */
        public static final int METRICS_COLLECTION_INTERVAL_MINUTES = 5;

        /**
         * 性能数据保留期：30天
         *
         * <p>30天的数据可以用于：</p>
         * <p>1. 短期性能分析和优化</p>
         * <p>2. 发现系统的周期性规律</p>
         * <p>3. 问题的历史追溯</p>
         * <p>更长期的数据通常会汇总到更大的时间粒度（如天、周）。</p>
         */
        public static final int PERFORMANCE_DATA_RETENTION_DAYS = 30;

        /**
         * 慢查询阈值：1秒（1000毫秒）
         *
         * <p>超过1秒的数据库查询被认为是"慢查询"：</p>
         * <p>1. 大多数用户界面操作应该在1秒内完成</p>
         * <p>2. 超过1秒的查询值得关注和优化</p>
         * <p>3. 这是数据库性能调优的常用基准</p>
         */
        public static final long SLOW_QUERY_THRESHOLD_MS = 1000;

        /**
         * 统计数据聚合级别
         */
        public static final String AGGREGATION_MINUTE = "minute";
        public static final String AGGREGATION_HOUR = "hour";
        public static final String AGGREGATION_DAY = "day";
        public static final String AGGREGATION_WEEK = "week";
    }

    /**
     * <h2>安全相关常量</h2>
     * <p>系统安全不容忽视，这些常量定义了安全策略的重要参数。</p>
     */
    public static final class Security {

        /**
         * 审计日志保留期：90天
         *
         * <p>审计日志的保留期考虑了多个因素：</p>
         * <p>1. 法规要求：某些行业要求保留至少90天的操作记录</p>
         * <p>2. 问题追溯：复杂问题的排查可能需要较长的历史数据</p>
         * <p>3. 安全分析：安全威胁的识别需要足够的历史数据</p>
         */
        public static final int AUDIT_LOG_RETENTION_DAYS = 90;

        /**
         * 管理员会话超时时间：60分钟
         *
         * <p>管理员会话的超时设置需要平衡安全性和便利性：</p>
         * <p>- 不能太短：否则管理员需要频繁重新登录</p>
         * <p>- 不能太长：增加账户被恶意使用的风险</p>
         * <p>60分钟是一个合理的平衡点。</p>
         */
        public static final int ADMIN_SESSION_TIMEOUT_MINUTES = 60;

        /**
         * 密码复杂度要求
         */
        public static final int MIN_PASSWORD_LENGTH = 8;
        public static final int MAX_PASSWORD_LENGTH = 128;

        /**
         * 操作日志级别
         */
        public static final String LOG_LEVEL_INFO = "info";
        public static final String LOG_LEVEL_WARN = "warn";
        public static final String LOG_LEVEL_ERROR = "error";
        public static final String LOG_LEVEL_DEBUG = "debug";
    }

    /**
     * <h2>错误代码常量</h2>
     * <p>标准化的错误代码体系，让错误处理更加规范和高效。
     * 就像医疗系统的疾病编码一样，统一的错误代码便于
     * 问题的识别、分类和处理。</p>
     */
    public static final class ErrorCode {

        /**
         * 任务相关错误代码
         */
        public static final String TASK_NOT_FOUND = "SYS_TASK_001";
        public static final String TASK_RETRY_LIMIT_EXCEEDED = "SYS_TASK_002";
        public static final String TASK_TIMEOUT = "SYS_TASK_003";
        public static final String TASK_EXECUTION_FAILED = "SYS_TASK_004";
        public static final String TASK_INVALID_PAYLOAD = "SYS_TASK_005";

        /**
         * 系统设置相关错误代码
         */
        public static final String SETTING_NOT_FOUND = "SYS_SETTING_001";
        public static final String SETTING_INVALID_VALUE = "SYS_SETTING_002";
        public static final String SETTING_READ_ONLY = "SYS_SETTING_003";
        public static final String SETTING_TYPE_MISMATCH = "SYS_SETTING_004";
        public static final String SETTING_VALIDATION_FAILED = "SYS_SETTING_005";

        /**
         * 权限相关错误代码
         */
        public static final String PERMISSION_DENIED = "SYS_AUTH_001";
        public static final String INSUFFICIENT_PRIVILEGES = "SYS_AUTH_002";
        public static final String SESSION_EXPIRED = "SYS_AUTH_003";
        public static final String INVALID_CREDENTIALS = "SYS_AUTH_004";

        /**
         * 系统健康相关错误代码
         */
        public static final String HEALTH_CHECK_FAILED = "SYS_HEALTH_001";
        public static final String COMPONENT_UNAVAILABLE = "SYS_HEALTH_002";
        public static final String SYSTEM_OVERLOADED = "SYS_HEALTH_003";
    }

    /**
     * <h2>HTTP响应消息常量</h2>
     * <p>标准化的用户消息，提供一致和友好的用户体验。</p>
     */
    public static final class Message {

        /**
         * 成功消息
         */
        public static final String TASK_CREATED_SUCCESS = "任务创建成功";
        public static final String TASK_RETRY_SUCCESS = "任务重试成功";
        public static final String SETTING_UPDATE_SUCCESS = "设置更新成功";
        public static final String BATCH_UPDATE_SUCCESS = "批量更新完成";
        public static final String HEALTH_CHECK_PASSED = "系统健康检查通过";

        /**
         * 错误消息
         */
        public static final String TASK_NOT_RETRYABLE = "任务无法重试";
        public static final String SETTING_VALUE_INVALID = "设置值格式无效";
        public static final String PERMISSION_INSUFFICIENT = "权限不足";
        public static final String SYSTEM_MAINTENANCE = "系统正在维护中";
        public static final String OPERATION_TIMEOUT = "操作超时";
    }

    /**
     * <h2>业务规则常量</h2>
     * <p>定义系统管理的核心业务逻辑规则。</p>
     */
    public static final class BusinessRule {

        /**
         * 并发任务执行限制
         *
         * <p>限制同时执行的任务数量，避免系统过载：</p>
         * <p>1. 保护系统资源不被耗尽</p>
         * <p>2. 确保每个任务都有足够的执行环境</p>
         * <p>3. 提高任务执行的成功率</p>
         */
        public static final int MAX_CONCURRENT_TASKS = 10;

        /**
         * 系统维护模式下允许的操作类型
         */
        public static final String[] MAINTENANCE_ALLOWED_OPERATIONS = {
                "health_check", "system_status", "read_settings"
        };

        /**
         * 关键系统设置（不允许通过API修改）
         */
        public static final String[] PROTECTED_SETTINGS = {
                "system.security.secret_key",
                "database.connection.password",
                "service.encryption.private_key"
        };
    }
}