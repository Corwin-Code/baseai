package com.clinflash.baseai.infrastructure.persistence.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * <h2>角色JPA实体</h2>
 */
@Setter
@Getter
@Entity
@Table(name = "sys_roles")
public class SysRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 64)
    private String name;

    @Column(name = "label", nullable = false, length = 64)
    private String label;

    protected SysRoleEntity() {
    }

    public SysRoleEntity(String name, String label) {
        this.name = name;
        this.label = label;
    }

    /**
     * 转换为领域对象
     */
    public com.clinflash.baseai.domain.user.model.Role toDomain() {
        return new com.clinflash.baseai.domain.user.model.Role(
                this.id,
                this.name,
                this.label
        );
    }

    /**
     * 从领域对象创建JPA实体
     */
    public static SysRoleEntity fromDomain(com.clinflash.baseai.domain.user.model.Role domain) {
        SysRoleEntity entity = new SysRoleEntity(domain.name(), domain.label());
        entity.id = domain.id();
        return entity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SysRoleEntity that = (SysRoleEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}