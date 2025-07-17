package com.cloud.baseai.infrastructure.monitoring;

import com.cloud.baseai.infrastructure.config.McpProperties;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <h2>MCP监控管理器</h2>
 *
 * <p>负责收集和管理MCP系统的运行指标，提供性能监控、告警触发等功能。
 * 这个组件帮助运维人员实时了解系统的健康状态和性能表现。</p>
 *
 * <p><b>监控维度：</b></p>
 * <ul>
 * <li><b>调用指标：</b>成功率、响应时间、并发量等</li>
 * <li><b>错误指标：</b>错误率、错误类型分布、故障频率等</li>
 * <li><b>资源指标：</b>内存使用、线程池状态、连接池状态等</li>
 * <li><b>业务指标：</b>配额使用率、热门工具、用户活跃度等</li>
 * </ul>
 */
@Component
public class McpMonitoringManager {

    private static final Logger log = LoggerFactory.getLogger(McpMonitoringManager.class);

    private final McpProperties.MonitoringConfig monitoringConfig;
    private final Map<String, ToolMetrics> toolMetricsMap;
    private final Map<String, TenantMetrics> tenantMetricsMap;
    /**
     * -- GETTER --
     * 获取系统监控指标
     */
    @Getter
    private final SystemMetrics systemMetrics;

    public McpMonitoringManager(McpProperties mcpProperties) {
        this.monitoringConfig = mcpProperties.getMonitoring();
        this.toolMetricsMap = new ConcurrentHashMap<>();
        this.tenantMetricsMap = new ConcurrentHashMap<>();
        this.systemMetrics = new SystemMetrics();
    }

    /**
     * 记录工具调用开始
     */
    public void recordToolCallStart(String toolCode, Long tenantId) {
        if (!monitoringConfig.isEnableMetrics()) {
            return;
        }

        // 更新系统级指标
        systemMetrics.totalCalls.incrementAndGet();
        systemMetrics.activeCalls.incrementAndGet();

        // 更新工具级指标
        ToolMetrics toolMetrics = toolMetricsMap.computeIfAbsent(toolCode, k -> new ToolMetrics());
        toolMetrics.totalCalls.incrementAndGet();
        toolMetrics.activeCalls.incrementAndGet();

        // 更新租户级指标
        String tenantKey = "tenant_" + tenantId;
        TenantMetrics tenantMetrics = tenantMetricsMap.computeIfAbsent(tenantKey, k -> new TenantMetrics());
        tenantMetrics.totalCalls.incrementAndGet();
        tenantMetrics.activeCalls.incrementAndGet();

        log.debug("记录工具调用开始: toolCode={}, tenantId={}", toolCode, tenantId);
    }

    /**
     * 记录工具调用结束
     */
    public void recordToolCallEnd(String toolCode, Long tenantId, boolean success, long latencyMs) {
        if (!monitoringConfig.isEnableMetrics()) {
            return;
        }

        // 更新系统级指标
        systemMetrics.activeCalls.decrementAndGet();
        if (success) {
            systemMetrics.successfulCalls.incrementAndGet();
        } else {
            systemMetrics.failedCalls.incrementAndGet();
        }
        updateLatencyMetrics(systemMetrics, latencyMs);

        // 更新工具级指标
        ToolMetrics toolMetrics = toolMetricsMap.get(toolCode);
        if (toolMetrics != null) {
            toolMetrics.activeCalls.decrementAndGet();
            if (success) {
                toolMetrics.successfulCalls.incrementAndGet();
            } else {
                toolMetrics.failedCalls.incrementAndGet();
            }
            updateLatencyMetrics(toolMetrics, latencyMs);
        }

        // 更新租户级指标
        String tenantKey = "tenant_" + tenantId;
        TenantMetrics tenantMetrics = tenantMetricsMap.get(tenantKey);
        if (tenantMetrics != null) {
            tenantMetrics.activeCalls.decrementAndGet();
            if (success) {
                tenantMetrics.successfulCalls.incrementAndGet();
            } else {
                tenantMetrics.failedCalls.incrementAndGet();
            }
        }

        // 检查告警条件
        checkAlerts(toolCode, tenantId, success, latencyMs);

        log.debug("记录工具调用结束: toolCode={}, tenantId={}, success={}, latency={}ms",
                toolCode, tenantId, success, latencyMs);
    }

    /**
     * 记录错误信息
     */
    public void recordError(String toolCode, Long tenantId, String errorType, String errorMessage) {
        if (!monitoringConfig.isEnableMetrics()) {
            return;
        }

        // 更新工具错误统计
        ToolMetrics toolMetrics = toolMetricsMap.get(toolCode);
        if (toolMetrics != null) {
            toolMetrics.errorCounts.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
        }

        log.warn("记录工具调用错误: toolCode={}, tenantId={}, errorType={}, error={}",
                toolCode, tenantId, errorType, errorMessage);
    }

