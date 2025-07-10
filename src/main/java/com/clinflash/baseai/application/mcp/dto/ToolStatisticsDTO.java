package com.clinflash.baseai.application.mcp.dto;

import java.util.List;

/**
 * <h2>MCP工具系统统计信息汇总</h2>
 *
 * <p>通过这个统计数据传输对象，可以全面了解工具生态的健康状况、
 * 使用模式和性能表现。</p>
 *
 * <p><b>统计维度解析：</b></p>
 * <ul>
 * <li><b>规模指标：</b>工具总数和启用状态，反映系统的服务能力</li>
 * <li><b>活跃度指标：</b>执行次数统计，显示系统的实际使用情况</li>
 * <li><b>质量指标：</b>成功率统计，评估系统的稳定性和可靠性</li>
 * <li><b>热点分析：</b>热门工具排行，揭示用户行为模式</li>
 * </ul>
 *
 * <p><b>决策支持价值：</b></p>
 * <p>这些统计数据不仅用于监控，更是重要的决策支持工具。
 * 低成功率可能表明需要改进工具的稳定性；高频使用的工具需要重点保障；
 * 时间维度的分析有助于发现使用模式和趋势变化。</p>
 *
 * <p><b>租户隔离：</b>支持租户级别的统计，确保多租户环境下的数据隔离，
 * 让每个组织都能看到自己专属的使用情况。</p>
 *
 * @param tenantId             租户标识符，支持多租户数据隔离，null表示全局统计
 * @param totalTools           工具总数量，反映系统提供的服务丰富度
 * @param enabledTools         已启用的工具数量，表示当前可用的服务数量
 * @param totalExecutions      总执行次数，反映系统的整体活跃度
 * @param successfulExecutions 成功执行次数，用于计算成功率
 * @param failedExecutions     失败执行次数，监控系统稳定性的重要指标
 * @param successRate          成功率（0.0-1.0），系统可靠性的关键指标
 * @param topTools             热门工具列表，按使用频率排序的工具统计
 * @param timeRange            统计时间范围，如"1h"、"1d"、"7d"、"30d"等
 */
public record ToolStatisticsDTO(
        Long tenantId,
        int totalTools,
        int enabledTools,
        int totalExecutions,
        int successfulExecutions,
        int failedExecutions,
        double successRate,
        List<ToolUsageDTO> topTools,
        String timeRange
) {
}