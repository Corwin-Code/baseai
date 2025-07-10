package com.clinflash.baseai.application.user.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 激活用户命令
 */
public record ActivateUserCommand(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email,

        @NotBlank(message = "激活码不能为空")
        @Size(min = 6, max = 32, message = "激活码长度必须在6-32位之间")
        String activationCode
) {
}