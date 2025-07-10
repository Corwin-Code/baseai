package com.clinflash.baseai.domain.flow.repository;

import com.clinflash.baseai.domain.flow.model.FlowRunLog;

import java.util.List;

/**
 * <h2>流程运行日志仓储接口</h2>
 */
public interface FlowRunLogRepository {

    FlowRunLog save(FlowRunLog log);

    List<FlowRunLog> findByRunId(Long runId);
}