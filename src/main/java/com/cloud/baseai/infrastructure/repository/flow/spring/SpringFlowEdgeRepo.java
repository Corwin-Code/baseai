package com.cloud.baseai.infrastructure.repository.flow.spring;

import com.cloud.baseai.infrastructure.persistence.flow.entity.FlowEdgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>流程边Spring Data JPA仓储</h2>
 */
@Repository
public interface SpringFlowEdgeRepo extends JpaRepository<FlowEdgeEntity, Long> {

    List<FlowEdgeEntity> findByDefinitionIdAndDeletedAtIsNull(Long definitionId);

    @Modifying
    @Query("DELETE FROM FlowEdgeEntity e WHERE e.definitionId = :definitionId")
    void deleteByDefinitionId(@Param("definitionId") Long definitionId);
}