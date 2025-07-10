package com.clinflash.baseai.infrastructure.repository.flow;

import com.clinflash.baseai.domain.flow.model.FlowRunLog;
import com.clinflash.baseai.domain.flow.repository.FlowRunLogRepository;
import com.clinflash.baseai.infrastructure.persistence.flow.entity.FlowRunLogEntity;
import com.clinflash.baseai.infrastructure.persistence.flow.mapper.FlowMapper;
import com.clinflash.baseai.infrastructure.repository.flow.spring.SpringFlowRunLogRepo;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>流程运行日志仓储实现</h2>
 */
@Repository
public class FlowRunLogJpaRepository implements FlowRunLogRepository {

    private final SpringFlowRunLogRepo springRepo;
    private final FlowMapper mapper;

    public FlowRunLogJpaRepository(SpringFlowRunLogRepo springRepo, FlowMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public FlowRunLog save(FlowRunLog log) {
        FlowRunLogEntity entity = mapper.toEntity(log);
        FlowRunLogEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<FlowRunLog> findByRunId(Long runId) {
        List<FlowRunLogEntity> entities = springRepo.findByRunIdAndDeletedAtIsNull(
                runId, Sort.by(Sort.Direction.ASC, "createdAt"));
        return mapper.toRunLogDomainList(entities);
    }
}