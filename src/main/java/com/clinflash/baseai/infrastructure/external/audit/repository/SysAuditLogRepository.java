package com.clinflash.baseai.infrastructure.external.audit.repository;

import com.clinflash.baseai.infrastructure.external.audit.model.SysAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>系统审计日志仓储接口</h2>
 */
public interface SysAuditLogRepository {

    /**
     * 保存日志（新增）
     */
    SysAuditLog save(SysAuditLog log);

    /**
     * 根据日志ID查找日志
     */
    Optional<SysAuditLog> findById(Long id);

    /**
     * 根据用户ID和时间范围查询审计日志
     */
    Page<SysAuditLog> findUserActions(Long userId, OffsetDateTime startTime, OffsetDateTime endTime, List<String> actions, Pageable pageable);

    /**
     * 根据租户ID查询审计日志
     */
    Page<SysAuditLog> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    /**
     * 根据时间范围查询审计日志
     */
    Page<SysAuditLog> findByTimeRange(OffsetDateTime startTime, OffsetDateTime endTime, Pageable pageable);

    /**
     * 统计指定时间范围内的审计日志数量
     */
    long countByTimeRange(OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * 按操作类型统计
     */
    List<Object[]> countByActionAndTimeRange(OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * 按用户统计
     */
    List<Object[]> countByUserAndTimeRange(OffsetDateTime startTime, OffsetDateTime endTime);

    /**
     * 删除指定时间之前的审计日志（数据清理）
     */
    void deleteOldAuditLogs(OffsetDateTime cutoffTime);
}