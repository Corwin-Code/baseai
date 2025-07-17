package com.cloud.baseai.infrastructure.utils;

/**
 * <h1>基础设施模块常量定义类</h1>
 *
 * <p>这个常量类将所有的魔法数字、固定字符串和配置值集中在一个地方管理。</p>
 *
 * <p><b>常量的命名原则：</b></p>
 * <p>我们使用全大写字母和下划线的命名方式，这是Java的约定俗成。
 * 好的常量名应该像一本好字典的词条一样——简洁明了，见名知意。</p>
 */
public final class MiscConstants {

    // 防止实例化：这个类只提供静态常量，不需要创建对象
    private MiscConstants() {
        throw new UnsupportedOperationException("这是一个工具类，不允许实例化");
    }

    /**
     * <h2>文件处理相关常量</h2>
     * <p>这些常量定义了文件处理的各种限制和默认值，就像交通规则一样，
     * 为文件操作提供了明确的边界和标准。</p>
     */
    public static final class File {

        /**
         * 最大文件大小：100GB
         *
         * <p>为什么是100GB？这个限制考虑了以下因素：</p>
         * <p>1. 存储成本：过大的文件会显著增加存储开销</p>
         * <p>2. 传输时间：超大文件的上传和下载会影响用户体验</p>
         * <p>3. 处理性能：文件越大，解析和处理的时间越长</p>
         */
        public static final long MAX_FILE_SIZE = 100L * 1024 * 1024 * 1024; // 100GB

        /**
         * 分片上传的最小分片大小：5MB
         *
         * <p>这个值来自云存储服务（如AWS S3）的最佳实践。
         * 太小的分片会增加请求次数，太大的分片会增加重传成本。</p>
         */
        public static final long MIN_CHUNK_SIZE = 5L * 1024 * 1024; // 5MB

        /**
         * SHA256哈希值的标准长度：64个十六进制字符
         *
         * <p>SHA256算法产生256位（32字节）的哈希值，
         * 用十六进制表示就是64个字符。这是文件完整性验证的标准。</p>
         */
        public static final int SHA256_LENGTH = 64;

        /**
         * 默认存储桶名称
         *
         * <p>当用户没有指定存储桶时，系统会使用这个默认值。
         * 就像邮件系统的"收件箱"一样，提供一个通用的存放位置。</p>
         */
        public static final String DEFAULT_BUCKET = "default";

        /**
         * 临时文件前缀
         *
         * <p>临时文件使用统一的前缀，便于识别和批量清理。
         * 这就像给临时工作加上统一的标签，方便管理。</p>
         */
        public static final String TEMP_FILE_PREFIX = "temp_";

        /**
         * 支持的图片类型MIME前缀
         */
        public static final String IMAGE_MIME_PREFIX = "image/";

        /**
         * 支持的文档类型
         */
        public static final String[] SUPPORTED_DOCUMENT_TYPES = {
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain",
                "text/markdown"
        };
    }

    /**
     * <h2>模板处理相关常量</h2>
     * <p>模板系统的配置参数，这些常量确保了模板处理的一致性和可靠性。</p>
     */
    public static final class Template {

        /**
         * 模板内容最大长度：10,000字符
         *
         * <p>这个限制平衡了功能性和性能：</p>
         * <p>- 足够大：可以容纳复杂的提示词模板</p>
         * <p>- 足够小：确保解析和处理的效率</p>
         */
        public static final int MAX_CONTENT_LENGTH = 10_000;

        /**
         * 模板名称最大长度：128字符
         *
         * <p>这个长度既能容纳描述性的名称，又不会在数据库索引时
         * 造成性能问题。就像文件名一样，太长了不好管理。</p>
         */
        public static final int MAX_NAME_LENGTH = 128;

        /**
         * 变量占位符的正则表达式模式
         *
         * <p>这个模式匹配 {{variable}} 格式的变量占位符。
         * 双花括号是模板引擎中常用的语法，直观且不容易与普通文本冲突。</p>
         */
        public static final String VARIABLE_PATTERN = "\\{\\{\\s*(\\w+)\\s*\\}\\}";

        /**
         * 初始版本号
         *
         * <p>新创建的模板从版本1开始，这是软件版本管理的通用约定。
         * 版本0通常表示"还未正式发布"的状态。</p>
         */
        public static final int INITIAL_VERSION = 1;

        /**
         * 系统模板的标识前缀
         *
         * <p>系统预置的模板使用这个前缀，便于区分用户创建的模板。
         * 就像操作系统的系统文件有特殊标识一样。</p>
         */
        public static final String SYSTEM_TEMPLATE_PREFIX = "system_";

        /**
         * 模板变量的最大数量
         *
         * <p>限制变量数量可以防止模板过于复杂，也避免了
         * 解析时的性能问题。20个变量对大多数应用场景已经足够。</p>
         */
        public static final int MAX_VARIABLES_COUNT = 20;
    }

    /**
     * <h2>缓存相关常量</h2>
     * <p>缓存配置参数，用于优化系统性能。合理的缓存策略
     * 就像商店的库存管理一样，既要保证货物新鲜，又要避免频繁进货。</p>
     */
    public static final class Cache {

