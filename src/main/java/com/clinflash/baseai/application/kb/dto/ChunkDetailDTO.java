package com.clinflash.baseai.application.kb.dto;

import java.util.List;

/**
 * <h2>知识块详情数据传输对象</h2>
 *
 * <p>包含知识块的完整信息，用于详情查看和编辑场景。</p>
 *
 * @param id            知识块ID
 * @param documentId    所属文档ID
 * @param documentTitle 文档标题
 * @param chunkNo       块序号
 * @param text          文本内容
 * @param langCode      语言代码
 * @param tokenSize     Token数量
 * @param vectorVersion 向量版本
 * @param tags          标签信息列表
 * @param hasEmbedding  是否有向量
 */
public record ChunkDetailDTO(
        Long id,
        Long documentId,
        String documentTitle,
        Integer chunkNo,
        String text,
        String langCode,
        Integer tokenSize,
        Integer vectorVersion,
        List<TagInfo> tags,
        Boolean hasEmbedding
) {
    /**
     * 标签信息
     *
     * @param id   标签ID
     * @param name 标签名称
     */
    public record TagInfo(Long id, String name) {
    }

    /**
     * 获取文本预览
     *
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    public String getTextPreview(int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    /**
     * 检查是否有标签
     *
     * @return true如果有标签
     */
    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }
}