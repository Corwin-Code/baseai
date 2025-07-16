package com.clinflash.baseai.domain.audit.event;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * <h3>安全审计事件</h3>
 *
 * <p>安全事件是审计系统中最重要的事件类型之一。它们记录了所有
 * 与系统安全相关的活动，就像安保日志一样重要。</p>
 *
 * <p><b>特殊处理：</b></p>
 * <p>安全事件通常需要特殊的处理，如立即通知安全团队、触发自动
 * 响应机制、生成告警等。因此，这个事件类包含了额外的安全相关字段。</p>
 */
@Getter
public class SecurityAuditEvent extends ApplicationEvent {

    private final Long userId;
    private final Long tenantId;
    private final String eventType;
    private final String description;
    private final String riskLevel;
    private final String sourceIp;
    private final Map<String, Object> securityContext;
    @Getter(AccessLevel.NONE)
    private final OffsetDateTime timestamp;

    /**
     * 构造安全审计事件
     */
    public SecurityAuditEvent(Object source, Long userId, Long tenantId, String eventType,
                              String description, String riskLevel, String sourceIp,
                              Map<String, Object> securityContext, OffsetDateTime timestamp) {
        super(source);
        this.userId = userId;
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.description = description;
        this.riskLevel = riskLevel;
        this.sourceIp = sourceIp;
        this.securityContext = securityContext;
        this.timestamp = timestamp;
    }

    /**
     * 判断是否为高风险安全事件
     */
    public boolean isHighRisk() {
        return "HIGH".equalsIgnoreCase(riskLevel) || "CRITICAL".equalsIgnoreCase(riskLevel);
    }

    /**
     * 判断是否需要立即响应
     */
    public boolean requiresImmediateResponse() {
        return isHighRisk() || eventType.contains("BREACH") || eventType.contains("ATTACK");
    }

    /**
     * 获取威胁评分（0-100）
     */
    public int getThreatScore() {
        return switch (riskLevel.toUpperCase()) {
            case "CRITICAL" -> 90;
            case "HIGH" -> 70;
            case "MEDIUM" -> 50;
            case "LOW" -> 20;
            default -> 0;
        };
    }

    @Override
    public String toString() {
        return String.format("SecurityAuditEvent{eventType='%s', riskLevel='%s', " +
                        "sourceIp='%s', timestamp=%s}",
                eventType, riskLevel, sourceIp, timestamp);
    }
}