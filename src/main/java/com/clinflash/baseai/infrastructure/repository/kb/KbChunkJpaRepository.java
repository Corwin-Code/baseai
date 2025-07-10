package com.clinflash.baseai.infrastructure.repository.kb;

import com.clinflash.baseai.domain.kb.model.Chunk;
import com.clinflash.baseai.domain.kb.repository.ChunkRepository;
import com.clinflash.baseai.infrastructure.persistence.kb.entity.KbChunkEntity;
import com.clinflash.baseai.infrastructure.persistence.kb.mapper.KbMapper;
import com.clinflash.baseai.infrastructure.repository.kb.spring.SpringKbChunkRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <h2>知识块仓储实现</h2>
 *
 * <p>知识块仓储的JPA实现，处理知识块的CRUD操作和复杂查询。
 * 特别注意批量操作的性能优化，因为知识块数量通常很大。</p>
 */
@Repository
public class KbChunkJpaRepository implements ChunkRepository {

    private final SpringKbChunkRepo springRepo;
    private final KbMapper mapper;

    public KbChunkJpaRepository(SpringKbChunkRepo springRepo, KbMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public Chunk save(Chunk chunk) {
        KbChunkEntity entity;

        if (chunk.id() == null) {
            entity = mapper.toEntity(chunk);
        } else {
            entity = springRepo.findById(chunk.id())
                    .orElse(mapper.toEntity(chunk));
            entity.updateFromDomain(chunk);
        }

        KbChunkEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Chunk> saveAll(List<Chunk> chunks) {
        List<KbChunkEntity> entities = mapper.toChunkEntityList(chunks);

        List<KbChunkEntity> saved = springRepo.saveAll(entities);
        return mapper.toChunkDomainList(saved);
    }

    @Override
    public Optional<Chunk> findById(Long id) {
        return springRepo.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public List<Chunk> findByIds(List<Long> ids) {
        return springRepo.findByIdsAndNotDeleted(ids)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Chunk> findByDocumentId(Long documentId) {
        return springRepo.findByDocumentIdAndDeletedAtIsNullOrderByChunkNo(documentId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Chunk> findByDocumentId(Long documentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("chunkNo"));
        return springRepo.findByDocumentIdAndDeletedAtIsNull(documentId, pageable)
                .getContent()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByDocumentId(Long documentId) {
        return springRepo.countByDocumentIdAndDeletedAtIsNull(documentId);
    }

    @Override
    public List<Chunk> findChunksNeedingVectorUpdate(String modelCode, int vectorVersion, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return springRepo.findChunksNeedingVectorUpdate(vectorVersion, pageable)
                .getContent()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Chunk> searchByText(Long tenantId, String query, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return springRepo.searchByText(tenantId, query, pageable)
                .getContent()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Chunk> findByTokenRange(int minTokens, int maxTokens) {
        return springRepo.findByTokenSizeBetweenAndDeletedAtIsNull(minTokens, maxTokens)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public int deleteByDocumentId(Long documentId) {
        // 这里实现软删除
        List<KbChunkEntity> chunks = springRepo.findByDocumentIdAndDeletedAtIsNullOrderByChunkNo(documentId);
        chunks.forEach(chunk -> chunk.setDeletedAt(java.time.OffsetDateTime.now()));
        springRepo.saveAll(chunks);
        return chunks.size();
    }
}