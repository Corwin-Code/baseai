package com.clinflash.baseai.application.flow.dto;

import java.time.OffsetDateTime;

public record ProjectDetailDTO(
        Long id,
        String name,
        int totalFlows,
        int publishedFlows,
        int totalRuns,
        int runningFlows,
        Long createdBy,
        String creatorName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}