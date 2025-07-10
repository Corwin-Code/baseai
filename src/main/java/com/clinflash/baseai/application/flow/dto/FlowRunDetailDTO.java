package com.clinflash.baseai.application.flow.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record FlowRunDetailDTO(
        Long id,
        Long snapshotId,
        String definitionName,
        String status,
        String resultJson,
        List<FlowRunLogDTO> logs,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        Long durationMs
) {
}