package com.cloud.baseai.domain.kb.repository;

import com.cloud.baseai.domain.kb.model.Chunk;

import java.util.List;
import java.util.Optional;

/**
 * <h2>知识块仓储接口</h2>
 *
 * <p>知识块是文档分析后的基本检索单元，
 * 仓储接口提供高效的查询和管理能力。</p>
 */
public interface ChunkRepository {

    /**
     * 保存知识块
     *
     * @param chunk 知识块实体
     * @return 保存后的知识块实体
     */
    Chunk save(Chunk chunk);

    /**
     * 批量保存知识块
     *
     * <p>文档解析时需要批量创建知识块，提供批量接口提高性能。</p>
     *
     * @param chunks 知识块列表
     * @return 保存后的知识块列表
     */
    List<Chunk> saveAll(List<Chunk> chunks);

    /**
     * 根据ID查找知识块
     *
     * @param id 知识块ID
     * @return 知识块实体
     */
    Optional<Chunk> findById(Long id);

    /**
     * 批量查询知识块
     *
     * @param ids 知识块ID列表
     * @return 匹配的知识块列表
     */
    List<Chunk> findByIds(List<Long> ids);

    /**
     * 查询文档下的所有知识块
     *
     * @param documentId 文档ID
     * @return 知识块列表，按chunkNo排序
     */
    List<Chunk> findByDocumentId(Long documentId);

    /**
     * 分页查询文档下的知识块
     *
     * @param documentId 文档ID
     * @param page       页码
     * @param size       每页大小
     * @return 知识块列表
     */
    List<Chunk> findByDocumentId(Long documentId, int page, int size);

    /**
     * 查询指定文档的知识块数量
     *
     * @param documentId 文档ID
     * @return 知识块数量
     */
    long countByDocumentId(Long documentId);

    /**
     * 根据向量版本查询需要重新生成向量的知识块
     *
     * @param modelCode     模型代码
     * @param vectorVersion 目标向量版本
     * @param limit         最大返回数量
     * @return 需要更新向量的知识块列表
     */
    List<Chunk> findChunksNeedingVectorUpdate(String modelCode, int vectorVersion, int limit);

    /**
     * 文本搜索知识块
     *
     * <p>基于PostgreSQL的全文检索功能，使用gin_trgm_ops索引。</p>
     *
     * @param tenantId 租户ID（通过文档关联）
     * @param query    搜索关键词
     * @param limit    最大返回数量
     * @return 匹配的知识块列表
     */
    List<Chunk> searchByText(Long tenantId, String query, int limit);

    /**
     * 根据Token数量范围查询知识块
     *
     * @param minTokens 最小Token数量
     * @param maxTokens 最大Token数量
     * @return 匹配的知识块列表
     */
    List<Chunk> findByTokenRange(int minTokens, int maxTokens);

    /**
     * 删除文档下的所有知识块
     *
     * <p>当文档被删除时，级联删除其知识块。</p>
     *
     * @param documentId 文档ID
     * @return 删除的知识块数量
     */
    int deleteByDocumentId(Long documentId);
}