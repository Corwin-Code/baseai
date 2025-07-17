package com.cloud.baseai.infrastructure.repository.chat.spring;

import com.cloud.baseai.infrastructure.persistence.chat.entity.ChatThreadEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * <h2>对话线程Spring Data JPA仓储</h2>
 */
@Repository
public interface SpringChatThreadRepo extends JpaRepository<ChatThreadEntity, Long> {

    Page<ChatThreadEntity> findByTenantIdAndUserIdAndDeletedAtIsNull(
            Long tenantId, Long userId, Pageable pageable);

    long countByTenantIdAndUserIdAndDeletedAtIsNull(Long tenantId, Long userId);

    Page<ChatThreadEntity> findByTenantIdAndUserIdAndTitleContainingIgnoreCaseAndDeletedAtIsNull(
            Long tenantId, Long userId, String title, Pageable pageable);

    long countByTenantIdAndUserIdAndTitleContainingIgnoreCaseAndDeletedAtIsNull(
            Long tenantId, Long userId, String title);

    long countByTenantIdAndDeletedAtIsNull(Long tenantId);

    @Query("SELECT COUNT(t) FROM ChatThreadEntity t WHERE t.deletedAt IS NULL")
    long countAllActive();
}