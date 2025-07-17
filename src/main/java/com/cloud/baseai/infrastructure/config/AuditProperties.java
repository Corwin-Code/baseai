package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.domain.audit.model.AuditMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * <h2>审计模块配置类</h2>
 *
 * <p>这个配置类就像是审计系统的"控制面板"，它定义了审计系统的各种参数和行为。
 * 想象一下，如果审计系统是一台精密的仪器，那么这个配置类就是它的设置界面，
 * 让我们可以根据不同的业务需求和环境来调整系统的运行方式。</p>
 *
 * <p><b>配置职责：</b></p>
 * <ul>
 * <li><b>性能调优：</b>配置线程池大小、批处理参数等性能相关设置</li>
 * <li><b>功能开关：</b>控制哪些功能启用，哪些功能禁用</li>
 * <li><b>存储策略：</b>定义数据保留期限、归档策略等</li>
 * <li><b>安全设置：</b>配置敏感信息过滤、访问控制等安全相关参数</li>
 * </ul>
 */
@Setter
@Getter
@Validated
@ConfigurationProperties(prefix = "baseai.audit")
public class AuditProperties {

    private static final Logger log = LoggerFactory.getLogger(AuditProperties.class);

    /**
     * 是否启用审计功能
     */
    private boolean enabled = true;

    /**
     * 审计系统的运行模式
     * <p>不同的模式适用于不同的场景：</p>
     * <ul>
     * <li>STANDARD：标准模式，平衡性能和功能</li>
     * <li>HIGH_PERFORMANCE：高性能模式，优先考虑性能</li>
     * <li>HIGH_SECURITY：高安全模式，记录更详细的信息</li>
     * <li>COMPLIANCE：合规模式，满足严格的法规要求</li>
     * </ul>
     */
    @NotNull(message = "审计模式不能为空")
    private AuditMode mode = AuditMode.STANDARD;

    /**
     * 审计数据的默认租户ID
     * <p>在某些情况下，如系统操作，可能无法确定具体的租户，
     * 这时会使用这个默认值。</p>
     */
    @Min(value = 1, message = "默认租户ID必须大于0")
    private Long defaultTenantId = 1L;

    /**
     * 是否启用调试模式
     * <p>调试模式会输出更详细的日志信息，但会影响性能，
     * 建议只在开发和测试环境中启用。</p>
     */
    private boolean debugMode = false;

    /**
     * 异步处理配置
     * <p>控制审计日志的异步写入行为，确保不影响主业务流程的性能</p>
     */
    @Valid
    @NestedConfigurationProperty
    private AsyncConfig async = new AsyncConfig();

    /**
     * 批处理配置
     */
    @Valid
    @NestedConfigurationProperty
    private BatchConfig batch = new BatchConfig();

    /**
     * Elasticsearch配置
     */
    @Valid
    @NestedConfigurationProperty
    private ElasticsearchConfig elasticsearch = new ElasticsearchConfig();

    /**
     * 性能监控配置
     * <p>控制性能指标的收集和报告</p>
     */
    @Valid
    @NestedConfigurationProperty
    private MonitoringConfig monitoring = new MonitoringConfig();

    /**
     * 缓存配置
     */
    @Valid
    @NestedConfigurationProperty
    private CacheConfig cache = new CacheConfig();

    /**
     * 数据压缩配置
     */
    @Valid
    @NestedConfigurationProperty
    private CompressionConfig compression = new CompressionConfig();

    /**
     * 数据保留配置
     * <p>定义审计数据的生命周期管理策略</p>
     */
    private RetentionConfig retention = new RetentionConfig();

    /**
     * 安全过滤配置
     * <p>控制敏感信息的记录和显示</p>
     */
    private SecurityConfig security = new SecurityConfig();



    // =================== 配置属性类 ===================

    /**
     * <h3>异步处理配置</h3>
     *
     * <p>这个内部类定义了异步处理相关的所有配置参数。异步处理是审计系统
     * 性能优化的关键，它让审计记录不会阻塞主业务流程。</p>
     */
    @Setter
    @Getter
    public static class AsyncConfig {

        /**
         * 是否启用异步处理
         * <p>默认启用。在某些特殊情况下（如调试、测试），可能需要禁用异步处理</p>
         */
        private boolean enabled = true;

        /**
         * 异步队列的最大容量
         * <p>当队列满时，新的审计请求会被降级为同步处理。
         * 这个设计确保了在高负载情况下系统的稳定性，防止内存溢出。</p>
         */
        @Min(value = 100, message = "队列容量不能小于100")
        @Max(value = 100000, message = "队列容量不能超过100000")
        private int queueCapacity = 10000;

