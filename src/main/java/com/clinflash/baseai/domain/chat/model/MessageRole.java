package com.clinflash.baseai.domain.chat.model;

import lombok.Getter;

/**
 * <h2>消息角色枚举</h2>
 */
@Getter
public enum MessageRole {
    SYSTEM("系统"),
    USER("用户"),
    ASSISTANT("助手"),
    TOOL("工具");

    private final String label;

    MessageRole(String label) {
        this.label = label;
    }

}