package com.cloud.baseai.application.kb.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <h2>向量搜索命令</h2>
 *
 * <p>执行基于向量相似度的知识库搜索。</p>
 *
 * @param tenantId       租户ID
 * @param query          搜索查询文本
 * @param modelCode      使用的模型代码
 * @param topK           返回结果数量
 * @param threshold      相似度阈值（0-1）
 * @param includeDeleted 是否包含已删除的内容
 */
public record VectorSearchCommand(
        @NotNull(message = "租户ID不能为空")
        Long tenantId,

        @NotBlank(message = "搜索查询不能为空")
        @Size(max = 1000, message = "查询文本长度不能超过1000字符")
        String query,

        @NotBlank(message = "模型代码不能为空")
        String modelCode,

        Integer topK,
        Float threshold,
        Boolean includeDeleted
) {

    /**
     * 构造函数，设置默认值
     */
    public VectorSearchCommand {
        if (topK == null || topK <= 0) {
            topK = 10;
        }
        if (topK > 100) {
            topK = 100; // 限制最大返回数量
        }
        if (threshold == null) {
            threshold = 0.7f;
        }
        if (includeDeleted == null) {
            includeDeleted = false;
        }
    }

    /**
     * 验证阈值范围
     *
     * @return true如果阈值有效
     */
    public boolean isThresholdValid() {
        return threshold >= 0.0f && threshold <= 1.0f;
    }
}