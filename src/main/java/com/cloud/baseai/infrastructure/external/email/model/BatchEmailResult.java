package com.cloud.baseai.infrastructure.external.email.model;

/**
 * 批量邮件发送结果
 */
public record BatchEmailResult(int totalCount, int successCount, int failureCount,
                               java.util.List<String> failedEmails) {
}