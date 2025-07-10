package com.clinflash.baseai.application.kb.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * <h2>文本搜索命令</h2>
 *
 * <p>基于关键词的全文检索搜索。</p>
 *
 * @param tenantId    租户ID
 * @param keywords    搜索关键词
 * @param tagIds      标签过滤（可选）
 * @param documentIds 文档范围过滤（可选）
 * @param page        页码
 * @param size        每页大小
 */
public record TextSearchCommand(
        @NotNull(message = "租户ID不能为空")
        Long tenantId,

        @NotBlank(message = "搜索关键词不能为空")
        @Size(max = 200, message = "关键词长度不能超过200字符")
        String keywords,

        Set<Long> tagIds,
        Set<Long> documentIds,

        Integer page,
        Integer size
) {

    /**
     * 构造函数，设置默认值
     */
    public TextSearchCommand {
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size <= 0) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }
    }

    /**
     * 检查是否有标签过滤
     *
     * @return true如果有标签过滤
     */
    public boolean hasTagFilter() {
        return tagIds != null && !tagIds.isEmpty();
    }

    /**
     * 检查是否有文档范围过滤
     *
     * @return true如果有文档过滤
     */
    public boolean hasDocumentFilter() {
        return documentIds != null && !documentIds.isEmpty();
    }
}