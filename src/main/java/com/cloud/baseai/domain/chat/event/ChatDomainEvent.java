package com.cloud.baseai.domain.chat.event;

import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * <h2>对话领域事件</h2>
 *
 * <p>事件驱动架构是现代微服务系统的重要模式。通过发布领域事件，
 * 我们可以实现松耦合的系统集成，让不同的服务能够响应对话系统中的关键活动。</p>
 *
 * <p><b>为什么需要事件驱动：</b></p>
 * <p>想象一下，当用户发送一条消息时，系统可能需要：</p>
 * <p>1. 更新用户活跃度统计</p>
 * <p>2. 触发内容审核流程</p>
 * <p>3. 发送实时通知给其他用户</p>
 * <p>4. 记录用户行为日志</p>
 * <p>5. 更新推荐算法的训练数据</p>
 * <p>如果让对话服务直接调用所有这些功能，会造成紧耦合和性能问题。
 * 通过事件发布，我们可以让各个服务独立响应，实现真正的解耦。</p>
 */
@Getter
public abstract class ChatDomainEvent {
    private final String eventId;
    private final String eventType;
    private final OffsetDateTime occurredAt;
    private final Long tenantId;
    private final Long userId;
    private final Map<String, Object> metadata;

    protected ChatDomainEvent(String eventType, Long tenantId, Long userId, Map<String, Object> metadata) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.eventType = eventType;
        this.occurredAt = OffsetDateTime.now();
        this.tenantId = tenantId;
        this.userId = userId;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }
}