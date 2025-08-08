package com.cloud.baseai.infrastructure.external.llm.model;

/**
 * <h2>嵌入模型信息</h2>
 *
 * <p>封装嵌入模型的详细信息，用于模型选择和成本估算。</p>
 */
public record ModelInfo(
        String name,              // 模型名称
        String provider,          // 服务提供商
        int dimension,           // 向量维度
        int maxTokens,           // 最大token数
        double costPerToken,     // 每token成本
        String description,      // 模型描述
        boolean available        // 是否可用
) {
    /**
     * 创建模型信息
     */
    public static ModelInfo create(String name, String provider, int dimension,
                                   int maxTokens, double costPerToken, String description) {
        return new ModelInfo(name, provider, dimension, maxTokens, costPerToken, description, true);
    }

    /**
     * 估算成本
     */
    public double estimateCost(int tokenCount) {
        return tokenCount * costPerToken;
    }
}