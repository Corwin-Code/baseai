package com.clinflash.baseai.application.kb.dto;

import java.util.List;

/**
 * <h2>向量生成过程统计</h2>
 *
 * <p>详细记录了生成过程中的成功与失败情况。</p>
 *
 * @param successCount 成功生成向量的数量
 * @param failureCount 生成失败的数量
 * @param errors       详细的错误信息列表，每个错误包含具体失败的原因
 */
public record VectorGenerationStats(
        int successCount,
        int failureCount,
        List<String> errors
) {
}