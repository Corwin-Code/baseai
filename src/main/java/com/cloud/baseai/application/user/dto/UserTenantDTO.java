package com.cloud.baseai.application.user.dto;

import java.time.OffsetDateTime;

/**
 * 用户租户DTO
 *
 * <p>表示用户与租户的关联关系，包含在该租户中的角色和状态。
 * 一个用户可能在多个租户中具有不同的角色。</p>
 */
public record UserTenantDTO(
        Long tenantId,
        String orgName,
        String planCode,
        String roleName,
        String roleLabel,
        String status,
        OffsetDateTime joinedAt
) {
    /**
     * 判断是否为活跃成员
     */
    public boolean isActiveMember() {
        return "ACTIVE".equals(status);
    }

    /**
     * 判断是否为管理员角色
     */
    public boolean isAdmin() {
        return roleName != null &&
                (roleName.contains("ADMIN") || roleName.contains("OWNER"));
    }
}