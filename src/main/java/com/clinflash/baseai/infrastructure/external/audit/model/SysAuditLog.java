package com.clinflash.baseai.infrastructure.external.audit.model;

import java.time.OffsetDateTime;

/**
 * <h2>系统审计日志领域对象</h2>
 */
public record SysAuditLog(
        Long id,
        Long tenantId,
        Long userId,
        String action,
        String targetType,
        Long targetId,
        String ipAddress,
        String userAgent,
        String detail,
        String resultStatus,
        String logLevel,
        OffsetDateTime createdAt
) {
}