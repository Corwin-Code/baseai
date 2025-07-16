package com.clinflash.baseai.infrastructure.repository.audit.spring;

import com.clinflash.baseai.infrastructure.persistence.audit.entity.SysAuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * <h2>系统审计日志Spring Data JPA仓储</h2>
 */
@Repository
public interface SpringSysAuditLogRepo extends JpaRepository<SysAuditLogEntity, Long> {

    /**
     * 根据用户ID和时间范围查询审计日志
     */
    @Query("SELECT a FROM SysAuditLogEntity a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) " +
            "AND (:startTime IS NULL OR a.createdAt >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdAt <= :endTime) " +
            "AND (:actions IS NULL OR a.action IN :actions) " +
            "ORDER BY a.createdAt DESC")
    Page<SysAuditLogEntity> findUserActions(
            @Param("userId") Long userId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("actions") List<String> actions,
            Pageable pageable
    );

    /**
     * 根据租户ID查询审计日志
     */
    Page<SysAuditLogEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    /**
     * 根据时间范围查询审计日志
     */
    @Query("SELECT a FROM SysAuditLogEntity a WHERE " +
            "a.createdAt >= :startTime AND a.createdAt <= :endTime " +
            "ORDER BY a.createdAt DESC")
    Page<SysAuditLogEntity> findByTimeRange(
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            Pageable pageable
    );

    /**
     * 统计指定时间范围内的审计日志数量
     */
    @Query("SELECT COUNT(a) FROM SysAuditLogEntity a WHERE " +
            "a.createdAt >= :startTime AND a.createdAt <= :endTime")
    long countByTimeRange(
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    /**
     * 按操作类型统计
     */
    @Query("SELECT a.action, COUNT(a) FROM SysAuditLogEntity a WHERE " +
            "a.createdAt >= :startTime AND a.createdAt <= :endTime " +
            "GROUP BY a.action")
    List<Object[]> countByActionAndTimeRange(
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    /**
     * 按用户统计
     */
    @Query("SELECT a.userId, COUNT(a) FROM SysAuditLogEntity a WHERE " +
            "a.createdAt >= :startTime AND a.createdAt <= :endTime " +
            "AND a.userId IS NOT NULL " +
            "GROUP BY a.userId")
    List<Object[]> countByUserAndTimeRange(
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    /**
     * 删除指定时间之前的审计日志（数据清理）
     */
    @Modifying
    @Query("DELETE FROM SysAuditLogEntity a WHERE a.createdAt < :cutoffTime")
    void deleteOldAuditLogs(@Param("cutoffTime") OffsetDateTime cutoffTime);

}