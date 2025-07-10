package com.clinflash.baseai.application.user.dto;

/**
 * 角色DTO
 *
 * <p>角色的基本信息，用于角色选择和权限管理。
 * 包含角色的代码、名称和显示标签。</p>
 */
public record RoleDTO(
        Long id,
        String name,      // 角色代码，如 TENANT_ADMIN
        String label      // 角色显示名，如 "租户管理员"
) {
    /**
     * 判断是否为系统级角色
     */
    public boolean isSystemRole() {
        return name != null && name.startsWith("SYSTEM_");
    }

    /**
     * 判断是否为租户级角色
     */
    public boolean isTenantRole() {
        return name != null && name.startsWith("TENANT_");
    }
}