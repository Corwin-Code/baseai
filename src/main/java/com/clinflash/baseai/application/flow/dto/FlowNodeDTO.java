package com.clinflash.baseai.application.flow.dto;

/**
 * <h2>流程节点传输对象</h2>
 *
 * <p>流程节点的详细信息，包括节点类型、配置和重试策略。
 * 不同类型的节点有不同的配置结构和行为模式。</p>
 */
public record FlowNodeDTO(
        Long id,
        String nodeTypeCode,
        String nodeKey,
        String name,
        String configJson,
        String retryPolicyJson
) {
    /**
     * 检查是否为控制流节点
     */
    public boolean isControlNode() {
        return nodeTypeCode != null && (
                nodeTypeCode.equals("CONDITION") ||
                        nodeTypeCode.equals("LOOP") ||
                        nodeTypeCode.equals("SWITCH") ||
                        nodeTypeCode.equals("PARALLEL")
        );
    }

    /**
     * 检查是否为AI节点
     */
    public boolean isAINode() {
        return nodeTypeCode != null && (
                nodeTypeCode.equals("LLM") ||
                        nodeTypeCode.equals("RETRIEVER") ||
                        nodeTypeCode.equals("EMBEDDER") ||
                        nodeTypeCode.equals("CLASSIFIER")
        );
    }

    /**
     * 获取节点类型的显示名称
     */
    public String getTypeDisplayName() {
        if (nodeTypeCode == null) return "未知";

        return switch (nodeTypeCode) {
            case "START" -> "开始";
            case "END" -> "结束";
            case "LLM" -> "大语言模型";
            case "RETRIEVER" -> "知识检索";
            case "CONDITION" -> "条件判断";
            case "LOOP" -> "循环";
            case "PARALLEL" -> "并行处理";
            case "HTTP" -> "HTTP请求";
            case "TOOL" -> "工具调用";
            default -> nodeTypeCode;
        };
    }
}