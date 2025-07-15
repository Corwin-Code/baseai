package com.clinflash.baseai.infrastructure.persistence.flow.entity;

import com.clinflash.baseai.domain.flow.model.FlowRun;
import com.clinflash.baseai.domain.flow.model.RunStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * <h2>流程运行JPA实体</h2>
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "flow_runs")
public class FlowRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_id", nullable = false)
    private Long snapshotId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "status", columnDefinition = "smallint")
    @Enumerated(EnumType.ORDINAL)
    private RunStatus status;

    @Column(name = "result_json", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String resultJson;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public static FlowRunEntity fromDomain(FlowRun domain) {
        if (domain == null) return null;

        FlowRunEntity entity = new FlowRunEntity();
        entity.setId(domain.id());
        entity.setSnapshotId(domain.snapshotId());
        entity.setUserId(domain.userId());
        entity.setStatus(domain.status());
        entity.setResultJson(domain.resultJson());
        entity.setCreatedBy(domain.createdBy());
        entity.setStartedAt(domain.startedAt());
        entity.setFinishedAt(domain.finishedAt());
        entity.setDeletedAt(domain.deletedAt());
        return entity;
    }

    public FlowRun toDomain() {
        return new FlowRun(
                this.id,
                this.snapshotId,
                this.userId,
                this.status,
                this.resultJson,
                this.createdBy,
                this.startedAt,
                this.finishedAt,
                this.deletedAt
        );
    }

    public void updateFromDomain(FlowRun domain) {
        if (domain == null) return;

        this.setSnapshotId(domain.snapshotId());
        this.setUserId(domain.userId());
        this.setStatus(domain.status());
        this.setResultJson(domain.resultJson());
        this.setCreatedBy(domain.createdBy());
        this.setStartedAt(domain.startedAt());
        this.setFinishedAt(domain.finishedAt());
        this.setDeletedAt(domain.deletedAt());
    }
}