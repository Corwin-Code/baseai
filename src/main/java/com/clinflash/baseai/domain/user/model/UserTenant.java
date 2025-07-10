package com.clinflash.baseai.domain.user.model;

import java.time.OffsetDateTime;

/**
 * <h2>用户-租户关联领域模型</h2>
 *
 * <p>这个模型表示用户与租户之间的多对多关系，
 * 包含了用户在特定租户中的角色和状态信息。</p>
 */
public record UserTenant(
        Long userId,
        Long tenantId,
        Long roleId,
        TenantMemberStatus status,
        OffsetDateTime joinedAt
) {
    /**
     * 创建新的用户-租户关联
     */
    public static UserTenant create(Long userId, Long tenantId, Long roleId, TenantMemberStatus status) {
        return new UserTenant(
                userId,
                tenantId,
                roleId,
                status,
                OffsetDateTime.now()
        );
    }

    /**
     * 更新角色
     */
    public UserTenant updateRole(Long newRoleId) {
        return new UserTenant(
                this.userId,
                this.tenantId,
                newRoleId,
                this.status,
                this.joinedAt
        );
    }

    /**
     * 更新状态
     */
    public UserTenant updateStatus(TenantMemberStatus newStatus) {
        return new UserTenant(
                this.userId,
                this.tenantId,
                this.roleId,
                newStatus,
                this.joinedAt
        );
    }

    /**
     * 激活成员
     */
    public UserTenant activate() {
        return updateStatus(TenantMemberStatus.ACTIVE);
    }

    /**
     * 暂停成员
     */
    public UserTenant suspend() {
        return updateStatus(TenantMemberStatus.SUSPENDED);
    }

    // =================== 业务查询方法 ===================

    /**
     * 判断是否为活跃成员
     */
    public boolean isActive() {
        return status == TenantMemberStatus.ACTIVE;
    }

    /**
     * 获取加入天数
     */
    public long getDaysSinceJoined() {
        if (joinedAt == null) return 0;
        return joinedAt.until(OffsetDateTime.now(), java.time.temporal.ChronoUnit.DAYS);
    }
}