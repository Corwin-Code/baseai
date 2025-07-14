package com.clinflash.baseai.application.misc.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <h2>更新提示词模板命令</h2>
 */
public record UpdatePromptTemplateCommand(
        @JsonProperty("templateId")
        @NotNull(message = "模板ID不能为空")
        Long templateId,

        @JsonProperty("name")
        @Size(max = 128, message = "模板名称长度不能超过128个字符")
        String name,

        @JsonProperty("content")
        @Size(max = 10000, message = "模板内容长度不能超过10000个字符")
        String content,

        @JsonProperty("modelCode")
        @Size(max = 32, message = "模型代码长度不能超过32个字符")
        String modelCode,

        @JsonProperty("operatorId")
        @NotNull(message = "操作者ID不能为空")
        Long operatorId
) {
}