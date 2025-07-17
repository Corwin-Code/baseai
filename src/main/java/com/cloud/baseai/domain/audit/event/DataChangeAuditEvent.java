package com.cloud.baseai.domain.audit.event;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * <h3>数据变更审计事件</h3>
 *
 * <p>数据变更事件专门记录重要数据的创建、修改、删除操作。
 * 这类事件对于数据合规和变更追踪很重要。</p>
 */
@Getter
public class DataChangeAuditEvent extends ApplicationEvent {

    private final Long userId;
    private final Long tenantId;
    private final String operationType;
    private final String entityType;
    private final Long entityId;
    private final Map<String, Object> changeDetails;
    private final String ipAddress;
    @Getter(AccessLevel.NONE)
    private final OffsetDateTime timestamp;

    /**
     * 构造数据变更审计事件
     */
    public DataChangeAuditEvent(Object source, Long userId, Long tenantId,
                                String operationType, String entityType, Long entityId,
                                Map<String, Object> changeDetails, String ipAddress,
                                OffsetDateTime timestamp) {
        super(source);
        this.userId = userId;
        this.tenantId = tenantId;
        this.operationType = operationType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.changeDetails = changeDetails;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
    }

    /**
     * 判断是否为敏感数据变更
     */
    public boolean isSensitiveDataChange() {
        return entityType != null && (
                entityType.contains("USER") ||
                        entityType.contains("PERMISSION") ||
                        entityType.contains("SECURITY")
        );
    }

    /**
     * 判断是否为删除操作
     */
    public boolean isDeleteOperation() {
        return "DELETE".equalsIgnoreCase(operationType);
    }

    /**
     * 获取变更摘要
     */
    public String getChangeSummary() {
        return String.format("%s %s: %s#%d",
                operationType, entityType, entityType, entityId);
    }

    @Override
    public String toString() {
        return String.format("DataChangeAuditEvent{operationType='%s', entityType='%s', " +
                        "entityId=%d, timestamp=%s}",
                operationType, entityType, entityId, timestamp);
    }
}