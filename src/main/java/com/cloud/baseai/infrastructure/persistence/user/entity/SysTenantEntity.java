package com.cloud.baseai.infrastructure.persistence.user.entity;

import com.cloud.baseai.domain.user.model.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * <h2>租户JPA实体</h2>
 */
@Setter
@Getter
@Entity
@Table(name = "sys_tenants")
public class SysTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_name", nullable = false, length = 128)
    private String orgName;

    @Column(name = "plan_code", length = 32)
    private String planCode;

    @Column(name = "expire_at")
    private OffsetDateTime expireAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected SysTenantEntity() {
    }

    public SysTenantEntity(String orgName, String planCode, OffsetDateTime expireAt) {
        this.orgName = orgName;
        this.planCode = planCode;
        this.expireAt = expireAt;
    }

    /**
     * 转换为领域对象
     */
    public Tenant toDomain() {
        return new Tenant(
                this.id,
                this.orgName,
                this.planCode,
                this.expireAt,
                this.createdAt,
                this.updatedAt,
                this.deletedAt
        );
    }

    /**
     * 从领域对象创建JPA实体
     */
    public static SysTenantEntity fromDomain(Tenant domain) {
        SysTenantEntity entity = new SysTenantEntity(
                domain.orgName(),
                domain.planCode(),
                domain.expireAt()
        );
        entity.id = domain.id();
        entity.createdAt = domain.createdAt();
        entity.updatedAt = domain.updatedAt();
        entity.deletedAt = domain.deletedAt();
        return entity;
    }

    /**
     * 更新实体字段
     */
    public void updateFromDomain(Tenant domain) {
        this.orgName = domain.orgName();
        this.planCode = domain.planCode();
        this.expireAt = domain.expireAt();
        this.deletedAt = domain.deletedAt();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SysTenantEntity that = (SysTenantEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}