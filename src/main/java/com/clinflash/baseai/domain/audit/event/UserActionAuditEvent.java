package com.clinflash.baseai.domain.audit.event;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * <h3>用户操作审计事件</h3>
 *
 * <p>这是最常见的审计事件类型，用于记录用户在系统中的各种操作。
 * 它包含了操作的完整上下文信息，就像一份详细的操作报告。</p>
 *
 * <p><b>使用场景：</b></p>
 * <p>用户登录、修改个人信息、上传文档、删除数据等所有用户主动
 * 发起的操作都会生成这种类型的事件。</p>
 */
@Getter
public class UserActionAuditEvent extends ApplicationEvent {

    private final Long userId;
    private final Long tenantId;
    private final String action;
    private final String targetType;
    private final Long targetId;
    private final String description;
    private final String ipAddress;
    private final String userAgent;
    private final Map<String, Object> contextData;
    @Getter(AccessLevel.NONE)
    private final OffsetDateTime timestamp;

    /**
     * 构造用户操作审计事件
     *
     * @param source      事件源对象
     * @param userId      操作用户ID
     * @param tenantId    租户ID
     * @param action      操作类型
     * @param targetType  操作目标类型
     * @param targetId    操作目标ID
     * @param description 操作描述
     * @param ipAddress   用户IP地址
     * @param userAgent   用户代理信息
     * @param contextData 上下文数据
     * @param timestamp   事件时间戳
     */
    public UserActionAuditEvent(Object source, Long userId, Long tenantId, String action,
                                String targetType, Long targetId, String description,
                                String ipAddress, String userAgent,
                                Map<String, Object> contextData, OffsetDateTime timestamp) {
        super(source);
        this.userId = userId;
        this.tenantId = tenantId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.description = description;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.contextData = contextData;
        this.timestamp = timestamp;
    }

    /**
     * 获取操作摘要信息
     */
    public String getOperationSummary() {
        return String.format("用户 %d 执行了 %s 操作，目标: %s#%d",
                userId, action, targetType, targetId);
    }

    /**
     * 判断是否为高风险操作
     */
    public boolean isHighRiskOperation() {
        return action != null && (
                action.contains("DELETE") ||
                        action.contains("ADMIN") ||
                        action.contains("PRIVILEGE")
        );
    }

    @Override
    public String toString() {
        return String.format("UserActionAuditEvent{userId=%d, action='%s', targetType='%s', " +
                        "targetId=%d, timestamp=%s}",
                userId, action, targetType, targetId, timestamp);
    }
}