package com.clinflash.baseai.application.user.command;

import jakarta.validation.constraints.*;

/**
 * 更新用户资料命令
 */
public record UpdateUserProfileCommand(
        @NotNull(message = "用户ID不能为空")
        @Positive(message = "用户ID必须为正数")
        Long userId,

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱长度不能超过128位")
        String email,

        @Size(max = 256, message = "头像URL长度不能超过256位")
        String avatarUrl
) {
}