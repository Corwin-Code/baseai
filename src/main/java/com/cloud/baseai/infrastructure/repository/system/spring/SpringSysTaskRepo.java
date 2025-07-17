package com.cloud.baseai.infrastructure.repository.system.spring;

import com.cloud.baseai.infrastructure.persistence.system.entity.SysTaskEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * <h2>系统任务Spring Data JPA仓储</h2>
 */
@Repository
public interface SpringSysTaskRepo extends JpaRepository<SysTaskEntity, Long> {

    /**
     * 查找待执行的任务
     */
    @Query("SELECT t FROM SysTaskEntity t WHERE t.status = 0 AND t.deletedAt IS NULL " +
            "ORDER BY t.createdAt ASC")
    List<SysTaskEntity> findPendingTasks(Pageable pageable);

    /**
     * 按状态查找任务
     */
    @Query("SELECT t FROM SysTaskEntity t WHERE t.status = :status AND t.deletedAt IS NULL " +
            "ORDER BY t.createdAt DESC")
    List<SysTaskEntity> findByStatus(@Param("status") Integer status, Pageable pageable);

    /**
     * 按租户查找任务
     */
    @Query("SELECT t FROM SysTaskEntity t WHERE t.tenantId = :tenantId AND t.deletedAt IS NULL " +
            "ORDER BY t.createdAt DESC")
    List<SysTaskEntity> findByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    /**
     * 按任务类型查找
     */
    @Query("SELECT t FROM SysTaskEntity t WHERE t.taskType = :taskType AND t.deletedAt IS NULL " +
            "ORDER BY t.createdAt DESC")
    List<SysTaskEntity> findByTaskType(@Param("taskType") String taskType, Pageable pageable);

    /**
     * 查找可重试的失败任务
     */
    @Query("SELECT t FROM SysTaskEntity t WHERE t.status = 3 AND t.retryCount < 3 AND t.deletedAt IS NULL " +
            "ORDER BY t.updatedAt ASC")
    List<SysTaskEntity> findRetryableTasks(Pageable pageable);

    /**
     * 查找长时间运行的任务
     */
    @Query("SELECT t FROM SysTaskEntity t WHERE t.status = 1 AND t.executedAt < :executedBefore " +
            "AND t.deletedAt IS NULL ORDER BY t.executedAt ASC")
    List<SysTaskEntity> findLongRunningTasks(@Param("executedBefore") OffsetDateTime executedBefore);

    /**
     * 统计指定状态的任务数量
     */
    long countByStatusAndDeletedAtIsNull(Integer status);

    /**
     * 统计租户任务数量
     */
    long countByTenantIdAndDeletedAtIsNull(Long tenantId);

    /**
     * 清理已完成的旧任务
     */
    @Query("DELETE FROM SysTaskEntity t WHERE (t.status = 2 OR t.status = 3) " +
            "AND t.finishedAt < :completedBefore")
    int deleteCompletedTasksBefore(@Param("completedBefore") OffsetDateTime completedBefore);
}