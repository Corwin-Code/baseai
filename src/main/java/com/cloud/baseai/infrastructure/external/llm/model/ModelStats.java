package com.cloud.baseai.infrastructure.external.llm.model;

import lombok.Data;

/**
 * 模型统计信息内部类
 */
@Data
public class ModelStats {
    private long requestCount = 0;
    private long totalLatencyMs = 0;
    private long lastUsedTime = System.currentTimeMillis();
}