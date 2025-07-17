package com.cloud.baseai.application.chat.command;

import jakarta.validation.constraints.*;

/**
 * 创建对话线程命令
 */
public record CreateChatThreadCommand(
        @NotNull(message = "租户ID不能为空")
        Long tenantId,

        @NotNull(message = "用户ID不能为空")
        Long userId,

        @Size(max = 256, message = "标题长度不能超过256字符")
        String title,

        @NotBlank(message = "默认模型不能为空")
        @Size(max = 32, message = "模型代码长度不能超过32字符")
        String defaultModel,

        @DecimalMin(value = "0.0", message = "温度参数不能小于0")
        @DecimalMax(value = "2.0", message = "温度参数不能大于2")
        Float temperature,

        Long flowSnapshotId,

        @Size(max = 2000, message = "系统提示词不能超过2000字符")
        String systemPrompt,

        @NotNull(message = "操作人ID不能为空")
        Long operatorId
) {
    public CreateChatThreadCommand {
        if (temperature == null) {
            temperature = 1.0f;
        }
        if (title == null || title.trim().isEmpty()) {
            title = "新对话";
        }
    }
}