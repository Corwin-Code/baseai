package com.cloud.baseai.application.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * <h2>审计统计信息数据传输对象</h2>
 *
 * <p>这个DTO承载着审计数据的统计分析结果，就像一份详细的数据分析报告。
 * 它不仅提供原始的统计数字，还包含经过分析处理的洞察信息，
 * 帮助管理者快速理解系统的运行状况和潜在问题。</p>
 *
 * <p><b>统计维度说明：</b></p>
 * <p>我们从多个维度分析审计数据：操作维度展示各种操作的频率分布，
 * 用户维度展示用户的活跃程度，安全维度展示安全事件的分布情况，
 * 洞察维度则提供经过算法分析的深度见解。</p>
 *
 * @param operationCounts     操作类型统计 key=操作类型, value=次数
 * @param userActivityCounts  用户活动统计 key=用户标识, value=活动次数
 * @param securityEventCounts 安全事件统计 key=事件类型, value=发生次数
 * @param additionalMetrics   附加洞察信息，包含趋势分析、异常检测等
 */
@Schema(description = "审计统计信息")
public record AuditStatisticsDTO(
        @Schema(description = "操作类型统计", example = "{\"USER_LOGIN\": 1250, \"DATA_UPDATE\": 890}")
        Map<String, Long> operationCounts,

        @Schema(description = "用户活动统计", example = "{\"user_123\": 45, \"user_456\": 32}")
        Map<String, Long> userActivityCounts,

        @Schema(description = "安全事件统计", example = "{\"LOGIN_FAILED\": 15, \"PERMISSION_DENIED\": 8}")
        Map<String, Long> securityEventCounts,

        @Schema(description = "附加洞察信息")
        Map<String, Object> additionalMetrics
) {

    /**
     * 获取总操作次数
     */
    public long getTotalOperations() {
        return operationCounts.values().stream().mapToLong(Long::longValue).sum();
    }

    /**
     * 获取活跃用户数量
     */
    public long getActiveUserCount() {
        return userActivityCounts.size();
    }

    /**
     * 获取安全事件总数
     */
    public long getTotalSecurityEvents() {
        return securityEventCounts.values().stream().mapToLong(Long::longValue).sum();
    }

    /**
     * 获取最常见的操作类型
     */
    public String getMostFrequentOperation() {
        return operationCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("无");
    }

    /**
     * 计算安全事件占比
     */
    public double getSecurityEventRatio() {
        long totalOps = getTotalOperations();
        if (totalOps == 0) return 0.0;

        return (double) getTotalSecurityEvents() / totalOps;
    }

    /**
     * 判断是否存在安全风险
     *
     * <p>基于简单的规则判断是否存在潜在的安全风险。
     * 实际项目中，这里应该有更复杂的风险评估算法。</p>
     */
    public boolean hasSecurityRisk() {
        // 如果安全事件占比超过5%，认为存在风险
        return getSecurityEventRatio() > 0.05;
    }

    /**
     * 获取系统健康度评分
     *
     * <p>基于各种指标计算一个综合的系统健康度评分（0-100分）。</p>
     */
    public int getSystemHealthScore() {
        int baseScore = 100;

        // 根据安全事件占比扣分
        double securityRatio = getSecurityEventRatio();
        if (securityRatio > 0.1) {
            baseScore -= 30; // 严重安全问题
        } else if (securityRatio > 0.05) {
            baseScore -= 15; // 中等安全问题
        } else if (securityRatio > 0.01) {
            baseScore -= 5;  // 轻微安全问题
        }

        // 可以根据其他指标继续调整分数

        return Math.max(0, baseScore);
    }
}