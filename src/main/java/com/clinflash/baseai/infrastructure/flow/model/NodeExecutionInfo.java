package com.clinflash.baseai.infrastructure.flow.model;

import com.clinflash.baseai.domain.flow.model.FlowNode;

import java.util.Map;

/**
 * <h2>流程节点执行信息</h2>
 *
 * <p>这是执行器与流程引擎之间的数据传输对象，包含了执行单个节点所需的所有信息。</p>
 *
 * <p><b>设计理念：</b></p>
 * <p>这个类将复杂的流程节点信息封装成执行器能够理解的标准格式。它屏蔽了
 * 底层数据结构的复杂性，让执行器只需要关注业务逻辑的实现。</p>
 */
public record NodeExecutionInfo(
        Long nodeId,
        String nodeTypeCode,
        String nodeKey,
        String name,
        String configJson,
        String retryPolicyJson,
        Map<String, Object> executionContext
) {

    /**
     * 从流程节点领域对象创建执行信息
     */
    public static NodeExecutionInfo fromDomainNode(FlowNode node) {
        return new NodeExecutionInfo(
                node.id(),
                node.nodeTypeCode(),
                node.nodeKey(),
                node.name(),
                node.configJson(),
                node.retryPolicyJson(),
                Map.of() // 基础上下文，可以后续扩展
        );
    }

    /**
     * 检查节点是否有有效配置
     */
    public boolean hasConfig() {
        return configJson != null && !configJson.trim().isEmpty();
    }

    /**
     * 检查节点是否有重试策略
     */
    public boolean hasRetryPolicy() {
        return retryPolicyJson != null && !retryPolicyJson.trim().isEmpty();
    }

    /**
     * 获取节点显示名称
     */
    public String getDisplayName() {
        return (name != null && !name.trim().isEmpty()) ? name : nodeKey;
    }
}