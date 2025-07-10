package com.clinflash.baseai.domain.flow.model;

import java.time.OffsetDateTime;

/**
 * <h2>流程快照领域模型</h2>
 *
 * <p>流程快照是流程定义在某个时间点的完整快照，包含了执行流程所需的所有信息。
 * 这就像拍照一样，保存了流程在某个瞬间的完整状态。</p>
 *
 * <p><b>不可变性：</b></p>
 * <p>快照一旦创建就不可更改，这保证了流程执行的一致性和可重现性。
 * 无论何时基于同一个快照执行流程，结果都应该是可预期的。</p>
 */
public record FlowSnapshot(
        Long id,
        Long definitionId,
        Integer version,
        String snapshotJson,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {

    /**
     * 创建新的流程快照
     */
    public static FlowSnapshot create(Long definitionId, Integer version,
                                      String snapshotJson, Long createdBy) {
        validateSnapshotJson(snapshotJson);

        return new FlowSnapshot(
                null,
                definitionId,
                version,
                snapshotJson,
                createdBy,
                OffsetDateTime.now(),
                null
        );
    }

    /**
     * 检查快照是否可用
     */
    public boolean isAvailable() {
        return this.deletedAt == null && this.snapshotJson != null;
    }

    private static void validateSnapshotJson(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.trim().isEmpty()) {
            throw new IllegalArgumentException("快照内容不能为空");
        }
        // 这里可以添加JSON格式验证
    }
}