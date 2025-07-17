package com.cloud.baseai.domain.audit.event;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * <h3>合规审计事件</h3>
 *
 * <p>合规事件专门记录与法规合规相关的操作，如数据导出、
 * 用户同意记录、隐私权行使等。</p>
 */
@Getter
public class ComplianceAuditEvent extends ApplicationEvent {

    private final Long userId;
    private final Long tenantId;
    private final String complianceType;
    private final String regulation;
    private final String action;
    private final String dataSubject;
    private final Map<String, Object> complianceData;
    @Getter(AccessLevel.NONE)
    private final OffsetDateTime timestamp;

    /**
     * 构造合规审计事件
     */
    public ComplianceAuditEvent(Object source, Long userId, Long tenantId,
                                String complianceType, String regulation, String action,
                                String dataSubject, Map<String, Object> complianceData,
                                OffsetDateTime timestamp) {
        super(source);
        this.userId = userId;
        this.tenantId = tenantId;
        this.complianceType = complianceType;
        this.regulation = regulation;
        this.action = action;
        this.dataSubject = dataSubject;
        this.complianceData = complianceData;
        this.timestamp = timestamp;
    }

    /**
     * 判断是否为GDPR相关的合规事件
     */
    public boolean isGDPRRelated() {
        return "GDPR".equalsIgnoreCase(regulation) ||
                complianceType.contains("PRIVACY") ||
                complianceType.contains("CONSENT");
    }

    /**
     * 获取合规摘要信息
     */
    public String getComplianceSummary() {
        return String.format("%s[%s]: %s for %s",
                complianceType, regulation, action, dataSubject);
    }

    @Override
    public String toString() {
        return String.format("ComplianceAuditEvent{complianceType='%s', regulation='%s', " +
                        "action='%s', timestamp=%s}",
                complianceType, regulation, action, timestamp);
    }
}