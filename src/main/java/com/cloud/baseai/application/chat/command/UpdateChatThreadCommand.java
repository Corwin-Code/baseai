package com.cloud.baseai.application.chat.command;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 更新对话线程命令
 */
public record UpdateChatThreadCommand(
        @NotNull(message = "线程ID不能为空")
        Long threadId,

        @Size(max = 256, message = "标题长度不能超过256字符")
        String title,

        @Size(max = 32, message = "模型代码长度不能超过32字符")
        String defaultModel,

        @DecimalMin(value = "0.0", message = "温度参数不能小于0")
        @DecimalMax(value = "2.0", message = "温度参数不能大于2")
        Float temperature,

        Long flowSnapshotId,

        @NotNull(message = "操作人ID不能为空")
        Long operatorId
) {
}