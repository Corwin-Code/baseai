package com.clinflash.baseai.application.flow.dto;

import java.util.List;

/**
 * <h2>流程结构传输对象</h2>
 *
 * <p>包含完整的流程结构信息，用于可视化编辑器的渲染和流程执行引擎的解析。
 * 这个DTO是连接前端设计器和后端执行引擎的重要桥梁。</p>
 */
public record FlowStructureDTO(
        Long definitionId,
        List<FlowNodeDTO> nodes,
        List<FlowEdgeDTO> edges,
        String diagramJson
) {
    /**
     * 检查结构是否为空
     */
    public boolean isEmpty() {
        return nodes.isEmpty() && edges.isEmpty();
    }

    /**
     * 获取节点数量
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /**
     * 获取连接数量
     */
    public int getEdgeCount() {
        return edges.size();
    }

    /**
     * 查找开始节点
     */
    public FlowNodeDTO findStartNode() {
        return nodes.stream()
                .filter(node -> "START".equals(node.nodeTypeCode()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 查找结束节点
     */
    public List<FlowNodeDTO> findEndNodes() {
        return nodes.stream()
                .filter(node -> "END".equals(node.nodeTypeCode()))
                .collect(java.util.stream.Collectors.toList());
    }
}