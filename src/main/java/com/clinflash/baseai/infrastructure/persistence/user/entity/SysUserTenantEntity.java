package com.clinflash.baseai.infrastructure.persistence.user.entity;

import com.clinflash.baseai.domain.user.model.TenantMemberStatus;
import com.clinflash.baseai.domain.user.model.UserTenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * <h2>用户-租户关联JPA实体</h2>
 *
 * <p>这个实体使用复合主键，需要配合主键类使用。</p>
 */
@Setter
@Getter
@Entity
@Table(name = "sys_user_tenants")
@IdClass(SysUserTenantEntityId.class)
public class SysUserTenantEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "status", nullable = false, columnDefinition = "smallint")
    @Enumerated(EnumType.ORDINAL)
    private TenantMemberStatus status;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    protected SysUserTenantEntity() {
    }

    public SysUserTenantEntity(Long userId, Long tenantId, Long roleId, TenantMemberStatus status) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.roleId = roleId;
        this.status = status;
    }

    /**
     * 转换为领域对象
     */
    public UserTenant toDomain() {
        return new UserTenant(
                this.userId,
                this.tenantId,
                this.roleId,
                this.status,
                this.joinedAt
        );
    }

    /**
     * 从领域对象创建JPA实体
     */
    public static SysUserTenantEntity fromDomain(UserTenant domain) {
        SysUserTenantEntity entity = new SysUserTenantEntity(
                domain.userId(),
                domain.tenantId(),
                domain.roleId(),
                domain.status()
        );
        entity.joinedAt = domain.joinedAt();
        return entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SysUserTenantEntity that = (SysUserTenantEntity) o;
        return Objects.equals(userId, that.userId) && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, tenantId);
    }
}