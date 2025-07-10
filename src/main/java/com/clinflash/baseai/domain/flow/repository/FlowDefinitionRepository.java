package com.clinflash.baseai.domain.flow.repository;

import com.clinflash.baseai.domain.flow.model.FlowDefinition;
import com.clinflash.baseai.domain.flow.model.FlowStatus;

import java.util.List;
import java.util.Optional;

/**
 * <h2>流程定义仓储接口</h2>
 */
public interface FlowDefinitionRepository {

    FlowDefinition save(FlowDefinition definition);

    Optional<FlowDefinition> findById(Long id);

    boolean existsByProjectIdAndName(Long projectId, String name);

    List<FlowDefinition> findByProjectId(Long projectId, int page, int size);

    int countByProjectId(Long projectId);

    List<FlowDefinition> findByProjectIdAndStatus(Long projectId, FlowStatus status, int page, int size);

    int countByProjectIdAndStatus(Long projectId, FlowStatus status);

    List<FlowDefinition> findVersionsByProjectIdAndName(Long projectId, String name);

    int countByTenantId(Long tenantId);

    int countByTenantIdAndStatus(Long tenantId, FlowStatus status);
}