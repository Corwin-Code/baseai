package com.clinflash.baseai.infrastructure.persistence.kb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * <h2>知识块JPA实体</h2>
 *
 * <p>对应数据库表 kb_chunks 的JPA映射。
 * 知识块是文档分割后的基本检索单元。</p>
 */
@Setter
@Getter
@Entity
@Table(name = "kb_chunks")
public class KbChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "chunk_no", nullable = false)
    private Integer chunkNo;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "lang_code", length = 5)
    private String langCode;

    @Column(name = "token_size", nullable = false)
    private Integer tokenSize;

    @Version
    @Column(name = "vector_version")
    private Integer vectorVersion;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected KbChunkEntity() {
    }

    public KbChunkEntity(Long documentId, Integer chunkNo, String text, String langCode,
                         Integer tokenSize, Integer vectorVersion, Long createdBy) {
        this.documentId = documentId;
        this.chunkNo = chunkNo;
        this.text = text;
        this.langCode = langCode;
        this.tokenSize = tokenSize;
        this.vectorVersion = vectorVersion;
        this.createdBy = createdBy;
    }

    /**
     * 转换为领域对象
     */
    public com.clinflash.baseai.domain.kb.model.Chunk toDomain() {
        return new com.clinflash.baseai.domain.kb.model.Chunk(
                this.id,
                this.documentId,
                this.chunkNo,
                this.text,
                this.langCode,
                this.tokenSize,
                this.vectorVersion,
                this.createdBy,
                this.updatedBy,
                this.createdAt,
                this.updatedAt,
                this.deletedAt
        );
    }

    /**
     * 从领域对象创建JPA实体
     */
    public static KbChunkEntity fromDomain(com.clinflash.baseai.domain.kb.model.Chunk domain) {
        KbChunkEntity entity = new KbChunkEntity(
                domain.documentId(),
                domain.chunkNo(),
                domain.text(),
                domain.langCode(),
                domain.tokenSize(),
                domain.vectorVersion(),
                domain.createdBy()
        );
        entity.id = domain.id();
        entity.updatedBy = domain.updatedBy();
        entity.createdAt = domain.createdAt();
        entity.updatedAt = domain.updatedAt();
        entity.deletedAt = domain.deletedAt();
        return entity;
    }

    /**
     * 更新实体字段
     */
    public void updateFromDomain(com.clinflash.baseai.domain.kb.model.Chunk domain) {
        this.text = domain.text();
        this.langCode = domain.langCode();
        this.tokenSize = domain.tokenSize();
        this.vectorVersion = domain.vectorVersion();
        this.updatedBy = domain.updatedBy();
        this.deletedAt = domain.deletedAt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KbChunkEntity that = (KbChunkEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}