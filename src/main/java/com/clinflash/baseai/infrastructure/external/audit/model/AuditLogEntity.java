package com.clinflash.baseai.infrastructure.external.audit.model;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
public class AuditLogEntity {
    private String eventType;
    private Long userId;
    private String action;
    private String targetType;
    private Long targetId;
    private String description;
    private OffsetDateTime timestamp;
    private String ipAddress;
    private String userAgent;
    private String metadata;
    private String integrityHash;
}