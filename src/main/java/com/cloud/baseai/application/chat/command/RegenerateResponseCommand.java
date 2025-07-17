package com.cloud.baseai.application.chat.command;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 重新生成回复命令
 */
public record RegenerateResponseCommand(
        Boolean enableKnowledgeRetrieval,

        Boolean enableToolCalling,

        @DecimalMin(value = "0.0", message = "温度参数不能小于0")
        @DecimalMax(value = "2.0", message = "温度参数不能大于2")
        Float temperature,

        @Min(value = 1, message = "最大Token数不能小于1")
        @Max(value = 32000, message = "最大Token数不能超过32000")
        Integer maxTokens
) {
    public RegenerateResponseCommand {
        if (enableKnowledgeRetrieval == null) {
            enableKnowledgeRetrieval = true;
        }
        if (enableToolCalling == null) {
            enableToolCalling = false;
        }
        if (temperature == null) {
            temperature = 0.8f;
        }
        if (maxTokens == null) {
            maxTokens = 2000;
        }
    }
}