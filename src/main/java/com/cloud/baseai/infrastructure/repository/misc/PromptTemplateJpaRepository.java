package com.cloud.baseai.infrastructure.repository.misc;

import com.cloud.baseai.domain.misc.model.PromptTemplate;
import com.cloud.baseai.domain.misc.repository.PromptTemplateRepository;
import com.cloud.baseai.infrastructure.persistence.misc.entity.PromptTemplateEntity;
import com.cloud.baseai.infrastructure.persistence.misc.mapper.MiscMapper;
import com.cloud.baseai.infrastructure.repository.misc.spring.SpringPromptTemplateRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <h2>提示词模板仓储实现</h2>
 *
 * <p>这个实现类是Repository接口的具体实现，它桥接了领域层的抽象定义和具体的数据访问技术。
 * 通过这种设计，我们可以轻松地更换底层的数据访问技术（比如从JPA切换到MyBatis），
 * 而不影响业务逻辑层的代码。</p>
 */
@Repository
public class PromptTemplateJpaRepository implements PromptTemplateRepository {

    private final SpringPromptTemplateRepo springRepo;
    private final MiscMapper mapper;

    public PromptTemplateJpaRepository(SpringPromptTemplateRepo springRepo, MiscMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public PromptTemplate save(PromptTemplate template) {
        PromptTemplateEntity entity = mapper.toEntity(template);
        PromptTemplateEntity savedEntity = springRepo.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PromptTemplate> findById(Long id) {
        return springRepo.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<PromptTemplate> findVisibleTemplates(Long tenantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return springRepo.findVisibleTemplates(tenantId, pageable)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countVisibleTemplates(Long tenantId) {
        return springRepo.countVisibleTemplates(tenantId);
    }

    @Override
    public Optional<PromptTemplate> findByTenantIdAndName(Long tenantId, String name) {
        return springRepo.findByTenantIdAndNameAndDeletedAtIsNull(tenantId, name)
                .map(mapper::toDomain);
    }

    @Override
    public List<PromptTemplate> searchTemplates(Long tenantId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return springRepo.searchTemplates(tenantId, keyword, pageable)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PromptTemplate> findByModelCode(Long tenantId, String modelCode, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return springRepo.findByModelCode(tenantId, modelCode, pageable)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PromptTemplate> findVersions(Long tenantId, String name) {
        return springRepo.findVersions(tenantId, name)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByTenantIdAndName(Long tenantId, String name) {
        return springRepo.existsByTenantIdAndNameAndDeletedAtIsNull(tenantId, name);
    }

    @Override
    public void softDelete(Long id, Long deletedBy) {
        springRepo.findById(id).ifPresent(entity -> {
            entity.setDeletedAt(OffsetDateTime.now());
            springRepo.save(entity);
        });
    }

    @Override
    public List<PromptTemplate> findPopularTemplates(Long tenantId, int limit) {
        // 简化实现，实际应该根据使用统计排序
        Pageable pageable = PageRequest.of(0, limit);
        return springRepo.findVisibleTemplates(tenantId, pageable)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}