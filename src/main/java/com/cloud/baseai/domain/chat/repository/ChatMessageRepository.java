package com.cloud.baseai.domain.chat.repository;

import com.cloud.baseai.domain.chat.model.ChatMessage;
import com.cloud.baseai.domain.chat.model.MessageRole;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>对话消息仓储接口</h2>
 */
public interface ChatMessageRepository {

    /**
     * 保存消息
     */
    ChatMessage save(ChatMessage message);

    /**
     * 根据ID查找消息
     */
    Optional<ChatMessage> findById(Long id);

    /**
     * 查找线程的消息
     */
    List<ChatMessage> findByThreadId(Long threadId, int page, int size);

    /**
     * 按时间倒序查找线程消息
     */
    List<ChatMessage> findByThreadIdOrderByCreatedAtDesc(Long threadId, int limit);

    /**
     * 统计线程的消息数量
     */
    int countByThreadId(Long threadId);

    /**
     * 统计特定角色的消息数量
     */
    int countByThreadIdAndRole(Long threadId, MessageRole role);

    /**
     * 统计租户的消息数量
     */
    int countByTenantIdSince(Long tenantId, OffsetDateTime since);

    /**
     * 统计租户特定角色的消息数量
     */
    int countByTenantIdAndRoleSince(Long tenantId, MessageRole role, OffsetDateTime since);

    /**
     * 统计用户的消息数量
     */
    int countByUserIdSince(Long userId, OffsetDateTime since);

    /**
     * 统计用户特定角色的消息数量
     */
    int countByUserIdAndRoleSince(Long userId, MessageRole role, OffsetDateTime since);

    /**
     * 统计最近消息数量（用于速率限制）
     */
    int countRecentMessages(Long userId, int windowMinutes);

    /**
     * 获取平均响应时间
     */
    Double getAverageResponseTime(Long tenantId, OffsetDateTime since);

    /**
     * 删除指定消息之后的助手回复
     */
    void deleteAssistantResponseAfter(Long messageId);

    /**
     * 软删除线程的所有消息
     */
    void softDeleteByThreadId(Long threadId, Long operatorId);
}