package com.clinflash.baseai.infrastructure.repository.flow.spring;

import com.clinflash.baseai.domain.flow.model.RunStatus;
import com.clinflash.baseai.infrastructure.persistence.flow.entity.FlowRunEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

/**
 * <h2>流程运行Spring Data JPA仓储</h2>
 */
@Repository
public interface SpringFlowRunRepo extends JpaRepository<FlowRunEntity, Long> {

    Page<FlowRunEntity> findBySnapshotIdAndDeletedAtIsNull(Long snapshotId, Pageable pageable);

    long countBySnapshotIdAndDeletedAtIsNull(Long snapshotId);

    Page<FlowRunEntity> findBySnapshotIdAndStatusAndDeletedAtIsNull(
            Long snapshotId, RunStatus status, Pageable pageable);

    long countBySnapshotIdAndStatusAndDeletedAtIsNull(Long snapshotId, RunStatus status);

    @Query("SELECT COUNT(r) FROM FlowRunEntity r " +
            "JOIN FlowSnapshotEntity s ON r.snapshotId = s.id " +
            "JOIN FlowDefinitionEntity d ON s.definitionId = d.id " +
            "WHERE d.projectId = :projectId AND r.deletedAt IS NULL")
    int countByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(r) FROM FlowRunEntity r " +
            "JOIN FlowSnapshotEntity s ON r.snapshotId = s.id " +
            "JOIN FlowDefinitionEntity d ON s.definitionId = d.id " +
            "WHERE d.projectId = :projectId AND r.status = :status AND r.deletedAt IS NULL")
    int countByProjectIdAndStatus(@Param("projectId") Long projectId, @Param("status") Integer status);

    @Query("SELECT COUNT(r) FROM FlowRunEntity r " +
            "JOIN FlowSnapshotEntity s ON r.snapshotId = s.id " +
            "JOIN FlowDefinitionEntity d ON s.definitionId = d.id " +
            "JOIN FlowProjectEntity p ON d.projectId = p.id " +
            "WHERE p.tenantId = :tenantId AND r.startedAt >= :since AND r.deletedAt IS NULL")
    int countByTenantIdAndStartedAtAfter(@Param("tenantId") Long tenantId, @Param("since") OffsetDateTime since);

    @Query("SELECT COUNT(r) FROM FlowRunEntity r " +
            "JOIN FlowSnapshotEntity s ON r.snapshotId = s.id " +
            "JOIN FlowDefinitionEntity d ON s.definitionId = d.id " +
            "JOIN FlowProjectEntity p ON d.projectId = p.id " +
            "WHERE p.tenantId = :tenantId AND r.status = :status AND r.startedAt >= :since AND r.deletedAt IS NULL")
    int countByTenantIdAndStatusAndStartedAtAfter(@Param("tenantId") Long tenantId,
                                                  @Param("status") Integer status,
                                                  @Param("since") OffsetDateTime since);

    @Query("SELECT COUNT(r) FROM FlowRunEntity r " +
            "JOIN FlowSnapshotEntity s ON r.snapshotId = s.id " +
            "JOIN FlowDefinitionEntity d ON s.definitionId = d.id " +
            "WHERE d.projectId = :projectId AND r.startedAt >= :since AND r.deletedAt IS NULL")
    int countByProjectIdAndStartedAtAfter(@Param("projectId") Long projectId, @Param("since") OffsetDateTime since);

    @Query("SELECT COUNT(r) FROM FlowRunEntity r " +
            "JOIN FlowSnapshotEntity s ON r.snapshotId = s.id " +
            "JOIN FlowDefinitionEntity d ON s.definitionId = d.id " +
            "WHERE d.projectId = :projectId AND r.status = :status AND r.startedAt >= :since AND r.deletedAt IS NULL")
    int countByProjectIdAndStatusAndStartedAtAfter(@Param("projectId") Long projectId,
                                                   @Param("status") Integer status,
                                                   @Param("since") OffsetDateTime since);
}