        /**
         * 异步处理超时时间（毫秒）
         * <p>如果异步任务在这个时间内没有完成，系统会记录警告。</p>
         */
        @Min(value = 1000, message = "超时时间不能小于1秒")
        @Max(value = 300000, message = "超时时间不能超过5分钟")
        private long timeoutMs = 30000;

        /**
         * 是否在异步处理失败时回退到同步处理
         */
        private boolean fallbackToSync = true;

        /**
         * 异步处理失败的最大重试次数
         */
        @Min(value = 0, message = "重试次数不能小于0")
        @Max(value = 10, message = "重试次数不能超过10")
        private int maxRetries = 3;

        /**
         * 最大线程池大小
         * <p>控制同时处理审计任务的最大线程数</p>
         */
        private int maxPoolSize = 20;

        /**
         * 批处理大小
         * <p>控制批量写入数据库时的批次大小，影响性能和内存使用</p>
         */
        private int batchSize = 100;

        /**
         * 批处理超时时间（毫秒）
         * <p>控制批处理的最大等待时间，确保数据及时写入</p>
         */
        private long batchTimeoutMs = 5000;

    }

    /**
     * <h3>数据保留配置</h3>
     *
     * <p>审计数据通常需要长期保存以满足合规要求，但也需要控制存储成本。
     * 这个配置类定义了数据生命周期管理的策略。</p>
     */
    @Setter
    @Getter
    public static class RetentionConfig {

        /**
         * 默认保留天数
         * <p>通常设置为7年（2555天），满足大多数行业的合规要求</p>
         */
        private int defaultRetentionDays = 2555; // 7年

        /**
         * 是否启用自动归档
         * <p>将旧数据移动到成本更低的存储介质</p>
         */
        private boolean autoArchiveEnabled = true;

        /**
         * 归档阈值天数
         * <p>超过这个天数的数据会被归档</p>
         */
        private int archiveAfterDays = 365; // 1年

        /**
         * 是否启用自动清理
         * <p>彻底删除超过保留期限的数据</p>
         */
        private boolean autoCleanupEnabled = false;

        /**
         * 数据压缩级别
         * <p>控制归档数据的压缩程度，平衡存储空间和访问性能</p>
         */
        private int compressionLevel = 6;

    }

    /**
     * <h3>安全过滤配置</h3>
     *
     * <p>在记录审计信息时，我们需要特别注意不能记录敏感信息，如密码、
     * 信用卡号等。这个配置类定义了敏感信息的过滤规则。</p>
     */
    @Setter
    @Getter
    public static class SecurityConfig {

        /**
         * 是否启用敏感信息过滤
         */
        private boolean sensitiveDataFilterEnabled = true;

        /**
         * 需要过滤的字段名模式
         * <p>使用正则表达式匹配需要过滤的字段名</p>
         */
        private String[] sensitiveFieldPatterns = {
                "password", "pwd", "secret", "token", "key", "credit.*card", "ssn", "phone"
        };

        /**
         * 是否记录IP地址
         * <p>某些隐私要求严格的场景可能需要禁用IP记录</p>
         */
        private boolean recordIpAddress = true;

        /**
         * 是否记录用户代理信息
         */
        private boolean recordUserAgent = true;

        /**
         * 数据脱敏级别
         * <p>控制敏感数据的脱敏程度：NONE, PARTIAL, FULL</p>
         */
        private String maskingLevel = "PARTIAL";

    }

    /**
     * <h3>批处理配置</h3>
     *
     * <p>批处理是提高数据库写入性能的重要手段。与其每次写入一条记录，
     * 不如积累一批记录后一次性写入。这就像装载货物时，用大卡车一次
     * 运输多个包裹比用摩托车一次运一个包裹要高效得多。</p>
     */
    @Setter
    @Getter
    public static class BatchConfig {

        /**
         * 是否启用批处理
         */
        private boolean enabled = true;

        /**
         * 批处理大小
         * <p>每批处理的记录数量。数值越大，数据库写入效率越高，
         * 但内存使用也会增加。需要根据系统性能找到平衡点。</p>
         */
        @Min(value = 1, message = "批处理大小不能小于1")
        @Max(value = 1000, message = "批处理大小不能超过1000")
        private int size = 100;

        /**
         * 批处理超时时间（毫秒）
         * <p>即使没有达到批处理大小，超过这个时间也会强制执行批处理。
         * 这确保了数据能够及时写入数据库。</p>
         */
        @Min(value = 100, message = "批处理超时不能小于100毫秒")
        @Max(value = 60000, message = "批处理超时不能超过60秒")
        private long timeoutMs = 5000;

