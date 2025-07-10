package com.clinflash.baseai.infrastructure.persistence.kb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * <h2>标签JPA实体</h2>
 *
 * <p>对应数据库表 kb_tags 的JPA映射。</p>
 */
@Setter
@Getter
@Entity
@Table(name = "kb_tags")
public class KbTagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 64)
    private String name;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected KbTagEntity() {
    }

    public KbTagEntity(String name, String remark, Long createdBy) {
        this.name = name;
        this.remark = remark;
        this.createdBy = createdBy;
    }

    /**
     * 转换为领域对象
     */
    public com.clinflash.baseai.domain.kb.model.Tag toDomain() {
        return new com.clinflash.baseai.domain.kb.model.Tag(
                this.id,
                this.name,
                this.remark,
                this.createdBy,
                this.createdAt,
                this.deletedAt
        );
    }

    /**
     * 从领域对象创建JPA实体
     */
    public static KbTagEntity fromDomain(com.clinflash.baseai.domain.kb.model.Tag domain) {
        KbTagEntity entity = new KbTagEntity(
                domain.name(),
                domain.remark(),
                domain.createdBy()
        );
        entity.id = domain.id();
        entity.createdAt = domain.createdAt();
        entity.deletedAt = domain.deletedAt();
        return entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KbTagEntity that = (KbTagEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}