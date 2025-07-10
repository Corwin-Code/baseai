package com.clinflash.baseai.infrastructure.repository.flow.spring;

import com.clinflash.baseai.infrastructure.persistence.flow.entity.FlowSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * <h2>流程快照Spring Data JPA仓储</h2>
 */
@Repository
public interface SpringFlowSnapshotRepo extends JpaRepository<FlowSnapshotEntity, Long> {

    Optional<FlowSnapshotEntity> findFirstByDefinitionIdAndDeletedAtIsNullOrderByVersionDesc(Long definitionId);
}