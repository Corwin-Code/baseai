package com.clinflash.baseai.application.kb.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * <h2>重新生成向量命令</h2>
 *
 * <p>为指定文档重新生成所有知识块的向量表示。</p>
 *
 * @param documentId 文档ID
 * @param modelCode  使用的模型代码
 * @param operatorId 操作人ID
 */
public record GenerateEmbeddingsCommand(
        @NotNull(message = "文档ID不能为空")
        Long documentId,

        @NotBlank(message = "模型代码不能为空")
        String modelCode,

        @NotNull(message = "操作人ID不能为空")
        Long operatorId
) {
}