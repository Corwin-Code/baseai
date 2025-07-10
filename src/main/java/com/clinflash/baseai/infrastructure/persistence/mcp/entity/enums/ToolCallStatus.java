package com.clinflash.baseai.infrastructure.persistence.mcp.entity.enums;

import lombok.Getter;

/**
 * <h2>工具调用状态枚举</h2>
 */
@Getter
public enum ToolCallStatus {
    STARTED("已开始"),
    RUNNING("运行中"),
    SUCCESS("成功"),
    FAILED("失败"),
    ERROR("错误"),
    TIMEOUT("超时");

    private final String label;

    ToolCallStatus(String label) {
        this.label = label;
    }

    public static ToolCallStatus fromString(String status) {
        if (status == null) {
            return STARTED;
        }
        try {
            return valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return STARTED;
        }
    }
}