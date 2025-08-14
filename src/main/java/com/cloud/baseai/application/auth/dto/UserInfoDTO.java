package com.cloud.baseai.application.auth.dto;

import java.util.List;

/**
 * 用户信息数据传输对象（DTO），用于封装系统中的基本用户资料。
 * <p>
 * 一般在登录响应、令牌验证等场景中返回，方便客户端展示
 * 用户的身份信息、权限角色和租户信息。
 * </p>
 *
 * <ul>
 *   <li>{@code id} 用户唯一标识（主键ID）。</li>
 *   <li>{@code username} 用户名。</li>
 *   <li>{@code email} 用户邮箱地址。</li>
 *   <li>{@code roles} 用户所属的角色列表，用于权限控制。</li>
 *   <li>{@code tenantIds} 用户所属的租户ID列表，用于多租户场景下的数据隔离。</li>
 * </ul>
 */
public record UserInfoDTO(
        Long id,
        String username,
        String email,
        List<String> roles,
        List<Long> tenantIds
) {
}