package com.cloud.baseai.infrastructure.persistence.user.entity;

import com.cloud.baseai.domain.user.model.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * <h2>用户-角色关联JPA实体</h2>
 */
@Setter
@Getter
@Entity
@Table(name = "sys_user_roles")
@IdClass(SysUserRoleEntityId.class)
public class SysUserRoleEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "role_id")
    private Long roleId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected SysUserRoleEntity() {
    }

    public SysUserRoleEntity(Long userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    /**
     * 转换为领域对象
     */
    public UserRole toDomain() {
        return new UserRole(
                this.userId,
                this.roleId,
                this.createdAt
        );
    }

    /**
     * 从领域对象创建JPA实体
     */
    public static SysUserRoleEntity fromDomain(UserRole domain) {
        SysUserRoleEntity entity = new SysUserRoleEntity(domain.userId(), domain.roleId());
        entity.createdAt = domain.createdAt();
        return entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SysUserRoleEntity that = (SysUserRoleEntity) o;
        return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }
}