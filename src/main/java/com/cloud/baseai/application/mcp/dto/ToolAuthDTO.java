package com.cloud.baseai.application.mcp.dto;

import java.time.OffsetDateTime;

/**
 * <h2>工具授权信息DTO</h2>
 *
 * <p>展示租户对特定工具的授权状态和配额使用情况。
 * 这个信息对于租户管理员监控资源使用非常重要。</p>
 */
public record ToolAuthDTO(
        Long toolId,
        String toolCode,
        String toolName,
        Long tenantId,
        Integer quotaLimit,
        Integer quotaUsed,
        Boolean enabled,
        OffsetDateTime grantedAt
) {
    /**
     * 计算配额使用率
     */
    public double getQuotaUsageRate() {
        if (quotaLimit == null || quotaLimit <= 0) {
            return 0.0; // 无限制
        }
        int used = quotaUsed != null ? quotaUsed : 0;
        return (double) used / quotaLimit;
    }

    /**
     * 检查配额是否即将用完（使用率超过80%）
     */
    public boolean isQuotaNearLimit() {
        return getQuotaUsageRate() > 0.8;
    }

    /**
     * 检查是否还有可用配额
     */
    public boolean hasAvailableQuota() {
        if (quotaLimit == null) {
            return true; // 无限制
        }
        int used = quotaUsed != null ? quotaUsed : 0;
        return used < quotaLimit;
    }

    /**
     * 获取剩余配额
     */
    public Integer getRemainingQuota() {
        if (quotaLimit == null) {
            return null; // 无限制
        }
        int used = quotaUsed != null ? quotaUsed : 0;
        return Math.max(0, quotaLimit - used);
    }
}