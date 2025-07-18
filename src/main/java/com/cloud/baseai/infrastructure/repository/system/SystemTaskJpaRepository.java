package com.cloud.baseai.infrastructure.repository.system;

import com.cloud.baseai.domain.system.model.SystemTask;
import com.cloud.baseai.domain.system.model.enums.TaskStatus;
import com.cloud.baseai.domain.system.repository.SystemTaskRepository;
import com.cloud.baseai.infrastructure.persistence.system.mapper.SystemMapper;
import com.cloud.baseai.infrastructure.repository.system.spring.SpringSysTaskRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <h2>系统任务仓储实现</h2>
 */
@Repository
public class SystemTaskJpaRepository implements SystemTaskRepository {

    private final SpringSysTaskRepo springRepo;
    private final SystemMapper mapper;

    public SystemTaskJpaRepository(SpringSysTaskRepo springRepo, SystemMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public SystemTask save(SystemTask task) {
        var entity = mapper.toEntity(task);
        var savedEntity = springRepo.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<SystemTask> findById(Long id) {
        return springRepo.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<SystemTask> findPendingTasks(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return springRepo.findPendingTasks(pageable)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SystemTask> findByStatus(TaskStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return springRepo.findByStatus(status, pageable)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SystemTask> findByTenantId(Long tenantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return springRepo.findByTenantId(tenantId, pageable)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SystemTask> findByTaskType(String taskType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return springRepo.findByTaskType(taskType, pageable)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SystemTask> findRetryableTasks(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return springRepo.findRetryableTasks(pageable)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SystemTask> findLongRunningTasks(OffsetDateTime executedBefore) {
        return springRepo.findLongRunningTasks(executedBefore)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByStatus(TaskStatus status) {
        return springRepo.countByStatusAndDeletedAtIsNull(status);
    }

    @Override
    public long countByTenantId(Long tenantId) {
        return springRepo.countByTenantIdAndDeletedAtIsNull(tenantId);
    }

    @Override
    public int cleanupCompletedTasks(OffsetDateTime completedBefore) {
        return springRepo.deleteCompletedTasksBefore(completedBefore);
    }
}