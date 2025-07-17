package com.cloud.baseai.infrastructure.repository.mcp.spring;

import com.cloud.baseai.infrastructure.persistence.mcp.entity.McpToolCallLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * <h2>工具调用日志Spring Data JPA仓储</h2>
 */
@Repository
public interface SpringMcpToolCallLogRepo extends JpaRepository<McpToolCallLogEntity, Long> {

    /**
     * 根据工具ID分页查询日志
     */
    Page<McpToolCallLogEntity> findByToolIdOrderByCreatedAtDesc(Long toolId, Pageable pageable);

    /**
     * 根据工具ID和租户ID分页查询日志
     */
    Page<McpToolCallLogEntity> findByToolIdAndTenantIdOrderByCreatedAtDesc(Long toolId, Long tenantId, Pageable pageable);

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
    @Query("SELECT COUNT(l) FROM McpToolCallLogEntity l WHERE l.createdAt >= :since")
    int countSince(@Param("since") OffsetDateTime since);

    /**
     * 统计指定时间后特定状态的调用次数
     */
    @Query("SELECT COUNT(l) FROM McpToolCallLogEntity l WHERE l.status = :status AND l.createdAt >= :since")
    int countByStatusSince(@Param("status") String status, @Param("since") OffsetDateTime since);

    /**
     * 统计工具在指定时间后的调用次数
     */
    @Query("SELECT COUNT(l) FROM McpToolCallLogEntity l WHERE l.toolId = :toolId AND l.createdAt >= :since")
    int countByToolIdSince(@Param("toolId") Long toolId, @Param("since") OffsetDateTime since);

    /**
     * 获取热门工具使用统计
     */
    @Query("SELECT l.toolId as toolId, COUNT(l) as callCount " +
            "FROM McpToolCallLogEntity l " +
            "WHERE l.createdAt >= :since " +
            "GROUP BY l.toolId " +
            "ORDER BY COUNT(l) DESC")
    List<Object[]> getTopUsedTools(@Param("since") OffsetDateTime since, Pageable pageable);

    /**
     * 根据用户ID查询调用历史
     */
    Page<McpToolCallLogEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 根据对话线程ID查询调用历史
     */
    List<McpToolCallLogEntity> findByThreadIdOrderByCreatedAtAsc(Long threadId);

    /**
     * 根据流程运行ID查询调用历史
     */
    List<McpToolCallLogEntity> findByFlowRunIdOrderByCreatedAtAsc(Long flowRunId);

    /**
     * 删除指定时间之前的日志
     */
    @Modifying
    @Query("DELETE FROM McpToolCallLogEntity l WHERE l.createdAt < :before")
    int deleteLogsBefore(@Param("before") OffsetDateTime before);
}