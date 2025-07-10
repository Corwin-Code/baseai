package com.clinflash.baseai.domain.kb.repository;

import com.clinflash.baseai.domain.kb.model.Embedding;

import java.util.List;
import java.util.Optional;

/**
 * <h2>向量嵌入仓储接口</h2>
 *
 * <p>负责向量数据的存储和检索，是RAG系统的核心组件。
 * 提供高效的向量相似度搜索能力。</p>
 */
public interface EmbeddingRepository {

    /**
     * 保存向量嵌入
     *
     * @param embedding 向量嵌入实体
     * @return 保存后的向量嵌入实体
     */
    Embedding save(Embedding embedding);

    /**
     * 批量保存向量嵌入
     *
     * <p>批量生成向量时使用，提高写入性能。</p>
     *
     * @param embeddings 向量嵌入列表
     * @return 保存后的向量嵌入列表
     */
    List<Embedding> saveAll(List<Embedding> embeddings);

    /**
     * 查找知识块的特定模型向量
     *
     * @param chunkId   知识块ID
     * @param modelCode 模型代码
     * @return 向量嵌入实体
     */
    Optional<Embedding> findByChunkIdAndModel(Long chunkId, String modelCode);

    /**
     * 批量查询知识块的向量
     *
     * @param chunkIds  知识块ID列表
     * @param modelCode 模型代码
     * @return 向量嵌入列表
     */
    List<Embedding> findByChunkIdsAndModel(List<Long> chunkIds, String modelCode);

    /**
     * 向量相似度搜索
     *
     * <p>这是RAG系统的核心功能，基于PostgreSQL的pgvector扩展。
     * 使用HNSW索引实现高效的近似最近邻搜索。</p>
     *
     * @param queryVector 查询向量（1536维浮点数组）
     * @param modelCode   模型代码，确保向量兼容性
     * @param tenantId    租户ID，实现数据隔离
     * @param limit       返回结果数量上限
     * @return 相似度搜索结果列表，按相似度倒序
     */
    List<EmbeddingSearchResult> searchSimilar(float[] queryVector, String modelCode,
                                              Long tenantId, int limit);

    /**
     * 带阈值的向量相似度搜索
     *
     * @param queryVector 查询向量
     * @param modelCode   模型代码
     * @param tenantId    租户ID
     * @param limit       返回结果数量上限
     * @param threshold   相似度阈值（0-1）
     * @return 相似度搜索结果列表
     */
    List<EmbeddingSearchResult> searchSimilarWithThreshold(float[] queryVector, String modelCode,
                                                           Long tenantId, int limit, float threshold);

    /**
     * 删除知识块的所有向量
     *
     * @param chunkId 知识块ID
     * @return 删除的向量数量
     */
    int deleteByChunkId(Long chunkId);

    /**
     * 删除指定模型和版本的向量
     *
     * <p>用于模型升级时清理旧版本向量。</p>
     *
     * @param modelCode     模型代码
     * @param vectorVersion 向量版本
     * @return 删除的向量数量
     */
    int deleteByModelAndVersion(String modelCode, int vectorVersion);

    /**
     * 统计指定模型的向量数量
     *
     * @param modelCode 模型代码
     * @return 向量数量
     */
    long countByModel(String modelCode);

    /**
     * 向量搜索结果
     *
     * <p>包含知识块ID和相似度分数的搜索结果记录。</p>
     *
     * @param chunkId 知识块ID
     * @param score   余弦相似度分数（0-1，越接近1越相似）
     */
    record EmbeddingSearchResult(Long chunkId, Float score) {
    }
}