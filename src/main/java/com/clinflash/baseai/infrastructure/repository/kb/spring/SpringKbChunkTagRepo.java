package com.clinflash.baseai.infrastructure.repository.kb.spring;

import com.clinflash.baseai.infrastructure.persistence.kb.entity.KbChunkTagEntity;
import com.clinflash.baseai.infrastructure.persistence.kb.entity.KbChunkTagEntityId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * <h2>知识块标签关联Spring Data JPA仓储</h2>
 *
 * <p>这个仓储管理知识块与标签之间的多对多关联关系。
 * 它提供了灵活的标签分配和查询功能，支持复杂的标签组合查询。</p>
 */
@Repository
public interface SpringKbChunkTagRepo extends JpaRepository<KbChunkTagEntity, KbChunkTagEntityId> {

    /**
     * 查询知识块的所有标签ID
     *
     * @param chunkId 知识块ID
     * @return 标签ID列表
     */
    @Query("SELECT ct.tagId FROM KbChunkTagEntity ct WHERE ct.chunkId = :chunkId")
    List<Long> findTagIdsByChunkId(@Param("chunkId") Long chunkId);

    /**
     * 批量查询知识块的标签ID
     *
     * <p>这个查询用于一次性获取多个知识块的标签信息，提高查询效率。</p>
     *
     * @param chunkIds 知识块ID列表
     * @return 知识块标签关联列表
     */
    @Query("SELECT ct FROM KbChunkTagEntity ct WHERE ct.chunkId IN :chunkIds")
    List<KbChunkTagEntity> findByChunkIdIn(@Param("chunkIds") List<Long> chunkIds);

    /**
     * 查询标签下的所有知识块ID
     *
     * @param tagId 标签ID
     * @return 知识块ID列表
     */
    @Query("SELECT ct.chunkId FROM KbChunkTagEntity ct WHERE ct.tagId = :tagId")
    List<Long> findChunkIdsByTagId(@Param("tagId") Long tagId);

    /**
     * 根据标签AND查询知识块
     *
     * <p>返回包含所有指定标签的知识块。这种查询用于精确匹配，
     * 要求知识块必须同时包含所有指定的标签。</p>
     *
     * @param tagIds   标签ID集合
     * @param tagCount 标签数量（用于确保知识块包含所有标签）
     *                 // @param limit  最大返回数量
     * @return 符合条件的知识块ID列表
     */
    @Query("""
            SELECT ct.chunkId
            FROM KbChunkTagEntity ct
            WHERE ct.tagId IN :tagIds
            GROUP BY ct.chunkId
            HAVING COUNT(DISTINCT ct.tagId) = :tagCount
            ORDER BY ct.chunkId
            """)
    List<Long> findChunkIdsByAllTags(@Param("tagIds") Set<Long> tagIds,
                                     @Param("tagCount") Integer tagCount,
                                     Pageable pageable);

    /**
     * 根据标签OR查询知识块
     *
     * <p>返回包含任一指定标签的知识块。这种查询用于扩大搜索范围，
     * 只要知识块包含任何一个指定标签就会被返回。</p>
     *
     * @param tagIds 标签ID集合
     *               // @param limit  最大返回数量
     * @return 符合条件的知识块ID列表
     */
    @Query("""
            SELECT DISTINCT ct.chunkId
            FROM KbChunkTagEntity ct
            WHERE ct.tagId IN :tagIds
            ORDER BY ct.chunkId
            """)
    List<Long> findChunkIdsByAnyTag(@Param("tagIds") Set<Long> tagIds, Pageable pageable);

    /**
     * 统计标签的使用次数
     *
     * @param tagId 标签ID
     * @return 使用次数
     */
    long countByTagId(Long tagId);

    /**
     * 删除知识块的所有标签关联
     *
     * @param chunkId 知识块ID
     * @return 删除的关联数量
     */
    int deleteByChunkId(Long chunkId);

    /**
     * 删除标签的所有关联
     *
     * @param tagId 标签ID
     * @return 删除的关联数量
     */
    int deleteByTagId(Long tagId);

    /**
     * 批量添加标签关联
     *
     * <p>使用原生SQL来实现高效的批量插入操作。
     * 这比逐个插入关联记录的效率高得多。</p>
     *
     * @param chunkId 知识块ID
     * @param tagIds  标签ID集合
     */
    @Modifying
    @Query(value = """
            INSERT INTO kb_chunk_tags (chunk_id, tag_id)
            SELECT :chunkId, t.tag_id
            FROM (SELECT unnest(ARRAY[:tagIds]) as tag_id) t
            ON CONFLICT (chunk_id, tag_id) DO NOTHING
            """, nativeQuery = true)
    void batchAddTags(@Param("chunkId") Long chunkId, @Param("tagIds") Long[] tagIds);

    /**
     * 批量删除标签关联
     *
     * @param chunkId 知识块ID
     * @param tagIds  要删除的标签ID集合
     */
    @Modifying
    @Query("DELETE FROM KbChunkTagEntity ct WHERE ct.chunkId = :chunkId AND ct.tagId IN :tagIds")
    void batchRemoveTags(@Param("chunkId") Long chunkId, @Param("tagIds") Set<Long> tagIds);

    /**
     * 检查知识块是否包含指定标签
     *
     * @param chunkId 知识块ID
     * @param tagId   标签ID
     * @return true如果包含该标签
     */
    boolean existsByChunkIdAndTagId(Long chunkId, Long tagId);
}