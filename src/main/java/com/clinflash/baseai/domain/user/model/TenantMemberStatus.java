package com.clinflash.baseai.domain.user.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <h2>租户成员状态枚举</h2>
 *
 * <p>定义了用户在租户中可能的状态。</p>
 */
@Getter
public enum TenantMemberStatus {
    /**
     * 已邀请但未接受
     */
    PENDING(0, "待激活"),
    /**
     * 正常活跃状态
     */
    ACTIVE(1, "正常"),
    /**
     * 临时暂停
     */
    SUSPENDED(2, "已暂停"),
    /**
     * 已禁用
     */
    DISABLED(3, "已禁用");

    private final Integer code;
    private final String description;

    TenantMemberStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 使用静态 Map 缓存 code -> Enum 的映射关系，避免每次调用都遍历
     */
    private static final Map<Integer, TenantMemberStatus> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(TenantMemberStatus::getCode, Function.identity()));

    /**
     * 根据状态代码获取枚举实例。
     *
     * @param code 状态代码
     * @return 对应的 TenantMemberStatus 实例
     * @throws IllegalArgumentException 如果代码无效
     */
    public static TenantMemberStatus fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("状态代码不能为 null");
        }
        TenantMemberStatus status = CODE_MAP.get(code);
        if (status == null) {
            throw new IllegalArgumentException("无效的状态代码: " + code);
        }
        return status;
    }

    /**
     * 根据枚举名称（忽略大小写）获取实例。
     *
     * @param name 枚举的名称 (e.g., "ACTIVE")
     * @return 对应的 TenantMemberStatus 实例, 如果找不到则返回 null
     */
    public static TenantMemberStatus valueOfIgnoreCase(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}