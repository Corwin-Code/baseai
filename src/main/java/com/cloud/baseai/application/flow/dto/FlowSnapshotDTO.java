package com.cloud.baseai.application.flow.dto;

import java.time.OffsetDateTime;

/**
 * <h2>流程快照传输对象</h2>
 *
 * <p>流程快照是执行时使用的不可变版本，确保执行的一致性和可重现性。</p>
 */
public record FlowSnapshotDTO(
        Long id,
        Long definitionId,
        Integer version,
        String snapshotJson,
        OffsetDateTime createdAt
) {
    /**
     * 获取快照大小（字节）
     */
    public int getSnapshotSize() {
        return snapshotJson != null ? snapshotJson.getBytes().length : 0;
    }

    /**
     * 获取快照大小的友好显示
     */
    public String getSnapshotSizeDisplay() {
        int size = getSnapshotSize();
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }
}