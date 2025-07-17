package com.cloud.baseai.application.system.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * <h2>重试任务命令</h2>
 */
public record RetryTaskCommand(
        @JsonProperty("taskId")
        @NotNull(message = "任务ID不能为空")
        Long taskId,

        @JsonProperty("operatorId")
        @NotNull(message = "操作者ID不能为空")
        Long operatorId
) {
}