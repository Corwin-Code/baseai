package com.clinflash.baseai.application.flow.dto;

import java.time.OffsetDateTime;

/**
 * <h2>流程项目传输对象</h2>
 *
 * <p>这个DTO提供了流程项目的基本信息视图，用于列表展示和基本信息查询。</p>
 */
public record FlowProjectDTO(
        Long id,
        String name,
        Long createdBy,
        String creatorName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * 计算项目存在的天数
     */
    public long getAgeDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(
                createdAt.toLocalDate(),
                OffsetDateTime.now().toLocalDate()
        );
    }
}