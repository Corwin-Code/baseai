package com.clinflash.baseai.infrastructure.repository.audit.spring;

import com.clinflash.baseai.infrastructure.persistence.audit.entity.SysAuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>系统审计日志Spring Data JPA仓储</h2>
 *
 * <p>这个接口是我们数据访问层的核心。</p>
 *
 * <p><b>查询优化考虑：</b></p>
 * <p>在设计这些查询方法时，我们特别考虑了性能优化。审计日志通常数据量很大，
 * 所以我们使用了合适的索引、分页查询、以及优化的SQL语句来确保查询效率。</p>
 */
@Repository
public interface SpringSysAuditLogRepo extends JpaRepository<SysAuditLogEntity, Long> {

    /**
     * 根据用户ID和时间范围查询审计日志
     *
     * <p>这个查询方法是审计系统中最常用的方法之一。
     * 它能够根据多个条件快速找到相关的审计记录。我们使用JPQL来编写这个查询，
     * 确保它能够充分利用数据库索引。</p>
     *
     * <p><b>查询逻辑说明：</b></p>
     * <p>这个查询支持灵活的参数组合：</p>
     * <ul>
     * <li>如果userId为null，则查询所有用户的记录</li>
     * <li>如果时间参数为null，则不限制时间范围</li>
     * <li>如果actions为null或空，则不限制操作类型</li>
     * </ul>
     *
     * @param userId    用户ID，null表示查询所有用户
     * @param startTime 开始时间，null表示不限制开始时间
     * @param endTime   结束时间，null表示不限制结束时间
     * @param actions   操作类型列表，null或空表示所有操作类型
     * @param pageable  分页参数，包含页码、页大小和排序信息
     * @return 分页的查询结果
     */
    @Query("SELECT a FROM SysAuditLogEntity a WHERE " +
            "(:userId IS NULL OR a.userId = :userId) " +
            "AND (:startTime IS NULL OR a.createdAt >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdAt <= :endTime) " +
            "AND (:actions IS NULL OR SIZE(:actions) = 0 OR a.action IN :actions) " +
            "ORDER BY a.createdAt DESC")
    Page<SysAuditLogEntity> findUserActions(
            @Param("userId") Long userId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("actions") List<String> actions,
            Pageable pageable
    );

    /**
     * 根据租户ID查询审计日志，按创建时间倒序排列
     *
     * <p>在多租户系统中，数据隔离是非常重要的。这个方法确保每个租户
     * 只能看到自己的审计日志。</p>
     *
     * <p><b>安全考虑：</b></p>
     * <p>这个查询方法是多租户数据隔离的重要组成部分。它确保了数据的安全性，
     * 防止租户之间的数据泄露。</p>
     *
     * @param tenantId 租户ID
     * @param pageable 分页参数
     * @return 分页的查询结果
     */
    Page<SysAuditLogEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    /**
     * 根据时间范围查询审计日志
     *
     * <p>这个方法专门用于按时间范围查询，在生成定期报告时特别有用。
     * 比如生成月度报告、季度报告等。我们使用自定义JPQL来确保查询的准确性。</p>
     *
     * <p><b>索引优化：</b></p>
     * <p>由于时间范围查询非常常见，我们在created_at字段上创建了专门的索引，
     * 这个查询能够快速定位到指定时间范围内的记录。</p>
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 分页的查询结果
     */
    @Query("SELECT a FROM SysAuditLogEntity a WHERE " +
            "a.createdAt >= :startTime AND a.createdAt <= :endTime " +
            "ORDER BY a.createdAt DESC")
    Page<SysAuditLogEntity> findByTimeRange(
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            Pageable pageable
    );

