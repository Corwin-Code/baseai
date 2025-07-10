package com.clinflash.baseai.application.user.dto;

/**
 * 邀请响应DTO
 *
 * <p>用户响应邀请后的结果信息。
 * 包含响应结果和相关的组织、角色信息。</p>
 */
public record InvitationResponseDTO(
        String action,      // ACCEPTED 或 REJECTED
        String orgName,
        String roleLabel,
        String message
) {
    /**
     * 判断是否接受了邀请
     */
    public boolean isAccepted() {
        return "ACCEPTED".equals(action);
    }
}