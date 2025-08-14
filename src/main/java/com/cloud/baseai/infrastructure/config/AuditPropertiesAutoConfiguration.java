package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.domain.audit.repository.SysAuditLogRepository;
import com.cloud.baseai.domain.audit.service.AuditService;
import com.cloud.baseai.infrastructure.config.base.BaseAutoConfiguration;
import com.cloud.baseai.infrastructure.config.properties.AuditProperties;
import com.cloud.baseai.infrastructure.monitoring.AuditHealthIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>审计模块自动配置类</h2>
 *
 * <p>这个自动配置类是审计模块的"总指挥官"，它负责协调和初始化审计系统的
 * 所有核心组件。就像交响乐团的指挥一样，确保每个组件都能在正确的时间
 * 以正确的方式发挥作用。</p>
 *
 * <p><b>配置策略：</b></p>
 * <p>我们采用了Spring Boot的自动配置机制，通过条件注解来智能地决定
 * 哪些组件需要被创建。这种设计让系统既能开箱即用，又保持了足够的灵活性
 * 来适应不同的部署环境和业务需求。</p>
 *
 * <p><b>依赖管理：</b></p>
 * <p>配置类会根据类路径上的依赖来决定创建哪些Bean。比如，只有在
 * Elasticsearch依赖存在时才会创建相关的配置，这样可以避免在简单
 * 部署场景中引入不必要的复杂性。</p>
 */
@Configuration
public class AuditPropertiesAutoConfiguration extends BaseAutoConfiguration {

    private final AuditProperties auditProperties;

    public AuditPropertiesAutoConfiguration(AuditProperties auditProperties) {
        this.auditProperties = auditProperties;

        // 统一初始化
        initializeConfiguration();
    }

    @Override
    protected String getConfigurationName() {
        return "审计日志系统";
    }

    @Override
    protected String getModuleName() {
        return "AUDIT";
    }

    @Override
    protected void validateConfiguration() {
        logInfo("开始验证审计系统配置...");

        // 基础配置验证
        validateNotNull(auditProperties, "审计配置属性");

        // 日志记录配置验证
        validateLoggingConfiguration();

        // 数据保留策略验证
        validateRetentionConfiguration();

        // Elasticsearch配置验证
        validateElasticsearchConfiguration();

        // 完整性检查配置验证
        validateIntegrityConfiguration();

        // 报告配置验证
        validateReportingConfiguration();

        logSuccess("审计系统配置验证通过");
    }

    /**
     * 验证日志记录配置
     */
    private void validateLoggingConfiguration() {
        logInfo("验证审计日志记录配置...");

        AuditProperties.LoggingProperties logging = auditProperties.getLogging();
        validateNotNull(logging, "日志记录配置");

        // 异步配置验证
        if (logging.getEnableAsync()) {
            validateRange(logging.getAsyncQueueSize(), 100, 100000, "异步队列大小");
            validateRange(logging.getBatchSize(), 10, 1000, "批量处理大小");
            validateRange(logging.getBatchTimeoutMs(), 1000, 30000, "批量处理超时时间");

            if (logging.getAsyncQueueSize() < logging.getBatchSize() * 10) {
                logWarning("异步队列大小可能过小，建议至少为批量大小的10倍");
            }
        }

        // 日志级别验证
        List<String> validLevels = List.of("ERROR", "WARN", "INFO", "DEBUG", "TRACE");
        for (String level : logging.getLogLevels()) {
            if (!validLevels.contains(level.toUpperCase())) {
                logWarning("无效的日志级别: %s", level);
            }
        }

        // 敏感字段验证
        validateNotEmpty(logging.getSensitiveFields(), "敏感字段列表");

        logInfo("日志记录配置 - 异步: %s, 批量大小: %d, 敏感字段: %d个",
                logging.getEnableAsync() ? "启用" : "禁用",
                logging.getBatchSize(),
                logging.getSensitiveFields().size());
    }

    /**
     * 验证数据保留策略配置
     */
    private void validateRetentionConfiguration() {
        logInfo("验证数据保留策略配置...");

        AuditProperties.RetentionProperties retention = auditProperties.getRetention();
        validateNotNull(retention, "数据保留策略配置");

        // 保留天数验证
        validateRange(retention.getDefaultDays(), 1, 10000, "默认保留天数");

        // 按级别保留策略验证
        Map<String, Integer> byLevel = retention.getByLevel();
        for (Map.Entry<String, Integer> entry : byLevel.entrySet()) {
            validateRange(entry.getValue(), 1, 10000,
                    String.format("'%s'级别保留天数", entry.getKey()));
        }

        // 归档配置验证
        if (retention.getEnableAutoArchive()) {
            validateRange(retention.getArchiveAfterDays(), 1, retention.getDefaultDays(),
                    "归档阈值天数");
            validateNotBlank(retention.getArchiveCompression(), "归档压缩方式");

            List<String> validCompressions = List.of("gzip", "zip", "bzip2", "lz4");
            if (!validCompressions.contains(retention.getArchiveCompression().toLowerCase())) {
                logWarning("不支持的压缩方式: %s，支持的方式: %s",
                        retention.getArchiveCompression(), validCompressions);
            }
        }

        // 彻底删除配置验证
        validateRange(retention.getPurgeAfterDays(), retention.getDefaultDays(), 20000,
                "彻底删除阈值天数");

        logInfo("数据保留策略 - 默认: %d天, 归档: %s, 法律保留: %s",
                retention.getDefaultDays(),
                retention.getEnableAutoArchive() ? "启用" : "禁用",
                retention.getEnableLegalHold() ? "启用" : "禁用");
    }

