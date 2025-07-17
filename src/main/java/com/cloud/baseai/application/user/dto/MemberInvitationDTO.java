package com.cloud.baseai.application.user.dto;

import java.time.OffsetDateTime;

/**
 * 成员邀请DTO
 *
 * <p>成员邀请的详细信息，包含邀请链接、有效期等。
 * 用于邀请发送确认和状态跟踪。</p>
 */
public record MemberInvitationDTO(
        String invitationToken,
        String email,
        String orgName,
        String roleLabel,
        OffsetDateTime expiresAt,
        String status
) {
    /**
     * 判断邀请是否已过期
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(OffsetDateTime.now());
    }

    /**
     * 获取邀请状态的友好显示
     */
    public String getStatusDisplay() {
        return switch (status) {
            case "PENDING" -> "待响应";
            case "ACCEPTED" -> "已接受";
            case "REJECTED" -> "已拒绝";
            case "EXPIRED" -> "已过期";
            default -> "未知";
        };
    }
}