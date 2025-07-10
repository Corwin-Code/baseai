package com.clinflash.baseai.domain.flow.repository;

import com.clinflash.baseai.domain.flow.model.FlowSnapshot;

import java.util.Optional;

/**
 * <h2>流程快照仓储接口</h2>
 */
public interface FlowSnapshotRepository {

    FlowSnapshot save(FlowSnapshot snapshot);

    Optional<FlowSnapshot> findById(Long id);

    Optional<FlowSnapshot> findLatestByDefinitionId(Long definitionId);
}