        /**
         * 批处理队列的最大大小
         * <p>防止内存溢出的保护机制。</p>
         */
        @Min(value = 100, message = "队列大小不能小于100")
        private int maxQueueSize = 10000;
    }

    /**
     * <h3>Elasticsearch配置</h3>
     *
     * <p>Elasticsearch为审计系统提供强大的搜索和分析能力。
     * 它就像为我们的审计数据建立了一个智能的图书馆索引系统，
     * 让我们能够快速找到需要的信息。</p>
     */
    @Setter
    @Getter
    public static class ElasticsearchConfig {

        /**
         * 是否启用Elasticsearch支持
         */
        private boolean enabled = true;

        /**
         * 索引名称前缀
         * <p>用于区分不同环境或租户的索引。比如在开发环境可以使用
         * "dev-audit"，在生产环境使用"prod-audit"。</p>
         */
        @NotBlank(message = "索引前缀不能为空")
        @Pattern(regexp = "^[a-z][a-z0-9-]*$", message = "索引前缀只能包含小写字母、数字和连字符，且必须以字母开头")
        private String indexPrefix = "audit";

        /**
         * 每个索引的最大文档数量
         * <p>当索引中的文档数量达到这个值时，系统会创建新的索引。
         * 这种设计称为"索引滚动"，有助于保持查询性能。</p>
         */
        @Min(value = 1000, message = "索引最大文档数不能小于1000")
        private long maxDocsPerIndex = 10000000; // 1千万

        /**
         * 索引的最大大小（字节）
         */
        @Min(value = 1024 * 1024, message = "索引最大大小不能小于1MB")
        private long maxIndexSize = 50L * 1024 * 1024 * 1024; // 50GB

        /**
         * 是否启用索引压缩
         * <p>压缩可以节省存储空间，但会增加CPU使用。</p>
         */
        private boolean compressionEnabled = true;

        /**
         * 索引的副本数量
         * <p>副本提供了数据冗余和查询性能提升。</p>
         */
        @Min(value = 0, message = "副本数量不能小于0")
        @Max(value = 5, message = "副本数量不能超过5")
        private int numberOfReplicas = 1;

        /**
         * 索引的分片数量
         */
        @Min(value = 1, message = "分片数量不能小于1")
        @Max(value = 10, message = "分片数量不能超过10")
        private int numberOfShards = 3;
    }

    /**
     * <h3>性能监控配置</h3>
     *
     * <p>为了确保审计系统本身不会成为性能瓶颈，我们需要监控审计系统的
     * 各项性能指标，如处理延迟、队列长度、错误率等。</p>
     */
    @Setter
    @Getter
    public static class MonitoringConfig {

        /**
         * 健康检查配置
         */
        @Valid
        @NestedConfigurationProperty
        private HealthCheckConfig healthCheck = new HealthCheckConfig();

        /**
         * 指标收集配置
         */
        @Valid
        @NestedConfigurationProperty
        private MetricsConfig metrics = new MetricsConfig();

        /**
         * 告警配置
         */
        @Valid
        @NestedConfigurationProperty
        private AlertConfig alert = new AlertConfig();

        @Setter
        @Getter
        public static class HealthCheckConfig {
            /**
             * 是否启用健康检查
             */
            private boolean enabled = true;

            /**
             * 健康检查间隔（秒）
             */
            @Min(value = 5, message = "健康检查间隔不能小于5秒")
            @Max(value = 3600, message = "健康检查间隔不能超过1小时")
            private int intervalSeconds = 30;

            /**
             * 健康检查超时时间（毫秒）
             */
            @Min(value = 100, message = "健康检查超时不能小于100毫秒")
            private long timeoutMs = 5000;

            /**
             * 连续失败多少次后认为不健康
             */
            @Min(value = 1, message = "失败阈值不能小于1")
            private int failureThreshold = 3;
        }

        @Setter
        @Getter
        public static class MetricsConfig {
            /**
             * 是否启用指标收集
             */
            private boolean enabled = true;

            /**
             * 指标收集间隔（秒）
             */
            @Min(value = 1, message = "指标收集间隔不能小于1秒")
            private int intervalSeconds = 60;

            /**
             * 是否收集详细指标
             * <p>详细指标包含更多信息，但会增加系统开销。</p>
             */
            private boolean detailedMetrics = false;
        }

        @Setter
        @Getter
        public static class AlertConfig {
            /**
             * 是否启用告警
             */
            private boolean enabled = true;

