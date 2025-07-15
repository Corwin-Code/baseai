package com.clinflash.baseai.infrastructure.persistence.flow.entity;

import com.clinflash.baseai.domain.flow.model.FlowDefinition;
import com.clinflash.baseai.domain.flow.model.FlowStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * <h2>流程定义JPA实体</h2>
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "flow_definitions")
public class FlowDefinitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "is_latest")
    private Boolean isLatest;

    @Column(name = "status", columnDefinition = "smallint")
    @Enumerated(EnumType.ORDINAL)
    private FlowStatus status;

    @Column(name = "diagram_json", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String diagramJson;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public static FlowDefinitionEntity fromDomain(FlowDefinition domain) {
        if (domain == null) return null;

        FlowDefinitionEntity entity = new FlowDefinitionEntity();
        entity.setId(domain.id());
        entity.setProjectId(domain.projectId());
        entity.setName(domain.name());
        entity.setVersion(domain.version());
        entity.setIsLatest(domain.isLatest());
        entity.setStatus(domain.status());
        entity.setDiagramJson(domain.diagramJson());
        entity.setDescription(domain.description());
        entity.setCreatedBy(domain.createdBy());
        entity.setUpdatedBy(domain.updatedBy());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        entity.setDeletedAt(domain.deletedAt());
        return entity;
    }

    public FlowDefinition toDomain() {
        return new FlowDefinition(
                this.id,
                this.projectId,
                this.name,
                this.version,
                this.isLatest,
                this.status,
                this.diagramJson,
                this.description,
                this.createdBy,
                this.updatedBy,
                this.createdAt,
                this.updatedAt,
                this.deletedAt
        );
    }

    public void updateFromDomain(FlowDefinition domain) {
        if (domain == null) return;

        this.setProjectId(domain.projectId());
        this.setName(domain.name());
        this.setVersion(domain.version());
        this.setIsLatest(domain.isLatest());
        this.setStatus(domain.status());
        this.setDiagramJson(domain.diagramJson());
        this.setDescription(domain.description());
        this.setCreatedBy(domain.createdBy());
        this.setUpdatedBy(domain.updatedBy());
        this.setCreatedAt(domain.createdAt());
        this.setUpdatedAt(domain.updatedAt());
        this.setDeletedAt(domain.deletedAt());
    }
}