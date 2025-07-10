package com.clinflash.baseai.application.kb.command;

import java.util.Set;

/**
 * <h2>混合智能搜索命令</h2>
 *
 * <p>系统通过可配置的权重参数来平衡两种搜索方式的贡献度。向量搜索擅长理解语义相关性，
 * 能找到概念相似但用词不同的内容；关键词搜索则确保重要术语的精确匹配。
 * 最终结果通过加权合并算法产生，既保证了召回率也维持了精确度。</p>
 *
 * @param tenantId     租户ID
 * @param query        用户查询文本
 * @param modelCode    向量模型代码
 * @param topK         返回结果的最大数量
 * @param threshold    相似度阈值
 * @param vectorWeight 向量搜索权重(0.0-1.0)，值越大越偏重语义理解，值越小越偏重精确匹配
 * @param tagIds       标签过滤条件，可选的结果范围限定
 * @param documentIds  文档过滤条件，可选的搜索范围限定
 */
public record HybridSearchCommand(
        Long tenantId,
        String query,
        String modelCode,
        int topK,
        float threshold,
        float vectorWeight,
        Set<Long> tagIds,
        Set<Long> documentIds
) {
}