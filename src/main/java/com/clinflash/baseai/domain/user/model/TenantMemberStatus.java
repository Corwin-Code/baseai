package com.clinflash.baseai.domain.user.model;

import lombok.Getter;

/**
 * <h2>租户成员状态枚举</h2>
 *
 * <p>定义了用户在租户中可能的状态。这个枚举体现了成员生命周期的不同阶段。</p>
 */
@Getter
public enum TenantMemberStatus {
    PENDING("待激活"),      // 已邀请但未接受
    ACTIVE("正常"),         // 正常活跃状态
    SUSPENDED("已暂停"),    // 临时暂停
    DISABLED("已禁用");     // 已禁用

    private final String label;

    TenantMemberStatus(String label) {
        this.label = label;
    }

    /**
     * 从代码获取状态
     */
    public static TenantMemberStatus fromCode(int code) {
        return switch (code) {
            case 0 -> PENDING;
            case 1 -> ACTIVE;
            case 2 -> SUSPENDED;
            case 3 -> DISABLED;
            default -> throw new IllegalArgumentException("无效的状态代码: " + code);
        };
    }

    /**
     * 获取状态代码
     */
    public int getCode() {
        return switch (this) {
            case PENDING -> 0;
            case ACTIVE -> 1;
            case SUSPENDED -> 2;
            case DISABLED -> 3;
        };
    }
}