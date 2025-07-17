package com.cloud.baseai.application.user.dto;

import java.time.OffsetDateTime;

/**
 * 用户搜索结果DTO
 *
 * <p>用户搜索功能的结果对象。
 * 包含搜索匹配的用户基本信息，用于用户选择器等场景。</p>
 */
public record UserSearchResultDTO(
        Long id,
        String username,
        String email,
        String avatarUrl,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt
) {
    /**
     * 获取用户状态描述
     */
    public String getStatusDescription() {
        if (lastLoginAt == null) {
            return "从未登录";
        }

        long daysAgo = lastLoginAt.until(OffsetDateTime.now(), java.time.temporal.ChronoUnit.DAYS);
        if (daysAgo == 0) {
            return "今日活跃";
        } else if (daysAgo <= 7) {
            return daysAgo + "天前活跃";
        } else if (daysAgo <= 30) {
            return "本月活跃";
        } else {
            return "不活跃";
        }
    }
}