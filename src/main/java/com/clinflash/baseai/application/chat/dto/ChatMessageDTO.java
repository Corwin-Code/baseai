package com.clinflash.baseai.application.chat.dto;

import java.time.OffsetDateTime;

/**
 * 对话消息DTO
 */
public record ChatMessageDTO(
        Long id,
        Long threadId,
        String role,
        String content,
        String toolCall,
        Integer tokenIn,
        Integer tokenOut,
        Integer latencyMs,
        OffsetDateTime createdAt
) {
}