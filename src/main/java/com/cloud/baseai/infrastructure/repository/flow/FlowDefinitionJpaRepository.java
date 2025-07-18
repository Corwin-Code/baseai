package com.cloud.baseai.infrastructure.repository.flow;

import com.cloud.baseai.domain.flow.model.FlowDefinition;
import com.cloud.baseai.domain.flow.model.FlowStatus;
import com.cloud.baseai.domain.flow.repository.FlowDefinitionRepository;
import com.cloud.baseai.infrastructure.persistence.flow.entity.FlowDefinitionEntity;
import com.cloud.baseai.infrastructure.persistence.flow.mapper.FlowMapper;
import com.cloud.baseai.infrastructure.repository.flow.spring.SpringFlowDefinitionRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>流程定义仓储实现</h2>
 */
@Repository
public class FlowDefinitionJpaRepository implements FlowDefinitionRepository {

    private final SpringFlowDefinitionRepo springRepo;
    private final FlowMapper mapper;

    public FlowDefinitionJpaRepository(SpringFlowDefinitionRepo springRepo, FlowMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public FlowDefinition save(FlowDefinition definition) {
        FlowDefinitionEntity entity;

        if (definition.id() == null) {
            entity = mapper.toEntity(definition);
        } else {
            entity = springRepo.findById(definition.id())
                    .orElse(mapper.toEntity(definition));
            entity.updateFromDomain(definition);
        }

        FlowDefinitionEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<FlowDefinition> findById(Long id) {
        return springRepo.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByProjectIdAndName(Long projectId, String name) {
        return springRepo.existsByProjectIdAndNameAndDeletedAtIsNull(projectId, name);
    }

    @Override
    public List<FlowDefinition> findByProjectId(Long projectId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<FlowDefinitionEntity> entityPage = springRepo.findByProjectIdAndDeletedAtIsNull(projectId, pageable);
        return mapper.toDefinitionDomainList(entityPage.getContent());
    }

    @Override
    public int countByProjectId(Long projectId) {
        return (int) springRepo.countByProjectIdAndDeletedAtIsNull(projectId);
    }

    @Override
    public List<FlowDefinition> findByProjectIdAndStatus(Long projectId, FlowStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<FlowDefinitionEntity> entityPage = springRepo.findByProjectIdAndStatusAndDeletedAtIsNull(
                projectId, status, pageable);
        return mapper.toDefinitionDomainList(entityPage.getContent());
    }

    @Override
    public int countByProjectIdAndStatus(Long projectId, FlowStatus status) {
        return (int) springRepo.countByProjectIdAndStatusAndDeletedAtIsNull(projectId, status);
    }

    @Override
    public List<FlowDefinition> findVersionsByProjectIdAndName(Long projectId, String name) {
        List<FlowDefinitionEntity> entities = springRepo.findByProjectIdAndNameAndDeletedAtIsNullOrderByVersionDesc(
                projectId, name);
        return mapper.toDefinitionDomainList(entities);
    }

    @Override
    public int countByTenantId(Long tenantId) {
        return springRepo.countByTenantId(tenantId);
    }

    @Override
    public int countByTenantIdAndStatus(Long tenantId, FlowStatus status) {
        return springRepo.countByTenantIdAndStatus(tenantId, status.getCode());
    }
}