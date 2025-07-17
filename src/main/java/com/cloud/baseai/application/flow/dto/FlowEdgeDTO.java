package com.cloud.baseai.application.flow.dto;

/**
 * <h2>流程连接传输对象</h2>
 *
 * <p>流程连接定义了节点之间的数据流和控制流。
 * 连接可以包含条件表达式，实现复杂的业务逻辑分支。</p>
 */
public record FlowEdgeDTO(
        Long id,
        String sourceKey,
        String targetKey,
        String configJson
) {
    /**
     * 检查是否有条件配置
     */
    public boolean hasCondition() {
        return configJson != null && configJson.contains("condition");
    }

    /**
     * 获取连接的显示标签
     */
    public String getDisplayLabel() {
        if (hasCondition()) {
            return "条件连接";
        }
        return "默认连接";
    }
}