package com.cloud.baseai.application.chat.command;

import jakarta.validation.constraints.*;

/**
 * 发送消息命令
 */
public record SendMessageCommand(
        @NotBlank(message = "消息内容不能为空")
        @Size(max = 32000, message = "消息内容不能超过32000字符")
        String content,

        @NotBlank(message = "消息类型不能为空")
        String messageType,

        Boolean enableKnowledgeRetrieval,

        Boolean enableToolCalling,

        @DecimalMin(value = "0.0", message = "温度参数不能小于0")
        @DecimalMax(value = "2.0", message = "温度参数不能大于2")
        Float temperature,

        @Min(value = 1, message = "最大Token数不能小于1")
        @Max(value = 32000, message = "最大Token数不能超过32000")
        Integer maxTokens,

        Boolean streamMode
) {
    public SendMessageCommand {
        if (messageType == null) {
            messageType = "TEXT";
        }
        if (enableKnowledgeRetrieval == null) {
            enableKnowledgeRetrieval = true;
        }
        if (enableToolCalling == null) {
            enableToolCalling = false;
        }
        if (streamMode == null) {
            streamMode = false;
        }
    }
}