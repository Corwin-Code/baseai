package com.clinflash.baseai.infrastructure.repository.user.spring;

import com.clinflash.baseai.infrastructure.persistence.user.entity.SysUserTenantEntity;
import com.clinflash.baseai.infrastructure.persistence.user.entity.SysUserTenantEntityId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface SpringSysUserTenantRepo extends JpaRepository<SysUserTenantEntity, SysUserTenantEntityId> {

    Optional<SysUserTenantEntity> findByUserIdAndTenantId(Long userId, Long tenantId);

    boolean existsByUserIdAndTenantId(Long userId, Long tenantId);

    List<SysUserTenantEntity> findByUserId(Long userId);

    List<SysUserTenantEntity> findByTenantId(Long tenantId);

    /**
     * 按条件查找租户成员
     */
    @Query("SELECT ut FROM SysUserTenantEntity ut WHERE ut.tenantId = :tenantId " +
            "AND (:status IS NULL OR ut.status = :statusCode) " +
            "AND (:roleId IS NULL OR ut.roleId = :roleId)")
    List<SysUserTenantEntity> findByTenantIdWithFilters(@Param("tenantId") Long tenantId,
                                                        @Param("statusCode") Integer statusCode,
                                                        @Param("roleId") Long roleId,
                                                        Pageable pageable);

    long countByTenantId(Long tenantId);

    @Query("SELECT COUNT(ut) FROM SysUserTenantEntity ut WHERE ut.tenantId = :tenantId " +
            "AND (:status IS NULL OR ut.status = :statusCode) " +
            "AND (:roleId IS NULL OR ut.roleId = :roleId)")
    long countByTenantIdWithFilters(@Param("tenantId") Long tenantId,
                                    @Param("statusCode") Integer statusCode,
                                    @Param("roleId") Long roleId);

    /**
     * 按状态分组统计
     */
    @Query("SELECT ut.status, COUNT(ut) FROM SysUserTenantEntity ut " +
            "WHERE ut.tenantId = :tenantId GROUP BY ut.status")
    List<Object[]> countByTenantIdGroupByStatus(@Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(ut) FROM SysUserTenantEntity ut WHERE ut.tenantId = :tenantId " +
            "AND ut.roleId IN :roleIds")
    long countByTenantIdAndRoleIds(@Param("tenantId") Long tenantId, @Param("roleIds") Set<Long> roleIds);

    void deleteByUserId(Long userId);

    void deleteByTenantId(Long tenantId);
}