package com.cloud.baseai.application.flow.dto;

import java.time.OffsetDateTime;

/**
 * <h2>流程定义传输对象</h2>
 *
 * <p>流程定义的核心信息，包括版本、状态等关键属性。
 * 这些信息帮助用户理解流程的当前状态和可进行的操作。</p>
 */
public record FlowDefinitionDTO(
        Long id,
        Long projectId,
        String name,
        Integer version,
        Boolean isLatest,
        String status,
        String description,
        Long createdBy,
        String creatorName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * 检查是否可以编辑
     */
    public boolean canEdit() {
        return "草稿".equals(status);
    }

    /**
     * 检查是否可以执行
     */
    public boolean canExecute() {
        return "已发布".equals(status);
    }

    /**
     * 获取版本显示文本
     */
    public String getVersionDisplay() {
        return "v" + version + (Boolean.TRUE.equals(isLatest) ? " (最新)" : "");
    }
}