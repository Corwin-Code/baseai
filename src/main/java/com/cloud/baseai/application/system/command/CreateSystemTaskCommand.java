package com.cloud.baseai.application.system.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <h2>创建系统任务命令</h2>
 */
public record CreateSystemTaskCommand(
        @JsonProperty("tenantId")
        Long tenantId,

        @JsonProperty("taskType")
        @NotBlank(message = "任务类型不能为空")
        @Size(max = 64, message = "任务类型长度不能超过64个字符")
        String taskType,

        @JsonProperty("payload")
        @NotNull(message = "任务参数不能为空")
        String payload,

        @JsonProperty("createdBy")
        @NotNull(message = "创建者ID不能为空")
        Long createdBy
) {
    /**
     * 验证任务参数的大小
     */
    public boolean hasValidPayloadSize() {
        // 简化验证：检查JSON序列化后的大小
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writeValueAsString(this.payload);
            return json.length() <= 10000; // 限制10KB
        } catch (Exception e) {
            return false;
        }
    }
}