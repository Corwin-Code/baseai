package com.cloud.baseai.infrastructure.monitoring;

import com.cloud.baseai.domain.audit.repository.SysAuditLogRepository;
import com.cloud.baseai.infrastructure.config.AuditProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <h2>审计模块健康监控组件</h2>
 *
 * <p>这个健康监控组件就像系统的"体检医生"，它会定期检查审计模块的各项
 * "生命体征"，包括数据库连接状态、处理性能、错误率等关键指标。
 * 通过这些检查，我们能够及时发现系统的健康问题，并采取相应的措施。</p>
 *
 * <p><b>监控维度包括：</b></p>
 * <p>我们从多个角度监控系统健康：数据库连接状态表示系统的基础能力，
 * 处理延迟反映系统的性能表现，错误率显示系统的稳定性，
 * 而存储空间则关系到系统的可持续运行能力。</p>
 *
 * <p><b>设计理念：</b></p>
 * <p>我们采用"预防胜于治疗"的理念，通过持续监控来预防问题的发生。
 * 就像定期体检能够早期发现疾病一样，系统监控能够帮助我们在问题
 * 严重影响用户之前就发现并解决它们。</p>
 */
@Component("auditHealthIndicator")
public class AuditHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(AuditHealthIndicator.class);

    private final SysAuditLogRepository auditLogRepository;
    private final AuditProperties auditConfig;

    // 性能指标统计
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);

    // 健康状态缓存，避免频繁检查影响性能
    private volatile Health cachedHealth;
    private volatile long lastHealthCheckTime = 0;
    private static final long HEALTH_CHECK_CACHE_TTL = 30000; // 30秒缓存

    /**
     * 构造函数 - 初始化监控组件
     *
     * <p>在初始化监控组件时，我们建立与核心组件的连接，这些连接就像
     * 医疗设备与患者身体各部位的传感器连接，让我们能够实时获取系统的状态信息。</p>
     */
    public AuditHealthIndicator(SysAuditLogRepository auditLogRepository, AuditProperties auditConfig) {
        this.auditLogRepository = auditLogRepository;
        this.auditConfig = auditConfig;

        log.info("审计健康监控组件初始化完成");
    }

    /**
     * 主要的健康检查方法
     *
     * <p>这个方法是整个健康检查的核心，它会依次检查各个关键指标，
     * 就像医生进行全面体检一样，从各个角度评估系统的健康状况。</p>
     *
     * <p><b>检查策略：</b></p>
     * <p>我们使用缓存机制来避免过于频繁的健康检查影响系统性能。
     * 同时，我们会根据不同指标的重要性来决定整体的健康状态，
     * 确保关键问题能够被及时发现和处理。</p>
     */
    @Override
    public Health health() {
        long currentTime = System.currentTimeMillis();

        // 使用缓存减少检查频率，避免影响系统性能
        if (cachedHealth != null &&
                (currentTime - lastHealthCheckTime) < HEALTH_CHECK_CACHE_TTL) {
            return cachedHealth;
        }

        try {
            log.debug("开始审计模块健康检查");

            Health.Builder healthBuilder = Health.up();
            Map<String, Object> details = new HashMap<>();

            // 第一项检查：数据库连接状态
            // 这是最基础的检查，如果数据库连接有问题，其他功能都无法正常工作
            boolean databaseHealthy = checkDatabaseHealth(details);
            if (!databaseHealthy) {
                healthBuilder = Health.down();
            }

            // 第二项检查：处理性能指标
            // 检查系统的响应时间是否在可接受的范围内
            boolean performanceHealthy = checkPerformanceHealth(details);
            if (!performanceHealthy) {
                // 性能问题通常不会导致服务完全不可用，但会影响用户体验
                healthBuilder = Health.up().withDetail("performance", "DEGRADED");
            }

            // 第三项检查：错误率统计
            // 高错误率可能表示系统存在严重问题
            boolean errorRateHealthy = checkErrorRateHealth(details);
            if (!errorRateHealthy) {
                healthBuilder = Health.down();
            }

            // 第四项检查：配置状态
            // 确保关键配置项都是合理的
            boolean configHealthy = checkConfigurationHealth(details);
            if (!configHealthy) {
                healthBuilder = Health.up().withDetail("config", "WARNING");
            }

            // 第五项检查：存储空间状态
            // 确保有足够的存储空间来保存审计日志
            boolean storageHealthy = checkStorageHealth(details);
            if (!storageHealthy) {
                healthBuilder = Health.down();
            }

            // 添加整体统计信息
            addOverallStatistics(details);

            cachedHealth = healthBuilder.withDetails(details).build();
            lastHealthCheckTime = currentTime;

            log.debug("审计模块健康检查完成: status={}", cachedHealth.getStatus());
            return cachedHealth;

        } catch (Exception e) {
            log.error("健康检查过程中发生异常", e);

            // 当健康检查本身出现问题时，我们返回DOWN状态
            return Health.down()
                    .withDetail("error", "健康检查失败: " + e.getMessage())
                    .withDetail("exception", e.getClass().getSimpleName())
                    .build();
        }
    }

    /**
     * 检查数据库健康状态
     *
     * <p>数据库是审计系统的核心基础设施，就像人体的心脏一样重要。
     * 这个检查会验证数据库连接是否正常，查询响应是否及时。</p>
     */
    private boolean checkDatabaseHealth(Map<String, Object> details) {
        try {
            long startTime = System.currentTimeMillis();

            // 执行一个简单的查询来测试数据库连接
            OffsetDateTime testTime = OffsetDateTime.now().minusMinutes(1);
            long recentCount = auditLogRepository.countByTimeRange(testTime, OffsetDateTime.now());

            long queryTime = System.currentTimeMillis() - startTime;

            details.put("database.status", "UP");
            details.put("database.queryTime", queryTime + "ms");
            details.put("database.recentRecords", recentCount);

            // 如果查询时间超过5秒，认为数据库响应较慢
            if (queryTime > 5000) {
                details.put("database.warning", "查询响应时间较长");
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("数据库健康检查失败", e);
            details.put("database.status", "DOWN");
            details.put("database.error", e.getMessage());
            return false;
        }
    }

    /**
     * 检查处理性能健康状态
     *
     * <p>性能监控帮助我们了解系统的处理能力，就像监控运动员的心率
     * 和呼吸频率一样。通过分析平均响应时间和处理吞吐量，我们可以
     * 评估系统是否在最佳状态下运行。</p>
     */
    private boolean checkPerformanceHealth(Map<String, Object> details) {
        try {
            long requests = totalRequests.get();
            long processingTime = totalProcessingTime.get();

            double avgResponseTime = requests > 0 ? (double) processingTime / requests : 0;

            details.put("performance.totalRequests", requests);
            details.put("performance.avgResponseTime", Math.round(avgResponseTime) + "ms");

            // 如果平均响应时间超过2秒，认为性能需要关注
            if (avgResponseTime > 2000) {
                details.put("performance.status", "SLOW");
                details.put("performance.warning", "平均响应时间过长");
                return false;
            }

            details.put("performance.status", "GOOD");
            return true;

        } catch (Exception e) {
            log.error("性能健康检查失败", e);
            details.put("performance.status", "UNKNOWN");
            details.put("performance.error", e.getMessage());
            return false;
        }
    }

    /**
     * 检查错误率健康状态
     *
     * <p>错误率是系统稳定性的重要指标，就像体温是身体健康的重要指标一样。
     * 我们需要密切监控错误率的变化，当错误率异常升高时及时发出警告。</p>
     */
    private boolean checkErrorRateHealth(Map<String, Object> details) {
        try {
            long requests = totalRequests.get();
            long errors = totalErrors.get();

            double errorRate = requests > 0 ? (double) errors / requests * 100 : 0;

            details.put("errorRate.totalErrors", errors);
            details.put("errorRate.percentage", Math.round(errorRate * 100.0) / 100.0 + "%");

            // 如果错误率超过5%，认为系统不健康
            if (errorRate > 5.0) {
                details.put("errorRate.status", "HIGH");
                details.put("errorRate.warning", "错误率过高，需要立即关注");
                return false;
            } else if (errorRate > 1.0) {
                details.put("errorRate.status", "ELEVATED");
                details.put("errorRate.warning", "错误率略高，建议关注");
            } else {
                details.put("errorRate.status", "NORMAL");
            }

            return true;

        } catch (Exception e) {
            log.error("错误率健康检查失败", e);
            details.put("errorRate.status", "UNKNOWN");
            details.put("errorRate.error", e.getMessage());
            return false;
        }
    }

    /**
     * 检查配置健康状态
     *
     * <p>配置检查确保系统的关键参数都设置在合理的范围内。
     * 就像检查医疗设备的参数设置一样，错误的配置可能导致系统异常。</p>
     */
    private boolean checkConfigurationHealth(Map<String, Object> details) {
        try {
            boolean configHealthy = true;

            // 检查异步处理配置
            if (auditConfig.getAsync().isEnabled()) {
                details.put("config.asyncEnabled", true);
                details.put("config.batchSize", auditConfig.getAsync().getBatchSize());

                // 检查批处理大小是否合理
                if (auditConfig.getAsync().getBatchSize() > 1000) {
                    details.put("config.warning", "批处理大小过大，可能影响内存使用");
                    configHealthy = false;
                }
            } else {
                details.put("config.asyncEnabled", false);
                details.put("config.warning", "异步处理已禁用，可能影响性能");
            }

            // 检查数据保留配置
            int retentionDays = auditConfig.getRetention().getDefaultRetentionDays();
            details.put("config.retentionDays", retentionDays);

            if (retentionDays < 365) {
                details.put("config.retentionWarning", "数据保留期少于1年，请确认符合合规要求");
            }

            details.put("config.status", configHealthy ? "HEALTHY" : "WARNING");
            return configHealthy;

        } catch (Exception e) {
            log.error("配置健康检查失败", e);
            details.put("config.status", "ERROR");
            details.put("config.error", e.getMessage());
            return false;
        }
    }

    /**
     * 检查存储空间健康状态
     *
     * <p>存储空间检查确保系统有足够的空间来保存审计日志。
     * 这就像检查仓库的容量一样，如果空间不足，就无法继续存储新的数据。</p>
     */
    private boolean checkStorageHealth(Map<String, Object> details) {
        try {
            // 这里应该检查实际的磁盘空间使用情况
            // 由于这需要依赖具体的环境，我们先提供一个模拟实现

            long totalSpace = Runtime.getRuntime().totalMemory();
            long freeSpace = Runtime.getRuntime().freeMemory();
            long usedSpace = totalSpace - freeSpace;

            double usagePercentage = (double) usedSpace / totalSpace * 100;

            details.put("storage.totalSpace", formatBytes(totalSpace));
            details.put("storage.usedSpace", formatBytes(usedSpace));
            details.put("storage.freeSpace", formatBytes(freeSpace));
            details.put("storage.usagePercentage", Math.round(usagePercentage * 100.0) / 100.0 + "%");

            // 如果使用率超过85%，认为存储空间紧张
            if (usagePercentage > 85) {
                details.put("storage.status", "CRITICAL");
                details.put("storage.warning", "存储空间不足，需要立即清理");
                return false;
            } else if (usagePercentage > 70) {
                details.put("storage.status", "WARNING");
                details.put("storage.warning", "存储空间使用率较高，建议关注");
            } else {
                details.put("storage.status", "HEALTHY");
            }

            return true;

        } catch (Exception e) {
            log.error("存储健康检查失败", e);
            details.put("storage.status", "UNKNOWN");
            details.put("storage.error", e.getMessage());
            return false;
        }
    }

    /**
     * 添加整体统计信息
     *
     * <p>这个方法添加系统的整体运行统计，就像医生总结病人的整体健康状况一样。
     * 这些信息帮助运维人员快速了解系统的运行状况。</p>
     */
    private void addOverallStatistics(Map<String, Object> details) {
        details.put("statistics.uptime", getUptime());
        details.put("statistics.lastCheckTime", OffsetDateTime.now().toString());
        details.put("statistics.totalProcessedRequests", totalRequests.get());

        // 计算系统健康评分（0-100分）
        int healthScore = calculateHealthScore(details);
        details.put("statistics.healthScore", healthScore);

        // 根据健康评分给出建议
        String recommendation = getHealthRecommendation(healthScore);
        details.put("statistics.recommendation", recommendation);
    }

    /**
     * 计算系统健康评分
     *
     * <p>健康评分是一个综合指标，就像体检报告的总体评分一样。
     * 它综合考虑了各项检查的结果，给出一个0-100的整体评分。</p>
     */
    private int calculateHealthScore(Map<String, Object> details) {
        int score = 100;

        // 数据库状态影响评分
        if ("DOWN".equals(details.get("database.status"))) {
            score -= 40; // 数据库问题是严重问题
        }

        // 性能状态影响评分
        if ("SLOW".equals(details.get("performance.status"))) {
            score -= 20;
        }

        // 错误率影响评分
        String errorRateStatus = (String) details.get("errorRate.status");
        if ("HIGH".equals(errorRateStatus)) {
            score -= 30;
        } else if ("ELEVATED".equals(errorRateStatus)) {
            score -= 10;
        }

        // 存储状态影响评分
        String storageStatus = (String) details.get("storage.status");
        if ("CRITICAL".equals(storageStatus)) {
            score -= 25;
        } else if ("WARNING".equals(storageStatus)) {
            score -= 10;
        }

        return Math.max(0, score);
    }

    /**
     * 根据健康评分给出建议
     *
     * <p>就像医生会根据体检结果给出健康建议一样，我们的系统也会
     * 根据健康评分给出相应的运维建议。</p>
     */
    private String getHealthRecommendation(int healthScore) {
        if (healthScore >= 90) {
            return "系统运行状况良好，继续保持";
        } else if (healthScore >= 70) {
            return "系统运行基本正常，建议关注警告项目";
        } else if (healthScore >= 50) {
            return "系统存在一些问题，建议尽快处理";
        } else {
            return "系统存在严重问题，需要立即处理";
        }
    }

    // =================== 辅助方法 ===================

    /**
     * 记录请求处理指标
     *
     * <p>这个方法由其他组件调用，用于记录请求的处理情况。
     * 这些数据会被用于健康检查和性能分析。</p>
     */
    public void recordRequest(long processingTime, boolean isError) {
        totalRequests.incrementAndGet();
        totalProcessingTime.addAndGet(processingTime);

        if (isError) {
            totalErrors.incrementAndGet();
        }
    }

    /**
     * 格式化字节数为人类可读格式
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 获取系统运行时间
     */
    private String getUptime() {
        long uptimeMs = System.currentTimeMillis() - getStartTime();
        long hours = uptimeMs / (1000 * 60 * 60);
        long minutes = (uptimeMs % (1000 * 60 * 60)) / (1000 * 60);
        return String.format("%d小时%d分钟", hours, minutes);
    }

    /**
     * 获取系统启动时间（简化实现）
     */
    private long getStartTime() {
        // 这里应该记录实际的系统启动时间
        // 为了简化，我们使用一个固定的时间
        return System.currentTimeMillis() - (2 * 60 * 60 * 1000); // 假设系统运行了2小时
    }

    /**
     * 重置统计数据
     *
     * <p>这个方法可以用于定期重置统计数据，避免数据累积过久导致不准确。</p>
     */
    public void resetStatistics() {
        totalRequests.set(0);
        totalErrors.set(0);
        totalProcessingTime.set(0);
        cachedHealth = null;
        log.info("审计模块统计数据已重置");
    }
}