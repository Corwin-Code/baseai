package com.clinflash.baseai.infrastructure.persistence.flow.entity;

import com.clinflash.baseai.domain.flow.model.FlowSnapshot;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * <h2>流程快照JPA实体</h2>
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "flow_snapshots")
public class FlowSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "definition_id", nullable = false)
    private Long definitionId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public static FlowSnapshotEntity fromDomain(FlowSnapshot domain) {
        if (domain == null) return null;

        FlowSnapshotEntity entity = new FlowSnapshotEntity();
        entity.setId(domain.id());
        entity.setDefinitionId(domain.definitionId());
        entity.setVersion(domain.version());
        entity.setSnapshotJson(domain.snapshotJson());
        entity.setCreatedBy(domain.createdBy());
        entity.setCreatedAt(domain.createdAt());
        entity.setDeletedAt(domain.deletedAt());
        return entity;
    }

    public FlowSnapshot toDomain() {
        return new FlowSnapshot(
                this.id,
                this.definitionId,
                this.version,
                this.snapshotJson,
                this.createdBy,
                this.createdAt,
                this.deletedAt
        );
    }
}