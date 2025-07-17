package com.cloud.baseai.infrastructure.repository.chat;

import com.cloud.baseai.domain.chat.model.ChatThread;
import com.cloud.baseai.domain.chat.repository.ChatThreadRepository;
import com.cloud.baseai.infrastructure.persistence.chat.entity.ChatThreadEntity;
import com.cloud.baseai.infrastructure.persistence.chat.mapper.ChatMapper;
import com.cloud.baseai.infrastructure.repository.chat.spring.SpringChatThreadRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>对话线程仓储实现</h2>
 */
@Repository
public class ChatThreadJpaRepository implements ChatThreadRepository {

    private final SpringChatThreadRepo springRepo;
    private final ChatMapper mapper;

    public ChatThreadJpaRepository(SpringChatThreadRepo springRepo, ChatMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public ChatThread save(ChatThread thread) {
        ChatThreadEntity entity;

        if (thread.id() == null) {
            entity = mapper.toEntity(thread);
        } else {
            entity = springRepo.findById(thread.id())
                    .orElse(mapper.toEntity(thread));
            entity.updateFromDomain(thread);
        }

        ChatThreadEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ChatThread> findById(Long id) {
        return springRepo.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public List<ChatThread> findByTenantIdAndUserId(Long tenantId, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<ChatThreadEntity> entityPage = springRepo.findByTenantIdAndUserIdAndDeletedAtIsNull(
                tenantId, userId, pageable);
        return mapper.toThreadDomainList(entityPage.getContent());
    }

    @Override
    public int countByTenantIdAndUserId(Long tenantId, Long userId) {
        return (int) springRepo.countByTenantIdAndUserIdAndDeletedAtIsNull(tenantId, userId);
    }

    @Override
    public List<ChatThread> searchByTitle(Long tenantId, Long userId, String title, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<ChatThreadEntity> entityPage = springRepo
                .findByTenantIdAndUserIdAndTitleContainingIgnoreCaseAndDeletedAtIsNull(
                        tenantId, userId, title, pageable);
        return mapper.toThreadDomainList(entityPage.getContent());
    }

    @Override
    public long countByTenantIdAndUserIdAndTitleContaining(Long tenantId, Long userId, String title) {
        return springRepo.countByTenantIdAndUserIdAndTitleContainingIgnoreCaseAndDeletedAtIsNull(
                tenantId, userId, title);
    }

    @Override
    public int countByTenantId(Long tenantId) {
        return (int) springRepo.countByTenantIdAndDeletedAtIsNull(tenantId);
    }

    @Override
    public boolean softDelete(Long id, Long operatorId) {
        Optional<ChatThreadEntity> entityOpt = springRepo.findById(id);
        if (entityOpt.isEmpty() || entityOpt.get().getDeletedAt() != null) {
            return false;
        }

        ChatThreadEntity entity = entityOpt.get();
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setUpdatedBy(operatorId);
        springRepo.save(entity);

        return true;
    }

    @Override
    public long count() {
        return springRepo.countAllActive();
    }
}