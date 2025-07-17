package com.cloud.baseai.infrastructure.external.sms.model;

import lombok.Getter;

/**
 * 短信发送状态枚举
 */
@Getter
public enum SmsStatus {
    PENDING("待发送"),
    SENDING("发送中"),
    DELIVERED("已送达"),
    FAILED("发送失败"),
    REJECTED("被拒绝"),
    EXPIRED("已过期");

    private final String description;

    SmsStatus(String description) {
        this.description = description;
    }
}