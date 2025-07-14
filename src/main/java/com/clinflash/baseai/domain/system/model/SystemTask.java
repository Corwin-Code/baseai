package com.clinflash.baseai.domain.system.model;

import com.clinflash.baseai.domain.system.model.enums.TaskPriority;
import com.clinflash.baseai.domain.system.model.enums.TaskStatus;

import java.time.OffsetDateTime;

/**
 * <h2>系统任务领域模型</h2>
 *
 * <p>系统任务是现代应用架构中的重要组成部分。就像一个高效的工厂流水线，
 * 任务系统将复杂的工作分解为独立的、可并行处理的单元，大大提升了系统的吞吐量和可靠性。</p>
 *
 * <p><b>异步处理的优势：</b></p>
 * <p>想象一下餐厅的运作方式——顾客点餐后不需要在柜台等待菜品制作完成，
 * 而是可以先找座位，厨房会异步处理订单。任务系统就是这样的理念：
 * 用户发起请求后立即得到响应，耗时的处理工作在后台异步完成。</p>
 */
public record SystemTask(
        Long id,
        Long tenantId,
        String taskType,
        String payload,
        TaskStatus status,
        Integer retryCount,
        String lastError,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime executedAt,
        OffsetDateTime finishedAt,
        OffsetDateTime deletedAt
) {

    /**
     * 创建新任务
     */
    public static SystemTask create(Long tenantId, String taskType, String payload, Long createdBy) {
        return new SystemTask(
                null, // ID由数据库生成
                tenantId,
                taskType,
                payload,
                TaskStatus.PENDING,
                0,
                null,
                createdBy,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                null,
                null,
                null
        );
    }

    /**
     * 开始执行任务
     */
    public SystemTask startExecution() {
        if (this.status != TaskStatus.PENDING) {
            throw new IllegalStateException("只有待执行的任务才能开始执行");
        }

        return new SystemTask(
                this.id,
                this.tenantId,
                this.taskType,
                this.payload,
                TaskStatus.PROCESSING,
                this.retryCount,
                this.lastError,
                this.createdBy,
                this.createdAt,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                this.finishedAt,
                this.deletedAt
        );
    }

    /**
     * 标记任务成功完成
     */
    public SystemTask markSuccess() {
        return new SystemTask(
                this.id,
                this.tenantId,
                this.taskType,
                this.payload,
                TaskStatus.SUCCESS,
                this.retryCount,
                null, // 清除错误信息
                this.createdBy,
                this.createdAt,
                OffsetDateTime.now(),
                this.executedAt,
                OffsetDateTime.now(),
                this.deletedAt
        );
    }

    /**
     * 标记任务失败
     */
    public SystemTask markFailure(String errorMessage) {
        return new SystemTask(
                this.id,
                this.tenantId,
                this.taskType,
                this.payload,
                TaskStatus.FAILED,
                this.retryCount,
                errorMessage,
                this.createdBy,
                this.createdAt,
                OffsetDateTime.now(),
                this.executedAt,
                OffsetDateTime.now(),
                this.deletedAt
        );
    }

    /**
     * 重试任务
     *
     * <p>重试机制是任务系统可靠性的重要保障。通过合理的重试策略，
     * 系统可以自动恢复临时性错误，大大提高处理成功率。</p>
     */
    public SystemTask retry() {
        if (this.retryCount >= 3) { // 最大重试3次
            throw new IllegalStateException("任务已达到最大重试次数");
        }

        return new SystemTask(
                this.id,
                this.tenantId,
                this.taskType,
                this.payload,
                TaskStatus.PENDING,
                this.retryCount + 1,
                this.lastError,
                this.createdBy,
                this.createdAt,
                OffsetDateTime.now(),
                this.executedAt,
                null, // 重置完成时间
                this.deletedAt
        );
    }

    /**
     * 检查任务是否可以重试
     */
    public boolean canRetry() {
        return this.status == TaskStatus.FAILED && this.retryCount < 3;
    }

    /**
     * 检查任务是否已完成（成功或失败）
     */
    public boolean isCompleted() {
        return this.status == TaskStatus.SUCCESS || this.status == TaskStatus.FAILED;
    }

    /**
     * 计算任务执行持续时间（毫秒）
     */
    public Long getExecutionDurationMs() {
        if (this.executedAt == null || this.finishedAt == null) {
            return null;
        }
        return java.time.Duration.between(this.executedAt, this.finishedAt).toMillis();
    }

    /**
     * 获取任务的优先级
     *
     * <p>不同类型的任务有不同的紧急程度。比如用户注册验证邮件应该优先处理，
     * 而数据统计报告可以稍后处理。</p>
     */
    public TaskPriority getPriority() {
        return switch (this.taskType) {
            case "EMAIL_SEND" -> TaskPriority.HIGH;
            case "DOCUMENT_PARSE" -> TaskPriority.MEDIUM;
            case "VECTOR_GENERATION" -> TaskPriority.MEDIUM;
            case "DATA_CLEANUP" -> TaskPriority.LOW;
            default -> TaskPriority.MEDIUM;
        };
    }
}