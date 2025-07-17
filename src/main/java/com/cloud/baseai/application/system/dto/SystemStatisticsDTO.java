package com.cloud.baseai.application.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * <h2>系统统计DTO</h2>
 */
public record SystemStatisticsDTO(
        @JsonProperty("totalSettings")
        int totalSettings,

        @JsonProperty("totalTasks")
        long totalTasks,

        @JsonProperty("pendingTasks")
        long pendingTasks,

        @JsonProperty("processingTasks")
        long processingTasks,

        @JsonProperty("successTasks")
        long successTasks,

        @JsonProperty("failedTasks")
        long failedTasks,

        @JsonProperty("taskSuccessRate")
        double taskSuccessRate,

        @JsonProperty("tasksByType")
        Map<String, Long> tasksByType,

        @JsonProperty("systemHealth")
        SystemHealthDTO systemHealth
) {
}