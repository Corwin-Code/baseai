package com.cloud.baseai.application.audit.dto;

import java.util.List;

/**
 * 批量审计结果
 */
public record BatchAuditResult(int totalCount, int successCount, int failureCount, List<String> errors) {
}