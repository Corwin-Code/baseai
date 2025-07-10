package com.clinflash.baseai.domain.chat.model;

import java.time.OffsetDateTime;

/**
 * <h2>对话线程领域模型</h2>
 *
 * <p>对话线程是AI对话系统中的核心概念，它代表了一次完整的多轮对话会话。
 * 就像人与人之间的谈话有开始、发展和结束一样，每个对话线程都维护着
 * 完整的上下文记忆和个性化配置。</p>
 */
public record ChatThread(
        Long id,
        Long tenantId,
        Long userId,
        String title,
        String defaultModel,
        Float temperature,
        Long flowSnapshotId,
        Long createdBy,
        Long updatedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt
) {
    /**
     * 创建新的对话线程
     */
    public static ChatThread create(Long tenantId, Long userId, String title,
                                    String defaultModel, Float temperature,
                                    Long flowSnapshotId, Long operatorId) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ChatThread(
                null,
                tenantId,
                userId,
                title != null ? title : "新对话",
                defaultModel,
                temperature != null ? temperature : 1.0f,
                flowSnapshotId,
                operatorId,
                operatorId,
                now,
                now,
                null
        );
    }

    /**
     * 更新对话线程
     */
    public ChatThread update(String newTitle, String newDefaultModel,
                             Float newTemperature, Long newFlowSnapshotId,
                             Long operatorId) {
        return new ChatThread(
                this.id,
                this.tenantId,
                this.userId,
                newTitle != null ? newTitle : this.title,
                newDefaultModel != null ? newDefaultModel : this.defaultModel,
                newTemperature != null ? newTemperature : this.temperature,
                newFlowSnapshotId,
                this.createdBy,
                operatorId,
                this.createdAt,
                OffsetDateTime.now(),
                this.deletedAt
        );
    }

    /**
     * 检查是否已删除
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 检查是否有流程编排
     */
    public boolean hasFlowOrchestration() {
        return flowSnapshotId != null;
    }
}