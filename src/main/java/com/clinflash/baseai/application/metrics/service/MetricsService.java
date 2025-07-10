package com.clinflash.baseai.application.metrics.service;

import java.time.OffsetDateTime;

/**
 * <h2>性能指标监控服务接口</h2>
 *
 * <p>系统性能数据收集和分析的标准契约，记录着每一个重要操作的性能表现。</p>
 *
 * <p><b>应用价值：</b>通过持续的性能监控，可以预警潜在问题、
 * 优化系统配置、制定扩容计划，确保知识库系统始终保持最佳性能状态。</p>
 *
 * <p><b>实现建议：</b>实现类应该考虑异步数据收集、批量数据传输、
 * 以及适当的采样策略，避免监控本身成为性能负担。</p>
 */
public interface MetricsService {
    /**
     * 记录业务操作的执行指标
     *
     * @param operation  操作名称，如"document.upload"、"search.vector"等
     * @param durationMs 操作执行时间，单位毫秒
     * @param success    操作是否成功
     */
    void recordOperation(String operation, long durationMs, boolean success);

    /**
     * 记录搜索操作的专项指标
     *
     * @param durationMs  搜索执行时间，单位毫秒
     * @param resultCount 搜索结果数量
     */
    void recordSearch(long durationMs, int resultCount);

    /**
     * 记录系统资源使用情况
     *
     * @param resourceType 资源类型，如"memory"、"cpu"、"disk"等
     * @param usagePercent 使用率百分比
     */
    void recordResourceUsage(String resourceType, double usagePercent);

    /**
     * 记录用户反馈信息
     *
     * @param messageId 被评价的消息标识符
     * @param rating    用户评分，通常为1-5分
     * @param comment   用户评价文本
     */
    void recordFeedback(Long messageId, Integer rating, String comment);

    /**
     * 获取指定时间范围内的性能统计
     *
     * @param operation 操作名称
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 性能统计数据
     */
    Object getMetrics(String operation, OffsetDateTime startTime, OffsetDateTime endTime);
}