package com.cloud.baseai.application.chat.dto;

/**
 * <h2>AI模型使用统计数据</h2>
 *
 * <p>记录了每个模型在实际应用中的表现和受欢迎程度。</p>
 *
 * @param modelCode   模型的唯一标识码
 * @param usageCount  模型的调用次数
 * @param totalTokens 模型处理的Token总数
 */
public record ModelUsageDTO(
        String modelCode,
        long usageCount,
        long totalTokens
) {
}