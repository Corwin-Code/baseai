package com.cloud.baseai.infrastructure.persistence.audit.mapper;

import com.cloud.baseai.domain.audit.model.SysAuditLog;
import com.cloud.baseai.infrastructure.persistence.audit.entity.SysAuditLogEntity;
import org.springframework.stereotype.Component;

/**
 * 审计日志实体转换器
 */
@Component
public class AuditMapper {

    /**
     * 将领域模型转换为JPA实体
     */
    public SysAuditLogEntity toEntity(SysAuditLog domain) {
        return SysAuditLogEntity.fromDomain(domain);
    }

    /**
     * 将JPA实体转换为领域模型
     */
    public SysAuditLog toDomain(SysAuditLogEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }
}