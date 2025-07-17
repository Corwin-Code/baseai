package com.cloud.baseai.infrastructure.repository.flow;

import com.cloud.baseai.domain.flow.model.FlowRun;
import com.cloud.baseai.domain.flow.model.RunStatus;
import com.cloud.baseai.domain.flow.repository.FlowRunRepository;
import com.cloud.baseai.infrastructure.persistence.flow.entity.FlowRunEntity;
import com.cloud.baseai.infrastructure.persistence.flow.mapper.FlowMapper;
import com.cloud.baseai.infrastructure.repository.flow.spring.SpringFlowRunRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>流程运行仓储实现</h2>
 */
@Repository
public class FlowRunJpaRepository implements FlowRunRepository {

    private final SpringFlowRunRepo springRepo;
    private final FlowMapper mapper;

    public FlowRunJpaRepository(SpringFlowRunRepo springRepo, FlowMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public FlowRun save(FlowRun run) {
        FlowRunEntity entity;

        if (run.id() == null) {
            entity = mapper.toEntity(run);
        } else {
            entity = springRepo.findById(run.id())
                    .orElse(mapper.toEntity(run));
            entity.updateFromDomain(run);
        }

        FlowRunEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<FlowRun> findById(Long id) {
        return springRepo.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public List<FlowRun> findBySnapshotId(Long snapshotId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        Page<FlowRunEntity> entityPage = springRepo.findBySnapshotIdAndDeletedAtIsNull(snapshotId, pageable);
        return mapper.toRunDomainList(entityPage.getContent());
    }

    @Override
    public long countBySnapshotId(Long snapshotId) {
        return springRepo.countBySnapshotIdAndDeletedAtIsNull(snapshotId);
    }

    @Override
    public List<FlowRun> findBySnapshotIdAndStatus(Long snapshotId, RunStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        Page<FlowRunEntity> entityPage = springRepo.findBySnapshotIdAndStatusAndDeletedAtIsNull(
                snapshotId, status, pageable);
        return mapper.toRunDomainList(entityPage.getContent());
    }

    @Override
    public long countBySnapshotIdAndStatus(Long snapshotId, RunStatus status) {
        return springRepo.countBySnapshotIdAndStatusAndDeletedAtIsNull(snapshotId, status);
    }

    @Override
    public int countByProjectId(Long projectId) {
        return springRepo.countByProjectId(projectId);
    }

    @Override
    public int countByProjectIdAndStatus(Long projectId, RunStatus status) {
        return springRepo.countByProjectIdAndStatus(projectId, status.getCode());
    }

    @Override
    public int countByTenantIdSince(Long tenantId, OffsetDateTime since) {
        return springRepo.countByTenantIdAndStartedAtAfter(tenantId, since);
    }

    @Override
    public int countByTenantIdAndStatusSince(Long tenantId, RunStatus status, OffsetDateTime since) {
        return springRepo.countByTenantIdAndStatusAndStartedAtAfter(tenantId, status.getCode(), since);
    }

    @Override
    public int countByProjectIdSince(Long projectId, OffsetDateTime since) {
        return springRepo.countByProjectIdAndStartedAtAfter(projectId, since);
    }

    @Override
    public int countByProjectIdAndStatusSince(Long projectId, RunStatus status, OffsetDateTime since) {
        return springRepo.countByProjectIdAndStatusAndStartedAtAfter(projectId, status.getCode(), since);
    }
}