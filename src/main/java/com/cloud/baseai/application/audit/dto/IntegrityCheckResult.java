package com.cloud.baseai.application.audit.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 完整性检查结果
 */
public record IntegrityCheckResult(boolean isIntegrityValid, long totalRecords, long corruptedRecords,
                                   List<String> issues, OffsetDateTime checkTimestamp) {

}