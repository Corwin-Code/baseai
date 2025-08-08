package com.cloud.baseai.infrastructure.external.llm.model;

/**
 * <h2>向量嵌入结果</h2>
 *
 * <p>封装向量化操作的完整结果信息，包括生成的向量和相关元数据。</p>
 */
public record EmbeddingResult(
        float[] embedding,        // 生成的向量
        String modelCode,         // 使用的模型
        Integer tokenCount,       // 消耗的token数量
        Integer latencyMs,        // 处理延迟(毫秒)
        Double cost,             // 费用(美元)
        String provider          // 服务提供商
) {
    /**
     * 创建成功的嵌入结果
     */
    public static EmbeddingResult success(float[] embedding, String modelCode, Integer tokenCount,
                                          Integer latencyMs, Double cost, String provider) {
        return new EmbeddingResult(embedding, modelCode, tokenCount, latencyMs, cost, provider);
    }

    /**
     * 创建简单的嵌入结果
     */
    public static EmbeddingResult simple(float[] embedding, String modelCode) {
        return new EmbeddingResult(embedding, modelCode, null, null, null, null);
    }

    /**
     * 获取向量维度
     */
    public int getDimension() {
        return embedding != null ? embedding.length : 0;
    }

    /**
     * 检查结果是否有效
     */
    public boolean isValid() {
        return embedding != null && embedding.length > 0;
    }
}