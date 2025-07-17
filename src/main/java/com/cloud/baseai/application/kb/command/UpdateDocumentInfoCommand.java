package com.cloud.baseai.application.kb.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * <h2>更新文档信息命令</h2>
 *
 * @param documentId 文档ID
 * @param title      新标题
 * @param langCode   新语言代码
 * @param operatorId 操作人ID
 */
public record UpdateDocumentInfoCommand(
        @NotNull(message = "文档ID不能为空")
        Long documentId,

        @NotBlank(message = "文档标题不能为空")
        @Size(max = 256, message = "文档标题长度不能超过256字符")
        String title,

        String langCode,

        @NotNull(message = "操作人ID不能为空")
        Long operatorId
) {
}