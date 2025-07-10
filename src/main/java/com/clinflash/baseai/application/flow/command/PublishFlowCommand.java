package com.clinflash.baseai.application.flow.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <h2>发布流程命令</h2>
 *
 * <p>发布是流程从设计阶段转向生产阶段的重要步骤。
 * 这个命令触发流程的验证、快照创建和状态更新。</p>
 */
public record PublishFlowCommand(
        @NotNull(message = "流程定义ID不能为空")
        Long definitionId,

        @Size(max = 512, message = "发布说明长度不能超过512个字符")
        String publishNote, // 发布说明，记录本次发布的改动

        @NotNull(message = "操作者ID不能为空")
        Long operatorId
) {
}