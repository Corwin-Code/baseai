package com.cloud.baseai.application.chat.dto;

import java.util.List;

/**
 * 使用统计DTO
 */
public record UsageStatisticsDTO(
        Long tenantId,
        String period,
        List<DailyUsageDTO> dailyUsage,
        UsageDetail totalUsage,
        List<ModelUsageBreakdown> modelBreakdown
) {
    public record DailyUsageDTO(
            String date,
            long promptTokens,
            long completionTokens,
            double cost
    ) {
    }

    public record UsageDetail(
            long promptTokens,
            long completionTokens,
            double cost
    ) {
    }

    public record ModelUsageBreakdown(
            String modelCode,
            long promptTokens,
            long completionTokens,
            double cost,
            int messageCount
    ) {
    }
}