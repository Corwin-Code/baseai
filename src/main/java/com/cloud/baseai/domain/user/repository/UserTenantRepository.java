package com.cloud.baseai.domain.user.repository;

import com.cloud.baseai.domain.user.model.UserTenant;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <h2>用户-租户关联仓储接口</h2>
 *
 * <p>这个仓储管理用户与租户之间的多对多关系，
 * 包含了用户在不同租户中的角色和状态信息。</p>
 */
public interface UserTenantRepository {

    /**
     * 保存用户-租户关联
     */
    UserTenant save(UserTenant userTenant);

    /**
     * 查找用户-租户关联
     */
    Optional<UserTenant> findByUserIdAndTenantId(Long userId, Long tenantId);

    /**
     * 检查用户是否为租户成员
     */
    boolean existsByUserIdAndTenantId(Long userId, Long tenantId);

    /**
     * 查找用户的所有租户
     */
    List<UserTenant> findByUserId(Long userId);

    /**
     * 查找租户的所有成员
     */
    List<UserTenant> findByTenantId(Long tenantId);

    /**
     * 按条件查找租户成员
     *
     * <p>支持按状态、角色等条件过滤租户成员，用于成员管理页面。</p>
     *
     * @param tenantId 租户ID
     * @param status   成员状态过滤
     * @param roleId   角色过滤
     * @param page     页码
     * @param size     每页大小
     * @return 匹配的成员列表
     */
    List<UserTenant> findByTenantIdWithFilters(Long tenantId, String status, Long roleId,
                                               int page, int size);

    /**
     * 统计租户成员数量
     */
    long countByTenantId(Long tenantId);

    /**
     * 按条件统计租户成员数量
     */
    long countByTenantIdWithFilters(Long tenantId, String status, Long roleId);

    /**
     * 按状态分组统计租户成员
     *
     * @param tenantId 租户ID
     * @return 状态 -> 数量的映射
     */
    Map<String, Long> countByTenantIdGroupByStatus(Long tenantId);

    /**
     * 统计用户拥有特定角色的租户成员数量
     *
     * <p>这个方法用于权限检查，比如确保租户至少有一个管理员。</p>
     */
    long countByTenantIdAndRoleIds(Long tenantId, Set<Long> roleIds);

    /**
     * 删除用户-租户关联
     */
    void delete(UserTenant userTenant);

    /**
     * 批量删除用户的所有租户关联
     */
    void deleteByUserId(Long userId);

    /**
     * 批量删除租户的所有成员关联
     */
    void deleteByTenantId(Long tenantId);
}