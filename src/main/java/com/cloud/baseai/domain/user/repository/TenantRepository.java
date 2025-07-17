package com.cloud.baseai.domain.user.repository;

import com.cloud.baseai.domain.user.model.Tenant;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>租户仓储接口</h2>
 *
 * <p>租户仓储负责管理组织实体的持久化操作。
 * 租户是多租户系统的核心，每个租户都代表一个独立的业务边界。</p>
 */
public interface TenantRepository {

    /**
     * 保存租户
     */
    Tenant save(Tenant tenant);

    /**
     * 根据ID查找租户
     */
    Optional<Tenant> findById(Long id);

    /**
     * 根据组织名称查找租户
     */
    Optional<Tenant> findByOrgName(String orgName);

    /**
     * 检查组织名称是否存在
     */
    boolean existsByOrgName(String orgName);

    /**
     * 检查租户是否存在
     */
    boolean existsById(Long id);

    /**
     * 批量查找租户
     */
    List<Tenant> findByIds(List<Long> ids);

    /**
     * 查找所有活跃租户
     */
    List<Tenant> findAllActive();

    /**
     * 查找即将过期的租户
     *
     * @param beforeDate 指定日期之前过期的租户
     * @return 即将过期的租户列表
     */
    List<Tenant> findExpiringBefore(OffsetDateTime beforeDate);

    /**
     * 按套餐查找租户
     */
    List<Tenant> findByPlanCode(String planCode);

    /**
     * 统计租户总数
     */
    long countAll();

    /**
     * 按创建时间范围统计租户
     */
    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    /**
     * 软删除租户
     */
    boolean softDelete(Long id, Long deletedBy);
}