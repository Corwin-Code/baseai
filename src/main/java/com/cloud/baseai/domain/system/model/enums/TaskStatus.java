package com.cloud.baseai.domain.system.model.enums;

import lombok.Getter;

/**
 * <h2>任务状态枚举</h2>
 */
@Getter
public enum TaskStatus {
    PENDING(0, "待执行"),
    PROCESSING(1, "处理中"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败");

    private final int code;
    private final String label;

    TaskStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 根据代码获取状态枚举
     *
     * @param code 状态代码
     * @return 对应的状态枚举
     * @throws IllegalArgumentException 如果代码无效
     */
    public static TaskStatus fromCode(int code) {
        for (TaskStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的解析状态代码: " + code);
    }
}