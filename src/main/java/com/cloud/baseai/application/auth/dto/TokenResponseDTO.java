package com.cloud.baseai.application.auth.dto;

/**
 * 令牌响应数据传输对象（DTO），用于封装令牌签发或刷新后的返回结果。
 * <p>
 * 一般在登录成功或刷新令牌（Refresh Token）成功后返回，
 * 包含新的访问令牌及相关信息，供客户端进行身份认证和会话管理。
 * </p>
 *
 * <ul>
 *   <li>{@code accessToken} 访问令牌（Access Token），用于后续请求的身份验证。</li>
 *   <li>{@code refreshToken} 刷新令牌（Refresh Token），用于在访问令牌过期后获取新的访问令牌。</li>
 *   <li>{@code tokenType} 令牌类型（通常为 "Bearer"）。</li>
 *   <li>{@code expiresIn} 访问令牌的有效期（单位：秒）。</li>
 * </ul>
 */
public record TokenResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}