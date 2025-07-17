package com.cloud.baseai.domain.system.model.enums;

import lombok.Getter;

/**
 * <h2>任务优先级枚举</h2>
 */
@Getter
public enum TaskPriority {
    LOW(1, "低"),
    MEDIUM(2, "中"),
    HIGH(3, "高");

    private final int level;
    private final String label;

    TaskPriority(int level, String label) {
        this.level = level;
        this.label = label;
    }
}