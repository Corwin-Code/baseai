package com.cloud.baseai.domain.flow.model;

import java.time.OffsetDateTime;

/**
 * <h2>流程边领域模型</h2>
 *
 * <p>流程边定义了节点之间的连接关系，决定了数据和控制流的传递路径。
 * 就像道路连接城市一样，边连接节点并定义了执行的顺序。</p>
 *
 * <p><b>条件执行：</b></p>
 * <p>边可以包含执行条件，这样流程就能根据运行时的数据状态
 * 选择不同的执行路径，实现动态的业务逻辑。</p>
 */
public record FlowEdge(
        Long id,
        Long definitionId,
        String sourceKey,
        String targetKey,
        String configJson,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {

    /**
     * 创建新的流程边
     */
    public static FlowEdge create(Long definitionId, String sourceKey,
                                  String targetKey, String configJson, Long createdBy) {
        validateEdgeKeys(sourceKey, targetKey);

        return new FlowEdge(
                null,
                definitionId,
                sourceKey,
                targetKey,
                configJson,
                createdBy,
                OffsetDateTime.now(),
                null
        );
    }

    /**
     * 检查边是否有条件配置
     */
    public boolean hasCondition() {
        return configJson != null && configJson.contains("condition");
    }

    private static void validateEdgeKeys(String sourceKey, String targetKey) {
        if (sourceKey == null || sourceKey.trim().isEmpty()) {
            throw new IllegalArgumentException("源节点Key不能为空");
        }
        if (targetKey == null || targetKey.trim().isEmpty()) {
            throw new IllegalArgumentException("目标节点Key不能为空");
        }
        if (sourceKey.equals(targetKey)) {
            throw new IllegalArgumentException("源节点和目标节点不能相同");
        }
    }
}