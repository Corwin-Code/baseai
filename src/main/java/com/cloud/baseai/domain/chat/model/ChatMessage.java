package com.cloud.baseai.domain.chat.model;

import java.time.OffsetDateTime;

/**
 * <h2>对话消息领域模型</h2>
 *
 * <p>对话消息是AI交互的基本单元。每条消息都有明确的角色（用户、助手、系统），
 * 完整的内容记录，以及详细的执行统计信息。这些信息共同构成了
 * AI学习和改进的重要数据基础。</p>
 */
public record ChatMessage(
        Long id,
        Long threadId,
        MessageRole role,
        String content,
        String toolCall,
        Integer tokenIn,
        Integer tokenOut,
        Integer latencyMs,
        Long parentId,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {
    /**
     * 创建新消息
     */
    public static ChatMessage create(Long threadId, MessageRole role, String content,
                                     String toolCall, Long createdBy, Long userId) {
        return new ChatMessage(
                null,
                threadId,
                role,
                content,
                toolCall,
                null,
                null,
                null,
                null,
                createdBy,
                OffsetDateTime.now(),
                null
        );
    }

    /**
     * 更新使用统计
     */
    public ChatMessage updateUsage(Integer tokenIn, Integer tokenOut, Integer latencyMs) {
        return new ChatMessage(
                this.id,
                this.threadId,
                this.role,
                this.content,
                this.toolCall,
                tokenIn,
                tokenOut,
                latencyMs,
                this.parentId,
                this.createdBy,
                this.createdAt,
                this.deletedAt
        );
    }

    /**
     * 检查是否为用户消息
     */
    public boolean isUserMessage() {
        return role == MessageRole.USER;
    }

    /**
     * 检查是否为助手消息
     */
    public boolean isAssistantMessage() {
        return role == MessageRole.ASSISTANT;
    }

    /**
     * 检查是否包含工具调用
     */
    public boolean hasToolCall() {
        return toolCall != null && !toolCall.trim().isEmpty();
    }
}