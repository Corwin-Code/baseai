package com.clinflash.baseai.domain.user.model;

import java.time.OffsetDateTime;

/**
 * <h2>租户领域模型</h2>
 *
 * <p>租户是多租户系统的核心概念，代表一个独立的组织或企业。
 * 每个租户都有自己的数据边界和权限体系。</p>
 */
public record Tenant(
        Long id,
        String orgName,
        String planCode,
        OffsetDateTime expireAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt
) {
    /**
     * 创建新租户
     */
    public static Tenant create(String orgName, String planCode, OffsetDateTime expireAt) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Tenant(
                null,
                orgName,
                planCode,
                expireAt,
                now,
                now,
                null
        );
    }

    /**
     * 更新租户信息
     */
    public Tenant updateInfo(String orgName, String planCode, OffsetDateTime expireAt) {
        return new Tenant(
                this.id,
                orgName != null ? orgName : this.orgName,
                planCode != null ? planCode : this.planCode,
                expireAt != null ? expireAt : this.expireAt,
                this.createdAt,
                OffsetDateTime.now(),
                this.deletedAt
        );
    }

    /**
     * 续费租户
     */
    public Tenant renew(OffsetDateTime newExpireAt) {
        return new Tenant(
                this.id,
                this.orgName,
                this.planCode,
                newExpireAt,
                this.createdAt,
                OffsetDateTime.now(),
                this.deletedAt
        );
    }

    /**
     * 升级套餐
     */
    public Tenant upgradePlan(String newPlanCode, OffsetDateTime newExpireAt) {
        return new Tenant(
                this.id,
                this.orgName,
                newPlanCode,
                newExpireAt,
                this.createdAt,
                OffsetDateTime.now(),
                this.deletedAt
        );
    }

    // =================== 业务查询方法 ===================

    /**
     * 判断租户是否已过期
     */
    public boolean isExpired() {
        return expireAt != null && expireAt.isBefore(OffsetDateTime.now());
    }

    /**
     * 判断租户是否即将过期（30天内）
     */
    public boolean isExpiringSoon() {
        return expireAt != null &&
                expireAt.isBefore(OffsetDateTime.now().plusDays(30)) &&
                expireAt.isAfter(OffsetDateTime.now());
    }

    /**
     * 获取剩余天数
     */
    public long getDaysUntilExpiry() {
        if (expireAt == null) return Long.MAX_VALUE;
        return OffsetDateTime.now().until(expireAt, java.time.temporal.ChronoUnit.DAYS);
    }

    /**
     * 判断是否为试用套餐
     */
    public boolean isTrialPlan() {
        return planCode != null && planCode.toLowerCase().contains("trial");
    }
}