package com.clinflash.baseai.domain.chat.event;

import lombok.Getter;

import java.util.Map;

/**
 * 消息发送事件
 */
@Getter
public class MessageSentEvent extends ChatDomainEvent {

    private final Long messageId;
    private final Long threadId;
    private final String role;
    private final String content;
    private final Integer tokenCount;

    public MessageSentEvent(Long tenantId, Long userId, Long messageId, Long threadId,
                            String role, String content, Integer tokenCount) {
        super("MESSAGE_SENT", tenantId, userId, Map.of(
                "messageId", messageId,
                "threadId", threadId,
                "role", role,
                "tokenCount", tokenCount != null ? tokenCount : 0
        ));
        this.messageId = messageId;
        this.threadId = threadId;
        this.role = role;
        this.content = content;
        this.tokenCount = tokenCount;
    }
}