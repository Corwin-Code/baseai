package com.cloud.baseai.domain.chat.event;

import lombok.Getter;

import java.util.Map;

/**
 * 对话线程创建事件
 */
@Getter
public class ChatThreadCreatedEvent extends ChatDomainEvent {

    private final Long threadId;
    private final String title;
    private final String modelCode;

    public ChatThreadCreatedEvent(Long tenantId, Long userId, Long threadId, String title, String modelCode) {
        super("CHAT_THREAD_CREATED", tenantId, userId, Map.of(
                "threadId", threadId,
                "title", title,
                "modelCode", modelCode
        ));
        this.threadId = threadId;
        this.title = title;
        this.modelCode = modelCode;
    }
}