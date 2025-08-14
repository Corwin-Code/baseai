package com.cloud.baseai.application.auth.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录命令对象，用于封装用户登录时提交的参数。
 * <p>
 * 该类通过 Bean Validation 注解对字段进行校验，
 * 确保用户名与密码的格式和长度符合要求。
 * </p>
 *
 * <ul>
 *   <li>{@code username} 用户名，不能为空，长度 3-50 字符。</li>
 *   <li>{@code password} 密码，不能为空，长度 6-100 字符。</li>
 *   <li>{@code rememberMe} 是否记住登录状态。</li>
 *   <li>{@code deviceInfo} 客户端设备信息，可选。</li>
 * </ul>
 */
public record LoginCommand(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度必须在3-50字符之间")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度必须在6-100字符之间")
        String password,

        boolean rememberMe,

        String deviceInfo
) {
}