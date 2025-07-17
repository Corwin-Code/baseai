package com.cloud.baseai.infrastructure.persistence.mcp.entity.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <h2>工具调用状态枚举</h2>
 */
@Getter
public enum ToolCallStatus {

    STARTED(0, "已开始"),
    RUNNING(1, "运行中"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败"),
    ERROR(4, "错误"),
    TIMEOUT(5, "超时");

    private final Integer code;
    private final String description;

    ToolCallStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    private static final Map<Integer, ToolCallStatus> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(ToolCallStatus::getCode, Function.identity()));

    /**
     * 根据 code 获取枚举实例。
     * 这种方式比遍历更高效，特别是当枚举项很多时。
     *
     * @param code 状态码
     * @return 对应的 ToolCallStatus 实例，如果找不到则返回 null
     */
    public static ToolCallStatus fromCode(Integer code) {
        return CODE_MAP.get(code);
    }

    /**
     * 根据枚举名称（忽略大小写）获取实例。
     *
     * @param name 枚举的名称 (e.g., "SUCCESS")
     * @return 对应的 ToolCallStatus 实例，如果找不到则返回 null (或抛出异常)
     */
    public static ToolCallStatus valueOfIgnoreCase(String name) {
        if (name == null || name.trim().isEmpty()) {
            return STARTED;
        }
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return STARTED;
        }
    }
}