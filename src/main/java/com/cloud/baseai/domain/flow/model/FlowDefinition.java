package com.cloud.baseai.domain.flow.model;

import java.time.OffsetDateTime;

/**
 * <h2>流程定义领域模型</h2>
 *
 * <p>流程定义描述了一个完整的工作流程，包括其结构、版本信息和状态。
 * 它是流程编排系统的核心概念，类似于程序设计中的类定义。</p>
 *
 * <p><b>版本管理：</b></p>
 * <p>每个流程定义都有版本概念，这允许我们在不影响正在运行的流程的情况下
 * 进行迭代和改进。就像软件版本一样，新版本可以修复问题或添加新功能。</p>
 */
public record FlowDefinition(
        Long id,
        Long projectId,
        String name,
        Integer version,
        Boolean isLatest,
        FlowStatus status,
        String diagramJson,
        String description,
        Long createdBy,
        Long updatedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt
) {

    /**
     * 创建新的流程定义
     *
     * <p>新创建的流程定义默认为草稿状态，版本号为1，
     * 并标记为最新版本。这为后续的编辑和发布奠定了基础。</p>
     */
    public static FlowDefinition create(Long projectId, String name,
                                        String description, String diagramJson,
                                        Long createdBy) {
        validateName(name);
        return new FlowDefinition(
                null,
                projectId,
                name,
                1, // 初始版本为1
                true, // 新创建的定义默认为最新版本
                FlowStatus.DRAFT,
                diagramJson,
                description,
                createdBy,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                null
        );
    }

    /**
     * 更新流程定义信息
     *
     * <p>只有处于草稿状态的流程才能被更新。一旦发布，
     * 流程定义就变为不可变，需要创建新版本才能修改。</p>
     */
    public FlowDefinition update(String newName, String newDescription,
                                 String newDiagramJson, Long updatedBy) {
        if (this.status != FlowStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态的流程才能修改");
        }

        validateName(newName);
        return new FlowDefinition(
                this.id,
                this.projectId,
                newName,
                this.version,
                this.isLatest,
                this.status,
                newDiagramJson,
                newDescription,
                this.createdBy,
                updatedBy,
                this.createdAt,
                OffsetDateTime.now(),
                this.deletedAt
        );
    }

    /**
     * 发布流程定义
     *
     * <p>发布操作将流程状态从草稿改为已发布，这意味着流程可以被执行，
     * 但不能再被修改。这保证了执行环境的稳定性。</p>
     */
    public FlowDefinition publish(Long publishedBy) {
        if (this.status != FlowStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态的流程才能发布");
        }

        return new FlowDefinition(
                this.id,
                this.projectId,
                this.name,
                this.version,
                this.isLatest,
                FlowStatus.PUBLISHED,
                this.diagramJson,
                this.description,
                this.createdBy,
                publishedBy,
                this.createdAt,
                OffsetDateTime.now(),
                this.deletedAt
        );
    }

    /**
     * 创建新版本
     *
     * <p>基于当前定义创建一个新版本的流程定义。新版本继承所有配置，
     * 但状态重置为草稿，版本号递增。</p>
     */
    public FlowDefinition createNewVersion(Long createdBy) {
        return new FlowDefinition(
                null, // 新版本需要新的ID
                this.projectId,
                this.name,
                this.version + 1,
                true, // 新版本成为最新版本
                FlowStatus.DRAFT,
                this.diagramJson,
                this.description,
                createdBy,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                null
        );
    }

    /**
     * 标记为非最新版本
     *
     * <p>当创建新版本时，需要将旧版本标记为非最新。
     * 这确保了每个流程名称只有一个最新版本。</p>
     */
    public FlowDefinition markAsNotLatest() {
        return new FlowDefinition(
                this.id,
                this.projectId,
                this.name,
                this.version,
                false, // 不再是最新版本
                this.status,
                this.diagramJson,
                this.description,
                this.createdBy,
                this.updatedBy,
                this.createdAt,
                this.updatedAt,
                this.deletedAt
        );
    }

    /**
     * 检查是否可以执行
     */
    public boolean canExecute() {
        return this.status == FlowStatus.PUBLISHED && !isDeleted();
    }

    /**
     * 检查是否已被删除
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("流程名称不能为空");
        }
        if (name.length() > 128) {
            throw new IllegalArgumentException("流程名称长度不能超过128个字符");
        }
    }
}