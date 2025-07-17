package com.cloud.baseai.domain.flow.repository;

import com.cloud.baseai.domain.flow.model.FlowEdge;

import java.util.List;
import java.util.Optional;

/**
 * <h2>流程边仓储接口</h2>
 */
public interface FlowEdgeRepository {

    FlowEdge save(FlowEdge edge);

    List<FlowEdge> saveAll(List<FlowEdge> edges);

    Optional<FlowEdge> findById(Long id);

    List<FlowEdge> findByDefinitionId(Long definitionId);

    void deleteByDefinitionId(Long definitionId);
}