package com.clinflash.baseai.application.user.dto;

import java.time.OffsetDateTime;

/**
 * 租户成员DTO
 *
 * <p>租户成员的详细信息，用于成员管理页面。
 * 包含用户基本信息、在租户中的角色和状态等。</p>
 */
public record TenantMemberDTO(
        Long userId,
        String username,
        String email,
        String avatarUrl,
        String roleName,
        String roleLabel,
        String status,
        OffsetDateTime joinedAt,
        OffsetDateTime lastLoginAt
) {
    /**
     * 获取成员状态的友好显示
     */
    public String getStatusDisplay() {
        return switch (status) {
            case "ACTIVE" -> "正常";
            case "PENDING" -> "待激活";
            case "SUSPENDED" -> "已暂停";
            case "DISABLED" -> "已禁用";
            default -> "未知";
        };
    }

    /**
     * 判断成员是否在线（基于最近登录时间）
     */
    public boolean isOnline() {
        return lastLoginAt != null &&
                lastLoginAt.isAfter(OffsetDateTime.now().minusMinutes(30));
    }
}