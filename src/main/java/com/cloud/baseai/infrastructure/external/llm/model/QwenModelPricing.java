package com.cloud.baseai.infrastructure.external.llm.model;

/**
 * 模型信息记录类
 *
 * <p>封装模型的完整信息，用于API文档和监控展示。</p>
 */
public record QwenModelPricing(
        String name,              // 模型名称
        double inputPrice,        // 输入价格(每千token)
        double outputPrice,       // 输出价格(每千token)
        int maxTokens,           // 最大token数
        String description       // 模型描述
) {
}