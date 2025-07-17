package com.cloud.baseai.infrastructure.repository.flow.spring;

import com.cloud.baseai.infrastructure.persistence.flow.entity.FlowRunLogEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>流程运行日志Spring Data JPA仓储</h2>
 */
@Repository
public interface SpringFlowRunLogRepo extends JpaRepository<FlowRunLogEntity, Long> {

    List<FlowRunLogEntity> findByRunIdAndDeletedAtIsNull(Long runId, Sort sort);
}