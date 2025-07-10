package com.clinflash.baseai.infrastructure.repository.chat;

import com.clinflash.baseai.domain.chat.model.ChatMessage;
import com.clinflash.baseai.domain.chat.model.MessageRole;
import com.clinflash.baseai.domain.chat.repository.ChatMessageRepository;
import com.clinflash.baseai.infrastructure.persistence.chat.entity.ChatMessageEntity;
import com.clinflash.baseai.infrastructure.persistence.chat.mapper.ChatMapper;
import com.clinflash.baseai.infrastructure.repository.chat.spring.SpringChatMessageRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>对话消息仓储实现</h2>
 *
 * <p>这个实现类是连接领域模型和数据持久化的桥梁。它将抽象的业务操作转换为具体的数据库操作，
 * 同时确保数据的一致性和完整性。就像图书馆管理员一样，负责消息的存储、检索和管理。</p>
 *
 * <p><b>设计要点：</b></p>
 * <p>仓储模式的核心价值在于将业务逻辑与数据访问技术解耦。这意味着如果将来需要从JPA切换到
 * MongoDB或其他数据存储方案，只需要更换这个实现类，而不需要修改任何业务逻辑代码。</p>
 */
@Repository
public class ChatMessageJpaRepository implements ChatMessageRepository {

    private final SpringChatMessageRepo springRepo;
    private final ChatMapper mapper;

    /**
     * 构造函数注入依赖
     *
     * <p>这里体现了依赖倒置原则：高层模块(业务逻辑)不依赖低层模块(数据访问)，
     * 两者都依赖抽象(Repository接口)。</p>
     */
    public ChatMessageJpaRepository(SpringChatMessageRepo springRepo, ChatMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public ChatMessage save(ChatMessage message) {
        ChatMessageEntity entity;

        if (message.id() == null) {
            // 新消息：直接转换为实体
            entity = mapper.toEntity(message);
        } else {
            // 更新现有消息：先查找再更新，保持数据库中的其他字段不变
            entity = springRepo.findById(message.id())
                    .orElse(mapper.toEntity(message));

            // 这里可以添加字段级别的更新逻辑
            updateEntityFromDomain(entity, message);
        }

        ChatMessageEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ChatMessage> findById(Long id) {
        return springRepo.findById(id)
                .filter(entity -> entity.getDeletedAt() == null) // 软删除过滤
                .map(mapper::toDomain);
    }

    @Override
    public List<ChatMessage> findByThreadId(Long threadId, int page, int size) {
        // 按创建时间正序排列，保持对话的时间顺序
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<ChatMessageEntity> entityPage = springRepo.findByThreadIdAndDeletedAtIsNull(threadId, pageable);
        return mapper.toMessageDomainList(entityPage.getContent());
    }

    @Override
    public List<ChatMessage> findByThreadIdOrderByCreatedAtDesc(Long threadId, int limit) {
        // 获取最新的N条消息，用于构建上下文或显示最近对话
        List<ChatMessageEntity> entities = springRepo.findByThreadIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                threadId, PageRequest.of(0, limit));
        return mapper.toMessageDomainList(entities);
    }

    @Override
    public int countByThreadId(Long threadId) {
        return (int) springRepo.countByThreadIdAndDeletedAtIsNull(threadId);
    }

    @Override
    public int countByThreadIdAndRole(Long threadId, MessageRole role) {
        return (int) springRepo.countByThreadIdAndRoleAndDeletedAtIsNull(threadId, role);
    }

    @Override
    public int countByTenantIdSince(Long tenantId, OffsetDateTime since) {
        // 通过JOIN查询统计租户下的消息数量
        // 这个查询涉及跨表操作，展示了Repository如何处理复杂查询
        return (int) springRepo.countMessagesByTenantIdSince(tenantId, since);
    }

    @Override
    public int countByTenantIdAndRoleSince(Long tenantId, MessageRole role, OffsetDateTime since) {
        return (int) springRepo.countMessagesByTenantIdAndRoleSince(tenantId, role, since);
    }

    @Override
    public int countByUserIdSince(Long userId, OffsetDateTime since) {
        return (int) springRepo.countMessagesByUserIdSince(userId, since);
    }

    @Override
    public int countByUserIdAndRoleSince(Long userId, MessageRole role, OffsetDateTime since) {
        return (int) springRepo.countMessagesByUserIdAndRoleSince(userId, role, since);
    }

    @Override
    public int countRecentMessages(Long userId, int windowMinutes) {
        // 速率限制检查：统计指定时间窗口内的消息数量
        OffsetDateTime since = OffsetDateTime.now().minusMinutes(windowMinutes);
        return (int) springRepo.countMessagesByUserIdSince(userId, since);
    }

    @Override
    public Double getAverageResponseTime(Long tenantId, OffsetDateTime since) {
        // 计算平均响应时间，这对性能监控很重要
        return springRepo.getAverageResponseTimeByTenantIdSince(tenantId, since);
    }

    @Override
    public void deleteAssistantResponseAfter(Long messageId) {
        // 重新生成功能：删除指定消息之后的所有助手回复
        ChatMessageEntity userMessage = springRepo.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("消息不存在: " + messageId));

        springRepo.softDeleteAssistantResponsesAfter(
                userMessage.getThreadId(),
                userMessage.getCreatedAt(),
                OffsetDateTime.now()
        );
    }

    @Override
    public void softDeleteByThreadId(Long threadId, Long operatorId) {
        // 批量软删除线程下的所有消息
        springRepo.softDeleteAllByThreadId(threadId, OffsetDateTime.now());
    }

    // =================== 私有辅助方法 ===================

    /**
     * 从领域对象更新实体字段
     *
     * <p>这个方法处理实体更新的细节，确保只更新需要修改的字段，
     * 而保持其他字段(如创建时间、ID等)不变。</p>
     */
    private void updateEntityFromDomain(ChatMessageEntity entity, ChatMessage domain) {
        entity.setThreadId(domain.threadId());
        entity.setRole(domain.role());
        entity.setContent(domain.content());
        entity.setToolCall(domain.toolCall());
        entity.setTokenIn(domain.tokenIn());
        entity.setTokenOut(domain.tokenOut());
        entity.setLatencyMs(domain.latencyMs());
        entity.setParentId(domain.parentId());
        entity.setCreatedBy(domain.createdBy());
        // 注意：不更新createdAt和id，这些是不可变的
        entity.setDeletedAt(domain.deletedAt());
    }
}