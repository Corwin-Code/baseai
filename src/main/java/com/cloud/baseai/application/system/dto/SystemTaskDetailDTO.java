package com.cloud.baseai.application.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * <h2>系统任务详情DTO</h2>
 */
public record SystemTaskDetailDTO(
        @JsonProperty("id")
        Long id,

        @JsonProperty("tenantId")
        Long tenantId,

        @JsonProperty("taskType")
        String taskType,

        @JsonProperty("payload")
        String payload,

        @JsonProperty("status")
        String status,

        @JsonProperty("retryCount")
        Integer retryCount,

        @JsonProperty("lastError")
        String lastError,

        @JsonProperty("createdBy")
        Long createdBy,

        @JsonProperty("creatorName")
        String creatorName,

        @JsonProperty("createdAt")
        OffsetDateTime createdAt,

        @JsonProperty("executedAt")
        OffsetDateTime executedAt,

        @JsonProperty("finishedAt")
        OffsetDateTime finishedAt,

        @JsonProperty("durationMs")
        Long durationMs,

        @JsonProperty("canRetry")
        Boolean canRetry
) {
}