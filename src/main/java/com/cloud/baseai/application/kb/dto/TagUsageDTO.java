package com.cloud.baseai.application.kb.dto;

/**
 * <h2>标签使用情况统计</h2>
 *
 * <p>标签使用统计不仅反映了知识库的主题分布，更重要的是揭示了用户的关注热点，
 * 为内容运营和系统优化提供了宝贵的数据洞察。</p>
 *
 * @param id         标签的唯一标识符
 * @param name       标签名称，用户可见的标签文本
 * @param remark     标签备注说明，提供额外的上下文信息
 * @param usageCount 标签的使用次数，反映该标签的热门程度
 */
public record TagUsageDTO(
        Long id,
        String name,
        String remark,
        Long usageCount
) {
}