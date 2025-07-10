package com.clinflash.baseai.domain.chat.repository;

import com.clinflash.baseai.domain.chat.model.ChatThread;

import java.util.List;
import java.util.Optional;

/**
 * <h2>对话线程仓储接口</h2>
 */
public interface ChatThreadRepository {

    /**
     * 保存对话线程
     */
    ChatThread save(ChatThread thread);

    /**
     * 根据ID查找线程
     */
    Optional<ChatThread> findById(Long id);

    /**
     * 查找用户的对话线程
     */
    List<ChatThread> findByTenantIdAndUserId(Long tenantId, Long userId, int page, int size);

    /**
     * 统计用户的线程数量
     */
    int countByTenantIdAndUserId(Long tenantId, Long userId);

    /**
     * 按标题搜索线程
     */
    List<ChatThread> searchByTitle(Long tenantId, Long userId, String title, int page, int size);

    /**
     * 统计标题匹配的线程数量
     */
    long countByTenantIdAndUserIdAndTitleContaining(Long tenantId, Long userId, String title);

    /**
     * 统计租户的线程数量
     */
    int countByTenantId(Long tenantId);

    /**
     * 软删除线程
     */
    boolean softDelete(Long id, Long operatorId);

    /**
     * 统计总数
     */
    long count();
}