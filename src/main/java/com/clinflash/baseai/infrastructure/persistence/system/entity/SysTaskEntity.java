package com.clinflash.baseai.infrastructure.persistence.system.entity;

import com.clinflash.baseai.domain.system.model.SystemTask;
import com.clinflash.baseai.domain.system.model.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * <h2>系统任务JPA实体</h2>
 */
@Setter
@Getter
@Entity
@Table(name = "sys_tasks")
public class SysTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "task_type", nullable = false, length = 64)
    private String taskType;

    @Column(name = "payload", nullable = false, columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Column(name = "status", nullable = false, columnDefinition = "smallint")
    @Enumerated(EnumType.ORDINAL)
    private TaskStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "executed_at")
    private OffsetDateTime executedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // 构造函数
    public SysTaskEntity() {
    }

    // =================== 领域对象转换 ===================

    /**
     * 从领域对象创建实体
     */
    public static SysTaskEntity fromDomain(SystemTask task) {
        if (task == null) {
            return null;
        }

        SysTaskEntity entity = new SysTaskEntity();
        entity.id = task.id();
        entity.tenantId = task.tenantId();
        entity.taskType = task.taskType();
        entity.payload = task.payload();
        entity.status = task.status();
        entity.retryCount = task.retryCount();
        entity.lastError = task.lastError();
        entity.createdBy = task.createdBy();
        entity.createdAt = task.createdAt();
        entity.updatedAt = task.updatedAt();
        entity.executedAt = task.executedAt();
        entity.finishedAt = task.finishedAt();
        entity.deletedAt = task.deletedAt();

        return entity;
    }

    /**
     * 转换为领域对象
     */
    public SystemTask toDomain() {
        return new SystemTask(
                id,
                tenantId,
                taskType,
                payload,
                status,
                retryCount,
                lastError,
                createdBy,
                createdAt,
                updatedAt,
                executedAt,
                finishedAt,
                deletedAt
        );
    }

    /**
     * 从领域对象更新实体状态
     */
    public void updateFromDomain(SystemTask task) {
        if (task == null) {
            return;
        }

        // ID和创建时间不可更改
        this.tenantId = task.tenantId();
        this.taskType = task.taskType();
        this.payload = task.payload();
        this.status = task.status();
        this.retryCount = task.retryCount();
        this.lastError = task.lastError();
        this.createdBy = task.createdBy();
        this.updatedAt = task.updatedAt();
        this.executedAt = task.executedAt();
        this.finishedAt = task.finishedAt();
        this.deletedAt = task.deletedAt();
    }
}