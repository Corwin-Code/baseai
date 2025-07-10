package com.clinflash.baseai.infrastructure.repository.chat.spring;

import com.clinflash.baseai.domain.chat.model.MessageRole;
import com.clinflash.baseai.infrastructure.persistence.chat.entity.ChatMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * <h2>对话消息Spring Data JPA仓储接口</h2>
 *
 * <p>Spring Data JPA的魔法在于它能够根据方法名自动生成SQL查询。
 * 这大大减少了样板代码，让开发者能够专注于业务逻辑而不是SQL编写。</p>
 *
 * <p><b>命名规范解读：</b></p>
 * <ul>
 * <li><code>findBy</code>: 查询操作的前缀</li>
 * <li><code>ThreadId</code>: 根据threadId字段查询</li>
 * <li><code>And</code>: 逻辑AND连接符</li>
 * <li><code>DeletedAtIsNull</code>: deletedAt字段为null的条件(软删除过滤)</li>
 * <li><code>OrderBy</code>: 排序指令</li>
 * <li><code>CreatedAtDesc</code>: 按createdAt字段降序排列</li>
 * </ul>
 */
@Repository
public interface SpringChatMessageRepo extends JpaRepository<ChatMessageEntity, Long> {

    // =================== 基础查询方法 ===================

    /**
     * 查找线程下的所有未删除消息，支持分页
     */
    Page<ChatMessageEntity> findByThreadIdAndDeletedAtIsNull(Long threadId, Pageable pageable);

    /**
     * 查找线程下最新的N条消息，按时间降序
     */
    List<ChatMessageEntity> findByThreadIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            Long threadId, Pageable pageable);

    /**
     * 统计线程下的消息总数(排除已删除)
     */
    long countByThreadIdAndDeletedAtIsNull(Long threadId);

    /**
     * 统计线程下特定角色的消息数量
     */
    long countByThreadIdAndRoleAndDeletedAtIsNull(Long threadId, MessageRole role);

    // =================== 复杂查询方法(使用@Query注解) ===================

    /**
     * 统计租户下指定时间后的消息数量
     *
     * <p>这个查询展示了如何使用JOIN操作连接多个表。通过连接chat_messages和chat_threads表，
     * 我们能够根据租户ID过滤消息，即使消息表本身不直接包含租户信息。</p>
     */
    @Query("""
            SELECT COUNT(m) FROM ChatMessageEntity m
            JOIN ChatThreadEntity t ON m.threadId = t.id
            WHERE t.tenantId = :tenantId
            AND m.createdAt >= :since
            AND m.deletedAt IS NULL
            AND t.deletedAt IS NULL
            """)
    long countMessagesByTenantIdSince(@Param("tenantId") Long tenantId,
                                      @Param("since") OffsetDateTime since);

    /**
     * 统计租户下指定角色和时间范围的消息数量
     */
    @Query("""
            SELECT COUNT(m) FROM ChatMessageEntity m
            JOIN ChatThreadEntity t ON m.threadId = t.id
            WHERE t.tenantId = :tenantId
            AND m.role = :role
            AND m.createdAt >= :since
            AND m.deletedAt IS NULL
            AND t.deletedAt IS NULL
            """)
    long countMessagesByTenantIdAndRoleSince(@Param("tenantId") Long tenantId,
                                             @Param("role") MessageRole role,
                                             @Param("since") OffsetDateTime since);

    /**
     * 统计用户指定时间后的消息数量
     */
    @Query("""
            SELECT COUNT(m) FROM ChatMessageEntity m
            JOIN ChatThreadEntity t ON m.threadId = t.id
            WHERE t.userId = :userId
            AND m.createdAt >= :since
            AND m.deletedAt IS NULL
            AND t.deletedAt IS NULL
            """)
    long countMessagesByUserIdSince(@Param("userId") Long userId,
                                    @Param("since") OffsetDateTime since);

    /**
     * 统计用户指定角色和时间范围的消息数量
     */
    @Query("""
            SELECT COUNT(m) FROM ChatMessageEntity m
            JOIN ChatThreadEntity t ON m.threadId = t.id
            WHERE t.userId = :userId
            AND m.role = :role
            AND m.createdAt >= :since
            AND m.deletedAt IS NULL
            AND t.deletedAt IS NULL
            """)
    long countMessagesByUserIdAndRoleSince(@Param("userId") Long userId,
                                           @Param("role") MessageRole role,
                                           @Param("since") OffsetDateTime since);

    /**
     * 计算租户的平均响应时间
     *
     * <p>这个查询只计算助手消息的延迟，因为用户消息通常没有处理延迟。
     * AVG函数会自动忽略NULL值，确保计算结果的准确性。</p>
     */
    @Query("""
            SELECT AVG(m.latencyMs) FROM ChatMessageEntity m
            JOIN ChatThreadEntity t ON m.threadId = t.id
            WHERE t.tenantId = :tenantId
            AND m.role = 'ASSISTANT'
            AND m.latencyMs IS NOT NULL
            AND m.createdAt >= :since
            AND m.deletedAt IS NULL
            AND t.deletedAt IS NULL
            """)
    Double getAverageResponseTimeByTenantIdSince(@Param("tenantId") Long tenantId,
                                                 @Param("since") OffsetDateTime since);

    // =================== 修改操作方法 ===================

    /**
     * 软删除指定消息之后的所有助手回复
     *
     * <p>@Modifying注解告诉Spring Data这是一个修改操作，不是查询操作。
     * 这对于UPDATE和DELETE语句是必需的。</p>
     */
    @Modifying
    @Query("""
            UPDATE ChatMessageEntity m
            SET m.deletedAt = :deletedAt
            WHERE m.threadId = :threadId
            AND m.role = 'ASSISTANT'
            AND m.createdAt > :afterTime
            AND m.deletedAt IS NULL
            """)
    void softDeleteAssistantResponsesAfter(@Param("threadId") Long threadId,
                                           @Param("afterTime") OffsetDateTime afterTime,
                                           @Param("deletedAt") OffsetDateTime deletedAt);

    /**
     * 软删除线程下的所有消息
     */
    @Modifying
    @Query("""
            UPDATE ChatMessageEntity m
            SET m.deletedAt = :deletedAt
            WHERE m.threadId = :threadId
            AND m.deletedAt IS NULL
            """)
    void softDeleteAllByThreadId(@Param("threadId") Long threadId,
                                 @Param("deletedAt") OffsetDateTime deletedAt);
}