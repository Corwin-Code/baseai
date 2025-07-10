package com.clinflash.baseai.application.user.dto;

import java.util.List;
import java.util.Map;

/**
 * 用户统计DTO
 *
 * <p>用户相关的统计信息，用于管理后台的数据分析。
 * 包含用户数量、增长趋势、活跃度等关键指标。</p>
 */
public record UserStatisticsDTO(
        int totalUsers,                    // 总用户数
        int newUsersThisMonth,            // 本月新增用户
        int activeUsersThisMonth,         // 本月活跃用户
        double userGrowthRate,            // 用户增长率
        Map<String, Integer> usersByTenant,   // 按租户统计用户数
        Map<String, Integer> usersByRole,     // 按角色统计用户数
        List<DailyUserStats> dailyStats   // 每日用户统计
) {
    /**
     * 计算用户活跃率
     */
    public double getActiveUserRate() {
        return totalUsers > 0 ? (double) activeUsersThisMonth / totalUsers : 0.0;
    }

    /**
     * 判断用户增长趋势
     */
    public String getGrowthTrend() {
        if (userGrowthRate > 0.1) return "快速增长";
        if (userGrowthRate > 0.05) return "稳定增长";
        if (userGrowthRate > 0) return "缓慢增长";
        if (userGrowthRate == 0) return "无增长";
        return "负增长";
    }

    /**
     * 每日用户统计内部记录
     */
    public record DailyUserStats(
            String date,
            int newUsers,
            int activeUsers,
            int totalLogins
    ) {
    }
}