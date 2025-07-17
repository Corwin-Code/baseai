package com.cloud.baseai.infrastructure.repository.kb;

import com.cloud.baseai.domain.kb.model.Document;
import com.cloud.baseai.domain.kb.model.ParsingStatus;
import com.cloud.baseai.domain.kb.repository.DocumentRepository;
import com.cloud.baseai.infrastructure.persistence.kb.entity.KbDocumentEntity;
import com.cloud.baseai.infrastructure.persistence.kb.mapper.KbMapper;
import com.cloud.baseai.infrastructure.repository.kb.spring.SpringKbDocumentRepo;
import com.cloud.baseai.infrastructure.utils.KbConstants;
import com.cloud.baseai.infrastructure.utils.KbUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <h2>文档仓储实现</h2>
 *
 * <p>这是领域仓储接口的具体实现，负责将领域层的抽象操作转换为具体的数据访问操作。
 * 它作为领域层和基础设施层之间的适配器，确保领域层不依赖于具体的持久化技术。</p>
 *
 * <p><b>设计模式：</b></p>
 * <ul>
 * <li><b>仓储模式（Repository Pattern）：</b>封装数据访问逻辑，为领域层提供类似集合的接口</li>
 * <li><b>适配器模式（Adapter Pattern）：</b>将Spring Data JPA的接口适配为领域仓储接口</li>
 * <li><b>映射器模式（Mapper Pattern）：</b>在领域对象和JPA实体之间进行转换</li>
 * </ul>
 */
@Repository
public class KbDocumentJpaRepository implements DocumentRepository {

    private final SpringKbDocumentRepo springRepo;
    private final KbMapper mapper;

    public KbDocumentJpaRepository(SpringKbDocumentRepo springRepo, KbMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public Document save(Document document) {
        KbDocumentEntity entity;

        if (document.id() == null) {
            // 新建文档
            entity = mapper.toEntity(document);
        } else {
            // 更新现有文档
            entity = springRepo.findById(document.id())
                    .orElse(mapper.toEntity(document));
            entity.updateFromDomain(document);
        }

        KbDocumentEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Document> findById(Long id) {
        return springRepo.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Document> findBySha256(String sha256) {
        return springRepo.findBySha256(sha256)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByTenantIdAndTitle(Long tenantId, String title) {
        return springRepo.existsByTenantIdAndTitleAndDeletedAtIsNull(tenantId, title);
    }

    @Override
    public List<Document> findByTenantId(Long tenantId, int page, int size) {
        int[] validatedParams = KbUtils.validatePagination(page, size,
                KbConstants.SearchLimits.MAX_PAGE_SIZE);

        Pageable pageable = PageRequest.of(validatedParams[0], validatedParams[1], Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<KbDocumentEntity> entityPage = springRepo.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);

        return mapper.toDomainList(entityPage.getContent());
    }

    @Override
    public long countByTenantId(Long tenantId) {
        return springRepo.countByTenantIdAndDeletedAtIsNull(tenantId);
    }

    @Override
    public List<Document> findByParsingStatus(ParsingStatus status, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<KbDocumentEntity> entityPage = springRepo.findByParsingStatusAndDeletedAtIsNull(
                status.getCode(), pageable);

        return mapper.toDomainList(entityPage.getContent());
    }

    @Override
    public List<Document> findByIds(List<Long> ids) {
        return springRepo.findAllById(ids)
                .stream()
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Document> findByTenantIdAndSourceType(Long tenantId, String sourceType) {
        return springRepo.findByTenantIdAndSourceTypeAndDeletedAtIsNull(tenantId, sourceType)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean softDelete(Long id, Long deletedBy) {
        Optional<KbDocumentEntity> entityOpt = springRepo.findById(id);
        if (entityOpt.isEmpty() || entityOpt.get().getDeletedAt() != null) {
            return false;
        }

        KbDocumentEntity entity = entityOpt.get();
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setUpdatedBy(deletedBy);
        springRepo.save(entity);

        return true;
    }
}