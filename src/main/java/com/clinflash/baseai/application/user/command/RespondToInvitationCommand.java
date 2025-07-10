package com.clinflash.baseai.application.user.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 响应邀请命令
 */
public record RespondToInvitationCommand(
        @NotBlank(message = "操作类型不能为空")
        @Pattern(regexp = "^(ACCEPT|REJECT)$", message = "操作类型必须是ACCEPT或REJECT")
        String action,

        Long userId  // 可选，如果是新用户注册则为空
) {
}