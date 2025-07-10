package com.clinflash.baseai.infrastructure.persistence.flow.entity;

import com.clinflash.baseai.domain.flow.model.FlowRunLog;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * <h2>流程运行日志JPA实体</h2>
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "flow_run_logs")
public class FlowRunLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "node_key", length = 64)
    private String nodeKey;

    @Column(name = "io_json", columnDefinition = "TEXT")
    private String ioJson;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public static FlowRunLogEntity fromDomain(FlowRunLog domain) {
        if (domain == null) return null;

        FlowRunLogEntity entity = new FlowRunLogEntity();
        entity.setId(domain.id());
        entity.setRunId(domain.runId());
        entity.setNodeKey(domain.nodeKey());
        entity.setIoJson(domain.ioJson());
        entity.setCreatedBy(domain.createdBy());
        entity.setCreatedAt(domain.createdAt());
        entity.setDeletedAt(domain.deletedAt());
        return entity;
    }

    public FlowRunLog toDomain() {
        return new FlowRunLog(
                this.id,
                this.runId,
                this.nodeKey,
                this.ioJson,
                this.createdBy,
                this.createdAt,
                this.deletedAt
        );
    }
}