package com.cloud.baseai.infrastructure.event;

import com.cloud.baseai.domain.chat.event.ChatDomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * <h2>对话事件发布器</h2>
 *
 * <p>这个发布器是事件驱动架构的核心组件。它负责将领域事件发布到Spring的事件总线，
 * 让其他组件能够异步响应这些事件。</p>
 */
@Component
public class ChatEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ChatEventPublisher.class);

    private final ApplicationEventPublisher eventPublisher;

    public ChatEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 发布对话事件
     */
    public void publishEvent(ChatDomainEvent event) {
        try {
            log.debug("发布对话事件: type={}, eventId={}, tenantId={}",
                    event.getEventType(), event.getEventId(), event.getTenantId());

            eventPublisher.publishEvent(event);

        } catch (Exception e) {
            log.error("发布对话事件失败: type={}, eventId={}",
                    event.getEventType(), event.getEventId(), e);
        }
    }
}