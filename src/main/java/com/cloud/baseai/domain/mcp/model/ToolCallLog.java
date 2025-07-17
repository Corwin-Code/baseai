package com.cloud.baseai.domain.mcp.model;

import com.cloud.baseai.infrastructure.persistence.mcp.entity.enums.ToolCallStatus;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * <h2>工具调用日志领域对象</h2>
 *
 * <p>记录每次工具调用的详细信息，用于审计、监控和故障排查。
 * 这个对象是系统可观测性的重要组成部分。</p>
 */
public record ToolCallLog(
        Long id,
        Long toolId,
        Long tenantId,
        Long userId,
        Long threadId,
        Long flowRunId,
        Map<String, Object> params,
        Map<String, Object> result,
        ToolCallStatus status,
        String errorMsg,
        Integer latencyMs,
        OffsetDateTime createdAt
) {
    /**
     * 创建新调用日志的工厂方法
     */
    public static ToolCallLog create(Long toolId, Long tenantId, Long userId,
                                     Long threadId, Long flowRunId, Map<String, Object> params) {
        return new ToolCallLog(
                null, // ID由数据库生成
                toolId,
                tenantId,
                userId,
                threadId,
                flowRunId,
                params,
                null, // 结果稍后更新
                ToolCallStatus.STARTED, // 初始状态
                null,
                null,
                OffsetDateTime.now()
        );
    }

    /**
     * 更新调用状态和结果
     */
    public ToolCallLog updateStatus(ToolCallStatus status, Map<String, Object> result, String errorMsg) {
        Integer latency = null;
        if (createdAt != null) {
            latency = (int) (System.currentTimeMillis() -
                    createdAt.toInstant().toEpochMilli());
        }

        return new ToolCallLog(
                id,
                toolId,
                tenantId,
                userId,
                threadId,
                flowRunId,
                params,
                result,
                status,
                errorMsg,
                latency,
                createdAt
        );
    }

    // =================== 业务规则方法 ===================

    /**
     * 检查调用是否成功
     */
    public boolean isSuccessful() {
        return "SUCCESS".equals(status);
    }

    /**
     * 检查调用是否失败
     */
    public boolean isFailed() {
        return "FAILED".equals(status) || "ERROR".equals(status);
    }

    /**
     * 检查是否还在执行中
     */
    public boolean isRunning() {
        return "STARTED".equals(status) || "RUNNING".equals(status);
    }

    /**
     * 获取执行时长的友好显示
     */
    public String getLatencyDisplay() {
        if (latencyMs == null) {
            return "未知";
        }
        if (latencyMs < 1000) {
            return latencyMs + "ms";
        }
        return String.format("%.2fs", latencyMs / 1000.0);
    }

    /**
     * 检查是否为流程调用
     */
    public boolean isFlowCall() {
        return flowRunId != null;
    }

    /**
     * 检查是否为对话调用
     */
    public boolean isChatCall() {
        return threadId != null;
    }
}