package com.clinflash.baseai.infrastructure.external.email.model;

import lombok.Getter;

/**
 * 邮件发送状态
 */
@Getter
public enum EmailStatus {
    PENDING("待发送"),
    SENDING("发送中"),
    SENT("已发送"),
    DELIVERED("已投递"),
    FAILED("发送失败"),
    BOUNCED("退回"),
    COMPLAINED("被举报");

    private final String description;

    EmailStatus(String description) {
        this.description = description;
    }
}