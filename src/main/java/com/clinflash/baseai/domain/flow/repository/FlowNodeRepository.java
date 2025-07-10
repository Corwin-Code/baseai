package com.clinflash.baseai.domain.flow.repository;

import com.clinflash.baseai.domain.flow.model.FlowNode;

import java.util.List;
import java.util.Optional;

/**
 * <h2>流程节点仓储接口</h2>
 */
public interface FlowNodeRepository {

    FlowNode save(FlowNode node);

    List<FlowNode> saveAll(List<FlowNode> nodes);

    Optional<FlowNode> findById(Long id);

    List<FlowNode> findByDefinitionId(Long definitionId);

    void deleteByDefinitionId(Long definitionId);
}