package com.clinflash.baseai.application.chat.dto;

/**
 * 对话引用DTO
 */
public record ChatCitationDTO(
        Long messageId,
        Long chunkId,
        Float score,
        String modelCode
) {
}