package com.clinflash.baseai.application.kb.command;

import jakarta.validation.constraints.NotNull;

/**
 * <h2>删除文档命令</h2>
 *
 * @param documentId 文档ID
 * @param operatorId 操作人ID
 * @param cascade    是否级联删除知识块和向量
 */
public record DeleteDocumentCommand(
        @NotNull(message = "文档ID不能为空")
        Long documentId,

        @NotNull(message = "操作人ID不能为空")
        Long operatorId,

        Boolean cascade
) {

    /**
     * 构造函数，设置默认值
     */
    public DeleteDocumentCommand {
        if (cascade == null) {
            cascade = true;
        }
    }
}