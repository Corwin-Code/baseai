package com.cloud.baseai.infrastructure.repository.kb.spring;

import com.cloud.baseai.infrastructure.persistence.kb.entity.KbChunkEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>知识块Spring Data JPA仓储</h2>
 *
 * <p>知识块是RAG系统的核心数据单元，这个仓储接口提供了丰富的查询功能来支持各种检索需求。
 * 特别值得注意的是文本检索功能，它利用了PostgreSQL的全文检索能力。</p>
 */
@Repository
public interface SpringKbChunkRepo extends JpaRepository<KbChunkEntity, Long> {

    /**
     * 查询文档下的所有知识块
     *
     * <p>当用户想要查看某个文档的完整内容分块时，会使用这个查询。
     * 结果按chunkNo排序，确保知识块按原文档的顺序返回。</p>
     *
     * @param documentId 文档ID
     * @return 知识块列表，按块序号排序
     */
    List<KbChunkEntity> findByDocumentIdAndDeletedAtIsNullOrderByChunkNo(Long documentId);

    /**
     * 分页查询文档下的知识块
     *
     * @param documentId 文档ID
     * @param pageable   分页参数
     * @return 分页的知识块结果
     */
    Page<KbChunkEntity> findByDocumentIdAndDeletedAtIsNull(Long documentId, Pageable pageable);

    /**
     * 统计文档下的知识块数量
     *
     * @param documentId 文档ID
     * @return 知识块数量
     */
    long countByDocumentIdAndDeletedAtIsNull(Long documentId);

    /**
     * 文本模糊检索知识块
     *
     * <p>这个查询使用PostgreSQL的三元组（trigram）索引来实现高效的模糊文本搜索。
     * 它可以找到包含指定关键词的知识块，支持拼写错误和部分匹配。</p>
     *
     * <p><b>技术说明：</b></p>
     * <p>ILIKE操作符用于不区分大小写的模糊匹配，结合gin_trgm_ops索引可以提供快速的全文检索能力。
     * 这种方法比简单的LIKE查询性能更好，特别是在处理大量文本数据时。</p>
     *
     * @param searchText 搜索文本
     * @param pageable   分页参数
     * @return 匹配的知识块列表
     */
    @Query("SELECT c FROM KbChunkEntity c JOIN KbDocumentEntity d ON c.documentId = d.id " +
            "WHERE d.tenantId = :tenantId AND c.text ILIKE %:searchText% " +
            "AND c.deletedAt IS NULL AND d.deletedAt IS NULL " +
            "ORDER BY c.createdAt DESC")
    Page<KbChunkEntity> searchByText(@Param("tenantId") Long tenantId,
                                     @Param("searchText") String searchText,
                                     Pageable pageable);

    /**
     * 根据Token数量范围查询知识块
     *
     * <p>这个查询用于分析和管理不同大小的知识块。
     * 例如，可以找出Token数量过大或过小的知识块，以便优化分块策略。</p>
     *
     * @param minTokens 最小Token数量
     * @param maxTokens 最大Token数量
     * @return 符合Token数量范围的知识块列表
     */
    List<KbChunkEntity> findByTokenSizeBetweenAndDeletedAtIsNull(Integer minTokens, Integer maxTokens);

    /**
     * 查询需要生成向量的知识块
     *
     * <p>这个查询用于找出还没有生成向量或向量版本过旧的知识块。
     * 批处理任务可以使用这个查询来识别需要重新生成向量的知识块。</p>
     *
     * @param currentVectorVersion 当前向量版本
     * @param pageable             分页参数
     * @return 需要更新向量的知识块列表
     */
    @Query("SELECT c FROM KbChunkEntity c WHERE c.vectorVersion < :currentVectorVersion " +
            "AND c.deletedAt IS NULL ORDER BY c.updatedAt ASC")
    Page<KbChunkEntity> findChunksNeedingVectorUpdate(@Param("currentVectorVersion") Integer currentVectorVersion,
                                                      Pageable pageable);

    /**
     * 根据语言代码查询知识块
     *
     * <p>这个查询支持多语言内容的管理，可以按语言分类查看知识块。</p>
     *
     * @param langCode 语言代码
     * @param pageable 分页参数
     * @return 指定语言的知识块列表
     */
    Page<KbChunkEntity> findByLangCodeAndDeletedAtIsNull(String langCode, Pageable pageable);

    /**
     * 批量查询知识块
     *
     * <p>用于向量检索后批量获取知识块详细信息。
     * 这是RAG流程中的关键查询，需要高效地返回检索到的知识块数据。</p>
     *
     * @param chunkIds 知识块ID列表
     * @return 匹配的知识块列表
     */
    @Query("SELECT c FROM KbChunkEntity c WHERE c.id IN :chunkIds AND c.deletedAt IS NULL")
    List<KbChunkEntity> findByIdsAndNotDeleted(@Param("chunkIds") List<Long> chunkIds);
}