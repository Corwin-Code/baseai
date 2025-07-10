package com.clinflash.baseai.application.user.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 邀请成员命令
 */
public record InviteMemberCommand(
        @NotNull(message = "邀请者ID不能为空")
        Long inviterId,

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email,

        @NotNull(message = "角色ID不能为空")
        Long roleId,

        @Size(max = 255, message = "邀请消息长度不能超过255位")
        String message
) {
}