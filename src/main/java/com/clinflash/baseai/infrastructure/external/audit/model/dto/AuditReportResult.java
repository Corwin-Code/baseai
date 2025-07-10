package com.clinflash.baseai.infrastructure.external.audit.model.dto;

import java.time.OffsetDateTime;

/**
 * 审计报告结果
 */
public record AuditReportResult(String reportId, String downloadUrl, String format, OffsetDateTime generatedAt) {
}