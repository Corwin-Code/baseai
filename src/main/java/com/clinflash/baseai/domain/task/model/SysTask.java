package com.clinflash.baseai.domain.task.model;

import com.clinflash.baseai.domain.task.model.enums.TaskStatus;
import com.clinflash.baseai.infrastructure.persistence.user.entity.SysTenantEntity;
import com.clinflash.baseai.infrastructure.persistence.user.entity.SysUserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * <h2>SysTask — 通用异步任务 / 队列表</h2>
 *
 * <p>支持重试、追踪、软删。</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "sys_tasks",
        indexes = @Index(name = "idx_sys_tasks_status", columnList = "status"))
public class SysTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* 多租户（可空） */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private SysTenantEntity tenant;

    /**
     * 任务类型（DOC_PARSE / EMBEDDING / FLOW_RUN …）
     */
    @Column(name = "task_type", length = 64, nullable = false)
    private String taskType;

    /**
     * 任务载荷 JSON
     */
    @Lob
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    /**
     * smallint → 枚举
     */
    @Column(name = "status")
    private Integer statusCode = TaskStatus.PENDING.getCode();

    public TaskStatus getStatus() {
        return TaskStatus.of(statusCode);
    }

    public void setStatus(TaskStatus s) {
        this.statusCode = s.getCode();
    }

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Lob
    @Column(name = "last_error")
    private String lastError;

    /* 人员 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private SysUserEntity creator;

    /* 时间戳 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "timestamptz")
    private OffsetDateTime updatedAt;

    @Column(name = "executed_at", columnDefinition = "timestamptz")
    private OffsetDateTime executedAt;

    @Column(name = "finished_at", columnDefinition = "timestamptz")
    private OffsetDateTime finishedAt;

    @Column(name = "deleted_at", columnDefinition = "timestamptz")
    private OffsetDateTime deletedAt;
}
