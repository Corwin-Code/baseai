package com.clinflash.baseai.application.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * <h2>系统健康状态DTO</h2>
 */
public record SystemHealthDTO(
        @JsonProperty("status")
        String status,

        @JsonProperty("components")
        Map<String, ComponentHealth> components,

        @JsonProperty("timestamp")
        OffsetDateTime timestamp
) {
    public record ComponentHealth(
            @JsonProperty("status")
            String status,

            @JsonProperty("details")
            Map<String, Object> details
    ) {
    }
}