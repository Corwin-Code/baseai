package com.clinflash.baseai.application.kb.dto;

import java.util.List;

/**
 * <h2>搜索结果数据传输对象</h2>
 *
 * <p>向量搜索或文本搜索的结果对象，包含相似度分数和高亮信息。</p>
 *
 * @param chunkId       知识块ID
 * @param documentId    文档ID
 * @param documentTitle 文档标题
 * @param text          知识块文本
 * @param score         相似度分数（0-1）
 * @param confidence    置信度等级
 * @param highlights    高亮片段列表
 * @param tags          相关标签
 */
public record SearchResultDTO(
        Long chunkId,
        Long documentId,
        String documentTitle,
        String text,
        Float score,
        String confidence,
        List<String> highlights,
        List<String> tags
) {
    /**
     * 获取置信度分类
     *
     * @return 置信度分类（高/中/低）
     */
    public String getConfidenceLevel() {
        if (score == null) {
            return "未知";
        }
        if (score >= 0.9f) {
            return "高";
        } else if (score >= 0.7f) {
            return "中";
        } else {
            return "低";
        }
    }

    /**
     * 检查是否有高亮
     *
     * @return true如果有高亮片段
     */
    public boolean hasHighlights() {
        return highlights != null && !highlights.isEmpty();
    }

    /**
     * 获取格式化的分数字符串
     *
     * @return 百分比格式的分数
     */
    public String getFormattedScore() {
        if (score == null) {
            return "N/A";
        }
        return String.format("%.1f%%", score * 100);
    }
}