package com.cloud.baseai.application.flow.dto;

import java.util.Map;

public record FlowHealthStatus(
        String status,
        Map<String, String> components,
        long responseTimeMs
) {
}