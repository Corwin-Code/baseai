package com.clinflash.baseai.application.chat.dto;

import java.time.OffsetDateTime;

/**
 * 对话线程DTO
 */
public record ChatThreadDTO(
        Long id,
        String title,
        String defaultModel,
        Float temperature,
        Long flowSnapshotId,
        Long userId,
        String userName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}