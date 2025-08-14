package com.cloud.baseai.application.auth.dto;

/**
 * 登录响应数据传输对象（DTO），用于封装用户成功登录后返回给客户端的信息。
 * <p>
 * 包含访问令牌、刷新令牌、令牌类型、有效期、用户信息等，
 * 便于客户端在后续请求中进行身份认证和会话管理。
 * </p>
 *
 * <ul>
 *   <li>{@code accessToken} 访问令牌（Access Token），用于后续请求的身份验证。</li>
 *   <li>{@code refreshToken} 刷新令牌（Refresh Token），用于在访问令牌过期后获取新的访问令牌。</li>
 *   <li>{@code tokenType} 令牌类型（通常为 "Bearer"）。</li>
 *   <li>{@code expiresIn} 访问令牌的有效期（单位：秒）。</li>
 *   <li>{@code userInfo} 用户信息对象，包含基本的用户资料。</li>
 *   <li>{@code deviceFingerprint} 客户端设备指纹，用于安全校验和设备识别。</li>
 *   <li>{@code rememberMe} 是否保持长期登录状态。</li>
 * </ul>
 */
public record LoginResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserInfoDTO userInfo,
        String deviceFingerprint,
        boolean rememberMe
) {
}