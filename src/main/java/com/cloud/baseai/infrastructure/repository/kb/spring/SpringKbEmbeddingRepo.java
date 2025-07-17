package com.cloud.baseai.infrastructure.repository.kb.spring;

import com.cloud.baseai.infrastructure.persistence.kb.entity.KbEmbeddingEntity;
import com.cloud.baseai.infrastructure.persistence.kb.entity.KbEmbeddingEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>向量嵌入Spring Data JPA仓储</h2>
 *
 * <p>这是RAG系统最核心的仓储接口，负责向量数据的存储和检索。
 * 它利用PostgreSQL的pgvector扩展来实现高效的向量相似度搜索。</p>
 *
 * <p><b>向量检索技术解释：</b></p>
 * <p>向量相似度搜索是现代AI检索系统的基础。通过将文本转换为高维向量（通常是1536维），
 * 我们可以用数学方法计算文本之间的语义相似度。PostgreSQL的pgvector扩展提供了专门的
 * 向量数据类型和索引（如HNSW），使得大规模向量检索成为可能。</p>
 */
@Repository
public interface SpringKbEmbeddingRepo extends JpaRepository<KbEmbeddingEntity, KbEmbeddingEntityId> {

    /**
     * 查找知识块的特定模型向量
     *
     * <p>由于同一个知识块可能有多个不同模型生成的向量，
     * 这个方法确保我们获取的是指定模型生成的向量。</p>
     *
     * @param chunkId   知识块ID
     * @param modelCode 模型代码
     * @return 匹配的向量嵌入
     */
    Optional<KbEmbeddingEntity> findByChunkIdAndModelCodeAndDeletedAtIsNull(Long chunkId, String modelCode);

    /**
     * 批量查询知识块的向量
     *
     * @param chunkIds  知识块ID列表
     * @param modelCode 模型代码
     * @return 向量嵌入列表
     */
    List<KbEmbeddingEntity> findByChunkIdInAndModelCodeAndDeletedAtIsNull(List<Long> chunkIds, String modelCode);

    /**
     * 向量相似度搜索
     *
     * <p>这是整个RAG系统的核心查询。它使用PostgreSQL的向量相似度计算功能来找到与查询向量最相似的知识块。
     * 查询使用余弦相似度（cosine similarity）作为距离度量，这是文本向量比较的标准方法。</p>
     *
     * <p><b>查询解释：</b></p>
     * <p>1. 使用余弦距离操作符(<=>)来计算向量相似度</p>
     * <p>2. 通过JOIN连接多个表来实现租户隔离</p>
     * <p>3. 按相似度排序，最相似的结果排在前面</p>
     * <p>4. 使用LIMIT来控制返回结果数量，提高查询性能</p>
     *
     * @param queryVector 查询向量（1536维浮点数组）
     * @param modelCode   模型代码，确保向量兼容性
     * @param tenantId    租户ID，实现数据隔离
     * @param limit       返回结果数量上限
     * @return 相似度搜索结果，包含知识块ID和相似度分数
     */
    @Query(value = """
            SELECT e.chunk_id as chunkId,
                   (1 - (e.embedding <=> CAST(:queryVector AS vector))) as score
            FROM kb_embeddings e
            JOIN kb_chunks c ON e.chunk_id = c.id
            JOIN kb_documents d ON c.document_id = d.id
            WHERE e.model_code = :modelCode
              AND d.tenant_id = :tenantId
              AND e.deleted_at IS NULL
              AND c.deleted_at IS NULL
              AND d.deleted_at IS NULL
            ORDER BY e.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<EmbeddingSearchResultProjection> searchSimilar(@Param("queryVector") String queryVector,
                                                        @Param("modelCode") String modelCode,
                                                        @Param("tenantId") Long tenantId,
                                                        @Param("limit") Integer limit);

    /**
     * 带阈值的向量相似度搜索
     *
     * <p>这个查询在基础相似度搜索的基础上增加了阈值过滤，
     * 只返回相似度超过指定阈值的结果，提高检索质量。</p>
     *
     * @param queryVector 查询向量
     * @param modelCode   模型代码
     * @param tenantId    租户ID
     * @param threshold   相似度阈值（0-1之间）
     * @param limit       返回结果数量上限
     * @return 过滤后的相似度搜索结果
     */
    @Query(value = """
            SELECT e.chunk_id as chunkId,
                   (1 - (e.embedding <=> CAST(:queryVector AS vector))) as score
            FROM kb_embeddings e
            JOIN kb_chunks c ON e.chunk_id = c.id
            JOIN kb_documents d ON c.document_id = d.id
            WHERE e.model_code = :modelCode
              AND d.tenant_id = :tenantId
              AND e.deleted_at IS NULL
              AND c.deleted_at IS NULL
              AND d.deleted_at IS NULL
              AND (1 - (e.embedding <=> CAST(:queryVector AS vector))) >= :threshold
            ORDER BY e.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<EmbeddingSearchResultProjection> searchSimilarWithThreshold(@Param("queryVector") String queryVector,
                                                                     @Param("modelCode") String modelCode,
                                                                     @Param("tenantId") Long tenantId,
                                                                     @Param("threshold") Float threshold,
                                                                     @Param("limit") Integer limit);

    /**
     * 删除知识块的所有向量
     *
     * @param chunkId 知识块ID
     * @return 删除的向量数量
     */
    int deleteByChunkId(Long chunkId);

    /**
     * 删除指定模型的所有向量
     *
     * <p>当模型升级或废弃时，需要清理相关的向量数据。</p>
     *
     * @param modelCode 模型代码
     * @return 删除的向量数量
     */
    int deleteByModelCode(String modelCode);

    /**
     * 统计指定模型的向量数量
     *
     * @param modelCode 模型代码
     * @return 向量数量
     */
    long countByModelCodeAndDeletedAtIsNull(String modelCode);

    /**
     * 向量搜索结果投影接口
     *
     * <p>Spring Data JPA使用这个接口来映射原生查询的结果。
     * 这种方式比返回Object[]数组更加类型安全。</p>
     */
    interface EmbeddingSearchResultProjection {
        Long getChunkId();

        Float getScore();
    }
}