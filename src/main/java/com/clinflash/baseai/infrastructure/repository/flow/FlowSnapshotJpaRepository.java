package com.clinflash.baseai.infrastructure.repository.flow;

import com.clinflash.baseai.domain.flow.model.FlowSnapshot;
import com.clinflash.baseai.domain.flow.repository.FlowSnapshotRepository;
import com.clinflash.baseai.infrastructure.persistence.flow.entity.FlowSnapshotEntity;
import com.clinflash.baseai.infrastructure.persistence.flow.mapper.FlowMapper;
import com.clinflash.baseai.infrastructure.repository.flow.spring.SpringFlowSnapshotRepo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * <h2>流程快照仓储实现</h2>
 */
@Repository
public class FlowSnapshotJpaRepository implements FlowSnapshotRepository {

    private final SpringFlowSnapshotRepo springRepo;
    private final FlowMapper mapper;

    public FlowSnapshotJpaRepository(SpringFlowSnapshotRepo springRepo, FlowMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public FlowSnapshot save(FlowSnapshot snapshot) {
        FlowSnapshotEntity entity = mapper.toEntity(snapshot);
        FlowSnapshotEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<FlowSnapshot> findById(Long id) {
        return springRepo.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<FlowSnapshot> findLatestByDefinitionId(Long definitionId) {
        return springRepo.findFirstByDefinitionIdAndDeletedAtIsNullOrderByVersionDesc(definitionId)
                .map(mapper::toDomain);
    }
}