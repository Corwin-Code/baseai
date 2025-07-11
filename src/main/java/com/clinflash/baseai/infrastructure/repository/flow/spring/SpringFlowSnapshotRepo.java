package com.clinflash.baseai.infrastructure.repository.flow.spring;

import com.clinflash.baseai.infrastructure.persistence.flow.entity.FlowSnapshotEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>流程快照Spring Data JPA仓储</h2>
 */
@Repository
public interface SpringFlowSnapshotRepo extends JpaRepository<FlowSnapshotEntity, Long> {
    /**
     * 根据流程定义ID和版本查找快照
     */
    Optional<FlowSnapshotEntity> findByDefinitionIdAndVersionAndDeletedAtIsNull(
            Long definitionId, Integer version);

    /**
     * 查找流程定义的最新版本快照
     */
    @Query("SELECT s FROM FlowSnapshotEntity s " +
            "WHERE s.definitionId = :definitionId " +
            "AND s.deletedAt IS NULL " +
            "ORDER BY s.version DESC " +
            "LIMIT 1")
    Optional<FlowSnapshotEntity> findLatestByDefinitionId(@Param("definitionId") Long definitionId);

    /**
     * 查找流程定义的所有快照
     *
     * <p>按版本号降序排列，让用户看到最新版本在前面。
     * 同时过滤掉已删除的快照。</p>
     */
    List<FlowSnapshotEntity> findByDefinitionIdAndDeletedAtIsNullOrderByVersionDesc(Long definitionId);

    /**
     * 分页查询所有可用快照
     */
    Page<FlowSnapshotEntity> findByDeletedAtIsNull(Pageable pageable);

    /**
     * 根据创建者分页查询快照
     */
    Page<FlowSnapshotEntity> findByCreatedByAndDeletedAtIsNull(Long createdBy, Pageable pageable);

    /**
     * 检查特定版本的快照是否存在
     */
    boolean existsByDefinitionIdAndVersionAndDeletedAtIsNull(Long definitionId, Integer version);

    /**
     * 统计流程定义的快照数量
     */
    long countByDefinitionIdAndDeletedAtIsNull(Long definitionId);

    /**
     * 统计流程定义的有效快照数量
     */
    long countByDeletedAtIsNull(Long definitionId);

    /**
     * 软删除快照
     *
     * <p>这是一个修改操作，使用@Modifying注解标记。我们不是真正删除记录，
     * 而是将deleted_at字段设置为当前时间。</p>
     */
    @Modifying
    @Query("UPDATE FlowSnapshotEntity s SET s.deletedAt = :now WHERE s.id = :id")
    void softDeleteById(@Param("id") Long id, @Param("now") OffsetDateTime now);

    /**
     * 批量软删除快照
     */
    @Modifying
    @Query("UPDATE FlowSnapshotEntity s SET s.deletedAt = :now WHERE s.id IN :ids")
    void softDeleteByIds(@Param("ids") List<Long> ids, @Param("now") OffsetDateTime now);
}