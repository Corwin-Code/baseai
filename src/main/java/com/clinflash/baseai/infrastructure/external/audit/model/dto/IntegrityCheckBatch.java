package com.clinflash.baseai.infrastructure.external.audit.model.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class IntegrityCheckBatch {
    private final long totalRecords;
    private final long corruptedRecords;
    private final List<String> issues;

    public IntegrityCheckBatch(long totalRecords, long corruptedRecords, List<String> issues) {
        this.totalRecords = totalRecords;
        this.corruptedRecords = corruptedRecords;
        this.issues = issues;
    }
}