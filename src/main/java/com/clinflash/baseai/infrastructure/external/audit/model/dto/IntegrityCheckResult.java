package com.clinflash.baseai.infrastructure.external.audit.model.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 完整性检查结果
 */
public record IntegrityCheckResult(boolean isIntegrityValid, long totalRecords, long corruptedRecords,
                                   List<String> issues, OffsetDateTime checkTimestamp) {

}