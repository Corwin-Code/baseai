package com.clinflash.baseai.domain.flow.model;

import lombok.Getter;

/**
 * <h2>流程状态枚举</h2>
 *
 * <p>定义流程定义的各种状态，控制流程的生命周期。</p>
 */
@Getter
public enum FlowStatus {
    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布");

    private final int code;
    private final String label;

    FlowStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static FlowStatus fromCode(int code) {
        for (FlowStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的流程状态代码: " + code);
    }
}