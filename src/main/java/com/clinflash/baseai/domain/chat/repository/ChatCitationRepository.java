package com.clinflash.baseai.domain.chat.repository;

import com.clinflash.baseai.domain.chat.model.ChatCitation;

import java.util.List;

/**
 * <h2>对话引用仓储接口</h2>
 */
public interface ChatCitationRepository {

    /**
     * 保存引用关系
     */
    ChatCitation save(ChatCitation citation);

    /**
     * 查找消息的引用
     */
    List<ChatCitation> findByMessageId(Long messageId);

    /**
     * 删除线程的所有引用
     */
    void deleteByThreadId(Long threadId);
}