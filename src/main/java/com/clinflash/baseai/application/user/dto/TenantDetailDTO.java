package com.clinflash.baseai.application.user.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 租户详情DTO
 *
 * <p>包含租户的完整信息，包括成员统计等额外数据。
 * 用于租户管理页面的详细信息展示。</p>
 */
public record TenantDetailDTO(
        Long id,
        String orgName,
        String planCode,
        OffsetDateTime expireAt,
        int memberCount,
        Map<String, Long> membersByStatus,  // 按状态统计成员数量
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * 获取活跃成员数量
     */
    public long getActiveMemberCount() {
        return membersByStatus.getOrDefault("ACTIVE", 0L);
    }

    /**
     * 获取待激活成员数量
     */
    public long getPendingMemberCount() {
        return membersByStatus.getOrDefault("PENDING", 0L);
    }
}