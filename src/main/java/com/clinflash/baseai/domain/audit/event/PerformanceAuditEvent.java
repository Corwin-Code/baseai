package com.clinflash.baseai.domain.audit.event;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * <h3>性能监控审计事件</h3>
 *
 * <p>性能事件记录系统性能相关的信息，如响应时间异常、
 * 资源使用高峰、性能阈值突破等。</p>
 */
@Getter
public class PerformanceAuditEvent extends ApplicationEvent {

    private final String metricName;
    private final Double metricValue;
    private final String threshold;
    private final String component;
    private final String severity;
    private final Map<String, Object> performanceData;
    @Getter(AccessLevel.NONE)
    private final OffsetDateTime timestamp;

    /**
     * 构造性能监控审计事件
     */
    public PerformanceAuditEvent(Object source, String metricName, Double metricValue,
                                 String threshold, String component, String severity,
                                 Map<String, Object> performanceData, OffsetDateTime timestamp) {
        super(source);
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.threshold = threshold;
        this.component = component;
        this.severity = severity;
        this.performanceData = performanceData;
        this.timestamp = timestamp;
    }

    /**
     * 判断是否超过了阈值
     */
    public boolean isThresholdExceeded() {
        return severity != null && (
                "HIGH".equalsIgnoreCase(severity) ||
                        "CRITICAL".equalsIgnoreCase(severity)
        );
    }

    /**
     * 获取性能摘要信息
     */
    public String getPerformanceSummary() {
        return String.format("%s: %.2f (threshold: %s, severity: %s)",
                metricName, metricValue, threshold, severity);
    }

    @Override
    public String toString() {
        return String.format("PerformanceAuditEvent{metricName='%s', metricValue=%.2f, " +
                        "component='%s', severity='%s', timestamp=%s}",
                metricName, metricValue, component, severity, timestamp);
    }
}