package com.cloud.baseai.application.auth.command;

import jakarta.validation.constraints.NotBlank;

/**
 * 令牌验证命令对象，用于封装客户端请求验证令牌有效性时提交的参数。
 * <p>
 * 一般用于在需要确认当前令牌（如访问令牌或其他授权令牌）是否有效时，
 * 由客户端发送到服务端进行校验。
 * </p>
 *
 * <ul>
 *   <li>{@code token} 需要验证的令牌，不能为空。</li>
 * </ul>
 */
public record TokenValidationCommand(
        @NotBlank(message = "令牌不能为空")
        String token
) {
}