    /**
     * 获取工具监控指标
     */
    public ToolMetrics getToolMetrics(String toolCode) {
        return toolMetricsMap.get(toolCode);
    }

    /**
     * 获取租户监控指标
     */
    public TenantMetrics getTenantMetrics(Long tenantId) {
        String tenantKey = "tenant_" + tenantId;
        return tenantMetricsMap.get(tenantKey);
    }

    /**
     * 获取所有工具的指标摘要
     */
    public Map<String, MetricsSummary> getAllToolMetricsSummary() {
        Map<String, MetricsSummary> summary = new ConcurrentHashMap<>();

        toolMetricsMap.forEach((toolCode, metrics) -> {
            long total = metrics.totalCalls.get();
            long successful = metrics.successfulCalls.get();
            long failed = metrics.failedCalls.get();
            double successRate = total > 0 ? (double) successful / total : 0.0;
            double errorRate = total > 0 ? (double) failed / total : 0.0;

            summary.put(toolCode, new MetricsSummary(
                    total, successful, failed, successRate, errorRate,
                    metrics.averageLatencyMs.get(), metrics.maxLatencyMs.get()
            ));
        });

        return summary;
    }

    // =================== 私有方法 ===================

    /**
     * 更新延迟指标
     */
    private void updateLatencyMetrics(BaseMetrics metrics, long latencyMs) {
        // 更新最大延迟
        long currentMax;
        do {
            currentMax = metrics.maxLatencyMs.get();
        } while (latencyMs > currentMax && !metrics.maxLatencyMs.compareAndSet(currentMax, latencyMs));

        // 简单的移动平均计算
        long currentAvg = metrics.averageLatencyMs.get();
        long newAvg = (currentAvg + latencyMs) / 2;
        metrics.averageLatencyMs.set(newAvg);
    }

    /**
     * 检查告警条件
     */
    private void checkAlerts(String toolCode, Long tenantId, boolean success, long latencyMs) {
        if (!monitoringConfig.isEnableAlerts()) {
            return;
        }

        // 检查响应时间告警
        if (latencyMs > monitoringConfig.getLatencyThresholdMs()) {
            triggerAlert("HIGH_LATENCY",
                    String.format("工具 %s 响应时间过长: %dms > %dms",
                            toolCode, latencyMs, monitoringConfig.getLatencyThresholdMs()));
        }

        // 检查错误率告警
        ToolMetrics toolMetrics = toolMetricsMap.get(toolCode);
        if (toolMetrics != null) {
            long total = toolMetrics.totalCalls.get();
            long failed = toolMetrics.failedCalls.get();

            if (total >= 100) { // 至少100次调用后才检查错误率
                double errorRate = (double) failed / total * 100;
                if (errorRate > monitoringConfig.getErrorRateThreshold()) {
                    triggerAlert("HIGH_ERROR_RATE",
                            String.format("工具 %s 错误率过高: %.2f%% > %.2f%%",
                                    toolCode, errorRate, monitoringConfig.getErrorRateThreshold()));
                }
            }
        }
    }

    /**
     * 触发告警
     */
    private void triggerAlert(String alertType, String message) {
        log.error("MCP系统告警 [{}]: {}", alertType, message);

        // 这里可以集成外部告警系统，如邮件、短信、钉钉等
        // 为了简化示例，这里只记录日志
    }

    // =================== 指标数据类 ===================

    /**
     * 基础指标类
     */
    public static class BaseMetrics {
        public final AtomicLong totalCalls = new AtomicLong(0);
        public final AtomicLong successfulCalls = new AtomicLong(0);
        public final AtomicLong failedCalls = new AtomicLong(0);
        public final AtomicLong activeCalls = new AtomicLong(0);
        public final AtomicLong averageLatencyMs = new AtomicLong(0);
        public final AtomicLong maxLatencyMs = new AtomicLong(0);
    }

    /**
     * 系统级指标
     */
    public static class SystemMetrics extends BaseMetrics {
        public final OffsetDateTime startTime = OffsetDateTime.now();

        public double getSuccessRate() {
            long total = totalCalls.get();
            return total > 0 ? (double) successfulCalls.get() / total : 0.0;
        }

        public double getErrorRate() {
            long total = totalCalls.get();
            return total > 0 ? (double) failedCalls.get() / total : 0.0;
        }
    }

    /**
     * 工具级指标
     */
    public static class ToolMetrics extends BaseMetrics {
        public final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
        public final OffsetDateTime firstCallTime = OffsetDateTime.now();
    }

    /**
     * 租户级指标
     */
    public static class TenantMetrics extends BaseMetrics {
        public final OffsetDateTime firstCallTime = OffsetDateTime.now();
    }

    /**
     * 指标摘要
     */
    public record MetricsSummary(long totalCalls, long successfulCalls, long failedCalls, double successRate,
                                 double errorRate, long averageLatencyMs, long maxLatencyMs) {
    }
}