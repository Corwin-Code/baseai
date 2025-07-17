package com.cloud.baseai.application.user.dto;

import java.time.OffsetDateTime;

/**
 * 租户DTO
 *
 * <p>租户的基本信息，用于租户列表和详情页面。
 * 包含了组织的核心信息和创建者信息。</p>
 */
public record TenantDTO(
        Long id,
        String orgName,
        String planCode,
        OffsetDateTime expireAt,
        String creatorName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * 判断租户是否即将过期（30天内）
     */
    public boolean isExpiringSoon() {
        return expireAt != null &&
                expireAt.isBefore(OffsetDateTime.now().plusDays(30));
    }

    /**
     * 判断租户是否已过期
     */
    public boolean isExpired() {
        return expireAt != null &&
                expireAt.isBefore(OffsetDateTime.now());
    }
}