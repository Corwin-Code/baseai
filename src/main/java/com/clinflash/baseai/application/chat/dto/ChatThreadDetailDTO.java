package com.clinflash.baseai.application.chat.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * <h2>对话线程详细信息数据传输对象</h2>
 *
 * @param id                    对话线程的唯一标识符
 * @param title                 对话线程的标题
 * @param defaultModel          线程使用的默认AI模型
 * @param temperature           创意程度参数（0.0-1.0）
 * @param flowSnapshotId        关联的业务流程快照ID
 * @param messageCount          消息总数
 * @param userMessageCount      用户消息数量
 * @param assistantMessageCount AI助手回复数量
 * @param userId                对话创建者的用户ID
 * @param userName              创建者的用户名
 * @param recentMessages        最近的消息列表
 * @param createdAt             对话线程的创建时间
 * @param updatedAt             最后更新时间
 */
public record ChatThreadDetailDTO(
        Long id,
        String title,
        String defaultModel,
        Float temperature,
        Long flowSnapshotId,
        int messageCount,
        int userMessageCount,
        int assistantMessageCount,
        Long userId,
        String userName,
        List<ChatMessageDTO> recentMessages,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}