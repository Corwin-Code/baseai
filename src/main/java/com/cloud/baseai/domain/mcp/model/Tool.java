package com.cloud.baseai.domain.mcp.model;

import java.time.OffsetDateTime;

/**
 * <h2>工具领域对象</h2>
 *
 * <p>MCP工具的核心领域模型，封装了工具的所有业务属性和行为。
 * 作为聚合根，它负责维护工具相关的业务不变性。</p>
 *
 * <p><b>设计理念：</b></p>
 * <p>这个领域对象采用了不可变设计，所有的状态变更都通过创建新实例来实现。
 * 这样确保了对象状态的一致性，避免了并发修改的问题。</p>
 */
public record Tool(
        Long id,
        String code,
        String name,
        ToolType type,
        String description,
        String iconUrl,
        String paramSchema,
        String resultSchema,
        String endpoint,
        String authType,
        Boolean enabled,
        Long ownerId,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {
    /**
     * 创建新工具的工厂方法
     */
    public static Tool create(String code, String name, ToolType type, String description,
                              String iconUrl, String paramSchema, String resultSchema,
                              String endpoint, String authType, Long createdBy) {
        return new Tool(
                null, // ID由数据库生成
                code,
                name,
                type,
                description,
                iconUrl,
                paramSchema,
                resultSchema,
                endpoint,
                authType,
                true, // 默认启用
                createdBy, // 创建者即拥有者
                createdBy,
                OffsetDateTime.now(),
                null // 未删除
        );
    }

    /**
     * 更新工具信息
     */
    public Tool update(String name, String description, String iconUrl,
                       String paramSchema, String resultSchema, String endpoint,
                       String authType, Long updatedBy) {
        return new Tool(
                id,
                code, // 代码不可更改
                name,
                type, // 类型不可更改
                description,
                iconUrl,
                paramSchema,
                resultSchema,
                endpoint,
                authType,
                enabled,
                ownerId,
                updatedBy,
                createdAt,
                deletedAt
        );
    }

    /**
     * 启用工具
     */
    public Tool enable() {
        return new Tool(id, code, name, type, description, iconUrl, paramSchema,
                resultSchema, endpoint, authType, true, ownerId, createdBy, createdAt, deletedAt);
    }

    /**
     * 禁用工具
     */
    public Tool disable() {
        return new Tool(id, code, name, type, description, iconUrl, paramSchema,
                resultSchema, endpoint, authType, false, ownerId, createdBy, createdAt, deletedAt);
    }

    /**
     * 软删除工具
     */
    public Tool softDelete() {
        return new Tool(id, code, name, type, description, iconUrl, paramSchema,
                resultSchema, endpoint, authType, false, ownerId, createdBy, createdAt,
                OffsetDateTime.now());
    }

    // =================== 业务规则方法 ===================

    /**
     * 检查是否为HTTP类型工具
     */
    public boolean isHttpTool() {
        return ToolType.HTTP == type;
    }

    /**
     * 检查是否需要认证
     */
    public boolean requiresAuth() {
        return authType != null && !authType.trim().isEmpty() && !"NONE".equals(authType);
    }

    /**
     * 检查工具是否可执行
     */
    public boolean canExecute() {
        return Boolean.TRUE.equals(enabled) && deletedAt == null;
    }

    /**
     * 检查工具是否可编辑
     */
    public boolean canEdit() {
        return deletedAt == null;
    }

    /**
     * 验证工具配置的完整性
     */
    public boolean isConfigurationValid() {
        // 基本信息验证
        if (code == null || code.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            return false;
        }

        // HTTP工具必须有端点
        if (isHttpTool() && (endpoint == null || endpoint.trim().isEmpty())) {
            return false;
        }

        return true;
    }

    /**
     * 获取工具的唯一标识
     */
    public String getUniqueKey() {
        return code + ":" + type.name();
    }
}