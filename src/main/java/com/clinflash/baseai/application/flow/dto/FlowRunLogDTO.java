package com.clinflash.baseai.application.flow.dto;

import java.time.OffsetDateTime;

/**
 * <h2>流程运行日志传输对象</h2>
 *
 * <p>记录流程执行过程中每个节点的详细信息，用于调试和审计。</p>
 */
public record FlowRunLogDTO(
        Long id,
        String nodeKey,
        String ioJson,
        OffsetDateTime createdAt
) {
    /**
     * 获取日志时间的友好显示
     */
    public String getTimeDisplay() {
        return createdAt.toLocalTime().toString();
    }
}