package com.cloud.baseai.infrastructure.external.sms.model;

import java.util.List;

/**
 * 批量短信发送结果
 */
public record BatchSmsResult(int totalCount, int successCount, int failureCount, List<String> failedNumbers,
                             String batchId) {
}