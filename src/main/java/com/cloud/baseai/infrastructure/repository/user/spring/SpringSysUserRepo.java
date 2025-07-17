package com.cloud.baseai.infrastructure.repository.user.spring;

import com.cloud.baseai.infrastructure.persistence.user.entity.SysUserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>用户Spring Data JPA仓储</h2>
 *
 * <p>Spring Data JPA仓储提供了强大的查询能力。通过方法命名约定，
 * 它可以自动生成大部分常用的查询。对于复杂查询，我们使用@Query注解
 * 来编写JPQL或原生SQL。</p>
 */
@Repository
public interface SpringSysUserRepo extends JpaRepository<SysUserEntity, Long> {

    /**
     * 根据用户名查找用户
     */
    Optional<SysUserEntity> findByUsernameAndDeletedAtIsNull(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<SysUserEntity> findByEmailAndDeletedAtIsNull(String email);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsernameAndDeletedAtIsNull(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmailAndDeletedAtIsNull(String email);

    /**
     * 查找未删除的用户
     */
    List<SysUserEntity> findByIdInAndDeletedAtIsNull(List<Long> ids);

    /**
     * 模糊搜索用户
     *
     * <p>这个查询支持按用户名、邮箱进行模糊搜索，是一个典型的业务查询。
     * 使用JPQL编写，支持可选的租户过滤。</p>
     */
    @Query("SELECT u FROM SysUserEntity u WHERE u.deletedAt IS NULL " +
            "AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:tenantId IS NULL OR u.id IN (" +
            "    SELECT ut.userId FROM SysUserTenantEntity ut WHERE ut.tenantId = :tenantId" +
            "))")
    Page<SysUserEntity> searchByKeyword(@Param("keyword") String keyword,
                                        @Param("tenantId") Long tenantId,
                                        Pageable pageable);

    /**
     * 统计搜索结果数量
     */
    @Query("SELECT COUNT(u) FROM SysUserEntity u WHERE u.deletedAt IS NULL " +
            "AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:tenantId IS NULL OR u.id IN (" +
            "    SELECT ut.userId FROM SysUserTenantEntity ut WHERE ut.tenantId = :tenantId" +
            "))")
    long countByKeyword(@Param("keyword") String keyword, @Param("tenantId") Long tenantId);

    /**
     * 统计未删除用户总数
     */
    long countByDeletedAtIsNull();

    /**
     * 按创建时间范围统计用户
     */
    @Query("SELECT COUNT(u) FROM SysUserEntity u WHERE u.deletedAt IS NULL " +
            "AND u.createdAt BETWEEN :start AND :end " +
            "AND (:tenantId IS NULL OR u.id IN (" +
            "    SELECT ut.userId FROM SysUserTenantEntity ut WHERE ut.tenantId = :tenantId" +
            "))")
    long countByCreatedAtBetween(@Param("start") OffsetDateTime start,
                                 @Param("end") OffsetDateTime end,
                                 @Param("tenantId") Long tenantId);

    /**
     * 按最后登录时间范围统计用户
     */
    @Query("SELECT COUNT(u) FROM SysUserEntity u WHERE u.deletedAt IS NULL " +
            "AND u.lastLoginAt BETWEEN :start AND :end " +
            "AND (:tenantId IS NULL OR u.id IN (" +
            "    SELECT ut.userId FROM SysUserTenantEntity ut WHERE ut.tenantId = :tenantId" +
            "))")
    long countByLastLoginAtBetween(@Param("start") OffsetDateTime start,
                                   @Param("end") OffsetDateTime end,
                                   @Param("tenantId") Long tenantId);

    /**
     * 软删除用户
     */
    @Modifying
    @Query("UPDATE SysUserEntity u SET u.deletedAt = :deletedAt WHERE u.id = :id AND u.deletedAt IS NULL")
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") OffsetDateTime deletedAt);

    /**
     * 查找过期用户（用于清理任务）
     */
    @Query("SELECT u FROM SysUserEntity u WHERE u.deletedAt IS NOT NULL AND u.deletedAt < :before")
    List<SysUserEntity> findExpiredUsers(@Param("before") OffsetDateTime before);
}