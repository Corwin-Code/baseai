package com.cloud.baseai.application.flow.command;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * <h2>执行流程命令</h2>
 *
 * <p>这个命令启动流程的实际执行。它包含了执行所需的输入数据、
 * 执行模式、超时设置等关键参数。</p>
 */
public record ExecuteFlowCommand(
        @NotNull(message = "流程定义ID不能为空")
        Long definitionId,

        Map<String, Object> inputData, // 流程的输入数据

        Boolean asyncMode, // 是否异步执行

        Integer timeoutMinutes, // 超时时间（分钟）

        @NotNull(message = "操作者ID不能为空")
        Long operatorId
) {
    /**
     * 获取有效的超时时间
     */
    public int getEffectiveTimeoutMinutes() {
        if (timeoutMinutes == null || timeoutMinutes <= 0) {
            return 30; // 默认30分钟
        }
        return Math.min(timeoutMinutes, 240); // 最大4小时
    }

    /**
     * 检查是否为异步模式
     */
    public Boolean asyncMode() {
        return Boolean.TRUE.equals(asyncMode);
    }

    /**
     * 获取安全的输入数据
     */
    public Map<String, Object> getSafeInputData() {
        return inputData != null ? inputData : Map.of();
    }
}