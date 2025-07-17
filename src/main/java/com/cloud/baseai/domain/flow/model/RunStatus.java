package com.cloud.baseai.domain.flow.model;

import lombok.Getter;

/**
 * <h2>运行状态枚举</h2>
 *
 * <p>定义流程运行实例的各种状态。</p>
 */
@Getter
public enum RunStatus {
    PENDING(0, "待执行"),
    RUNNING(1, "运行中"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败");

    private final int code;
    private final String label;

    RunStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static RunStatus fromCode(int code) {
        for (RunStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的运行状态代码: " + code);
    }
}