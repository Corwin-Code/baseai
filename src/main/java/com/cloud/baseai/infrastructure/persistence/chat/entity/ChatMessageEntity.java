package com.cloud.baseai.infrastructure.persistence.chat.entity;

import com.cloud.baseai.domain.chat.model.ChatMessage;
import com.cloud.baseai.domain.chat.model.MessageRole;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * <h2>对话消息JPA实体</h2>
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "chat_messages")
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thread_id", nullable = false)
    private Long threadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", nullable = false, length = 16)
    private MessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "tool_call", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String toolCall;

    @Column(name = "token_in")
    private Integer tokenIn;

    @Column(name = "token_out")
    private Integer tokenOut;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /**
     * 从领域对象创建实体
     */
    public static ChatMessageEntity fromDomain(ChatMessage domain) {
        if (domain == null) return null;

        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setId(domain.id());
        entity.setThreadId(domain.threadId());
        entity.setRole(domain.role());
        entity.setContent(domain.content());
        entity.setToolCall(domain.toolCall());
        entity.setTokenIn(domain.tokenIn());
        entity.setTokenOut(domain.tokenOut());
        entity.setLatencyMs(domain.latencyMs());
        entity.setParentId(domain.parentId());
        entity.setCreatedBy(domain.createdBy());
        entity.setCreatedAt(domain.createdAt());
        entity.setDeletedAt(domain.deletedAt());
        return entity;
    }

    /**
     * 转换为领域对象
     */
    public ChatMessage toDomain() {
        return new ChatMessage(
                this.id,
                this.threadId,
                this.role,
                this.content,
                this.toolCall,
                this.tokenIn,
                this.tokenOut,
                this.latencyMs,
                this.parentId,
                this.createdBy,
                this.createdAt,
                this.deletedAt
        );
    }
}