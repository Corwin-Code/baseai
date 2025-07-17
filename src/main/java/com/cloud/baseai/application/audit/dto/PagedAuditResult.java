package com.cloud.baseai.application.audit.dto;

import com.cloud.baseai.domain.audit.service.AuditService;

import java.util.List;

/**
 * 分页审计结果
 */
public record PagedAuditResult(List<AuditService.AuditEvent> events, long totalCount, int page, int size) {
}