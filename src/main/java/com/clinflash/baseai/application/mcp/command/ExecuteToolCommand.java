package com.clinflash.baseai.application.mcp.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;

/**
 * <h2>执行工具命令</h2>
 *
 * <p>这是MCP系统执行工具的统一入口命令。它封装了执行工具所需的
 * 全部上下文信息，包括调用参数、执行配置和追踪信息。</p>
 *
 * <p><b>设计考量：</b></p>
 * <p>命令中包含了执行模式（同步/异步）和超时控制，这让调用方
 * 能够根据具体场景选择最适合的执行策略。</p>
 */
public record ExecuteToolCommand(
        @NotNull(message = "租户ID不能为空")
        Long tenantId,

        @NotNull(message = "用户ID不能为空")
        Long userId,

        Long threadId,

        Long flowRunId,

        @Valid
        @NotNull(message = "工具参数不能为空")
        Map<String, Object> params,

        boolean asyncMode,

        @Positive(message = "超时时间必须为正数")
        Integer timeoutSeconds
) {
    /**
     * 获取有效的超时时间，提供默认值
     */
    public int getEffectiveTimeout() {
        return timeoutSeconds != null && timeoutSeconds > 0 ? timeoutSeconds : 30;
    }

    /**
     * 检查是否为流程中的调用
     */
    public boolean isFlowExecution() {
        return flowRunId != null;
    }

    /**
     * 检查是否为对话中的调用
     */
    public boolean isChatExecution() {
        return threadId != null;
    }

    /**
     * 获取安全的参数副本
     */
    public Map<String, Object> getSafeParams() {
        return params != null ? Map.copyOf(params) : Map.of();
    }
}