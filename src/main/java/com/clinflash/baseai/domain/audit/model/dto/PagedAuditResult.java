package com.clinflash.baseai.domain.audit.model.dto;

import com.clinflash.baseai.domain.audit.service.AuditService;

import java.util.List;

/**
 * 分页审计结果
 */
public record PagedAuditResult(List<AuditService.AuditEvent> events, long totalCount, int page, int size) {
}