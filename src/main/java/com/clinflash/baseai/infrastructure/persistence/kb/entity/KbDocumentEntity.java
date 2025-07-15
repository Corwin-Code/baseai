package com.clinflash.baseai.infrastructure.persistence.kb.entity;

import com.clinflash.baseai.domain.kb.model.Document;
import com.clinflash.baseai.domain.kb.model.ParsingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * <h2>文档JPA实体</h2>
 *
 * <p>对应数据库表 kb_documents 的JPA映射。
 * 这是基础设施层的数据映射对象，负责与数据库交互。</p>
 *
 * <p><b>设计说明：</b></p>
 * <ul>
 * <li>使用JPA注解进行ORM映射</li>
 * <li>支持乐观锁和审计字段自动填充</li>
 * <li>实现软删除机制</li>
 * <li>提供与领域对象的转换方法</li>
 * </ul>
 */
@Setter
@Getter
@Entity
@Table(name = "kb_documents")
public class KbDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "source_uri", columnDefinition = "TEXT")
    private String sourceUri;

    @Column(name = "mime_type", length = 128)
    private String mimeType;

    @Column(name = "lang_code", length = 5)
    private String langCode;

    @Column(name = "parsing_status", nullable = false, columnDefinition = "smallint")
    @Enumerated(EnumType.ORDINAL)
    private ParsingStatus parsingStatus;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @Column(name = "sha256", length = 64, unique = true)
    private String sha256;

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

    /**
     * 默认构造函数（JPA要求）
     */
    protected KbDocumentEntity() {
    }

    /**
     * 构造函数
     */
    public KbDocumentEntity(Long tenantId, String title, String sourceType, String sourceUri,
                            String mimeType, String langCode, ParsingStatus parsingStatus,
                            Integer chunkCount, String sha256, Long createdBy) {
        this.tenantId = tenantId;
        this.title = title;
        this.sourceType = sourceType;
        this.sourceUri = sourceUri;
        this.mimeType = mimeType;
        this.langCode = langCode;
        this.parsingStatus = parsingStatus;
        this.chunkCount = chunkCount;
        this.sha256 = sha256;
        this.createdBy = createdBy;
    }

    /**
     * 转换为领域对象
     *
     * <p>将JPA实体转换为领域模型对象，实现基础设施层与领域层的解耦。</p>
     *
     * @return 对应的领域模型对象
     */
    public Document toDomain() {
        return new Document(
                this.id,
                this.tenantId,
                this.title,
                this.sourceType,
                this.sourceUri,
                this.mimeType,
                this.langCode,
                this.parsingStatus,
                this.chunkCount,
                this.sha256,
                this.createdBy,
                this.updatedBy,
                this.createdAt,
                this.updatedAt,
                this.deletedAt
        );
    }

    /**
     * 从领域对象创建JPA实体
     *
     * @param domain 领域模型对象
     * @return JPA实体对象
     */
    public static KbDocumentEntity fromDomain(Document domain) {
        KbDocumentEntity entity = new KbDocumentEntity(
                domain.tenantId(),
                domain.title(),
                domain.sourceType(),
                domain.sourceUri(),
                domain.mimeType(),
                domain.langCode(),
                domain.parsingStatus(),
                domain.chunkCount(),
                domain.sha256(),
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
     * 更新实体字段（用于更新操作）
     *
     * @param domain 新的领域对象
     */
    public void updateFromDomain(Document domain) {
        this.title = domain.title();
        this.sourceType = domain.sourceType();
        this.sourceUri = domain.sourceUri();
        this.mimeType = domain.mimeType();
        this.langCode = domain.langCode();
        this.parsingStatus = domain.parsingStatus();
        this.chunkCount = domain.chunkCount();
        this.updatedBy = domain.updatedBy();
        this.deletedAt = domain.deletedAt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KbDocumentEntity that = (KbDocumentEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}