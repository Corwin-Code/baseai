package com.clinflash.baseai.application.user.dto;

import java.time.OffsetDateTime;

/**
 * 待处理邀请DTO
 *
 * <p>用户收到的待处理邀请列表。
 * 用于显示用户所有未处理的组织邀请。</p>
 */
public record PendingInvitationDTO(
        String token,
        String orgName,
        String roleName,
        String inviterName,
        OffsetDateTime expiresAt
) {
    /**
     * 计算剩余有效天数
     */
    public long getDaysUntilExpiry() {
        if (expiresAt == null) return 0;
        return OffsetDateTime.now().until(expiresAt, java.time.temporal.ChronoUnit.DAYS);
    }

    /**
     * 判断是否即将过期（3天内）
     */
    public boolean isExpiringSoon() {
        return getDaysUntilExpiry() <= 3;
    }
}