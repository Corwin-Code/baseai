package com.clinflash.baseai.domain.kb.model;

import java.time.OffsetDateTime;

/**
 * <h2>知识块实体</h2>
 *
 * <p>文档分块后的基本单元，是RAG检索的核心对象。
 * 每个知识块包含一段连贯的文本内容，并可以生成对应的向量表示。</p>
 *
 * <p><b>业务规则：</b></p>
 * <ul>
 * <li>同一文档内的知识块按 chunkNo 顺序排列</li>
 * <li>向量版本号用于支持模型升级和重新生成</li>
 * <li>Token 数量用于成本计算和上下文窗口管理</li>
 * </ul>
 *
 * @param id            知识块唯一标识
 * @param documentId    所属文档ID
 * @param chunkNo       在文档中的序号（从0开始）
 * @param text          知识块文本内容
 * @param langCode      语言代码
 * @param tokenSize     Token数量
 * @param vectorVersion 向量版本号
 * @param createdBy     创建人ID
 * @param updatedBy     修改人ID
 * @param createdAt     创建时间
 * @param updatedAt     更新时间
 * @param deletedAt     软删除时间
 */
public record Chunk(
        Long id,
        Long documentId,
        Integer chunkNo,
        String text,
        String langCode,
        Integer tokenSize,
        Integer vectorVersion,
        Long createdBy,
        Long updatedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt
) {

    /**
     * 创建新知识块的工厂方法
     *
     * @param documentId 文档ID
     * @param chunkNo    块序号
     * @param text       文本内容
     * @param langCode   语言代码
     * @param tokenSize  Token数量
     * @param createdBy  创建人ID
     * @return 新知识块实例
     */
    public static Chunk create(Long documentId, Integer chunkNo, String text,
                               String langCode, Integer tokenSize, Long createdBy) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Chunk(
                null, documentId, chunkNo, text, langCode, tokenSize, 1,
                createdBy, null, now, now, null
        );
    }

    /**
     * 递增向量版本号
     *
     * <p>当需要重新生成向量时调用，确保向量版本的一致性。</p>
     *
     * @param updatedBy 修改人ID
     * @return 更新版本号后的知识块实例
     */
    public Chunk incrementVectorVersion(Long updatedBy) {
        return new Chunk(
                this.id, this.documentId, this.chunkNo, this.text, this.langCode,
                this.tokenSize, this.vectorVersion + 1, this.createdBy, updatedBy,
                this.createdAt, OffsetDateTime.now(), this.deletedAt
        );
    }

    /**
     * 更新文本内容
     *
     * <p>重新解析时可能需要更新知识块内容。</p>
     *
     * @param newText      新文本内容
     * @param newTokenSize 新Token数量
     * @param updatedBy    修改人ID
     * @return 更新后的知识块实例
     */
    public Chunk updateContent(String newText, Integer newTokenSize, Long updatedBy) {
        return new Chunk(
                this.id, this.documentId, this.chunkNo, newText, this.langCode,
                newTokenSize, this.vectorVersion, this.createdBy, updatedBy,
                this.createdAt, OffsetDateTime.now(), this.deletedAt
        );
    }

    /**
     * 检查是否已删除
     *
     * @return true如果已被软删除
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 获取简短的文本预览
     *
     * @param maxLength 最大长度
     * @return 截断后的文本预览
     */
    public String getTextPreview(int maxLength) {
        if (this.text == null || this.text.length() <= maxLength) {
            return this.text;
        }
        return this.text.substring(0, maxLength) + "...";
    }
}