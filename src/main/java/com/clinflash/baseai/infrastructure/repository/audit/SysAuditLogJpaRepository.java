package com.clinflash.baseai.infrastructure.repository.audit;

import com.clinflash.baseai.infrastructure.external.audit.model.SysAuditLog;
import com.clinflash.baseai.infrastructure.external.audit.repository.SysAuditLogRepository;
import com.clinflash.baseai.infrastructure.persistence.audit.entity.SysAuditLogEntity;
import com.clinflash.baseai.infrastructure.persistence.audit.mapper.AuditMapper;
import com.clinflash.baseai.infrastructure.repository.audit.spring.SpringSysAuditLogRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 系统审计日志仓储实现
 */
@Repository
public class SysAuditLogJpaRepository implements SysAuditLogRepository {

    private final SpringSysAuditLogRepo springRepo;
    private final AuditMapper mapper;

    public SysAuditLogJpaRepository(SpringSysAuditLogRepo springRepo, AuditMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public SysAuditLog save(SysAuditLog log) {
        SysAuditLogEntity entity;

        if (log.id() == null) {
            entity = mapper.toEntity(log);
        } else {
            entity = springRepo.findById(log.id())
                    .orElse(mapper.toEntity(log));
            entity.updateFromDomain(log);
        }

        SysAuditLogEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<SysAuditLog> findById(Long id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<SysAuditLog> findUserActions(Long userId, OffsetDateTime startTime, OffsetDateTime endTime, List<String> actions, Pageable pageable) {
        Page<SysAuditLogEntity> entityPage = springRepo
                .findUserActions(userId, startTime, endTime, actions, pageable);

        return entityPage.map(SysAuditLogEntity::toDomain);
    }

    @Override
    public Page<SysAuditLog> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable) {
        Page<SysAuditLogEntity> entityPage = springRepo
                .findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);

        return entityPage.map(SysAuditLogEntity::toDomain);
    }

    @Override
    public Page<SysAuditLog> findByTimeRange(OffsetDateTime startTime, OffsetDateTime endTime, Pageable pageable) {
        Page<SysAuditLogEntity> entityPage = springRepo
                .findByTimeRange(startTime, endTime, pageable);

        return entityPage.map(SysAuditLogEntity::toDomain);
    }

    @Override
    public long countByTimeRange(OffsetDateTime startTime, OffsetDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return springRepo.countByTimeRange(startTime, endTime);
    }

    @Override
    public List<Object[]> countByActionAndTimeRange(OffsetDateTime startTime, OffsetDateTime endTime) {
        if (startTime == null || endTime == null) {
            return new ArrayList<>();
        }
        return springRepo.countByActionAndTimeRange(startTime, endTime);
    }

    @Override
    public List<Object[]> countByUserAndTimeRange(OffsetDateTime startTime, OffsetDateTime endTime) {
        if (startTime == null || endTime == null) {
            return new ArrayList<>();
        }
        return springRepo.countByUserAndTimeRange(startTime, endTime);
    }

    @Override
    public void deleteOldAuditLogs(OffsetDateTime cutoffTime) {
        springRepo.deleteOldAuditLogs(cutoffTime);
    }
}