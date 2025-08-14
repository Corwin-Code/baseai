package com.cloud.baseai.application.auth.dto;

/**
 * 令牌验证响应数据传输对象（DTO），用于封装令牌校验的结果。
 * <p>
 * 一般用于客户端请求验证令牌有效性后，服务端返回验证状态、
 * 相关提示信息以及关联的用户信息。
 * </p>
 *
 * <ul>
 *   <li>{@code valid} 令牌是否有效，true 表示有效，false 表示无效或已过期。</li>
 *   <li>{@code message} 验证结果的描述信息，例如失效原因或成功提示。</li>
 *   <li>{@code userInfo} 关联的用户信息对象，仅在令牌有效时返回。</li>
 * </ul>
 */
public record TokenValidationResponseDTO(
        boolean valid,
        String message,
        UserInfoDTO userInfo
) {
}