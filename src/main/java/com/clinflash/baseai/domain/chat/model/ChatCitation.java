package com.clinflash.baseai.domain.chat.model;

/**
 * <h2>对话引用领域模型</h2>
 *
 * <p>对话引用记录了AI回复与知识库内容之间的关联关系。
 * 这是RAG系统的重要组成部分，确保AI的回答有据可查，增强可信度。</p>
 */
public record ChatCitation(
        Long messageId,
        Long chunkId,
        Float score,
        String modelCode
) {
    /**
     * 创建新的引用关系
     */
    public static ChatCitation create(Long messageId, Long chunkId, Float score, String modelCode) {
        return new ChatCitation(messageId, chunkId, score, modelCode);
    }

    /**
     * 设置消息ID
     */
    public ChatCitation setMessageId(Long messageId) {
        return new ChatCitation(messageId, this.chunkId, this.score, this.modelCode);
    }

    /**
     * 检查是否为高质量引用
     */
    public boolean isHighQuality() {
        return score != null && score >= 0.8f;
    }
}