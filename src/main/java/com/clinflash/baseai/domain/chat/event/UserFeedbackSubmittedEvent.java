package com.clinflash.baseai.domain.chat.event;

import lombok.Getter;

import java.util.Map;

/**
 * 用户反馈事件
 */
@Getter
public class UserFeedbackSubmittedEvent extends ChatDomainEvent {

    private final Long messageId;
    private final Integer rating;
    private final String comment;

    public UserFeedbackSubmittedEvent(Long tenantId, Long userId, Long messageId, Integer rating, String comment) {
        super("USER_FEEDBACK_SUBMITTED", tenantId, userId, Map.of(
                "messageId", messageId,
                "rating", rating,
                "hasComment", comment != null && !comment.trim().isEmpty()
        ));
        this.messageId = messageId;
        this.rating = rating;
        this.comment = comment;
    }
}