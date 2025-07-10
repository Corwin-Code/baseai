package com.clinflash.baseai.infrastructure.persistence.user.entity;

import com.clinflash.baseai.domain.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * <h2>用户JPA实体</h2>
 *
 * <p>这个实体类是数据库表 sys_users 的对象关系映射。它使用JPA注解来定义
 * 与数据库的映射关系，同时提供了与领域对象转换的方法。</p>
 *
 * <p><b>设计考虑：</b></p>
 * <ul>
 * <li><b>ORM映射：</b>使用JPA注解定义表结构和约束</li>
 * <li><b>审计字段：</b>自动维护创建和更新时间</li>
 * <li><b>软删除：</b>支持逻辑删除而非物理删除</li>
 * <li><b>领域转换：</b>提供与领域对象的双向转换</li>
 * </ul>
 */
@Setter
@Getter
@Entity
@Table(name = "sys_users")
public class SysUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;

    @Column(name = "email", unique = true, length = 128)
    private String email;

    @Column(name = "avatar_url", length = 256)
    private String avatarUrl;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // 版本号用于乐观锁
    @Version
    private Long version;

    /**
     * 默认构造函数（JPA要求）
     */
    protected SysUserEntity() {
    }

    /**
     * 构造函数
     */
    public SysUserEntity(String username, String passwordHash, String email, String avatarUrl) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.avatarUrl = avatarUrl;
    }

    /**
     * 转换为领域对象
     *
     * <p>将JPA实体转换为领域模型对象。这个转换过程确保了领域层的纯净性，
     * 让领域对象不需要依赖任何基础设施层的技术。</p>
     */
    public User toDomain() {
        return new User(
                this.id,
                this.username,
                this.passwordHash,
                this.email,
                this.avatarUrl,
                this.lastLoginAt,
                this.createdAt,
                this.updatedAt,
                this.deletedAt
        );
    }

    /**
     * 从领域对象创建JPA实体
     *
     * <p>这是一个静态工厂方法，用于从领域对象创建JPA实体。
     * 它确保了从领域层到基础设施层的正确转换。</p>
     */
    public static SysUserEntity fromDomain(User domain) {
        SysUserEntity entity = new SysUserEntity(
                domain.username(),
                domain.passwordHash(),
                domain.email(),
                domain.avatarUrl()
        );
        entity.id = domain.id();
        entity.lastLoginAt = domain.lastLoginAt();
        entity.createdAt = domain.createdAt();
        entity.updatedAt = domain.updatedAt();
        entity.deletedAt = domain.deletedAt();
        return entity;
    }

    /**
     * 更新实体字段（用于更新操作）
     *
     * <p>这个方法用于更新现有实体的字段值，它保留了实体的ID和审计信息，
     * 只更新业务相关的字段。</p>
     */
    public void updateFromDomain(User domain) {
        this.username = domain.username();
        this.passwordHash = domain.passwordHash();
        this.email = domain.email();
        this.avatarUrl = domain.avatarUrl();
        this.lastLoginAt = domain.lastLoginAt();
        this.deletedAt = domain.deletedAt();
        // updatedAt 由 @UpdateTimestamp 自动更新
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SysUserEntity that = (SysUserEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SysUserEntity{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}