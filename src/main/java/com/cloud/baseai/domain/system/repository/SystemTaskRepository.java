package com.cloud.baseai.domain.system.repository;

import com.cloud.baseai.domain.system.model.SystemTask;
import com.cloud.baseai.domain.system.model.enums.TaskStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>系统任务仓储接口</h2>
 */
public interface SystemTaskRepository {

    /**
     * 保存任务
     */
    SystemTask save(SystemTask task);

    /**
     * 根据ID查找任务
     */
    Optional<SystemTask> findById(Long id);

    /**
     * 查找待执行的任务
     *
     * <p>任务调度器会定期调用这个方法，获取需要处理的任务。
     * 支持按优先级和创建时间排序，确保重要任务优先处理。</p>
     */
    List<SystemTask> findPendingTasks(int limit);

    /**
     * 查找指定状态的任务
     */
    List<SystemTask> findByStatus(TaskStatus status, int page, int size);

    /**
     * 查找租户的任务
     */
    List<SystemTask> findByTenantId(Long tenantId, int page, int size);

    /**
     * 查找指定类型的任务
     */
    List<SystemTask> findByTaskType(String taskType, int page, int size);

    /**
     * 查找需要重试的失败任务
     */
    List<SystemTask> findRetryableTasks(int limit);

    /**
     * 查找长时间运行的任务
     *
     * <p>用于监控和告警，发现可能卡住的任务。</p>
     */
    List<SystemTask> findLongRunningTasks(OffsetDateTime executedBefore);

    /**
     * 统计任务数量
     */
    long countByStatus(TaskStatus status);

    /**
     * 统计租户任务数量
     */
    long countByTenantId(Long tenantId);

    /**
     * 清理已完成的旧任务
     *
     * <p>定期清理历史任务数据，避免数据表过大影响性能。</p>
     */
    int cleanupCompletedTasks(OffsetDateTime completedBefore);
}