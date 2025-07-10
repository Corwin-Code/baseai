package com.clinflash.baseai.domain.flow.model;

import java.time.OffsetDateTime;

/**
 * <h2>流程项目领域模型</h2>
 *
 * <p>流程项目是组织和管理相关流程定义的容器。就像软件开发中的项目概念一样，
 * 它为一组相关的工作流提供了逻辑分组和权限边界。</p>
 *
 * <p><b>设计思想：</b></p>
 * <p>项目作为聚合根，管理其下的所有流程定义。这种设计让团队协作变得更加有序，
 * 不同的业务领域可以维护自己的流程项目，避免混乱。</p>
 */
public record FlowProject(
        Long id,
        Long tenantId,
        String name,
        Long createdBy,
        Long updatedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt
) {

    /**
     * 创建新的流程项目
     *
     * <p>这是一个工厂方法，用于创建新的流程项目实例。
     * 在创建时会自动设置创建时间，确保数据的完整性。</p>
     *
     * @param tenantId  租户ID，确保多租户隔离
     * @param name      项目名称，应该具有业务意义
     * @param createdBy 创建者用户ID
     * @return 新创建的流程项目实例
     */
    public static FlowProject create(Long tenantId, String name, Long createdBy) {
        validateName(name);
        return new FlowProject(
                null, // ID由数据库生成
                tenantId,
                name,
                createdBy,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                null
        );
    }

    /**
     * 更新项目信息
     *
     * <p>创建一个包含更新信息的新实例。这体现了值对象的不可变性原则，
     * 每次修改都会产生新的实例而不是修改现有实例。</p>
     *
     * @param newName   新的项目名称
     * @param updatedBy 更新者用户ID
     * @return 包含更新信息的新实例
     */
    public FlowProject update(String newName, Long updatedBy) {
        validateName(newName);
        return new FlowProject(
                this.id,
                this.tenantId,
                newName,
                this.createdBy,
                updatedBy,
                this.createdAt,
                OffsetDateTime.now(),
                this.deletedAt
        );
    }

    /**
     * 软删除项目
     *
     * <p>标记项目为已删除状态，但不从数据库中物理删除。
     * 这样可以保留历史记录和审计轨迹。</p>
     *
     * @param deletedBy 执行删除操作的用户ID
     * @return 标记为已删除的新实例
     */
    public FlowProject softDelete(Long deletedBy) {
        return new FlowProject(
                this.id,
                this.tenantId,
                this.name,
                this.createdBy,
                deletedBy,
                this.createdAt,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    /**
     * 检查项目是否已被删除
     *
     * @return 如果项目已被软删除则返回true
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 验证项目名称的有效性
     */
    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("项目名称不能为空");
        }
        if (name.length() > 128) {
            throw new IllegalArgumentException("项目名称长度不能超过128个字符");
        }
    }
}