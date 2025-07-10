package com.clinflash.baseai.domain.task.model.enums;

import lombok.Getter;

/**
 * 任务状态：0 待执行 | 1 处理中 | 2 成功 | 3 失败
 */
@Getter
public enum TaskStatus {

    PENDING(0),

    RUNNING(1),

    SUCCESS(2),

    FAILED(3);

    private final int code;

    TaskStatus(int c) {
        this.code = c;
    }

    public static TaskStatus of(int c) {
        for (var s : values()) if (s.code == c) return s;
        throw new IllegalArgumentException("bad code " + c);
    }
}
