package com.clinflash.baseai.domain.chat.model;

/**
 * <h2>每日使用量领域模型</h2>
 *
 * <p>记录每日的Token使用量和费用统计，为计费和监控提供数据支持。</p>
 */
public record ChatUsageDaily(
        java.time.LocalDate statDate,
        Long tenantId,
        String modelCode,
        Long promptTokens,
        Long completionTokens,
        java.math.BigDecimal costUsd
) {
    /**
     * 创建新的使用量记录
     */
    public static ChatUsageDaily create(java.time.LocalDate statDate, Long tenantId,
                                        String modelCode, Long promptTokens,
                                        Long completionTokens, java.math.BigDecimal costUsd) {
        return new ChatUsageDaily(statDate, tenantId, modelCode, promptTokens, completionTokens, costUsd);
    }

    /**
     * 累加使用量
     */
    public ChatUsageDaily addUsage(Long additionalPromptTokens, Long additionalCompletionTokens,
                                   java.math.BigDecimal additionalCost) {
        return new ChatUsageDaily(
                this.statDate,
                this.tenantId,
                this.modelCode,
                this.promptTokens + additionalPromptTokens,
                this.completionTokens + additionalCompletionTokens,
                this.costUsd.add(additionalCost)
        );
    }

    /**
     * 获取总Token数
     */
    public Long getTotalTokens() {
        return promptTokens + completionTokens;
    }
}