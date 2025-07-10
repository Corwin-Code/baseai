package com.clinflash.baseai.infrastructure.persistence.user.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * <h2>用户-租户关联复合主键</h2>
 */
@Setter
@Getter
public class SysUserTenantEntityId {
    private Long userId;
    private Long tenantId;

    public SysUserTenantEntityId() {
    }

    public SysUserTenantEntityId(Long userId, Long tenantId) {
        this.userId = userId;
        this.tenantId = tenantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SysUserTenantEntityId that = (SysUserTenantEntityId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, tenantId);
    }
}