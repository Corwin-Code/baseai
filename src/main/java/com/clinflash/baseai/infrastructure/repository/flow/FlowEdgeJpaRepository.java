package com.clinflash.baseai.infrastructure.repository.flow;

import com.clinflash.baseai.domain.flow.model.FlowEdge;
import com.clinflash.baseai.domain.flow.repository.FlowEdgeRepository;
import com.clinflash.baseai.infrastructure.persistence.flow.entity.FlowEdgeEntity;
import com.clinflash.baseai.infrastructure.persistence.flow.mapper.FlowMapper;
import com.clinflash.baseai.infrastructure.repository.flow.spring.SpringFlowEdgeRepo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>流程边仓储实现</h2>
 */
@Repository
public class FlowEdgeJpaRepository implements FlowEdgeRepository {

    private final SpringFlowEdgeRepo springRepo;
    private final FlowMapper mapper;

    public FlowEdgeJpaRepository(SpringFlowEdgeRepo springRepo, FlowMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public FlowEdge save(FlowEdge edge) {
        FlowEdgeEntity entity = mapper.toEntity(edge);
        FlowEdgeEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<FlowEdge> saveAll(List<FlowEdge> edges) {
        List<FlowEdgeEntity> entities = mapper.toEdgeEntityList(edges);
        List<FlowEdgeEntity> saved = springRepo.saveAll(entities);
        return mapper.toEdgeDomainList(saved);
    }

    @Override
    public Optional<FlowEdge> findById(Long id) {
        return springRepo.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public List<FlowEdge> findByDefinitionId(Long definitionId) {
        List<FlowEdgeEntity> entities = springRepo.findByDefinitionIdAndDeletedAtIsNull(definitionId);
        return mapper.toEdgeDomainList(entities);
    }

    @Override
    public void deleteByDefinitionId(Long definitionId) {
        springRepo.deleteByDefinitionId(definitionId);
    }
}