package com.cloud.baseai.infrastructure.persistence.chat.entity;

import com.cloud.baseai.domain.chat.model.ChatThread;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * <h2>对话线程JPA实体</h2>
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "chat_threads")
public class ChatThreadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", length = 256)
    private String title;

    @Column(name = "default_model", nullable = false, length = 32)
    private String defaultModel;

    @Column(name = "temperature", nullable = false)
    private Float temperature;

    @Column(name = "flow_snapshot_id")
    private Long flowSnapshotId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /**
     * 从领域对象创建实体
     */
    public static ChatThreadEntity fromDomain(ChatThread domain) {
        if (domain == null) return null;

        ChatThreadEntity entity = new ChatThreadEntity();
        entity.setId(domain.id());
        entity.setTenantId(domain.tenantId());
        entity.setUserId(domain.userId());
        entity.setTitle(domain.title());
        entity.setDefaultModel(domain.defaultModel());
        entity.setTemperature(domain.temperature());
        entity.setFlowSnapshotId(domain.flowSnapshotId());
        entity.setCreatedBy(domain.createdBy());
        entity.setUpdatedBy(domain.updatedBy());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        entity.setDeletedAt(domain.deletedAt());
        return entity;
    }

    /**
     * 转换为领域对象
     */
    public ChatThread toDomain() {
        return new ChatThread(
                this.id,
                this.tenantId,
                this.userId,
                this.title,
                this.defaultModel,
                this.temperature,
                this.flowSnapshotId,
                this.createdBy,
                this.updatedBy,
                this.createdAt,
                this.updatedAt,
                this.deletedAt
        );
    }

    /**
     * 从领域对象更新实体
     */
    public void updateFromDomain(ChatThread domain) {
        if (domain == null) return;

        this.setTenantId(domain.tenantId());
        this.setUserId(domain.userId());
        this.setTitle(domain.title());
        this.setDefaultModel(domain.defaultModel());
        this.setTemperature(domain.temperature());
        this.setFlowSnapshotId(domain.flowSnapshotId());
        this.setCreatedBy(domain.createdBy());
        this.setUpdatedBy(domain.updatedBy());
        this.setCreatedAt(domain.createdAt());
        this.setUpdatedAt(domain.updatedAt());
        this.setDeletedAt(domain.deletedAt());
    }
}