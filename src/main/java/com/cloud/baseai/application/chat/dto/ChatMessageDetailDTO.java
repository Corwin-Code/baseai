package com.cloud.baseai.application.chat.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * <h2>对话消息详细信息数据传输对象</h2>
 *
 * <p>记录了从用户输入到AI生成回复的完整过程，
 * 包括性能指标、引用来源、工具调用等所有相关信息。</p>
 *
 * @param id        消息的唯一标识符
 * @param threadId  所属对话线程的ID，建立消息与对话上下文的关联
 * @param role      消息角色，如"user"（用户）、"assistant"（AI助手）、"system"（系统）
 * @param content   消息的文本内容，是对话的核心信息载体
 * @param toolCall  工具调用的详细信息，记录AI使用外部工具的过程和结果
 * @param tokenIn   输入Token数量，用于成本计算和性能分析
 * @param tokenOut  输出Token数量，反映生成内容的复杂度
 * @param latencyMs 消息处理延迟（毫秒），衡量系统响应速度
 * @param citations 知识引用列表，提供回答依据的可追溯性
 * @param createdAt 消息创建时间，用于时序分析和审计追踪
 */
public record ChatMessageDetailDTO(
        Long id,
        Long threadId,
        String role,
        String content,
        String toolCall,
        Integer tokenIn,
        Integer tokenOut,
        Integer latencyMs,
        List<ChatCitationDTO> citations,
        OffsetDateTime createdAt
) {
}