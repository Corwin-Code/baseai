package com.clinflash.baseai.application.user.dto;

import java.time.OffsetDateTime;

/**
 * 用户资料DTO
 *
 * <p>用户的完整资料信息，用于个人中心页面展示。
 * 包含了用户的基本信息和关键的时间戳，但不包含敏感信息如密码。</p>
 */
public record UserProfileDTO(
        Long id,
        String username,
        String email,
        String avatarUrl,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * 判断用户是否为新注册用户（7天内注册）
     */
    public boolean isNewUser() {
        return createdAt != null &&
                createdAt.isAfter(OffsetDateTime.now().minusDays(7));
    }

    /**
     * 判断用户是否活跃（30天内登录过）
     */
    public boolean isActiveUser() {
        return lastLoginAt != null &&
                lastLoginAt.isAfter(OffsetDateTime.now().minusDays(30));
    }
}