    /**
     * 验证Elasticsearch配置
     */
    private void validateElasticsearchConfiguration() {
        AuditProperties.ElasticsearchProperties es = auditProperties.getElasticsearch();

        if (!es.getEnabled()) {
            logInfo("Elasticsearch存储已禁用，将使用数据库存储");
            return;
        }

        logInfo("验证Elasticsearch配置...");

        validateNotBlank(es.getIndexPrefix(), "索引前缀");
        validateNotBlank(es.getIndexTemplate(), "索引模板名称");
        validateRange(es.getShardsPerIndex(), 1, 10, "每个索引的分片数");
        validateRange(es.getReplicasPerIndex(), 0, 5, "每个索引的副本数");
        validateNotBlank(es.getRefreshInterval(), "刷新间隔");

        // ILM策略验证
        if (es.getEnableIlm()) {
            AuditProperties.IlmPolicyProperties ilm = es.getIlmPolicy();
            validateRange(ilm.getHotPhaseDays(), 1, 365, "热阶段天数");
            validateRange(ilm.getWarmPhaseDays(), ilm.getHotPhaseDays(), 365, "温阶段天数");
            validateRange(ilm.getColdPhaseDays(), ilm.getWarmPhaseDays(), 3650, "冷阶段天数");
            validateRange(ilm.getDeletePhaseDays(), ilm.getColdPhaseDays(), 10000, "删除阶段天数");
        }

        logInfo("Elasticsearch配置 - 索引前缀: %s, 分片: %d, 副本: %d, ILM: %s",
                es.getIndexPrefix(), es.getShardsPerIndex(), es.getReplicasPerIndex(),
                es.getEnableIlm() ? "启用" : "禁用");
    }

    /**
     * 验证完整性检查配置
     */
    private void validateIntegrityConfiguration() {
        AuditProperties.IntegrityProperties integrity = auditProperties.getIntegrity();

        if (!integrity.getEnabled()) {
            logInfo("完整性检查已禁用");
            return;
        }

        logInfo("验证完整性检查配置...");

        validateNotBlank(integrity.getSchedule(), "检查调度表达式");

        List<String> validMethods = List.of("checksum", "count", "sampling");
        validateEnum(integrity.getCheckMethod(), validMethods, "检查方式");

        if ("sampling".equals(integrity.getCheckMethod())) {
            validateRange(integrity.getSamplingRatio().intValue(), 0, 1, "采样检查比例");
            if (integrity.getSamplingRatio() < 0.01) {
                logWarning("采样比例过低 (%.3f)，可能影响检查效果", integrity.getSamplingRatio());
            }
        }

        logInfo("完整性检查 - 方式: %s, 调度: %s",
                integrity.getCheckMethod(), integrity.getSchedule());
    }

    /**
     * 验证报告配置
     */
    private void validateReportingConfiguration() {
        AuditProperties.ReportingProperties reporting = auditProperties.getReporting();

        if (!reporting.getEnabled()) {
            logInfo("报告功能已禁用");
            return;
        }

        logInfo("验证报告配置...");

        validateNotBlank(reporting.getOutputLocation(), "报告输出位置");

        List<String> validFormats = List.of("PDF", "EXCEL", "CSV", "HTML");
        validateEnum(reporting.getDefaultFormat().toUpperCase(), validFormats, "报告格式");

        validateNotBlank(reporting.getAutoSchedule(), "自动报告调度表达式");
        validateRange(reporting.getReportRetentionDays(), 1, 3650, "报告保留天数");

        logInfo("报告配置 - 格式: %s, 输出位置: %s, 保留: %d天",
                reporting.getDefaultFormat(), reporting.getOutputLocation(),
                reporting.getReportRetentionDays());
    }

    @Override
    protected Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();

        // 日志记录配置摘要
        AuditProperties.LoggingProperties logging = auditProperties.getLogging();
        summary.put("异步处理", logging.getEnableAsync() ? "启用" : "禁用");
        summary.put("批量大小", logging.getBatchSize());
        summary.put("日志级别", String.join(", ", logging.getLogLevels()));
        summary.put("敏感字段数量", logging.getSensitiveFields().size());

