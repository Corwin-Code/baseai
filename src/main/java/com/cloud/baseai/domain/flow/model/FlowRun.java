package com.cloud.baseai.domain.flow.model;

import java.time.OffsetDateTime;

/**
 * <h2>流程运行实例领域模型</h2>
 *
 * <p>流程运行实例代表一次具体的流程执行，记录了从开始到结束的完整生命周期。
 * 就像一次旅行的记录，包含了出发时间、路线、结果等所有重要信息。</p>
 *
 * <p><b>状态追踪：</b></p>
 * <p>运行实例会跟踪执行状态的变化，从创建到开始执行，再到成功或失败结束。
 * 这种状态管理让我们能够监控和调试流程执行过程。</p>
 */
public record FlowRun(
        Long id,
        Long snapshotId,
        Long userId,
        RunStatus status,
        String resultJson,
        Long createdBy,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        OffsetDateTime deletedAt
) {

    /**
     * 创建新的流程运行实例
     */
    public static FlowRun create(Long snapshotId, Long userId, Long createdBy) {
        return new FlowRun(
                null,
                snapshotId,
                userId,
                RunStatus.PENDING,
                null,
                createdBy,
                null,
                null,
                null
        );
    }

    /**
     * 开始执行
     */
    public FlowRun start() {
        if (this.status != RunStatus.PENDING) {
            throw new IllegalStateException("只有待执行状态的流程才能开始");
        }

        return new FlowRun(
                this.id,
                this.snapshotId,
                this.userId,
                RunStatus.RUNNING,
                this.resultJson,
                this.createdBy,
                OffsetDateTime.now(),
                this.finishedAt,
                this.deletedAt
        );
    }

    /**
     * 成功完成
     */
    public FlowRun success(String resultJson) {
        if (this.status != RunStatus.RUNNING) {
            throw new IllegalStateException("只有运行中的流程才能标记为成功");
        }

        return new FlowRun(
                this.id,
                this.snapshotId,
                this.userId,
                RunStatus.SUCCESS,
                resultJson,
                this.createdBy,
                this.startedAt,
                OffsetDateTime.now(),
                this.deletedAt
        );
    }

    /**
     * 失败结束
     */
    public FlowRun fail(String errorJson) {
        if (this.status != RunStatus.RUNNING) {
            throw new IllegalStateException("只有运行中的流程才能标记为失败");
        }

        return new FlowRun(
                this.id,
                this.snapshotId,
                this.userId,
                RunStatus.FAILED,
                errorJson,
                this.createdBy,
                this.startedAt,
                OffsetDateTime.now(),
                this.deletedAt
        );
    }

    /**
     * 检查是否正在运行
     */
    public boolean isRunning() {
        return this.status == RunStatus.RUNNING;
    }

    /**
     * 检查是否已完成（无论成功或失败）
     */
    public boolean isFinished() {
        return this.status == RunStatus.SUCCESS || this.status == RunStatus.FAILED;
    }

    /**
     * 获取执行时长（毫秒）
     */
    public Long getDurationMs() {
        if (startedAt == null || finishedAt == null) {
            return null;
        }
        return finishedAt.toInstant().toEpochMilli() -
                startedAt.toInstant().toEpochMilli();
    }
}