package com.cloud.baseai.application.auth.command;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新令牌命令对象，用于封装客户端请求刷新访问令牌时提交的参数。
 * <p>
 * 一般用于在访问令牌（Access Token）过期后，通过有效的刷新令牌
 * 获取新的访问令牌，以维持会话状态。
 * </p>
 *
 * <ul>
 *   <li>{@code refreshToken} 刷新令牌，不能为空。</li>
 * </ul>
 */
public record RefreshTokenCommand(
        @NotBlank(message = "刷新令牌不能为空")
        String refreshToken
) {
}