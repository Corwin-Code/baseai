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

    /**
     * 快照主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的流程定义ID
     */
    @Column(name = "definition_id", nullable = false)
    private Long definitionId;

    /**
     * 快照名称
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 快照版本号
     */
    @Column(name = "version", nullable = false)
    private Integer version;

    /**
     * 快照JSON内容
     */
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "LONGTEXT")
    private String snapshotJson;

    /**
     * 创建者ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * 删除时间（软删除）
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /**
     * 从领域对象转换为实体对象
     *
     * @param domain 领域对象
     * @return 对应的实体对象，如果输入为null则返回null
     */
    public static FlowSnapshotEntity fromDomain(FlowSnapshot domain) {
        if (domain == null) return null;

        FlowSnapshotEntity entity = new FlowSnapshotEntity();
        entity.setId(domain.id());
        entity.setDefinitionId(domain.definitionId());
        entity.setName(domain.name());
        entity.setVersion(domain.version());
        entity.setSnapshotJson(domain.snapshotJson());
        entity.setCreatedBy(domain.createdBy());
        entity.setCreatedAt(domain.createdAt());
        entity.setDeletedAt(domain.deletedAt());

        return entity;
    }

    /**
     * 转换为领域对象
     *
     * @return 对应的领域对象
     */
    public FlowSnapshot toDomain() {
        return FlowSnapshot.fromPersistence(
                this.id,
                this.definitionId,
                this.name,
                this.version,
                this.snapshotJson,
                this.createdBy,
                this.createdAt,
                this.deletedAt
        );
    }

    /**
     * 检查快照是否已被删除
     *
     * @return 如果已删除则返回true
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 检查快照是否可用
     *
     * @return 如果快照可用则返回true
     */
    public boolean isAvailable() {
        return !isDeleted() &&
                this.snapshotJson != null &&
                !this.snapshotJson.trim().isEmpty();
    }
}