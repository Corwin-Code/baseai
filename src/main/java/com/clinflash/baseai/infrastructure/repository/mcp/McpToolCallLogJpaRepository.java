package com.clinflash.baseai.infrastructure.repository.mcp;

import com.clinflash.baseai.application.mcp.dto.ToolUsageDTO;
import com.clinflash.baseai.domain.mcp.model.ToolCallLog;
import com.clinflash.baseai.domain.mcp.repository.ToolCallLogRepository;
import com.clinflash.baseai.infrastructure.persistence.mcp.entity.McpToolCallLogEntity;
import com.clinflash.baseai.infrastructure.persistence.mcp.mapper.McpMapper;
import com.clinflash.baseai.infrastructure.repository.mcp.spring.SpringMcpToolCallLogRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <h2>工具调用日志仓储JPA实现</h2>
 *
 * <p>工具调用日志仓储接口的JPA实现，负责管理所有工具调用的历史记录。
 * 这些日志数据对于系统监控、性能分析、问题排查和业务分析都极为重要。</p>
 *
 * <p><b>性能考虑：</b></p>
 * <p>由于调用日志数据量可能很大，我们在查询方法中都使用了分页，
 * 并在数据库层面建立了适当的索引来保证查询性能。</p>
 */
@Repository
public class McpToolCallLogJpaRepository implements ToolCallLogRepository {

    private final SpringMcpToolCallLogRepo springRepo;
    private final McpMapper mapper;

    public McpToolCallLogJpaRepository(SpringMcpToolCallLogRepo springRepo, McpMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public ToolCallLog save(ToolCallLog log) {
        McpToolCallLogEntity entity;

        if (log.id() == null) {
            // 新建日志
            entity = mapper.toEntity(log);
        } else {
            // 更新现有日志（通常是更新执行结果）
            entity = springRepo.findById(log.id())
                    .orElse(mapper.toEntity(log));
            entity.updateFromDomain(log);
        }

        McpToolCallLogEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ToolCallLog> findById(Long id) {
        return springRepo.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<ToolCallLog> findByToolId(Long toolId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<McpToolCallLogEntity> entityPage = springRepo.findByToolIdOrderByCreatedAtDesc(toolId, pageable);
        return mapper.toLogDomainList(entityPage.getContent());
    }

    @Override
    public List<ToolCallLog> findByToolIdAndTenantId(Long toolId, Long tenantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<McpToolCallLogEntity> entityPage = springRepo.findByToolIdAndTenantIdOrderByCreatedAtDesc(
                toolId, tenantId, pageable);
        return mapper.toLogDomainList(entityPage.getContent());
    }

    @Override
    public long countByToolId(Long toolId) {
        return springRepo.countByToolId(toolId);
    }

    @Override
    public long countByToolIdAndTenantId(Long toolId, Long tenantId) {
        return springRepo.countByToolIdAndTenantId(toolId, tenantId);
    }

    @Override
    public int countSince(OffsetDateTime since) {
        return springRepo.countSince(since);
    }

    @Override
    public int countByStatusSince(String status, OffsetDateTime since) {
        return springRepo.countByStatusSince(status, since);
    }

    @Override
    public int countByToolIdSince(Long toolId, OffsetDateTime since) {
        return springRepo.countByToolIdSince(toolId, since);
    }

    @Override
    public List<ToolUsageDTO> getTopUsedTools(int limit, OffsetDateTime since) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> results = springRepo.getTopUsedTools(since, pageable);

        return results.stream()
                .map(row -> {
                    Long toolId = (Long) row[0];
                    Long callCount = (Long) row[1];

                    // 这里可以进一步查询工具名称等信息
                    return new ToolUsageDTO(toolId, null, null, callCount);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ToolCallLog> findByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<McpToolCallLogEntity> entityPage = springRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return mapper.toLogDomainList(entityPage.getContent());
    }

    @Override
    public List<ToolCallLog> findByThreadId(Long threadId) {
        List<McpToolCallLogEntity> entities = springRepo.findByThreadIdOrderByCreatedAtAsc(threadId);
        return mapper.toLogDomainList(entities);
    }

    @Override
    public List<ToolCallLog> findByFlowRunId(Long flowRunId) {
        List<McpToolCallLogEntity> entities = springRepo.findByFlowRunIdOrderByCreatedAtAsc(flowRunId);
        return mapper.toLogDomainList(entities);
    }

    @Override
    @Transactional
    public int deleteLogsBefore(OffsetDateTime before) {
        return springRepo.deleteLogsBefore(before);
    }

    /**
     * 获取指定时间段内的调用统计
     *
     * <p>这个方法用于生成时间段内的调用趋势图表数据，
     * 帮助运营人员了解工具使用的模式。</p>
     */
    public List<DailyCallStatistics> getDailyStatistics(OffsetDateTime from, OffsetDateTime to) {
        // 这里可以使用原生SQL查询来获取按日期分组的统计数据
        // 为了简化示例，这里只提供接口定义
        throw new UnsupportedOperationException("需要实现原生SQL查询");
    }

    /**
     * 获取工具的平均响应时间
     */
    public Double getAverageLatency(Long toolId, OffsetDateTime since) {
        // 可以使用JPA的@Query注解来实现复杂的聚合查询
        throw new UnsupportedOperationException("需要实现聚合查询");
    }

    /**
     * 内部类：每日调用统计
     */
    public record DailyCallStatistics(String date,
                                      long totalCalls,
                                      long successfulCalls,
                                      long failedCalls,
                                      double averageLatency) {

    }
}