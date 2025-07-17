package com.cloud.baseai.application.audit.dto;

import java.util.Map;

/**
 * 审计统计信息
 */
public record AuditStatistics(Map<String, Long> operationCounts, Map<String, Long> userActivityCounts,
                       Map<String, Long> securityEventCounts, Map<String, Object> additionalMetrics) {
}