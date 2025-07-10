package com.clinflash.baseai.application.kb.dto;

import java.time.OffsetDateTime;

/**
 * <h2>文档数据传输对象</h2>
 *
 * <p>用于API层与应用服务层之间的文档数据传输。
 * 包含文档的基本信息和状态，但不包含大量的内容数据。</p>
 *
 * @param id            文档ID
 * @param title         文档标题
 * @param sourceType    来源类型（PDF、URL、MARKDOWN等）
 * @param sourceUri     源地址
 * @param mimeType      MIME类型
 * @param langCode      语言代码
 * @param parsingStatus 解析状态标签
 * @param chunkCount    知识块数量
 * @param createdBy     创建人ID
 * @param creatorName   创建人姓名（可选）
 * @param createdAt     创建时间
 * @param updatedAt     更新时间
 */
public record DocumentDTO(
        Long id,
        String title,
        String sourceType,
        String sourceUri,
        String mimeType,
        String langCode,
        String parsingStatus,
        Integer chunkCount,
        Long createdBy,
        String creatorName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * 创建基础文档DTO的静态工厂方法
     *
     * @param id            文档ID
     * @param title         标题
     * @param sourceType    来源类型
     * @param parsingStatus 解析状态
     * @param chunkCount    块数量
     * @return 基础文档DTO
     */
    public static DocumentDTO basic(Long id, String title, String sourceType,
                                    String parsingStatus, Integer chunkCount) {
        return new DocumentDTO(
                id, title, sourceType, null, null, null, parsingStatus,
                chunkCount, null, null, null, null
        );
    }

    /**
     * 检查文档是否解析成功
     *
     * @return true如果解析成功
     */
    public boolean isParsed() {
        return "解析成功".equals(parsingStatus);
    }

    /**
     * 获取文档大小估算（基于知识块数量）
     *
     * @return 大小类别
     */
    public String getSizeCategory() {
        if (chunkCount == null || chunkCount == 0) {
            return "空文档";
        } else if (chunkCount <= 10) {
            return "小型";
        } else if (chunkCount <= 100) {
            return "中型";
        } else {
            return "大型";
        }
    }
}