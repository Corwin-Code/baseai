package com.cloud.baseai.application.chat.dto;

import java.util.List;

/**
 * <h2>智能对话系统统计信息汇总</h2>
 *
 * <p>这些统计数据不仅展示了系统的使用规模，更重要的是揭示了用户的行为模式、
 * 系统的服务质量和业务价值的实现情况。</p>
 *
 * @param tenantId          租户标识符
 * @param userId            用户标识符，null表示租户级统计，非null表示用户级统计
 * @param totalThreads      对话线程总数
 * @param totalMessages     消息总数，包括用户消息和AI回复
 * @param userMessages      用户发送的消息数量
 * @param assistantMessages AI助手的回复数量
 * @param avgResponseTimeMs 平均响应时间（毫秒）
 * @param topModels         热门AI模型的使用统计排行
 * @param timeRange         统计的时间范围，如"1d"、"7d"、"30d"等
 */
public record ChatStatisticsDTO(
        Long tenantId,
        Long userId,
        int totalThreads,
        int totalMessages,
        int userMessages,
        int assistantMessages,
        long avgResponseTimeMs,
        List<ModelUsageDTO> topModels,
        String timeRange
) {
}