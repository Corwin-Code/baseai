package com.cloud.baseai.domain.kb.model;

import java.time.OffsetDateTime;

/**
 * <h2>向量嵌入实体</h2>
 *
 * <p>知识块的向量表示，支持语义相似度搜索。
 * 同一个知识块可以有多个不同模型生成的向量。</p>
 *
 * <p><b>业务规则：</b></p>
 * <ul>
 * <li>复合主键：(chunkId, modelCode, vectorVersion)</li>
 * <li>向量维度必须与模型规格一致</li>
 * <li>支持向量版本管理</li>
 * </ul>
 *
 * @param chunkId       知识块ID
 * @param modelCode     生成向量的模型代码
 * @param vectorVersion 向量版本号
 * @param embedding     向量数据（1536维float数组）
 * @param createdBy     创建人ID
 * @param createdAt     创建时间
 * @param deletedAt     软删除时间
 */
public record Embedding(
        Long chunkId,
        String modelCode,
        Integer vectorVersion,
        float[] embedding,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {

    /**
     * 创建新向量嵌入的工厂方法
     *
     * @param chunkId       知识块ID
     * @param modelCode     模型代码
     * @param vectorVersion 向量版本
     * @param embedding     向量数据
     * @param createdBy     创建人ID
     * @return 新向量实例
     */
    public static Embedding create(Long chunkId, String modelCode, Integer vectorVersion,
                                   float[] embedding, Long createdBy) {
        return new Embedding(
                chunkId, modelCode, vectorVersion, embedding,
                createdBy, OffsetDateTime.now(), null
        );
    }

    /**
     * 检查向量维度是否正确
     *
     * @param expectedDimension 期望的维度
     * @return true如果维度匹配
     */
    public boolean hasCorrectDimension(int expectedDimension) {
        return this.embedding != null && this.embedding.length == expectedDimension;
    }

    /**
     * 计算向量的L2范数（模长）
     *
     * @return 向量的L2范数
     */
    public double getNorm() {
        if (this.embedding == null) {
            return 0.0;
        }

        double sum = 0.0;
        for (float value : this.embedding) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }

    /**
     * 检查是否已删除
     *
     * @return true如果已被软删除
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}