        /**
         * 模板缓存的键前缀
         */
        public static final String TEMPLATE_CACHE_PREFIX = "template:";

        /**
         * 文件元数据缓存的键前缀
         */
        public static final String FILE_METADATA_CACHE_PREFIX = "file_meta:";

        /**
         * 存储统计缓存的键前缀
         */
        public static final String STORAGE_STATS_CACHE_PREFIX = "storage_stats:";

        /**
         * 默认缓存过期时间：30分钟
         *
         * <p>30分钟是一个经验值：</p>
         * <p>- 足够长：减少数据库查询压力</p>
         * <p>- 足够短：确保数据的时效性</p>
         */
        public static final int DEFAULT_CACHE_TTL_MINUTES = 30;

        /**
         * 存储统计缓存过期时间：6小时
         *
         * <p>存储统计数据变化较慢，可以使用更长的缓存时间。
         * 就像人口统计数据一样，不需要实时更新。</p>
         */
        public static final int STORAGE_STATS_CACHE_TTL_HOURS = 6;
    }

    /**
     * <h2>错误代码常量</h2>
     * <p>标准化的错误代码，便于错误处理和问题诊断。
     * 就像医院的疾病代码一样，统一的编码便于交流和处理。</p>
     */
    public static final class ErrorCode {

        /**
         * 文件相关错误代码
         */
        public static final String FILE_TOO_LARGE = "MISC_FILE_001";
        public static final String FILE_TYPE_NOT_SUPPORTED = "MISC_FILE_002";
        public static final String FILE_HASH_MISMATCH = "MISC_FILE_003";
        public static final String FILE_NOT_FOUND = "MISC_FILE_004";
        public static final String FILE_UPLOAD_FAILED = "MISC_FILE_005";

        /**
         * 模板相关错误代码
         */
        public static final String TEMPLATE_CONTENT_TOO_LONG = "MISC_TEMPLATE_001";
        public static final String TEMPLATE_NAME_DUPLICATE = "MISC_TEMPLATE_002";
        public static final String TEMPLATE_INVALID_VARIABLES = "MISC_TEMPLATE_003";
        public static final String TEMPLATE_NOT_FOUND = "MISC_TEMPLATE_004";
        public static final String TEMPLATE_VERSION_CONFLICT = "MISC_TEMPLATE_005";

        /**
         * 存储相关错误代码
         */
        public static final String STORAGE_QUOTA_EXCEEDED = "MISC_STORAGE_001";
        public static final String STORAGE_ACCESS_DENIED = "MISC_STORAGE_002";
        public static final String STORAGE_SERVICE_UNAVAILABLE = "MISC_STORAGE_003";
    }

    /**
     * <h2>业务规则常量</h2>
     * <p>定义了业务逻辑中的重要规则和限制。</p>
     */
    public static final class BusinessRule {

        /**
         * 文件清理的保留期：已删除文件保留30天
         *
         * <p>这个"缓冲期"的设计考虑了：</p>
         * <p>1. 用户可能需要恢复误删的文件</p>
         * <p>2. 法规要求可能需要保留一定时间的数据</p>
         * <p>3. 系统备份和同步需要时间</p>
         */
        public static final int DELETED_FILE_RETENTION_DAYS = 30;

        /**
         * 每个用户最多可创建的模板数量
         *
         * <p>这个限制防止单个用户创建过多模板，
         * 避免影响系统性能和存储空间。</p>
         */
        public static final int MAX_TEMPLATES_PER_USER = 100;

        /**
         * 批量操作的最大数量
         *
         * <p>限制批量操作的规模，防止单次操作耗时过长，
         * 影响系统的响应性能。</p>
         */
        public static final int MAX_BATCH_OPERATION_SIZE = 50;

        /**
         * Token数量估算的平均字符比例
         *
         * <p>这是一个经验值：平均每3.5个字符相当于1个token。
         * 不同语言和模型会有差异，但这个比例适用于大多数情况。</p>
         */
        public static final double AVERAGE_CHARS_PER_TOKEN = 3.5;
    }

    /**
     * <h2>HTTP响应消息常量</h2>
     * <p>标准化的响应消息，提供一致的用户体验。</p>
     */
    public static final class Message {

        /**
         * 成功消息
         */
        public static final String FILE_UPLOAD_SUCCESS = "文件上传成功";
        public static final String TEMPLATE_CREATE_SUCCESS = "模板创建成功";
        public static final String TEMPLATE_UPDATE_SUCCESS = "模板更新成功";
        public static final String TEMPLATE_DELETE_SUCCESS = "模板删除成功";

        /**
         * 错误消息
         */
        public static final String FILE_SIZE_EXCEEDED = "文件大小超出限制";
        public static final String FILE_TYPE_INVALID = "不支持的文件类型";
        public static final String TEMPLATE_NAME_EXISTS = "模板名称已存在";
        public static final String TEMPLATE_CONTENT_INVALID = "模板内容格式无效";
        public static final String STORAGE_SPACE_INSUFFICIENT = "存储空间不足";
    }
}