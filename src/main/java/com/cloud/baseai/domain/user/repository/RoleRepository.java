package com.cloud.baseai.domain.user.repository;

import com.cloud.baseai.domain.user.model.Role;

import java.util.List;
import java.util.Optional;

/**
 * <h2>角色仓储接口</h2>
 *
 * <p>角色仓储管理系统中的角色定义。角色是权限系统的基础，
 * 定义了用户在系统中的行为边界。</p>
 */
public interface RoleRepository {

    /**
     * 保存角色
     */
    Role save(Role role);

    /**
     * 根据ID查找角色
     */
    Optional<Role> findById(Long id);

    /**
     * 根据角色名查找角色
     */
    Optional<Role> findByName(String name);

    /**
     * 检查角色名是否存在
     */
    boolean existsByName(String name);

    /**
     * 批量查找角色
     */
    List<Role> findByIds(List<Long> ids);

    /**
     * 查找所有角色
     */
    List<Role> findAll();

    /**
     * 查找系统角色
     */
    List<Role> findSystemRoles();

    /**
     * 查找租户角色
     */
    List<Role> findTenantRoles();

    /**
     * 查找管理员角色
     *
     * <p>这个方法用于权限检查，查找所有具有管理员权限的角色。</p>
     */
    List<Role> findAdminRoles();

    /**
     * 按权重排序查找角色
     */
    List<Role> findAllOrderByWeight();

    /**
     * 统计角色总数
     */
    long countAll();
}