package com.clinflash.baseai.domain.user.repository;

import com.clinflash.baseai.domain.user.model.UserRole;

import java.util.List;

/**
 * <h2>用户-角色关联仓储接口</h2>
 *
 * <p>管理用户的全局角色关联。与UserTenantRepository不同，
 * 这里的角色是系统级别的，不限定在特定租户内。</p>
 */
public interface UserRoleRepository {

    /**
     * 保存用户-角色关联
     */
    UserRole save(UserRole userRole);

    /**
     * 批量保存用户-角色关联
     */
    List<UserRole> saveAll(List<UserRole> userRoles);

    /**
     * 查找用户的所有全局角色
     */
    List<UserRole> findByUserId(Long userId);

    /**
     * 查找拥有特定角色的所有用户
     */
    List<UserRole> findByRoleId(Long roleId);

    /**
     * 检查用户是否拥有特定角色
     */
    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    /**
     * 删除用户的特定角色
     */
    void deleteByUserIdAndRoleId(Long userId, Long roleId);

    /**
     * 删除用户的所有全局角色
     */
    void deleteByUserId(Long userId);

    /**
     * 删除角色的所有用户关联
     */
    void deleteByRoleId(Long roleId);

    /**
     * 统计拥有特定角色的用户数量
     */
    long countByRoleId(Long roleId);
}