        // 数据保留配置摘要
        AuditProperties.RetentionProperties retention = auditProperties.getRetention();
        summary.put("默认保留天数", retention.getDefaultDays());
        summary.put("自动归档", retention.getEnableAutoArchive() ? "启用" : "禁用");
        summary.put("法律保留", retention.getEnableLegalHold() ? "启用" : "禁用");

        // Elasticsearch配置摘要
        AuditProperties.ElasticsearchProperties es = auditProperties.getElasticsearch();
        summary.put("Elasticsearch存储", es.getEnabled() ? "启用" : "禁用");
        if (es.getEnabled()) {
            summary.put("ES索引前缀", es.getIndexPrefix());
            summary.put("ES生命周期管理", es.getEnableIlm() ? "启用" : "禁用");
        }

        // 完整性检查配置摘要
        AuditProperties.IntegrityProperties integrity = auditProperties.getIntegrity();
        summary.put("完整性检查", integrity.getEnabled() ? "启用" : "禁用");
        if (integrity.getEnabled()) {
            summary.put("检查方式", integrity.getCheckMethod());
        }

        // 报告配置摘要
        AuditProperties.ReportingProperties reporting = auditProperties.getReporting();
        summary.put("报告功能", reporting.getEnabled() ? "启用" : "禁用");
        if (reporting.getEnabled()) {
            summary.put("报告格式", reporting.getDefaultFormat());
        }

