package com.cloud.baseai.infrastructure.repository.flow;

import com.cloud.baseai.domain.flow.model.FlowSnapshot;
import com.cloud.baseai.domain.flow.repository.FlowSnapshotRepository;
import com.cloud.baseai.infrastructure.persistence.flow.entity.FlowSnapshotEntity;
import com.cloud.baseai.infrastructure.persistence.flow.mapper.FlowMapper;
import com.cloud.baseai.infrastructure.repository.flow.spring.SpringFlowSnapshotRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <h2>流程快照仓储实现类</h2>
 */
@Repository
@Transactional(readOnly = true) // 默认只读事务，提高性能
public class FlowSnapshotJpaRepository implements FlowSnapshotRepository {

    private static final Logger log = LoggerFactory.getLogger(FlowSnapshotJpaRepository.class);

    private final SpringFlowSnapshotRepo springRepo;
    private final FlowMapper mapper;

    public FlowSnapshotJpaRepository(SpringFlowSnapshotRepo springRepo, FlowMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional // 需要写事务
    public FlowSnapshot save(FlowSnapshot snapshot) {
        log.debug("保存流程快照: definitionId={}, version={}",
                snapshot.definitionId(), snapshot.version());

        try {
            // 转换为实体对象
            FlowSnapshotEntity entity = mapper.toEntity(snapshot);

            // 执行保存操作
            FlowSnapshotEntity savedEntity = springRepo.save(entity);

            // 转换回领域对象
            FlowSnapshot savedSnapshot = mapper.toDomain(savedEntity);

            log.debug("快照保存成功: id={}", savedSnapshot.id());
            return savedSnapshot;

        } catch (Exception e) {
            log.error("保存快照失败: definitionId={}, version={}",
                    snapshot.definitionId(), snapshot.version(), e);
            throw new RuntimeException("保存快照失败", e);
        }
    }

    @Override
    public Optional<FlowSnapshot> findById(Long id) {
        log.debug("根据ID查找快照: id={}", id);

        if (id == null) {
            log.warn("快照ID为空，返回空结果");
            return Optional.empty();
        }

        return springRepo.findById(id)
                .filter(entity -> !entity.isDeleted())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<FlowSnapshot> findByDefinitionIdAndVersion(Long definitionId, Integer version) {
        log.debug("根据定义ID和版本查找快照: definitionId={}, version={}",
                definitionId, version);

        if (definitionId == null || version == null) {
            log.warn("查询参数为空: definitionId={}, version={}", definitionId, version);
            return Optional.empty();
        }

        return springRepo.findByDefinitionIdAndVersionAndDeletedAtIsNull(definitionId, version)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<FlowSnapshot> findLatestByDefinitionId(Long definitionId) {
        log.debug("查找最新版本快照: definitionId={}", definitionId);

        if (definitionId == null) {
            log.warn("流程定义ID为空，返回空结果");
            return Optional.empty();
        }

        Optional<FlowSnapshot> result = springRepo.findLatestByDefinitionId(definitionId)
                .map(mapper::toDomain);

        if (result.isPresent()) {
            log.debug("找到最新快照: id={}, version={}",
                    result.get().id(), result.get().version());
        } else {
            log.debug("未找到流程定义的快照: definitionId={}", definitionId);
        }

        return result;
    }

    @Override
    public List<FlowSnapshot> findByDefinitionId(Long definitionId) {
        log.debug("查找流程定义的所有快照: definitionId={}", definitionId);

        if (definitionId == null) {
            log.warn("流程定义ID为空，返回空列表");
            return List.of();
        }

        List<FlowSnapshotEntity> entities = springRepo
                .findByDefinitionIdAndDeletedAtIsNullOrderByVersionDesc(definitionId);

        List<FlowSnapshot> snapshots = entities.stream()
                .map(FlowSnapshotEntity::toDomain)
                .collect(Collectors.toList());

        log.debug("找到{}个快照", snapshots.size());
        return snapshots;
    }

    @Override
    public Page<FlowSnapshot> findAllAvailable(Pageable pageable) {
        log.debug("分页查询可用快照: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<FlowSnapshotEntity> entityPage = springRepo.findByDeletedAtIsNull(pageable);

        // 转换实体页面为领域对象页面
        Page<FlowSnapshot> snapshotPage = entityPage.map(FlowSnapshotEntity::toDomain);

        log.debug("查询结果: 总数={}, 页数={}",
                snapshotPage.getTotalElements(), snapshotPage.getTotalPages());

        return snapshotPage;
    }

    @Override
    public Page<FlowSnapshot> findByCreatedBy(Long createdBy, Pageable pageable) {
        log.debug("根据创建者分页查询快照: createdBy={}, page={}, size={}",
                createdBy, pageable.getPageNumber(), pageable.getPageSize());

        if (createdBy == null) {
            log.warn("创建者ID为空，返回空页面");
            return Page.empty(pageable);
        }

        Page<FlowSnapshotEntity> entityPage = springRepo
                .findByCreatedByAndDeletedAtIsNull(createdBy, pageable);

        return entityPage.map(FlowSnapshotEntity::toDomain);
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null) {
            return false;
        }

        boolean exists = springRepo.findById(id)
                .map(entity -> !entity.isDeleted())
                .orElse(false);

        log.debug("检查快照是否存在: id={}, exists={}", id, exists);
        return exists;
    }

    @Override
    public boolean existsByDefinitionIdAndVersion(Long definitionId, Integer version) {
        if (definitionId == null || version == null) {
            return false;
        }

        boolean exists = springRepo.existsByDefinitionIdAndVersionAndDeletedAtIsNull(
                definitionId, version);

        log.debug("检查快照版本是否存在: definitionId={}, version={}, exists={}",
                definitionId, version, exists);

        return exists;
    }

    @Override
    public long countByDefinitionId(Long definitionId) {
        if (definitionId == null) {
            return 0;
        }

        long count = springRepo.countByDefinitionIdAndDeletedAtIsNull(definitionId);

        log.debug("统计快照数量: definitionId={}, count={}", definitionId, count);
        return count;
    }

    @Override
    @Transactional // 需要写事务
    public void deleteById(Long id) {
        log.info("删除快照: id={}", id);

        if (id == null) {
            log.warn("快照ID为空，忽略删除操作");
            return;
        }

        try {
            // 检查快照是否存在
            if (!springRepo.existsById(id)) {
                log.warn("要删除的快照不存在: id={}", id);
                return;
            }

            // 执行软删除
            springRepo.softDeleteById(id, OffsetDateTime.now());

            log.info("快照删除成功: id={}", id);

        } catch (Exception e) {
            log.error("删除快照失败: id={}", id, e);
            throw new RuntimeException("删除快照失败", e);
        }
    }

    @Override
    @Transactional // 需要写事务
    public void deleteByIds(List<Long> ids) {
        log.info("批量删除快照: count={}", ids.size());

        if (ids == null || ids.isEmpty()) {
            log.warn("快照ID列表为空，忽略删除操作");
            return;
        }

        try {
            // 过滤掉null值
            List<Long> validIds = ids.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (validIds.isEmpty()) {
                log.warn("没有有效的快照ID，忽略删除操作");
                return;
            }

            // 执行批量软删除
            springRepo.softDeleteByIds(validIds, OffsetDateTime.now());

            log.info("批量删除完成: 处理了{}个快照", validIds.size());

        } catch (Exception e) {
            log.error("批量删除快照失败: ids={}", ids, e);
            throw new RuntimeException("批量删除快照失败", e);
        }
    }

    /**
     * 获取快照统计信息
     *
     * <p>这是一个额外的方法，提供快照的统计信息。虽然不在接口中定义，
     * 但在实际应用中很有用，比如用于管理界面的仪表板显示。</p>
     *
     * @return 快照统计信息
     */
    public SnapshotStatistics getStatistics() {
        log.debug("获取快照统计信息");

        try {
            long totalCount = springRepo.count();
            long availableCount = springRepo.countByDeletedAtIsNull();
            long deletedCount = totalCount - availableCount;

            SnapshotStatistics stats = new SnapshotStatistics(
                    totalCount, availableCount, deletedCount);

            log.debug("快照统计: {}", stats);
            return stats;

        } catch (Exception e) {
            log.error("获取快照统计信息失败", e);
            return new SnapshotStatistics(0, 0, 0);
        }
    }

    /**
     * 快照统计信息记录
     *
     * <p>这个record提供了快照的基本统计数据，包括总数、可用数和已删除数。
     * 这种信息对于系统监控和容量规划很有价值。</p>
     */
    public record SnapshotStatistics(
            long totalCount,
            long availableCount,
            long deletedCount
    ) {
        public double getDeletionRate() {
            return totalCount > 0 ? (double) deletedCount / totalCount : 0.0;
        }

        public boolean isHealthy() {
            return getDeletionRate() < 0.5; // 删除率低于50%认为是健康的
        }
    }
}