package com.clinflash.baseai.application.flow.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <h2>创建流程定义命令</h2>
 *
 * <p>流程定义是整个工作流的蓝图，它描述了任务的执行顺序、条件判断、
 * 数据流转等核心逻辑。这个命令包含了创建流程定义的基本信息。</p>
 */
public record CreateFlowDefinitionCommand(
        @NotNull(message = "项目ID不能为空")
        Long projectId,

        @NotBlank(message = "流程名称不能为空")
        @Size(max = 128, message = "流程名称长度不能超过128个字符")
        String name,

        @Size(max = 1024, message = "流程描述长度不能超过1024个字符")
        String description,

        String diagramJson, // 可视化流程图的JSON表示

        @NotNull(message = "操作者ID不能为空")
        Long operatorId
) {
    /**
     * 检查是否包含初始的流程图数据
     */
    public boolean hasInitialDiagram() {
        return diagramJson != null && !diagramJson.trim().isEmpty();
    }
}