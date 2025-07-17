package com.cloud.baseai.domain.kb.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <h2>知识块标签关联仓储接口</h2>
 *
 * <p>管理知识块与标签的多对多关联关系，
 * 提供高效的标签分配和查询能力。</p>
 */
public interface ChunkTagRepository {

    /**
     * 为知识块添加标签
     *
     * @param chunkId 知识块ID
     * @param tagIds  标签ID集合
     */
    void addTags(Long chunkId, Set<Long> tagIds);

    /**
     * 为知识块移除标签
     *
     * @param chunkId 知识块ID
     * @param tagIds  要移除的标签ID集合
     */
    void removeTags(Long chunkId, Set<Long> tagIds);

    /**
     * 替换知识块的所有标签
     *
     * @param chunkId 知识块ID
     * @param tagIds  新的标签ID集合
     */
    void replaceTags(Long chunkId, Set<Long> tagIds);

    /**
     * 查询知识块的所有标签ID
     *
     * @param chunkId 知识块ID
     * @return 标签ID集合
     */
    Set<Long> findTagIdsByChunkId(Long chunkId);

    /**
     * 批量查询知识块的标签ID
     *
     * @param chunkIds 知识块ID列表
     * @return 知识块ID到标签ID集合的映射
     */
    Map<Long, Set<Long>> findTagIdsByChunkIds(List<Long> chunkIds);

    /**
     * 查询标签下的所有知识块ID
     *
     * @param tagId 标签ID
     * @return 知识块ID列表
     */
    List<Long> findChunkIdsByTagId(Long tagId);

    /**
     * 根据标签查询知识块
     *
     * <p>支持多标签AND/OR查询。</p>
     *
     * @param tagIds   标签ID集合
     * @param operator 操作符：AND表示必须包含所有标签，OR表示包含任一标签
     * @param limit    最大返回数量
     * @return 匹配的知识块ID列表
     */
    List<Long> findChunkIdsByTags(Set<Long> tagIds, String operator, int limit);

    /**
     * 统计标签的使用次数
     *
     * @param tagId 标签ID
     * @return 使用次数
     */
    long countChunksByTagId(Long tagId);

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
     * 检查知识块是否包含指定标签
     *
     * @param chunkId 知识块ID
     * @param tagId   标签ID
     * @return true如果包含该标签
     */
    boolean hasTag(Long chunkId, Long tagId);
}