package com.cloud.baseai.application.flow.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * <h2>更新流程结构命令</h2>
 *
 * <p>这个命令承载了流程设计器的核心功能。当用户在可视化编辑器中
 * 设计流程图时，所有的节点、连接和配置信息都通过这个命令传递到后端。</p>
 */
public record UpdateFlowStructureCommand(
        @NotNull(message = "流程定义ID不能为空")
        Long definitionId,

        @Valid
        @NotNull(message = "节点列表不能为空")
        List<NodeCommand> nodes,

        @Valid
        @NotNull(message = "连接列表不能为空")
        List<EdgeCommand> edges,

        String diagramJson, // 更新后的流程图JSON

        @NotNull(message = "操作者ID不能为空")
        Long operatorId
) {
    /**
     * 节点配置命令
     */
    public record NodeCommand(
            @NotBlank(message = "节点类型不能为空")
            String nodeTypeCode,

            @NotBlank(message = "节点Key不能为空")
            @Size(max = 64, message = "节点Key长度不能超过64个字符")
            String nodeKey,

            @NotBlank(message = "节点名称不能为空")
            @Size(max = 128, message = "节点名称长度不能超过128个字符")
            String name,

            String configJson, // 节点特定的配置

            String retryPolicyJson // 重试策略配置
    ) {
        /**
         * 验证节点Key是否符合命名规范
         */
        public boolean isValidNodeKey() {
            return nodeKey != null && nodeKey.matches("^[a-zA-Z][a-zA-Z0-9_]*$");
        }
    }

    /**
     * 连接配置命令
     */
    public record EdgeCommand(
            @NotBlank(message = "源节点Key不能为空")
            String sourceKey,

            @NotBlank(message = "目标节点Key不能为空")
            String targetKey,

            String configJson // 连接特定的配置（如条件表达式）
    ) {
        /**
         * 验证连接的有效性
         */
        public boolean isValidEdge() {
            return sourceKey != null && targetKey != null &&
                    !sourceKey.equals(targetKey); // 不能自己连接自己
        }
    }

    /**
     * 检查流程结构是否为空
     */
    public boolean isEmpty() {
        return nodes.isEmpty() && edges.isEmpty();
    }

    /**
     * 获取所有节点的Key集合
     */
    public java.util.Set<String> getNodeKeys() {
        return nodes.stream()
                .map(NodeCommand::nodeKey)
                .collect(java.util.stream.Collectors.toSet());
    }
}