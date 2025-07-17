package com.cloud.baseai.domain.flow.model;

import java.time.OffsetDateTime;

/**
 * <h2>流程运行日志领域模型</h2>
 *
 * <p>流程运行日志记录了执行过程中每个节点的详细信息，
 * 包括输入数据、输出结果、执行时间等。这些信息对于调试和审计至关重要。</p>
 */
public record FlowRunLog(
        Long id,
        Long runId,
        String nodeKey,
        String ioJson,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {

    /**
     * 创建新的运行日志
     */
    public static FlowRunLog create(Long runId, String nodeKey,
                                    String ioJson, Long createdBy) {
        validateNodeKey(nodeKey);
        validateIoJson(ioJson);

        return new FlowRunLog(
                null,
                runId,
                nodeKey,
                ioJson,
                createdBy,
                OffsetDateTime.now(),
                null
        );
    }

    private static void validateNodeKey(String nodeKey) {
        if (nodeKey == null || nodeKey.trim().isEmpty()) {
            throw new IllegalArgumentException("节点Key不能为空");
        }
    }

    private static void validateIoJson(String ioJson) {
        if (ioJson == null || ioJson.trim().isEmpty()) {
            throw new IllegalArgumentException("输入输出数据不能为空");
        }
    }
}