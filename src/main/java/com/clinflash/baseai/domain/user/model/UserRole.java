package com.clinflash.baseai.domain.user.model;

import java.time.OffsetDateTime;

/**
 * <h2>用户-角色关联领域模型</h2>
 *
 * <p>表示用户的全局角色关联，用于系统级权限控制。</p>
 */
public record UserRole(
        Long userId,
        Long roleId,
        OffsetDateTime createdAt
) {
    /**
     * 创建新的用户-角色关联
     */
    public static UserRole create(Long userId, Long roleId) {
        return new UserRole(
                userId,
                roleId,
                OffsetDateTime.now()
        );
    }
}