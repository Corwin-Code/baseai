package com.cloud.baseai.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * <h2>审计系统配置属性类</h2>
 *
 * <p>该类管理系统审计功能的配置，包括日志记录、数据保留策略、
 * Elasticsearch存储、完整性检查和报告生成等核心审计功能。</p>
 */
@Data
@ConfigurationProperties(prefix = "baseai.audit")
public class AuditProperties {

    /**
     * 日志记录配置
     */
    private LoggingProperties logging = new LoggingProperties();

    /**
     * 数据保留策略
     */
    private RetentionProperties retention = new RetentionProperties();

    /**
     * Elasticsearch存储配置
     */
    private ElasticsearchProperties elasticsearch = new ElasticsearchProperties();

    /**
     * 完整性检查配置
     */
    private IntegrityProperties integrity = new IntegrityProperties();

    /**
     * 报告配置
     */
    private ReportingProperties reporting = new ReportingProperties();

    /**
     * 日志记录配置内部类
     */
    @Data
    public static class LoggingProperties {
        /**
         * 是否启用异步处理
         */
        private Boolean enableAsync = true;

        /**
         * 异步队列大小
         */
        private Integer asyncQueueSize = 10000;

        /**
         * 批量处理大小
         */
        private Integer batchSize = 100;

        /**
         * 批量处理超时时间（毫秒）
         */
        private Integer batchTimeoutMs = 5000;

        /**
         * 是否启用压缩
         */
        private Boolean enableCompression = true;

        /**
         * 需要记录的日志级别
         */
        private List<String> logLevels = List.of("ERROR", "WARN", "INFO", "DEBUG");

        /**
         * 敏感字段列表（将被屏蔽）
         */
        private List<String> sensitiveFields = List.of("password", "token", "key", "secret", "apiKey");

        /**
         * 是否启用字段屏蔽
         */
        private Boolean enableFieldMasking = true;
    }

    /**
     * 数据保留策略配置内部类
     */
    @Data
    public static class RetentionProperties {
        /**
         * 默认保留天数（7年）
         */
        private Integer defaultDays = 2555;

        /**
         * 按级别的保留策略
         */
        private Map<String, Integer> byLevel = Map.of(
                "ERROR", 3650,  // 错误日志保留10年
                "WARN", 1825,   // 警告日志保留5年
                "INFO", 365,    // 信息日志保留1年
                "DEBUG", 30     // 调试日志保留30天
        );

        /**
         * 是否启用自动归档
         */
        private Boolean enableAutoArchive = true;

        /**
         * 归档阈值天数
         */
        private Integer archiveAfterDays = 365;

        /**
         * 归档压缩方式
         */
        private String archiveCompression = "gzip";

        /**
         * 彻底删除阈值天数
         */
        private Integer purgeAfterDays = 2555;

        /**
         * 是否启用法律保留
         */
        private Boolean enableLegalHold = true;
    }

    /**
     * Elasticsearch存储配置内部类
     */
    @Data
    public static class ElasticsearchProperties {
        /**
         * 是否启用ES存储
         */
        private Boolean enabled = true;

        /**
         * 索引名称前缀
         */
        private String indexPrefix = "baseai-audit";

        /**
         * 索引模板名称
         */
        private String indexTemplate = "audit-template";

        /**
         * 每个索引的分片数
         */
        private Integer shardsPerIndex = 1;

        /**
         * 每个索引的副本数
         */
        private Integer replicasPerIndex = 1;

        /**
         * 刷新间隔
         */
        private String refreshInterval = "30s";

        /**
         * 是否启用索引生命周期管理
         */
        private Boolean enableIlm = true;

        /**
         * 生命周期管理策略
         */
        private IlmPolicyProperties ilmPolicy = new IlmPolicyProperties();
    }

    /**
     * 索引生命周期管理策略内部类
     */
    @Data
    public static class IlmPolicyProperties {
        /**
         * 热阶段：7天
         */
        private Integer hotPhaseDays = 7;

        /**
         * 温阶段：30天
         */
        private Integer warmPhaseDays = 30;

        /**
         * 冷阶段：90天
         */
        private Integer coldPhaseDays = 90;

        /**
         * 删除阶段：7年
         */
        private Integer deletePhaseDays = 2555;
    }

    /**
     * 完整性检查配置内部类
     */
    @Data
    public static class IntegrityProperties {
        /**
         * 是否启用完整性检查
         */
        private Boolean enabled = true;

        /**
         * 检查调度表达式（每天凌晨2点）
         */
        private String schedule = "0 2 * * *";

        /**
         * 检查方式：checksum、count、sampling
         */
        private String checkMethod = "checksum";

        /**
         * 采样检查比例（当使用sampling方法时）
         */
        private Double samplingRatio = 0.1;
    }

    /**
     * 报告配置内部类
     */
    @Data
    public static class ReportingProperties {
        /**
         * 是否启用报告功能
         */
        private Boolean enabled = true;

        /**
         * 报告输出位置
         */
        private String outputLocation = "/tmp/audit-reports/";

        /**
         * 报告格式：PDF、EXCEL、CSV
         */
        private String defaultFormat = "PDF";

        /**
         * 自动报告调度（每月1号生成月度报告）
         */
        private String autoSchedule = "0 0 1 * *";

        /**
         * 报告保留天数
         */
        private Integer reportRetentionDays = 365;
    }
}