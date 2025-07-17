package com.cloud.baseai.application.system.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <h2>更新系统设置命令</h2>
 *
 * <p>系统设置的更新是一个需要格外谨慎的操作。
 * 每一个配置的改变都可能对整个系统产生深远影响。因此我们需要严格的验证和控制机制。</p>
 */
public record UpdateSystemSettingCommand(
        @JsonProperty("key")
        @NotBlank(message = "设置键不能为空")
        @Size(max = 64, message = "设置键长度不能超过64个字符")
        String key,

        @JsonProperty("value")
        @NotBlank(message = "设置值不能为空")
        String value,

        @JsonProperty("remark")
        @Size(max = 255, message = "备注长度不能超过255个字符")
        String remark,

        @JsonProperty("operatorId")
        @NotNull(message = "操作者ID不能为空")
        Long operatorId
) {
    /**
     * 检查是否为敏感配置
     */
    public boolean isSensitiveSetting() {
        String lowerKey = this.key.toLowerCase();
        return lowerKey.contains("password") ||
                lowerKey.contains("secret") ||
                lowerKey.contains("key") ||
                lowerKey.contains("token");
    }
}