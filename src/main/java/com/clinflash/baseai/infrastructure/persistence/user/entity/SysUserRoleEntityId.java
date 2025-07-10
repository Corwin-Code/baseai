package com.clinflash.baseai.infrastructure.persistence.user.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * <h2>用户-角色关联复合主键</h2>
 */
@Setter
@Getter
public class SysUserRoleEntityId {
    private Long userId;
    private Long roleId;

    public SysUserRoleEntityId() {
    }

    public SysUserRoleEntityId(Long userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SysUserRoleEntityId that = (SysUserRoleEntityId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }
}