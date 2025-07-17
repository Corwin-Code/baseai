package com.cloud.baseai.application.kb.command;

import java.util.List;

/**
 * <h2>批量向量生成命令</h2>
 *
 * <p>用于批量向量生成操作。
 * 当需要为大量文档重新生成向量表示时，使用批量处理可以显著提升效率。</p>
 *
 * @param documentIds 需要处理的文档ID列表
 * @param modelCode   AI嵌入模型代码，决定了向量的维度和语义理解能力
 * @param operatorId  操作人员ID
 */
public record BatchVectorGenerationCommand(
        List<Long> documentIds,
        String modelCode,
        Long operatorId
) {
}