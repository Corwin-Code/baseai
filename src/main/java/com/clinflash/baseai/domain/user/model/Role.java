package com.clinflash.baseai.domain.user.model;

/**
 * <h2>角色领域模型</h2>
 *
 * <p>角色是权限系统的基础，它定义了用户在系统中可以执行的操作。
 * 我们采用基于角色的访问控制（RBAC）模型。</p>
 */
public record Role(
        Long id,
        String name,    // 角色代码，如 TENANT_ADMIN
        String label    // 角色显示名，如 "租户管理员"
) {
    /**
     * 创建新角色
     */
    public static Role create(String name, String label) {
        return new Role(null, name, label);
    }

    /**
     * 更新角色信息
     */
    public Role updateInfo(String name, String label) {
        return new Role(
                this.id,
                name != null ? name : this.name,
                label != null ? label : this.label
        );
    }

    // =================== 业务查询方法 ===================

    /**
     * 判断是否为管理员角色
     */
    public boolean isAdminRole() {
        return name != null && name.contains("ADMIN");
    }

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

    /**
     * 获取角色权重（用于权限比较）
     */
    public int getRoleWeight() {
        if (name == null) return 0;

        if (name.contains("OWNER")) return 100;
        if (name.contains("ADMIN")) return 80;
        if (name.contains("MANAGER")) return 60;
        if (name.contains("MEMBER")) return 40;
        if (name.contains("GUEST")) return 20;

        return 10; // 默认权重
    }
}