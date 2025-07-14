package com.clinflash.baseai.domain.system.model.enums;

import lombok.Getter;

/**
 * <h2>设置值类型枚举</h2>
 */
@Getter
public enum SettingValueType {
    STRING("字符串"),
    INTEGER("整数"),
    BOOLEAN("布尔值"),
    JSON("JSON对象");

    private final String label;

    SettingValueType(String label) {
        this.label = label;
    }
}