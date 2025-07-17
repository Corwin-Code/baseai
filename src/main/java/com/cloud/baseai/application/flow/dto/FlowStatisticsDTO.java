package com.cloud.baseai.application.flow.dto;

public record FlowStatisticsDTO(
        Long tenantId,
        Long projectId,
        int totalProjects,
        int totalFlows,
        int publishedFlows,
        int totalRuns,
        int successfulRuns,
        int failedRuns,
        double successRate,
        String timeRange
) {
}