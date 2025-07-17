package com.cloud.baseai.application.mcp.dto;

import java.time.OffsetDateTime;

/**
 * <h2>工具信息DTO</h2>
 *
 * <p>工具的基本信息传输对象，用于API响应和前端展示。
 * 这个DTO专注于工具的核心属性，避免暴露敏感的内部信息。</p>
 */
public record ToolDTO(
        Long id,
        String code,
        String name,
        String type,
        String description,
        String iconUrl,
        Boolean enabled,
        OffsetDateTime createdAt
) {
    /**
     * 获取工具状态的友好显示
     */
    public String getStatusDisplay() {
        return Boolean.TRUE.equals(enabled) ? "启用" : "禁用";
    }

    /**
     * 检查工具是否可用
     */
    public boolean isAvailable() {
        return Boolean.TRUE.equals(enabled);
    }
}