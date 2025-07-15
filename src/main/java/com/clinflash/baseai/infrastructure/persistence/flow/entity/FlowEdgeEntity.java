package com.clinflash.baseai.infrastructure.persistence.flow.entity;

import com.clinflash.baseai.domain.flow.model.FlowEdge;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * <h2>流程边JPA实体</h2>
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "flow_edges")
public class FlowEdgeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "definition_id", nullable = false)
    private Long definitionId;

    @Column(name = "source_key", nullable = false, length = 64)
    private String sourceKey;

    @Column(name = "target_key", nullable = false, length = 64)
    private String targetKey;

    @Column(name = "config_json", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String configJson;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public static FlowEdgeEntity fromDomain(FlowEdge domain) {
        if (domain == null) return null;

        FlowEdgeEntity entity = new FlowEdgeEntity();
        entity.setId(domain.id());
        entity.setDefinitionId(domain.definitionId());
        entity.setSourceKey(domain.sourceKey());
        entity.setTargetKey(domain.targetKey());
        entity.setConfigJson(domain.configJson());
        entity.setCreatedBy(domain.createdBy());
        entity.setCreatedAt(domain.createdAt());
        entity.setDeletedAt(domain.deletedAt());
        return entity;
    }

    public FlowEdge toDomain() {
        return new FlowEdge(
                this.id,
                this.definitionId,
                this.sourceKey,
                this.targetKey,
                this.configJson,
                this.createdBy,
                this.createdAt,
                this.deletedAt
        );
    }
}