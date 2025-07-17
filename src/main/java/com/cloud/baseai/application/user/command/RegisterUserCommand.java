package com.cloud.baseai.application.user.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册命令
 *
 * <p>用户注册是整个用户生命周期的起点，这个命令包含了注册所需的所有信息。
 * 我们使用Bean Validation注解来确保数据的完整性和正确性，
 * 就像建筑工程中的质量检查一样，每个环节都要符合标准。</p>
 */
public record RegisterUserCommand(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名长度必须在3-32位之间")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "用户名只能包含字母、数字、下划线和连字符")
        String username,

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱长度不能超过128位")
        String email,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度必须在8-64位之间")
        String password,

        @NotBlank(message = "确认密码不能为空")
        String confirmPassword,

        @Size(max = 256, message = "头像URL长度不能超过256位")
        String avatarUrl,

        @Size(max = 64, message = "邀请码长度不能超过64位")
        String inviteCode
) {
    /**
     * 验证密码是否一致
     */
    public boolean isPasswordMatched() {
        return password != null && password.equals(confirmPassword);
    }

    /**
     * 是否包含邀请码
     */
    public boolean hasInviteCode() {
        return inviteCode != null && !inviteCode.trim().isEmpty();
    }
}