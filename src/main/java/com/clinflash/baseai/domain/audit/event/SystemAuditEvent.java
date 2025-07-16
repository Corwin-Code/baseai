package com.clinflash.baseai.domain.audit.event;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * <h3>系统审计事件</h3>
 *
 * <p>系统事件记录系统级别的操作和状态变化，如服务启动、配置变更、
 * 定时任务执行等。这些事件对于系统监控和故障诊断很重要。</p>
 */
@Getter
public class SystemAuditEvent extends ApplicationEvent {

    private final String eventType;
    private final String component;
    private final String description;
    private final String severity;
    private final Map<String, Object> systemContext;
    @Getter(AccessLevel.NONE)
    private final OffsetDateTime timestamp;

    /**
     * 构造系统审计事件
     */
    public SystemAuditEvent(Object source, String eventType, String component,
                            String description, String severity,
                            Map<String, Object> systemContext, OffsetDateTime timestamp) {
        super(source);
        this.eventType = eventType;
        this.component = component;
        this.description = description;
        this.severity = severity;
        this.systemContext = systemContext;
        this.timestamp = timestamp;
    }

    /**
     * 判断是否为严重级别的系统事件
     */
    public boolean isCritical() {
        return "CRITICAL".equalsIgnoreCase(severity) || "ERROR".equalsIgnoreCase(severity);
    }

    /**
     * 获取系统组件信息
     */
    public String getComponentInfo() {
        return String.format("%s[%s]", component, severity);
    }

    @Override
    public String toString() {
        return String.format("SystemAuditEvent{eventType='%s', component='%s', " +
                        "severity='%s', timestamp=%s}",
                eventType, component, severity, timestamp);
    }
}