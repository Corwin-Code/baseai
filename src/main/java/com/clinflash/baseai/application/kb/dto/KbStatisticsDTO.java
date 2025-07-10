package com.clinflash.baseai.application.kb.dto;

import java.util.Map;

/**
 * <h2>知识库统计信息汇总</h2>
 *
 * <p>通过这些数据，可以了解知识库的规模、内容分布、处理效率等关键指标，
 * 为系统优化和资源规划提供数据支撑。</p>
 *
 * @param tenantId          租户ID，统计范围的标识
 * @param totalDocuments    文档总数量
 * @param totalChunks       知识块总数量，反映内容的颗粒度
 * @param totalEmbeddings   向量总数量，反映AI处理的完成度
 * @param totalTags         标签总数量，反映知识组织的丰富度
 * @param documentsByType   按文档类型分组的统计，如PDF、Word、Markdown等
 * @param documentsByStatus 按处理状态分组的统计，如成功、失败、处理中等
 * @param embeddingsByModel 按AI模型分组的向量统计，反映模型使用分布
 */
public record KbStatisticsDTO(
        Long tenantId,
        int totalDocuments,
        int totalChunks,
        int totalEmbeddings,
        int totalTags,
        Map<String, Integer> documentsByType,
        Map<String, Integer> documentsByStatus,
        Map<String, Integer> embeddingsByModel
) {
}