package com.cloud.baseai.application.user.dto;

/**
 * 用户注册结果DTO
 *
 * <p>包含用户注册成功后的基本信息和后续操作指引。
 * 这个DTO设计得简洁明了，只包含前端需要的核心信息。</p>
 */
public record RegisterUserResult(
        Long userId,
        String username,
        String email,
        String message
) {
}