package com.clinflash.baseai.domain.mcp.repository;

import com.clinflash.baseai.application.mcp.dto.ToolUsageDTO;
import com.clinflash.baseai.domain.mcp.model.ToolCallLog;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>工具调用日志仓储接口</h2>
 *
 * <p>管理工具调用的历史记录和统计数据。这些日志对于系统监控、
 * 性能分析和问题排查都至关重要。</p>
 */
public interface ToolCallLogRepository {

    /**
     * 保存调用日志
     */
    ToolCallLog save(ToolCallLog log);

    /**
     * 根据ID查找日志
     */
    Optional<ToolCallLog> findById(Long id);

    /**
     * 根据工具ID分页查询日志
     */
    List<ToolCallLog> findByToolId(Long toolId, int page, int size);

    /**
     * 根据工具ID和租户ID分页查询日志
     */
    List<ToolCallLog> findByToolIdAndTenantId(Long toolId, Long tenantId, int page, int size);

    /**
     * 统计工具的调用次数
     */
    long countByToolId(Long toolId);

    /**
     * 统计工具在指定租户下的调用次数
     */
    long countByToolIdAndTenantId(Long toolId, Long tenantId);

    /**
     * 统计指定时间后的总调用次数
     */
    int countSince(OffsetDateTime since);

    /**
     * 统计指定时间后特定状态的调用次数
     */
    int countByStatusSince(String status, OffsetDateTime since);

    /**
     * 统计工具在指定时间后的调用次数
     */
    int countByToolIdSince(Long toolId, OffsetDateTime since);

    /**
     * 获取热门工具使用统计
     */
    List<ToolUsageDTO> getTopUsedTools(int limit, OffsetDateTime since);

    /**
     * 根据用户ID查询调用历史
     */
    List<ToolCallLog> findByUserId(Long userId, int page, int size);

    /**
     * 根据对话线程ID查询调用历史
     */
    List<ToolCallLog> findByThreadId(Long threadId);

    /**
     * 根据流程运行ID查询调用历史
     */
    List<ToolCallLog> findByFlowRunId(Long flowRunId);

    /**
     * 删除指定时间之前的日志（数据清理）
     */
    int deleteLogsBefore(OffsetDateTime before);
}