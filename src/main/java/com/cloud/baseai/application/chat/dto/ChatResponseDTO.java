package com.cloud.baseai.application.chat.dto;

import java.util.List;

/**
 * 对话回复DTO
 */
public record ChatResponseDTO(
        ChatMessageDTO userMessage,
        ChatMessageDTO assistantMessage,
        List<ChatCitationDTO> citations,
        List<Object> toolCalls,
        UsageStatisticsDTO.UsageDetail usage
) {
}