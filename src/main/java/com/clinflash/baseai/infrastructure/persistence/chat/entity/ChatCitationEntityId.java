package com.clinflash.baseai.infrastructure.persistence.chat.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * <h2>对话引用复合主键</h2>
 */
@Data
@NoArgsConstructor
public class ChatCitationEntityId implements Serializable {
    private Long messageId;
    private Long chunkId;
    private String modelCode;

    public ChatCitationEntityId(Long messageId, Long chunkId, String modelCode) {
        this.messageId = messageId;
        this.chunkId = chunkId;
        this.modelCode = modelCode;
    }
}