            /**
             * 错误率告警阈值（百分比）
             */
            @DecimalMin(value = "0.0", message = "错误率阈值不能小于0")
            @DecimalMax(value = "100.0", message = "错误率阈值不能超过100")
            private double errorRateThreshold = 5.0;

            /**
             * 响应时间告警阈值（毫秒）
             */
            @Min(value = 1, message = "响应时间阈值不能小于1毫秒")
            private long responseTimeThreshold = 5000;
        }
    }

    /**
     * <h3>缓存配置</h3>
     *
     * <p>缓存能够显著提高查询性能，特别是对于频繁访问的数据。
     * 但缓存也会占用内存，需要合理配置。</p>
     */
    @Setter
    @Getter
    public static class CacheConfig {

        /**
         * 是否启用缓存
         */
        private boolean enabled = true;

        /**
         * 缓存提供商
         * <p>支持的缓存提供商：CAFFEINE, REDIS, HAZELCAST</p>
         */
        @NotNull(message = "缓存提供商不能为空")
        private CacheProvider provider = CacheProvider.CAFFEINE;

        /**
         * 缓存的最大条目数
         */
        @Min(value = 100, message = "缓存最大条目数不能小于100")
        private long maxEntries = 10000;

        /**
         * 缓存过期时间（秒）
         */
        @Min(value = 60, message = "缓存过期时间不能小于60秒")
        private long expireAfterWriteSeconds = 3600; // 1小时

        /**
         * 缓存访问后过期时间（秒）
         */
        @Min(value = 60, message = "缓存访问后过期时间不能小于60秒")
        private long expireAfterAccessSeconds = 7200; // 2小时

        public enum CacheProvider {
            CAFFEINE, REDIS, HAZELCAST
        }
    }

    /**
     * <h3>数据压缩配置</h3>
     *
     * <p>对于大量的审计数据，压缩可以显著节省存储空间。</p>
     */
    @Setter
    @Getter
    public static class CompressionConfig {

        /**
         * 是否启用压缩
         */
        private boolean enabled = false;

        /**
         * 压缩算法
         * <p>支持的压缩算法：GZIP, SNAPPY, LZ4</p>
         */
        @NotNull(message = "压缩算法不能为空")
        private CompressionAlgorithm algorithm = CompressionAlgorithm.GZIP;

        /**
         * 压缩级别（1-9）
         * <p>级别越高，压缩率越高，但CPU消耗也越大。</p>
         */
        @Min(value = 1, message = "压缩级别不能小于1")
        @Max(value = 9, message = "压缩级别不能超过9")
        private int level = 6;

        /**
         * 启用压缩的最小数据大小（字节）
         * <p>只有超过这个大小的数据才会被压缩。</p>
         */
        @Min(value = 1, message = "最小压缩数据大小不能小于1字节")
        private int minSizeBytes = 1024; // 1KB

        public enum CompressionAlgorithm {
            GZIP, SNAPPY, LZ4
        }
    }

    // =================== 配置验证方法 ===================

    /**
     * 验证配置的一致性
     *
     * <p>有些配置之间存在依赖关系，这个方法检查这些依赖关系是否合理。</p>
     */
    public void validateConfiguration() {
        // 在高性能模式下，建议启用批处理和异步处理
        if (mode == AuditMode.HIGH_PERFORMANCE) {
            if (!async.isEnabled() || !batch.isEnabled()) {
                throw new IllegalArgumentException(
                        "高性能模式下建议启用异步处理和批处理功能");
            }
        }

        // 在合规模式下，不建议启用压缩（可能影响数据完整性检查）
        if (mode == AuditMode.COMPLIANCE && compression.isEnabled()) {
            throw new IllegalArgumentException(
                    "合规模式下不建议启用数据压缩");
        }

        // 验证Elasticsearch配置
        if (elasticsearch.isEnabled() && elasticsearch.getMaxDocsPerIndex() <= 0) {
            throw new IllegalArgumentException(
                    "Elasticsearch最大文档数必须大于0");
        }
    }

    /**
     * 获取配置摘要信息
     *
     * <p>返回当前配置的概要信息，便于日志记录和调试。</p>
     */
    public String getConfigurationSummary() {
        return String.format(
                "审计配置摘要 - 模式: %s, 异步: %s, 批处理: %s, ES: %s, 监控: %s",
                mode.getDescription(),
                async.isEnabled() ? "启用" : "禁用",
                batch.isEnabled() ? "启用" : "禁用",
                elasticsearch.isEnabled() ? "启用" : "禁用",
                monitoring.getHealthCheck().isEnabled() ? "启用" : "禁用"
        );
    }
}