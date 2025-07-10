package com.clinflash.baseai.infrastructure.flow.model;

import lombok.Builder;
import lombok.Data;

/**
 * <h2>节点类型信息</h2>
 *
 * <p>描述流程节点类型的元信息，包括节点的能力、配置要求、端口数量等。
 * 这些信息用于节点验证、UI渲染和执行计划生成。</p>
 */
@Data
@Builder
public class NodeTypeInfo {

    /**
     * 节点类型代码
     */
    private String code;

    /**
     * 节点显示名称
     */
    private String name;

    /**
     * 节点描述
     */
    private String description;

    /**
     * 节点分类
     */
    private String category;

    /**
     * 是否需要配置
     */
    private boolean configRequired;

    /**
     * 输入端口数量
     */
    private int inputPorts;

    /**
     * 输出端口数量
     */
    private int outputPorts;

    /**
     * 是否支持并行执行
     */
    @Builder.Default
    private boolean supportsParallel = false;

    /**
     * 是否为控制流节点
     */
    @Builder.Default
    private boolean isControlFlow = false;

    /**
     * 预估执行时间（毫秒）
     */
    @Builder.Default
    private long estimatedExecutionTimeMs = 1000;

    /**
     * 资源消耗级别
     */
    @Builder.Default
    private ResourceLevel resourceLevel = ResourceLevel.LOW;

    /**
     * 节点图标
     */
    private String icon;

    /**
     * 节点颜色
     */
    private String color;

    /**
     * 配置模板
     */
    private String configTemplate;

    /**
     * 是否已弃用
     */
    @Builder.Default
    private boolean deprecated = false;

    /**
     * 资源消耗级别枚举
     */
    public enum ResourceLevel {
        LOW, MEDIUM, HIGH
    }

    /**
     * 检查是否为AI节点
     */
    public boolean isAINode() {
        return "AI节点".equals(category);
    }

    /**
     * 检查是否为控制节点
     */
    public boolean isControlNode() {
        return "控制节点".equals(category);
    }

    /**
     * 检查是否为工具节点
     */
    public boolean isToolNode() {
        return "工具节点".equals(category);
    }

    /**
     * 获取节点的复杂度分数
     */
    public int getComplexityScore() {
        int score = 1;

        if (configRequired) {
            score += 1;
        }

        if (supportsParallel) {
            score += 1;
        }

        if (isControlFlow) {
            score += 2;
        }

        switch (resourceLevel) {
            case MEDIUM:
                score += 1;
                break;
            case HIGH:
                score += 3;
                break;
        }

        return score;
    }
}