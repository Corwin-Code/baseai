package com.clinflash.baseai.infrastructure.persistence.kb.entity;

import com.clinflash.baseai.infrastructure.persistence.kb.mapper.VectorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * <h2>向量嵌入JPA实体</h2>
 *
 * <p>对应数据库表 kb_embeddings 的JPA映射。
 * 使用自定义的VectorType来支持PostgreSQL的vector类型。</p>
 */
@Setter
@Getter
@Entity
@Table(name = "kb_embeddings")
@IdClass(KbEmbeddingEntityId.class)
public class KbEmbeddingEntity {

    @Id
    @Column(name = "chunk_id")
    private Long chunkId;

    @Id
    @Column(name = "model_code", length = 32)
    private String modelCode;

    @Id
    @Column(name = "vector_version")
    private Integer vectorVersion;

    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
    @Convert(converter = VectorType.class)
    private float[] embedding;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected KbEmbeddingEntity() {
    }

    public KbEmbeddingEntity(Long chunkId, String modelCode, Integer vectorVersion,
                             float[] embedding, Long createdBy) {
        this.chunkId = chunkId;
        this.modelCode = modelCode;
        this.vectorVersion = vectorVersion;
        this.embedding = embedding;
        this.createdBy = createdBy;
    }

    /**
     * 转换为领域对象
     */
    public com.clinflash.baseai.domain.kb.model.Embedding toDomain() {
        return new com.clinflash.baseai.domain.kb.model.Embedding(
                this.chunkId,
                this.modelCode,
                this.vectorVersion,
                this.embedding,
                this.createdBy,
                this.createdAt,
                this.deletedAt
        );
    }

    /**
     * 从领域对象创建JPA实体
     */
    public static KbEmbeddingEntity fromDomain(com.clinflash.baseai.domain.kb.model.Embedding domain) {
        KbEmbeddingEntity entity = new KbEmbeddingEntity(
                domain.chunkId(),
                domain.modelCode(),
                domain.vectorVersion(),
                domain.embedding(),
                domain.createdBy()
        );
        entity.createdAt = domain.createdAt();
        entity.deletedAt = domain.deletedAt();
        return entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KbEmbeddingEntity that = (KbEmbeddingEntity) o;
        return Objects.equals(chunkId, that.chunkId) &&
                Objects.equals(modelCode, that.modelCode) &&
                Objects.equals(vectorVersion, that.vectorVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chunkId, modelCode, vectorVersion);
    }
}