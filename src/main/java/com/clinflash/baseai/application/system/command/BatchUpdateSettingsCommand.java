package com.clinflash.baseai.application.system.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * <h2>批量更新系统设置命令</h2>
 */
public record BatchUpdateSettingsCommand(
        @JsonProperty("settings")
        @NotNull(message = "设置列表不能为空")
        Map<String, String> settings,

        @JsonProperty("operatorId")
        @NotNull(message = "操作者ID不能为空")
        Long operatorId
) {
    /**
     * 验证批量更新的大小限制
     */
    public boolean isValidBatchSize() {
        return this.settings.size() <= 50; // 限制单次最多更新50个设置
    }
}