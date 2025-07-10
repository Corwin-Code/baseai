package com.clinflash.baseai.application.kb.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * <h2>为知识块添加标签命令</h2>
 *
 * @param chunkId    知识块ID
 * @param tagIds     标签ID集合
 * @param operatorId 操作人ID
 */
public record AddChunkTagsCommand(
        @NotNull(message = "知识块ID不能为空")
        Long chunkId,

        @NotNull(message = "标签ID集合不能为空")
        @Size(min = 1, message = "至少需要一个标签")
        Set<Long> tagIds,

        @NotNull(message = "操作人ID不能为空")
        Long operatorId
) {
}