package com.clinflash.baseai.domain.audit.event;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * <h3>业务流程审计事件</h3>
 *
 * <p>业务流程事件记录复杂业务流程的执行情况，如工作流启动、
 * 审批流程、业务规则执行等。</p>
 */
@Getter
public class BusinessProcessAuditEvent extends ApplicationEvent {

    // Getters
    private final Long userId;
    private final Long tenantId;
    private final String processType;
    private final String processId;
    private final String stepName;
    private final String status;
    private final Map<String, Object> processData;
    @Getter(AccessLevel.NONE)
    private final OffsetDateTime timestamp;

    /**
     * 构造业务流程审计事件
     */
    public BusinessProcessAuditEvent(Object source, Long userId, Long tenantId,
                                     String processType, String processId, String stepName,
                                     String status, Map<String, Object> processData,
                                     OffsetDateTime timestamp) {
        super(source);
        this.userId = userId;
        this.tenantId = tenantId;
        this.processType = processType;
        this.processId = processId;
        this.stepName = stepName;
        this.status = status;
        this.processData = processData;
        this.timestamp = timestamp;
    }

    /**
     * 判断流程是否成功完成
     */
    public boolean isSuccessful() {
        return "SUCCESS".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status);
    }

    /**
     * 判断流程是否失败
     */
    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(status) || "ERROR".equalsIgnoreCase(status);
    }

    /**
     * 获取流程摘要信息
     */
    public String getProcessSummary() {
        return String.format("%s[%s]: %s - %s",
                processType, processId, stepName, status);
    }

    @Override
    public String toString() {
        return String.format("BusinessProcessAuditEvent{processType='%s', processId='%s', " +
                        "stepName='%s', status='%s', timestamp=%s}",
                processType, processId, stepName, status, timestamp);
    }
}