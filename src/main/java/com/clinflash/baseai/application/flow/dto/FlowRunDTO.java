package com.clinflash.baseai.application.flow.dto;

import java.time.OffsetDateTime;

/**
 * <h2>流程运行传输对象</h2>
 *
 * <p>流程运行实例的基本信息，用于运行历史列表和状态监控。</p>
 */
public record FlowRunDTO(
        Long id,
        Long snapshotId,
        Long userId,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        Long durationMs
) {
    /**
     * 检查是否正在运行
     */
    public boolean isRunning() {
        return "运行中".equals(status);
    }

    /**
     * 检查是否已完成
     */
    public boolean isFinished() {
        return "成功".equals(status) || "失败".equals(status);
    }

    /**
     * 获取执行时间的友好显示
     */
    public String getDurationDisplay() {
        if (durationMs == null) {
            return isRunning() ? "运行中..." : "-";
        }

        long seconds = durationMs / 1000;
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分" + (seconds % 60) + "秒";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "小时" + minutes + "分钟";
        }
    }

    /**
     * 获取状态对应的颜色样式
     */
    public String getStatusColor() {
        return switch (status) {
            case "成功" -> "success";
            case "失败" -> "error";
            case "运行中" -> "processing";
            case "待执行" -> "default";
            default -> "default";
        };
    }
}