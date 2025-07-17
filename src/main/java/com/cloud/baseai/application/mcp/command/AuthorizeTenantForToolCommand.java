package com.cloud.baseai.application.mcp.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * <h2>租户工具授权命令</h2>
 *
 * <p>此命令用于为特定租户授权使用某个工具，是MCP系统权限管理的
 * 核心操作。通过精细化的授权控制，我们确保工具的使用既安全又可控。</p>
 *
 * @param toolCode   要授权的工具编码
 * @param tenantId   被授权的租户ID
 * @param apiKey     租户专用的API密钥，用于工具认证
 * @param quotaLimit 调用配额限制，null表示无限制
 */
public record AuthorizeTenantForToolCommand(
        @NotNull(message = "工具代码不能为空")
        String toolCode,

        @NotNull(message = "租户ID不能为空")
        Long tenantId,

        @Size(max = 128, message = "API密钥长度不能超过128个字符")
        String apiKey,

        @PositiveOrZero(message = "配额限制必须为非负数")
        Integer quotaLimit
) {
    /**
     * 检查是否设置了API密钥
     */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    /**
     * 检查是否设置了配额限制
     */
    public boolean hasQuotaLimit() {
        return quotaLimit != null && quotaLimit > 0;
    }
}