    /**
     * 统计指定时间范围内的审计日志数量
     *
     * <p>这个方法提供快速的统计功能，不需要加载具体的数据内容。</p>
     *
     * <p><b>性能优势：</b></p>
     * <p>使用COUNT查询比加载所有数据然后计算数量要高效得多，
     * 特别是在数据量很大的情况下。</p>
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计数量
     */
    @Query("SELECT COUNT(a) FROM SysAuditLogEntity a WHERE " +
            "a.createdAt >= :startTime AND a.createdAt <= :endTime")
    long countByTimeRange(
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    /**
     * 按操作类型统计审计日志数量
     *
     * <p>这个统计查询帮助我们了解系统中各种操作的频率分布。
     * 就像分析网站访问日志一样，我们可以了解哪些操作最常见，
     * 哪些操作比较少见，这对系统优化很有价值。</p>
     *
     * <p><b>返回数据格式：</b></p>
     * <p>返回的Object[]数组中，第一个元素是操作类型（String），
     * 第二个元素是该操作类型的数量（Long）。</p>
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计结果列表，每个元素包含[操作类型, 数量]
     */
    @Query("SELECT a.action, COUNT(a) FROM SysAuditLogEntity a WHERE " +
            "a.createdAt >= :startTime AND a.createdAt <= :endTime " +
            "GROUP BY a.action " +
            "ORDER BY COUNT(a) DESC")
    List<Object[]> countByActionAndTimeRange(
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    /**
     * 按用户统计审计日志数量
     *
     * <p>这个统计查询帮助我们了解各个用户的活跃程度。
     * 这对于用户行为分析、异常检测都很有帮助。比如，
     * 如果某个用户的操作次数突然大幅增加，可能需要关注。</p>
     *
     * <p><b>数据过滤：</b></p>
     * <p>我们只统计userId不为null的记录，因为null表示系统操作，
     * 不属于特定用户。</p>
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计结果列表，每个元素包含[用户ID, 数量]
     */
    @Query("SELECT a.userId, COUNT(a) FROM SysAuditLogEntity a WHERE " +
            "a.createdAt >= :startTime AND a.createdAt <= :endTime " +
            "AND a.userId IS NOT NULL " +
            "GROUP BY a.userId " +
            "ORDER BY COUNT(a) DESC")
    List<Object[]> countByUserAndTimeRange(
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    /**
     * 删除指定时间之前的审计日志
     *
     * <p>这个方法用于数据清理，删除过期的审计日志。这是一个危险的操作，
     * 就像销毁过期文件一样，需要确保符合法规要求和公司政策。</p>
     *
     * <p><b>重要提醒：</b></p>
     * <p>@Modifying注解表示这是一个修改操作，不是查询操作。
     * 执行这种操作时需要特别小心，建议在删除前先备份数据。</p>
     *
     * <p><b>性能考虑：</b></p>
     * <p>大批量删除可能影响数据库性能，建议在低峰期执行，
     * 或者分批删除以减少对系统的影响。</p>
     *
     * @param cutoffTime 截止时间，在此时间之前的日志将被删除
     */
    @Modifying
    @Query("DELETE FROM SysAuditLogEntity a WHERE a.createdAt < :cutoffTime")
    void deleteOldAuditLogs(@Param("cutoffTime") OffsetDateTime cutoffTime);

    /**
     * 根据操作类型和租户ID查询审计日志
     *
     * <p>这个方法提供了按操作类型过滤的查询功能。当我们需要专门分析
     * 某种类型的操作时，这个方法会很有用。比如，专门查看登录操作、
     * 数据修改操作等。</p>
     *
     * @param action   操作类型
     * @param tenantId 租户ID
     * @param pageable 分页参数
     * @return 分页的查询结果
     */
    Page<SysAuditLogEntity> findByActionAndTenantIdOrderByCreatedAtDesc(
            String action, Long tenantId, Pageable pageable);

    /**
     * 根据目标类型和目标ID查询审计日志
     *
     * <p>这个方法用于查询针对特定对象的所有操作。比如，
     * 查看某个文档的所有操作历史、某个用户账户的所有变更记录等。
     * 这对于问题追踪和审计调查非常重要。</p>
     *
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @param pageable   分页参数
     * @return 分页的查询结果
     */
    Page<SysAuditLogEntity> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            String targetType, Long targetId, Pageable pageable);

    /**
     * 根据IP地址查询审计日志
     *
     * <p>这个方法用于安全分析，查看来自特定IP地址的所有操作。
     * 当我们怀疑某个IP地址存在异常行为时，这个查询会很有帮助。</p>
     *
     * @param ipAddress IP地址
     * @param pageable  分页参数
     * @return 分页的查询结果
     */
    Page<SysAuditLogEntity> findByIpAddressOrderByCreatedAtDesc(String ipAddress, Pageable pageable);

    /**
     * 查询失败的操作记录
     *
     * <p>这个方法专门查询失败的操作，这对于问题诊断和安全监控很重要。
     * 通过分析失败的操作，我们可以发现系统问题或潜在的安全威胁。</p>
     *
     * @param tenantId  租户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 分页的查询结果
     */
    @Query("SELECT a FROM SysAuditLogEntity a WHERE " +
            "a.tenantId = :tenantId " +
            "AND a.resultStatus = 'FAILED' " +
            "AND (:startTime IS NULL OR a.createdAt >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdAt <= :endTime) " +
            "ORDER BY a.createdAt DESC")
    Page<SysAuditLogEntity> findFailedOperations(
            @Param("tenantId") Long tenantId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            Pageable pageable
    );

    /**
     * 查询高级别的审计日志
     *
     * <p>这个方法查询高级别（WARN、ERROR、FATAL）的审计日志，
     * 这些记录通常表示需要关注的重要事件。</p>
     *
     * @param tenantId 租户ID
     * @param pageable 分页参数
     * @return 分页的查询结果
     */
    @Query("SELECT a FROM SysAuditLogEntity a WHERE " +
            "a.tenantId = :tenantId " +
            "AND a.logLevel IN ('WARN', 'ERROR', 'FATAL') " +
            "ORDER BY a.createdAt DESC")
    Page<SysAuditLogEntity> findHighLevelLogs(
            @Param("tenantId") Long tenantId,
            Pageable pageable
    );

    /**
     * 统计指定用户在指定时间范围内的操作次数
     *
     * <p>这个方法用于监控用户活动频率，可以帮助发现异常行为。
     * 比如，如果某个用户的操作频率突然异常增高，可能需要进一步调查。</p>
     *
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 操作次数
     */
    @Query("SELECT COUNT(a) FROM SysAuditLogEntity a WHERE " +
            "a.userId = :userId " +
            "AND a.createdAt >= :startTime " +
            "AND a.createdAt <= :endTime")
    long countUserOperations(
            @Param("userId") Long userId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    /**
     * 查询最近的审计日志
     *
     * <p>这个方法用于快速查看系统的最新活动，类似于查看最新的新闻动态。
     * 在系统监控和实时分析中很有用。</p>
     *
     * @param tenantId 租户ID
     * @param limit    返回记录数量限制
     * @return 最近的审计日志列表
     */
    @Query("SELECT a FROM SysAuditLogEntity a WHERE " +
            "a.tenantId = :tenantId " +
            "ORDER BY a.createdAt DESC " +
            "LIMIT :limit")
    List<SysAuditLogEntity> findRecentLogs(
            @Param("tenantId") Long tenantId,
            @Param("limit") int limit
    );

    /**
     * 根据关键词搜索审计日志详情
     *
     * <p>这个方法提供全文搜索功能，可以在审计日志的详情中搜索关键词。
     * 这对于问题调查和审计分析很有帮助。</p>
     *
     * <p><b>搜索说明：</b></p>
     * <p>我们使用ILIKE操作符进行模糊匹配，支持大小写不敏感的搜索。
     * 这个查询会在detail字段中搜索包含关键词的记录。</p>
     *
     * @param tenantId 租户ID
     * @param keyword  搜索关键词
     * @param pageable 分页参数
     * @return 分页的查询结果
     */
    @Query("SELECT a FROM SysAuditLogEntity a WHERE " +
            "a.tenantId = :tenantId " +
            "AND (a.detail ILIKE %:keyword% OR a.action ILIKE %:keyword%) " +
            "ORDER BY a.createdAt DESC")
    Page<SysAuditLogEntity> searchByKeyword(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 查询系统操作记录
     *
     * <p>这个方法专门查询系统自动执行的操作（userId为null的记录）。
     * 这些操作通常是定时任务、系统维护等自动化操作。</p>
     *
     * @param tenantId 租户ID
     * @param pageable 分页参数
     * @return 分页的查询结果
     */
    @Query("SELECT a FROM SysAuditLogEntity a WHERE " +
            "a.tenantId = :tenantId " +
            "AND a.userId IS NULL " +
            "ORDER BY a.createdAt DESC")
    Page<SysAuditLogEntity> findSystemOperations(
            @Param("tenantId") Long tenantId,
            Pageable pageable
    );

    /**
     * 统计指定时间之前的审计日志数量
     *
     * <p>这个方法用于在执行数据清理前预估要删除的记录数量。
     * 这样可以帮助我们评估清理操作的影响和所需时间。</p>
     *
     * @param cutoffTime 截止时间
     * @return 要删除的记录数量
     */
    long countByCreatedAtBefore(OffsetDateTime cutoffTime);

    /**
     * 检查是否存在指定条件的审计日志
     *
     * <p>这个方法用于快速检查是否存在满足特定条件的审计日志，
     * 而不需要加载具体的数据。这在某些业务逻辑判断中很有用。</p>
     *
     * @param userId    用户ID
     * @param action    操作类型
     * @param startTime 开始时间
     * @return 如果存在则返回true，否则返回false
     */
    boolean existsByUserIdAndActionAndCreatedAtAfter(
            Long userId, String action, OffsetDateTime startTime);

    /**
     * 查找指定用户的第一条审计日志
     *
     * <p>这个方法用于查找用户的首次操作记录，这在用户行为分析中很有价值。
     * 比如，我们可以通过这个方法了解用户的首次登录时间。</p>
     *
     * @param userId 用户ID
     * @return 用户的第一条审计日志，如果不存在则返回空Optional
     */
    Optional<SysAuditLogEntity> findFirstByUserIdOrderByCreatedAtAsc(Long userId);

    /**
     * 查找指定用户的最后一条审计日志
     *
     * <p>这个方法用于查找用户的最近操作记录，这在用户活跃度分析中很有用。</p>
     *
     * @param userId 用户ID
     * @return 用户的最后一条审计日志，如果不存在则返回空Optional
     */
    Optional<SysAuditLogEntity> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}