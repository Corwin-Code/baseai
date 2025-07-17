package com.cloud.baseai.application.kb.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * <h2>文档详情数据传输对象</h2>
 *
 * <p>提供了文档的完整生命周期信息，从上传创建到AI处理状态，
 * 再到后续的搜索能力，为用户呈现了一个全方位的文档画像。</p>
 *
 * @param id              文档唯一标识符
 * @param title           文档标题
 * @param sourceType      来源类型，如PDF、MARKDOWN、WORD等
 * @param sourceUri       原始文档的URI地址，便于追溯来源
 * @param mimeType        MIME类型，标识文档的格式信息
 * @param langCode        文档主要语言代码，影响AI处理策略
 * @param parsingStatus   AI解析处理状态，如"成功"、"失败"、"处理中"等
 * @param chunkCount      文档被分割的知识块数量，反映文档的复杂度
 * @param sizeCategory    文档大小分类，如"小型"、"中型"、"大型"
 * @param availableModels 支持的AI模型列表，决定可用的搜索能力
 * @param createdBy       创建者用户ID
 * @param creatorName     创建者用户名，便于用户识别
 * @param createdAt       创建时间
 * @param updatedAt       最后更新时间
 */
public record DocumentDetailDTO(
        Long id,
        String title,
        String sourceType,
        String sourceUri,
        String mimeType,
        String langCode,
        String parsingStatus,
        Integer chunkCount,
        String sizeCategory,
        List<String> availableModels,
        Long createdBy,
        String creatorName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}