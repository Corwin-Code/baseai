package com.cloud.baseai.domain.user.model;

import java.time.OffsetDateTime;

/**
 * <h2>用户领域模型</h2>
 *
 * <p>用户是整个系统最核心的领域概念。在DDD中，实体不仅仅是数据的容器，
 * 更是业务行为的载体。用户实体包含了与用户相关的所有业务逻辑和不变量。</p>
 *
 * <p><b>设计原则：</b></p>
 * <ul>
 * <li><b>封装性：</b>所有字段都是不可变的，只能通过特定的业务方法修改</li>
 * <li><b>完整性：</b>确保对象在任何时候都处于有效状态</li>
 * <li><b>表达性：</b>方法名称清晰地表达业务意图</li>
 * </ul>
 */
public record User(
        Long id,
        String username,
        String passwordHash,
        String email,
        String avatarUrl,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt
) {
    /**
     * 创建新用户
     *
     * <p>这是一个工厂方法，用于创建新的用户实例。
     * 它确保新用户具有正确的初始状态。</p>
     */
    public static User create(String username, String passwordHash, String email, String avatarUrl) {
        OffsetDateTime now = OffsetDateTime.now();
        return new User(
                null,           // id 由持久化层分配
                username,
                passwordHash,
                email,
                avatarUrl,
                null,           // 新用户尚未登录
                now,            // 创建时间
                now,            // 更新时间
                null            // 未删除
        );
    }

    /**
     * 激活用户账户
     *
     * <p>激活是用户生命周期中的重要事件。
     * 这个方法返回一个新的用户实例，体现了不可变对象的设计原则。</p>
     */
    public User activate() {
        return new User(
                this.id,
                this.username,
                this.passwordHash,
                this.email,
                this.avatarUrl,
                this.lastLoginAt,
                this.createdAt,
                OffsetDateTime.now(), // 更新时间
                this.deletedAt
        );
    }

    /**
     * 更新用户资料
     */
    public User updateProfile(String email, String avatarUrl) {
        return new User(
                this.id,
                this.username,
                this.passwordHash,
                email != null ? email : this.email,
                avatarUrl != null ? avatarUrl : this.avatarUrl,
                this.lastLoginAt,
                this.createdAt,
                OffsetDateTime.now(),
                this.deletedAt
        );
    }

    /**
     * 修改密码
     */
    public User changePassword(String newPasswordHash) {
        return new User(
                this.id,
                this.username,
                newPasswordHash,
                this.email,
                this.avatarUrl,
                this.lastLoginAt,
                this.createdAt,
                OffsetDateTime.now(),
                this.deletedAt
        );
    }

    /**
     * 更新最后登录时间
     */
    public User updateLastLogin() {
        return new User(
                this.id,
                this.username,
                this.passwordHash,
                this.email,
                this.avatarUrl,
                OffsetDateTime.now(),
                this.createdAt,
                OffsetDateTime.now(),
                this.deletedAt
        );
    }

    /**
     * 软删除用户
     */
    public User softDelete() {
        return new User(
                this.id,
                this.username,
                this.passwordHash,
                this.email,
                this.avatarUrl,
                this.lastLoginAt,
                this.createdAt,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    // =================== 业务查询方法 ===================

    /**
     * 判断用户是否已激活
     */
    public boolean isActivated() {
        // 简化实现：如果有登录记录或创建时间超过验证期，则认为已激活
        return lastLoginAt != null ||
                (createdAt != null && createdAt.isBefore(OffsetDateTime.now().minusHours(1)));
    }

    /**
     * 判断用户是否已删除
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 判断用户是否活跃（30天内登录过）
     */
    public boolean isActive() {
        return lastLoginAt != null &&
                lastLoginAt.isAfter(OffsetDateTime.now().minusDays(30));
    }

    /**
     * 获取用户年龄（天数）
     */
    public long getAccountAgeInDays() {
        if (createdAt == null) return 0;
        return createdAt.until(OffsetDateTime.now(), java.time.temporal.ChronoUnit.DAYS);
    }
}