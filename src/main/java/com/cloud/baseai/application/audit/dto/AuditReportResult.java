package com.cloud.baseai.application.audit.dto;

import java.time.OffsetDateTime;

/**
 * 审计报告结果
 */
public record AuditReportResult(String reportId, String downloadUrl, String format, OffsetDateTime generatedAt) {
}