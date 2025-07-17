package com.cloud.baseai.infrastructure.persistence.chat.entity;

import com.cloud.baseai.domain.chat.model.ChatCitation;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <h2>对话引用JPA实体</h2>
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "chat_citations")
@IdClass(ChatCitationEntityId.class)
public class ChatCitationEntity {

    @Id
    @Column(name = "message_id")
    private Long messageId;

    @Id
    @Column(name = "chunk_id")
    private Long chunkId;

    @Id
    @Column(name = "model_code", length = 32)
    private String modelCode;

    @Column(name = "score")
    private Float score;

    /**
     * 从领域对象创建实体
     */
    public static ChatCitationEntity fromDomain(ChatCitation domain) {
        if (domain == null) return null;

        ChatCitationEntity entity = new ChatCitationEntity();
        entity.setMessageId(domain.messageId());
        entity.setChunkId(domain.chunkId());
        entity.setModelCode(domain.modelCode());
        entity.setScore(domain.score());
        return entity;
    }

    /**
     * 转换为领域对象
     */
    public ChatCitation toDomain() {
        return new ChatCitation(
                this.messageId,
                this.chunkId,
                this.score,
                this.modelCode
        );
    }
}