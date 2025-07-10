package com.clinflash.baseai.domain.mcp.model;

import java.time.OffsetDateTime;

/**
 * <h2>工具授权领域对象</h2>
 *
 * <p>表示租户对特定工具的使用授权。这个对象管理着访问控制、
 * 配额限制和使用统计等重要的业务逻辑。</p>
 */
public record ToolAuth(
        Long toolId,
        Long tenantId,
        String apiKey,
        Integer quotaLimit,
        Integer quotaUsed,
        Boolean enabled,
        OffsetDateTime grantedAt
) {
    /**
     * 创建新授权的工厂方法
     */
    public static ToolAuth create(Long toolId, Long tenantId, String apiKey, Integer quotaLimit) {
        return new ToolAuth(
                toolId,
                tenantId,
                apiKey,
                quotaLimit,
                0, // 初始使用量为0
                true, // 默认启用
                OffsetDateTime.now()
        );
    }

    /**
     * 增加配额使用量
     */
    public ToolAuth incrementQuotaUsed() {
        int newUsed = (quotaUsed != null ? quotaUsed : 0) + 1;
        return new ToolAuth(toolId, tenantId, apiKey, quotaLimit, newUsed, enabled, grantedAt);
    }

    /**
     * 重置配额使用量
     */
    public ToolAuth resetQuota() {
        return new ToolAuth(toolId, tenantId, apiKey, quotaLimit, 0, enabled, grantedAt);
    }

    /**
     * 更新API密钥
     */
    public ToolAuth updateApiKey(String newApiKey) {
        return new ToolAuth(toolId, tenantId, newApiKey, quotaLimit, quotaUsed, enabled, grantedAt);
    }

    /**
     * 启用授权
     */
    public ToolAuth enable() {
        return new ToolAuth(toolId, tenantId, apiKey, quotaLimit, quotaUsed, true, grantedAt);
    }

    /**
     * 禁用授权
     */
    public ToolAuth disable() {
        return new ToolAuth(toolId, tenantId, apiKey, quotaLimit, quotaUsed, false, grantedAt);
    }

    // =================== 业务规则方法 ===================

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
     * 检查授权是否有效
     */
    public boolean isValid() {
        return Boolean.TRUE.equals(enabled) && hasAvailableQuota();
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

    /**
     * 计算配额使用率
     */
    public double getUsageRate() {
        if (quotaLimit == null || quotaLimit <= 0) {
            return 0.0;
        }
        int used = quotaUsed != null ? quotaUsed : 0;
        return (double) used / quotaLimit;
    }
}