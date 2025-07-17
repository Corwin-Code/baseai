package com.cloud.baseai.infrastructure.repository.flow;

import com.cloud.baseai.domain.flow.model.FlowProject;
import com.cloud.baseai.domain.flow.repository.FlowProjectRepository;
import com.cloud.baseai.infrastructure.persistence.flow.entity.FlowProjectEntity;
import com.cloud.baseai.infrastructure.persistence.flow.mapper.FlowMapper;
import com.cloud.baseai.infrastructure.repository.flow.spring.SpringFlowProjectRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>流程项目仓储实现</h2>
 *
 * <p>负责流程项目数据的持久化操作，提供项目的增删改查功能。
 * 作为领域层和数据层之间的适配器，确保领域逻辑与数据存储技术的解耦。</p>
 */
@Repository
public class FlowProjectJpaRepository implements FlowProjectRepository {

    private final SpringFlowProjectRepo springRepo;
    private final FlowMapper mapper;

    public FlowProjectJpaRepository(SpringFlowProjectRepo springRepo, FlowMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public FlowProject save(FlowProject project) {
        FlowProjectEntity entity;

        if (project.id() == null) {
            // 新建项目
            entity = mapper.toEntity(project);
        } else {
            // 更新现有项目
            entity = springRepo.findById(project.id())
                    .orElse(mapper.toEntity(project));
            entity.updateFromDomain(project);
        }

        FlowProjectEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<FlowProject> findById(Long id) {
        return springRepo.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByTenantIdAndName(Long tenantId, String name) {
        return springRepo.existsByTenantIdAndNameAndDeletedAtIsNull(tenantId, name);
    }

    @Override
    public List<FlowProject> findByTenantId(Long tenantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<FlowProjectEntity> entityPage = springRepo.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
        return mapper.toProjectDomainList(entityPage.getContent());
    }

    @Override
    public long countByTenantId(Long tenantId) {
        return springRepo.countByTenantIdAndDeletedAtIsNull(tenantId);
    }

    @Override
    public List<FlowProject> searchByName(Long tenantId, String name, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<FlowProjectEntity> entityPage = springRepo.findByTenantIdAndNameContainingIgnoreCaseAndDeletedAtIsNull(
                tenantId, name, pageable);
        return mapper.toProjectDomainList(entityPage.getContent());
    }

    @Override
    public long countByTenantIdAndNameContaining(Long tenantId, String name) {
        return springRepo.countByTenantIdAndNameContainingIgnoreCaseAndDeletedAtIsNull(tenantId, name);
    }

    @Override
    public boolean softDelete(Long id, Long deletedBy) {
        Optional<FlowProjectEntity> entityOpt = springRepo.findById(id);
        if (entityOpt.isEmpty() || entityOpt.get().getDeletedAt() != null) {
            return false;
        }

        FlowProjectEntity entity = entityOpt.get();
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setUpdatedBy(deletedBy);
        springRepo.save(entity);

        return true;
    }
}