package com.clinflash.baseai.infrastructure.repository.kb;

import com.clinflash.baseai.domain.kb.model.Embedding;
import com.clinflash.baseai.domain.kb.repository.EmbeddingRepository;
import com.clinflash.baseai.infrastructure.persistence.kb.entity.KbEmbeddingEntity;
import com.clinflash.baseai.infrastructure.persistence.kb.mapper.KbMapper;
import com.clinflash.baseai.infrastructure.repository.kb.spring.SpringKbEmbeddingRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <h2>向量嵌入仓储实现</h2>
 *
 * <p>这是RAG系统的核心仓储实现，负责向量数据的存储和检索。
 * 特别处理了向量数据的序列化和PostgreSQL向量类型的转换。</p>
 *
 * <p><b>性能考虑：</b></p>
 * <ul>
 * <li>向量搜索查询使用原生SQL以获得最佳性能</li>
 * <li>向量数据需要特殊的序列化处理</li>
 * <li>大量向量操作时考虑批处理优化</li>
 * </ul>
 */
@Repository
public class KbEmbeddingJpaRepository implements EmbeddingRepository {

    private static final Logger log = LoggerFactory.getLogger(KbEmbeddingJpaRepository.class);

    private final SpringKbEmbeddingRepo springRepo;
    private final KbMapper mapper;

    public KbEmbeddingJpaRepository(SpringKbEmbeddingRepo springRepo, KbMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public Embedding save(Embedding embedding) {
        KbEmbeddingEntity entity = KbEmbeddingEntity.fromDomain(embedding);
        KbEmbeddingEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Embedding> saveAll(List<Embedding> embeddings) {
        List<KbEmbeddingEntity> entities = mapper.toEmbeddingEntityList(embeddings);

        List<KbEmbeddingEntity> saved = springRepo.saveAll(entities);
        return mapper.toEmbeddingDomainList(saved);
    }

    @Override
    public Optional<Embedding> findByChunkIdAndModel(Long chunkId, String modelCode) {
        return springRepo.findByChunkIdAndModelCodeAndDeletedAtIsNull(chunkId, modelCode)
                .map(mapper::toDomain);
    }

    @Override
    public List<Embedding> findByChunkIdsAndModel(List<Long> chunkIds, String modelCode) {
        return springRepo.findByChunkIdInAndModelCodeAndDeletedAtIsNull(chunkIds, modelCode)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<EmbeddingSearchResult> searchSimilar(float[] queryVector, String modelCode,
                                                     Long tenantId, int limit) {
        try {
            // 将浮点数组转换为PostgreSQL向量格式
            String vectorString = formatVectorForPostgreSQL(queryVector);

            List<SpringKbEmbeddingRepo.EmbeddingSearchResultProjection> results =
                    springRepo.searchSimilar(vectorString, modelCode, tenantId, limit);

            return results.stream()
                    .map(result -> new EmbeddingSearchResult(result.getChunkId(), result.getScore()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("向量搜索失败: modelCode={}, tenantId={}, limit={}", modelCode, tenantId, limit, e);
            return List.of();
        }
    }

    @Override
    public List<EmbeddingSearchResult> searchSimilarWithThreshold(float[] queryVector, String modelCode,
                                                                  Long tenantId, int limit, float threshold) {
        try {
            String vectorString = formatVectorForPostgreSQL(queryVector);

            List<SpringKbEmbeddingRepo.EmbeddingSearchResultProjection> results =
                    springRepo.searchSimilarWithThreshold(vectorString, modelCode, tenantId, threshold, limit);

            return results.stream()
                    .map(result -> new EmbeddingSearchResult(result.getChunkId(), result.getScore()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("带阈值向量搜索失败: modelCode={}, tenantId={}, threshold={}, limit={}",
                    modelCode, tenantId, threshold, limit, e);
            return List.of();
        }
    }

    @Override
    public int deleteByChunkId(Long chunkId) {
        return springRepo.deleteByChunkId(chunkId);
    }

    @Override
    public int deleteByModelAndVersion(String modelCode, int vectorVersion) {
        // 由于复合主键，需要先查询再删除
        // 这里简化实现，实际可能需要更复杂的查询
        return springRepo.deleteByModelCode(modelCode);
    }

    @Override
    public long countByModel(String modelCode) {
        return springRepo.countByModelCodeAndDeletedAtIsNull(modelCode);
    }

    /**
     * 将float数组格式化为PostgreSQL向量字符串格式
     *
     * <p>PostgreSQL的vector类型要求特定的字符串格式，例如: '[0.1,0.2,0.3]'</p>
     *
     * @param vector 向量数组
     * @return PostgreSQL向量格式字符串
     */
    private String formatVectorForPostgreSQL(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException("向量不能为空");
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector[i]);
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * 验证向量维度
     *
     * @param vector            向量数组
     * @param expectedDimension 期望维度
     * @throws IllegalArgumentException 如果维度不匹配
     */
    private void validateVectorDimension(float[] vector, int expectedDimension) {
        if (vector == null) {
            throw new IllegalArgumentException("向量不能为null");
        }
        if (vector.length != expectedDimension) {
            throw new IllegalArgumentException(
                    String.format("向量维度不匹配，期望%d维，实际%d维", expectedDimension, vector.length));
        }
    }
}