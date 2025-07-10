package com.clinflash.baseai.infrastructure.repository.flow;

import com.clinflash.baseai.domain.flow.model.FlowNode;
import com.clinflash.baseai.domain.flow.repository.FlowNodeRepository;
import com.clinflash.baseai.infrastructure.persistence.flow.entity.FlowNodeEntity;
import com.clinflash.baseai.infrastructure.persistence.flow.mapper.FlowMapper;
import com.clinflash.baseai.infrastructure.repository.flow.spring.SpringFlowNodeRepo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>流程节点仓储实现</h2>
 */
@Repository
public class FlowNodeJpaRepository implements FlowNodeRepository {

    private final SpringFlowNodeRepo springRepo;
    private final FlowMapper mapper;

    public FlowNodeJpaRepository(SpringFlowNodeRepo springRepo, FlowMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public FlowNode save(FlowNode node) {
        FlowNodeEntity entity = mapper.toEntity(node);
        FlowNodeEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<FlowNode> saveAll(List<FlowNode> nodes) {
        List<FlowNodeEntity> entities = mapper.toNodeEntityList(nodes);
        List<FlowNodeEntity> saved = springRepo.saveAll(entities);
        return mapper.toNodeDomainList(saved);
    }

    @Override
    public Optional<FlowNode> findById(Long id) {
        return springRepo.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public List<FlowNode> findByDefinitionId(Long definitionId) {
        List<FlowNodeEntity> entities = springRepo.findByDefinitionIdAndDeletedAtIsNull(definitionId);
        return mapper.toNodeDomainList(entities);
    }

    @Override
    public void deleteByDefinitionId(Long definitionId) {
        springRepo.deleteByDefinitionId(definitionId);
    }
}