        return summary;
    }

    /**
     * 审计健康指示器配置
     *
     * <p>健康检查是生产环境中的重要组件，它能够及时发现审计系统的问题。</p>
     */
    @Bean
    @ConditionalOnMissingBean(AuditHealthIndicator.class)
    @ConditionalOnProperty(name = "baseai.audit.monitoring.health-check.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(SysAuditLogRepository.class)
    public AuditHealthIndicator auditHealthIndicator(SysAuditLogRepository auditLogRepository) {
        logBeanCreation("AuditHealthIndicator", "审计系统健康检查指示器");

        AuditHealthIndicator indicator = new AuditHealthIndicator(auditLogRepository, auditProperties);

        logInfo("健康检查配置完成，将监控审计日志存储状态");
        logBeanSuccess("AuditHealthIndicator");

        return indicator;
    }

    /**
     * 审计数据保留管理器配置
     *
     * <p>这个组件负责管理审计数据的生命周期，包括归档和清理操作。</p>
     */
    @Bean
    @ConditionalOnProperty(name = "baseai.audit.retention.auto-cleanup.enabled", havingValue = "true")
    @ConditionalOnBean(AuditService.class)
    public AuditRetentionManager auditRetentionManager(AuditService auditService) {
        logBeanCreation("AuditRetentionManager", "审计数据保留管理器");

        AuditRetentionManager manager = new AuditRetentionManager(auditService, auditProperties);

        logInfo("数据保留管理器配置完成 - 默认保留: %d天, 自动归档: %s",
                auditProperties.getRetention().getDefaultDays(),
                auditProperties.getRetention().getEnableAutoArchive() ? "启用" : "禁用");

        logBeanSuccess("AuditRetentionManager");
        return manager;
    }

    /**
     * 审计完整性检查器配置
     */
    @Bean
    @ConditionalOnProperty(name = "baseai.audit.integrity.enabled", havingValue = "true")
    @ConditionalOnBean(AuditService.class)
    public AuditIntegrityChecker auditIntegrityChecker(AuditService auditService) {
        logBeanCreation("AuditIntegrityChecker", "审计数据完整性检查器");

        AuditIntegrityChecker checker = new AuditIntegrityChecker(auditService, auditProperties);

        logInfo("完整性检查器配置完成 - 方式: %s, 调度: %s",
                auditProperties.getIntegrity().getCheckMethod(),
                auditProperties.getIntegrity().getSchedule());

        logBeanSuccess("AuditIntegrityChecker");
        return checker;
    }

    /**
     * 审计报告生成器配置
     */
    @Bean
    @ConditionalOnProperty(name = "baseai.audit.reporting.enabled", havingValue = "true")
    @ConditionalOnBean(AuditService.class)
    public AuditReportGenerator auditReportGenerator(AuditService auditService) {
        logBeanCreation("AuditReportGenerator", "审计报告生成器");

        AuditReportGenerator generator = new AuditReportGenerator(auditService, auditProperties);

        logInfo("报告生成器配置完成 - 格式: %s, 输出位置: %s",
                auditProperties.getReporting().getDefaultFormat(),
                auditProperties.getReporting().getOutputLocation());

        logBeanSuccess("AuditReportGenerator");
        return generator;
    }

    // =================== 内部配置组件 ===================

    /**
     * 审计数据保留管理器
     *
     * <p>负责执行数据清理和归档策略。</p>
     */
    public static class AuditRetentionManager {
        private final AuditService auditService;
        private final AuditProperties properties;

        public AuditRetentionManager(AuditService auditService, AuditProperties properties) {
            this.auditService = auditService;
            this.properties = properties;
        }

        /**
         * 定期执行数据清理任务
         */
//        @Scheduled(cron = "0 2 * * * ?") // 每天凌晨2点执行
        public void performRetentionTasks() {
            if (!properties.getRetention().getEnableAutoArchive()) {
                return;
            }

            try {
                AuditProperties.RetentionProperties retention = properties.getRetention();

                // 执行归档
                if (retention.getEnableAutoArchive()) {
//                    int archivedCount = auditService.archiveOldRecords(retention.getArchiveAfterDays());
//                    if (archivedCount > 0) {
//                        logInfo("已归档 %d 条审计记录", archivedCount);
//                    }
                }

                // 执行清理
//                int purgedCount = auditService.purgeOldRecords(retention.getPurgeAfterDays());
//                if (purgedCount > 0) {
//                    logInfo("已清理 %d 条过期审计记录", purgedCount);
//                }

            } catch (Exception e) {
                logWarning("执行数据保留任务时发生错误: %s", e.getMessage());
            }
        }

        private void logInfo(String message, Object... args) {
            // 这里应该使用实际的日志记录器
            System.out.printf("[AUDIT-RETENTION] " + message + "%n", args);
        }

        private void logWarning(String message, Object... args) {
            // 这里应该使用实际的日志记录器
            System.err.printf("[AUDIT-RETENTION] " + message + "%n", args);
        }
    }

    /**
     * 审计完整性检查器
     */
    public static class AuditIntegrityChecker {
        private final AuditService auditService;
        private final AuditProperties properties;

        public AuditIntegrityChecker(AuditService auditService, AuditProperties properties) {
            this.auditService = auditService;
            this.properties = properties;
        }

        /**
         * 定期执行完整性检查
         */
//        @Scheduled(cron = "${baseai.audit.integrity.schedule:0 2 * * *}")
        public void performIntegrityCheck() {
            if (!properties.getIntegrity().getEnabled()) {
                return;
            }

            try {
                AuditProperties.IntegrityProperties integrity = properties.getIntegrity();

//                boolean isValid = switch (integrity.getCheckMethod()) {
//                    case "checksum" -> auditService.validateChecksums();
//                    case "count" -> auditService.validateRecordCounts();
//                    case "sampling" -> auditService.validateSampling(integrity.getSamplingRatio());
//                    default -> false;
//                };
//
//                if (isValid) {
//                    logInfo("审计数据完整性检查通过");
//                } else {
//                    logWarning("审计数据完整性检查发现问题，请立即检查");
//                }

            } catch (Exception e) {
                logWarning("执行完整性检查时发生错误: %s", e.getMessage());
            }
        }

        private void logInfo(String message, Object... args) {
            System.out.printf("[AUDIT-INTEGRITY] " + message + "%n", args);
        }

        private void logWarning(String message, Object... args) {
            System.err.printf("[AUDIT-INTEGRITY] " + message + "%n", args);
        }
    }

    /**
     * 审计报告生成器
     */
    public static class AuditReportGenerator {
        private final AuditService auditService;
        private final AuditProperties properties;

        public AuditReportGenerator(AuditService auditService, AuditProperties properties) {
            this.auditService = auditService;
            this.properties = properties;
        }

        /**
         * 定期生成审计报告
         */
//        @Scheduled(cron = "${baseai.audit.reporting.auto-schedule:0 0 1 * *}")
        public void generateMonthlyReport() {
            if (!properties.getReporting().getEnabled()) {
                return;
            }

            try {
                AuditProperties.ReportingProperties reporting = properties.getReporting();

                LocalDateTime endDate = LocalDateTime.now();
                LocalDateTime startDate = endDate.minusMonths(1);

//                String reportPath = auditService.generateReport(
//                        startDate, endDate, reporting.getDefaultFormat(), reporting.getOutputLocation());
//
//                logInfo("月度审计报告已生成: %s", reportPath);
//
//                // 清理过期报告
//                auditService.cleanupOldReports(reporting.getReportRetentionDays());

            } catch (Exception e) {
                logWarning("生成审计报告时发生错误: %s", e.getMessage());
            }
        }

        private void logInfo(String message, Object... args) {
            System.out.printf("[AUDIT-REPORT] " + message + "%n", args);
        }

        private void logWarning(String message, Object... args) {
            System.err.printf("[AUDIT-REPORT] " + message + "%n", args);
        }
    }
}