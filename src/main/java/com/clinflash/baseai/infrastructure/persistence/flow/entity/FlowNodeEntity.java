package com.clinflash.baseai.infrastructure.persistence.flow.entity;

import com.clinflash.baseai.domain.flow.model.FlowNode;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * <h2>流程节点JPA实体</h2>
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "flow_nodes")
public class FlowNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "definition_id", nullable = false)
    private Long definitionId;

    @Column(name = "node_type_code", nullable = false, length = 32)
    private String nodeTypeCode;

    @Column(name = "node_key", nullable = false, length = 64)
    private String nodeKey;

    @Column(name = "name", length = 128)
    private String name;

    @Column(name = "config_json", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String configJson;

    @Column(name = "retry_policy_json", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String retryPolicyJson;

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

    public static FlowNodeEntity fromDomain(FlowNode domain) {
        if (domain == null) return null;

        FlowNodeEntity entity = new FlowNodeEntity();
        entity.setId(domain.id());
        entity.setDefinitionId(domain.definitionId());
        entity.setNodeTypeCode(domain.nodeTypeCode());
        entity.setNodeKey(domain.nodeKey());
        entity.setName(domain.name());
        entity.setConfigJson(domain.configJson());
        entity.setRetryPolicyJson(domain.retryPolicyJson());
        entity.setCreatedBy(domain.createdBy());
        entity.setUpdatedBy(domain.updatedBy());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        entity.setDeletedAt(domain.deletedAt());
        return entity;
    }

    public FlowNode toDomain() {
        return new FlowNode(
                this.id,
                this.definitionId,
                this.nodeTypeCode,
                this.nodeKey,
                this.name,
                this.configJson,
                this.retryPolicyJson,
                this.createdBy,
                this.updatedBy,
                this.createdAt,
                this.updatedAt,
                this.deletedAt
        );
    }
}