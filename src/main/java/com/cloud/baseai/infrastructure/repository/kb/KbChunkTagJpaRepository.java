package com.cloud.baseai.infrastructure.repository.kb;

import com.cloud.baseai.domain.kb.repository.ChunkTagRepository;
import com.cloud.baseai.infrastructure.persistence.kb.entity.KbChunkTagEntity;
import com.cloud.baseai.infrastructure.repository.kb.spring.SpringKbChunkTagRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * <h2>知识块标签关联仓储实现</h2>
 *
 * <p>管理知识块与标签的多对多关联关系，提供高效的批量操作和复杂查询支持。</p>
 */
@Repository
public class KbChunkTagJpaRepository implements ChunkTagRepository {

    private final SpringKbChunkTagRepo springRepo;

    public KbChunkTagJpaRepository(SpringKbChunkTagRepo springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public void addTags(Long chunkId, Set<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return;
        }

        // 使用批量插入提高性能
        Long[] tagIdArray = tagIds.toArray(new Long[0]);
        springRepo.batchAddTags(chunkId, tagIdArray);
    }

    @Override
    public void removeTags(Long chunkId, Set<Long> tagIds) {
        if (tagIds.isEmpty()) {
            return;
        }

        springRepo.batchRemoveTags(chunkId, tagIds);
    }

    @Override
    public void replaceTags(Long chunkId, Set<Long> tagIds) {
        // 先删除所有现有标签
        springRepo.deleteByChunkId(chunkId);

        // 再添加新标签
        if (!tagIds.isEmpty()) {
            addTags(chunkId, tagIds);
        }
    }

    @Override
    public Set<Long> findTagIdsByChunkId(Long chunkId) {
        List<Long> tagIds = springRepo.findTagIdsByChunkId(chunkId);
        return new HashSet<>(tagIds);
    }

    @Override
    public Map<Long, Set<Long>> findTagIdsByChunkIds(List<Long> chunkIds) {
        if (chunkIds.isEmpty()) {
            return new HashMap<>();
        }

        List<KbChunkTagEntity> associations = springRepo.findByChunkIdIn(chunkIds);

        Map<Long, Set<Long>> result = new HashMap<>();
        for (KbChunkTagEntity association : associations) {
            result.computeIfAbsent(association.getChunkId(), k -> new HashSet<>())
                    .add(association.getTagId());
        }

        return result;
    }

    @Override
    public List<Long> findChunkIdsByTagId(Long tagId) {
        return springRepo.findChunkIdsByTagId(tagId);
    }

    @Override
    public List<Long> findChunkIdsByTags(Set<Long> tagIds, String operator, int limit) {
        if (tagIds.isEmpty()) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, limit);

        if ("AND".equalsIgnoreCase(operator)) {
            // 必须包含所有标签
            return springRepo.findChunkIdsByAllTags(tagIds, tagIds.size(), pageable);
        } else {
            // 包含任一标签
            return springRepo.findChunkIdsByAnyTag(tagIds, pageable);
        }
    }

    @Override
    public long countChunksByTagId(Long tagId) {
        return springRepo.countByTagId(tagId);
    }

    @Override
    public int deleteByChunkId(Long chunkId) {
        return springRepo.deleteByChunkId(chunkId);
    }

    @Override
    public int deleteByTagId(Long tagId) {
        return springRepo.deleteByTagId(tagId);
    }

    @Override
    public boolean hasTag(Long chunkId, Long tagId) {
        return springRepo.existsByChunkIdAndTagId(chunkId, tagId);
    }
}