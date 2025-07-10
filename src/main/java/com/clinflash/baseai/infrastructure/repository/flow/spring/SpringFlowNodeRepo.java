package com.clinflash.baseai.infrastructure.repository.flow.spring;

import com.clinflash.baseai.infrastructure.persistence.flow.entity.FlowNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>流程节点Spring Data JPA仓储</h2>
 */
@Repository
public interface SpringFlowNodeRepo extends JpaRepository<FlowNodeEntity, Long> {

    List<FlowNodeEntity> findByDefinitionIdAndDeletedAtIsNull(Long definitionId);

    @Modifying
    @Query("DELETE FROM FlowNodeEntity n WHERE n.definitionId = :definitionId")
    void deleteByDefinitionId(@Param("definitionId") Long definitionId);
}