package com.cloud.baseai.infrastructure.persistence.misc.entity;

import com.cloud.baseai.domain.misc.model.PromptTemplate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * <h2>提示词模板JPA实体</h2>
 */
@Setter
@Getter
@Entity
@Table(name = "prompt_templates")
public class PromptTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "model_code", nullable = false, length = 32)
    private String modelCode;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // 构造函数
    public PromptTemplateEntity() {
    }

    // =================== 领域对象转换 ===================

    /**
     * 从领域对象创建实体
     */
    public static PromptTemplateEntity fromDomain(PromptTemplate template) {
        if (template == null) {
            return null;
        }

        PromptTemplateEntity entity = new PromptTemplateEntity();
        entity.id = template.id();
        entity.tenantId = template.tenantId();
        entity.name = template.name();
        entity.content = template.content();
        entity.modelCode = template.modelCode();
        entity.version = template.version();
        entity.isSystem = template.isSystem();
        entity.createdBy = template.createdBy();
        entity.createdAt = template.createdAt();
        entity.deletedAt = template.deletedAt();

        return entity;
    }

    /**
     * 转换为领域对象
     */
    public PromptTemplate toDomain() {
        return new PromptTemplate(
                id,
                tenantId,
                name,
                content,
                modelCode,
                version,
                isSystem,
                createdBy,
                createdAt,
                deletedAt
        );
    }

    /**
     * 从领域对象更新实体状态
     */
    public void updateFromDomain(PromptTemplate template) {
        if (template == null) {
            return;
        }

        // ID和创建时间不可更改
        this.tenantId = template.tenantId();
        this.name = template.name();
        this.content = template.content();
        this.modelCode = template.modelCode();
        this.version = template.version();
        this.isSystem = template.isSystem();
        this.createdBy = template.createdBy();
        this.deletedAt = template.deletedAt();
    }
}