package com.clinflash.baseai.infrastructure.repository.chat.spring;

import com.clinflash.baseai.infrastructure.persistence.chat.entity.ChatCitationEntity;
import com.clinflash.baseai.infrastructure.repository.chat.ChatCitationJpaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>对话引用Spring Data JPA仓储接口</h2>
 *
 * <p>这个接口展示了如何处理复合主键的实体。在Spring Data JPA中，
 * 复合主键需要特别的处理方式，通常使用@IdClass或@EmbeddedId注解。</p>
 *
 * <p><b>复合主键的优缺点：</b></p>
 * <p><strong>优点：</strong>天然防止重复记录，语义清晰，查询效率高</p>
 * <p><strong>缺点：</strong>代码复杂度增加，外键关联较复杂</p>
 * <p>在引用关系这种多对多关联场景中，复合主键是最佳选择。</p>
 */
@Repository
public interface SpringChatCitationRepo extends JpaRepository<ChatCitationEntity, Object> {

    // =================== 基础查询方法 ===================

    /**
     * 根据消息ID查找所有引用，按相关性分数降序排列
     *
     * <p>OrderByScoreDesc确保最相关的知识内容排在前面。
     * 这对用户体验很重要：用户首先看到的应该是最可靠的参考来源。</p>
     */
    List<ChatCitationEntity> findByMessageIdOrderByScoreDesc(Long messageId);

    /**
     * 根据知识块ID查找所有引用它的消息
     */
    List<ChatCitationEntity> findByChunkId(Long chunkId);

    /**
     * 根据模型查找引用
     *
     * <p>不同的embedding模型可能产生不同的检索结果。
     * 这个方法可以用来比较不同模型的检索效果。</p>
     */
    List<ChatCitationEntity> findByModelCode(String modelCode);

    // =================== 统计查询方法 ===================

    /**
     * 查找最常被引用的知识块
     *
     * <p>这个查询使用GROUP BY聚合统计每个知识块的引用次数，
     * 并计算平均相关性分数。这种统计信息对知识库优化很有价值。</p>
     */
    @Query("""
            SELECT ChatCitationJpaRepository$ChunkCitationStats(
                c.chunkId,
                COUNT(c),
                AVG(c.score)
            )
            FROM ChatCitationEntity c
            GROUP BY c.chunkId
            ORDER BY COUNT(c) DESC
            """)
    List<ChatCitationJpaRepository.ChunkCitationStats> findMostCitedChunks(@Param("limit") int limit);

    /**
     * 获取租户的引用质量统计
     *
     * <p>这个查询通过JOIN操作连接多个表，统计租户级别的引用质量分布。
     * CASE WHEN语句用于将连续的分数值分组到离散的质量等级中。</p>
     */
    @Query("""
            SELECT ChatCitationJpaRepository$CitationQualityStats(
                COUNT(c),
                AVG(c.score),
                SUM(CASE WHEN c.score >= 0.8 THEN 1 ELSE 0 END),
                SUM(CASE WHEN c.score >= 0.5 AND c.score < 0.8 THEN 1 ELSE 0 END),
                SUM(CASE WHEN c.score < 0.5 THEN 1 ELSE 0 END)
            )
            FROM ChatCitationEntity c
            JOIN ChatMessageEntity m ON c.messageId = m.id
            JOIN ChatThreadEntity t ON m.threadId = t.id
            WHERE t.tenantId = :tenantId
            AND m.deletedAt IS NULL
            AND t.deletedAt IS NULL
            """)
    ChatCitationJpaRepository.CitationQualityStats getCitationQualityStatsByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 统计消息的引用数量
     */
    long countByMessageId(Long messageId);

    /**
     * 统计知识块被引用的次数
     */
    long countByChunkId(Long chunkId);

    // =================== 修改操作方法 ===================

    /**
     * 删除线程相关的所有引用
     *
     * <p>这是一个复杂的删除操作，需要通过JOIN找到线程下的所有消息，
     * 然后删除这些消息的所有引用关系。</p>
     */
    @Modifying
    @Query("""
            DELETE FROM ChatCitationEntity c
            WHERE c.messageId IN (
                SELECT m.id FROM ChatMessageEntity m
                WHERE m.threadId = :threadId
            )
            """)
    void deleteByThreadId(@Param("threadId") Long threadId);

    /**
     * 删除知识块相关的所有引用
     *
     * <p>当知识块被删除时，需要清理所有相关的引用关系。
     * 这确保了数据库的引用完整性。</p>
     */
    @Modifying
    @Query("DELETE FROM ChatCitationEntity c WHERE c.chunkId = :chunkId")
    void deleteByChunkId(@Param("chunkId") Long chunkId);

    /**
     * 清理低质量引用
     *
     * <p>定期清理相关性分数过低的引用可以提高系统性能，
     * 减少噪音数据对分析的影响。</p>
     */
    @Modifying
    @Query("DELETE FROM ChatCitationEntity c WHERE c.score < :threshold")
    void deleteLowQualityCitations(@Param("threshold") Float threshold);

    // =================== 高级分析查询 ===================

    /**
     * 获取知识块的引用趋势
     *
     * <p>这个查询按时间维度统计知识块的引用变化，
     * 可以用来分析知识内容的生命周期和价值变化。</p>
     */
    @Query("""
            SELECT
                DATE(m.createdAt) as date,
                COUNT(c) as citationCount
            FROM ChatCitationEntity c
            JOIN ChatMessageEntity m ON c.messageId = m.id
            WHERE c.chunkId = :chunkId
            AND m.createdAt >= :since
            AND m.deletedAt IS NULL
            GROUP BY DATE(m.createdAt)
            ORDER BY DATE(m.createdAt)
            """)
    List<Object[]> findCitationTrendByChunkId(@Param("chunkId") Long chunkId,
                                              @Param("since") java.time.OffsetDateTime since);

    /**
     * 查找相似引用模式
     *
     * <p>通过分析哪些知识块经常被一起引用，可以发现知识之间的关联关系，
     * 这对知识图谱构建和推荐系统很有价值。</p>
     */
    @Query("""
            SELECT
                c1.chunkId as chunkId1,
                c2.chunkId as chunkId2,
                COUNT(*) as coOccurrence
            FROM ChatCitationEntity c1
            JOIN ChatCitationEntity c2 ON c1.messageId = c2.messageId
            WHERE c1.chunkId < c2.chunkId
            GROUP BY c1.chunkId, c2.chunkId
            HAVING COUNT(*) >= :minOccurrence
            ORDER BY COUNT(*) DESC
            """)
    List<Object[]> findCoOccurringChunks(@Param("minOccurrence") int minOccurrence);
}