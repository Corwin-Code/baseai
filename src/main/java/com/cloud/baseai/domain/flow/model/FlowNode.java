package com.cloud.baseai.domain.flow.model;

import java.time.OffsetDateTime;

/**
 * <h2>流程节点领域模型</h2>
 *
 * <p>流程节点是流程定义的基本组成单元，每个节点代表一个具体的处理步骤。
 * 就像程序中的函数一样，每个节点都有明确的输入、处理逻辑和输出。</p>
 *
 * <p><b>节点类型多样性：</b></p>
 * <p>系统支持多种类型的节点，从简单的数据传递到复杂的AI推理，
 * 每种节点类型都有其特定的配置和行为模式。</p>
 */
public record FlowNode(
        Long id,
        Long definitionId,
        String nodeTypeCode,
        String nodeKey,
        String name,
        String configJson,
        String retryPolicyJson,
        Long createdBy,
        Long updatedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt
) {

    /**
     * 创建新的流程节点
     */
    public static FlowNode create(Long definitionId, String nodeTypeCode,
                                  String nodeKey, String name, String configJson,
                                  String retryPolicyJson, Long createdBy) {
        validateNodeKey(nodeKey);
        validateNodeTypeCode(nodeTypeCode);
        validateName(name);

        return new FlowNode(
                null,
                definitionId,
                nodeTypeCode,
                nodeKey,
                name,
                configJson,
                retryPolicyJson,
                createdBy,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                null
        );
    }

    /**
     * 更新节点配置
     *
     * <p>更新节点的配置信息，包括名称、配置JSON和重试策略。
     * 这允许在流程开发过程中调整节点的行为。</p>
     */
    public FlowNode updateConfig(String newName, String newConfigJson,
                                 String newRetryPolicyJson, Long updatedBy) {
        validateName(newName);

        return new FlowNode(
                this.id,
                this.definitionId,
                this.nodeTypeCode,
                this.nodeKey,
                newName,
                newConfigJson,
                newRetryPolicyJson,
                this.createdBy,
                updatedBy,
                this.createdAt,
                OffsetDateTime.now(),
                this.deletedAt
        );
    }

    /**
     * 检查节点是否为开始节点
     */
    public boolean isStartNode() {
        return NodeTypes.START.equals(this.nodeTypeCode);
    }

    /**
     * 检查节点是否为结束节点
     */
    public boolean isEndNode() {
        return NodeTypes.END.equals(this.nodeTypeCode);
    }

    /**
     * 检查节点是否为控制节点
     */
    public boolean isControlNode() {
        return NodeTypes.CONDITION.equals(this.nodeTypeCode) ||
                NodeTypes.LOOP.equals(this.nodeTypeCode) ||
                NodeTypes.SWITCH.equals(this.nodeTypeCode) ||
                NodeTypes.PARALLEL.equals(this.nodeTypeCode);
    }

    private static void validateNodeKey(String nodeKey) {
        if (nodeKey == null || nodeKey.trim().isEmpty()) {
            throw new IllegalArgumentException("节点Key不能为空");
        }
        if (!nodeKey.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("节点Key必须以字母开头，只能包含字母、数字和下划线");
        }
    }

    private static void validateNodeTypeCode(String nodeTypeCode) {
        if (nodeTypeCode == null || nodeTypeCode.trim().isEmpty()) {
            throw new IllegalArgumentException("节点类型不能为空");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("节点名称不能为空");
        }
        if (name.length() > 128) {
            throw new IllegalArgumentException("节点名称长度不能超过128个字符");
        }
    }
}