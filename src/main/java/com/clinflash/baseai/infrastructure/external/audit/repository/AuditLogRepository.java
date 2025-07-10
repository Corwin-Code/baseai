package com.clinflash.baseai.infrastructure.external.audit.repository;

import com.clinflash.baseai.infrastructure.external.audit.model.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    Page<AuditLogEntity> findUserActions(Long userId, OffsetDateTime startTime, OffsetDateTime endTime, List<String> actions, Pageable pageable);
}