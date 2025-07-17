package com.cloud.baseai.application.user.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 修改密码命令
 */
public record ChangePasswordCommand(
        @NotNull(message = "用户ID不能为空")
        Long userId,

        @NotBlank(message = "原密码不能为空")
        String oldPassword,

        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 64, message = "新密码长度必须在8-64位之间")
        String newPassword,

        @NotBlank(message = "确认新密码不能为空")
        String confirmNewPassword
) {
    /**
     * 验证新密码是否一致
     */
    public boolean isNewPasswordMatched() {
        return newPassword != null && newPassword.equals(confirmNewPassword);
    }
}