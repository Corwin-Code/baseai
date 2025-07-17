package com.cloud.baseai.infrastructure.persistence.flow.entity;

import com.cloud.baseai.domain.flow.model.FlowProject;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * <h2>流程项目JPA实体</h2>
 *
 * <p>流程项目的数据库映射实体，负责与flow_projects表的映射。
 * 项目是流程组织的最高层级，为相关流程提供逻辑分组和权限边界。</p>
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "flow_projects")
public class FlowProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

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

    /**
     * 从领域对象创建实体
     */
    public static FlowProjectEntity fromDomain(FlowProject domain) {
        if (domain == null) return null;

        FlowProjectEntity entity = new FlowProjectEntity();
        entity.setId(domain.id());
        entity.setTenantId(domain.tenantId());
        entity.setName(domain.name());
        entity.setCreatedBy(domain.createdBy());
        entity.setUpdatedBy(domain.updatedBy());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        entity.setDeletedAt(domain.deletedAt());
        return entity;
    }

    /**
     * 转换为领域对象
     */
    public FlowProject toDomain() {
        return new FlowProject(
                this.id,
                this.tenantId,
                this.name,
                this.createdBy,
                this.updatedBy,
                this.createdAt,
                this.updatedAt,
                this.deletedAt
        );
    }

    /**
     * 从领域对象更新实体
     */
    public void updateFromDomain(FlowProject domain) {
        if (domain == null) return;

        this.setTenantId(domain.tenantId());
        this.setName(domain.name());
        this.setCreatedBy(domain.createdBy());
        this.setUpdatedBy(domain.updatedBy());
        this.setCreatedAt(domain.createdAt());
        this.setUpdatedAt(domain.updatedAt());
        this.setDeletedAt(domain.deletedAt());
    }
}