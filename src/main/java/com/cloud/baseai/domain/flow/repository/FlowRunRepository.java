package com.cloud.baseai.domain.flow.repository;

import com.cloud.baseai.domain.flow.model.FlowRun;
import com.cloud.baseai.domain.flow.model.RunStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>流程运行仓储接口</h2>
 */
public interface FlowRunRepository {

    FlowRun save(FlowRun run);

    Optional<FlowRun> findById(Long id);

    List<FlowRun> findBySnapshotId(Long snapshotId, int page, int size);

    long countBySnapshotId(Long snapshotId);

    List<FlowRun> findBySnapshotIdAndStatus(Long snapshotId, RunStatus status, int page, int size);

    long countBySnapshotIdAndStatus(Long snapshotId, RunStatus status);

    int countByProjectId(Long projectId);

    int countByProjectIdAndStatus(Long projectId, RunStatus status);

    int countByTenantIdSince(Long tenantId, OffsetDateTime since);

    int countByTenantIdAndStatusSince(Long tenantId, RunStatus status, OffsetDateTime since);

    int countByProjectIdSince(Long projectId, OffsetDateTime since);

    int countByProjectIdAndStatusSince(Long projectId, RunStatus status, OffsetDateTime since);
}