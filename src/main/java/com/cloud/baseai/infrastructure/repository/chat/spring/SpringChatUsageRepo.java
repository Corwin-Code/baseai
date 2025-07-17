package com.cloud.baseai.infrastructure.repository.chat.spring;

import com.cloud.baseai.application.chat.dto.ModelUsageDTO;
import com.cloud.baseai.infrastructure.persistence.chat.entity.ChatUsageDailyEntity;
import com.cloud.baseai.infrastructure.repository.chat.ChatUsageJpaRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * <h2>对话使用量Spring Data JPA仓储接口</h2>
 *
 * <p>这个接口展现了Spring Data JPA在处理复合主键和聚合查询方面的强大能力。
 * 使用量统计往往涉及复杂的GROUP BY和聚合函数，Spring Data的@Query注解
 * 让我们能够编写原生SQL级别的复杂查询。</p>
 *
 * <p><b>性能优化考虑：</b></p>
 * <p>由于使用量查询经常涉及大量数据的聚合计算，索引设计至关重要：</p>
 * <p>1. 在(tenantId, statDate)上建立复合索引支持时间范围查询</p>
 * <p>2. 在(modelCode, statDate)上建立索引支持模型分析</p>
 * <p>3. 考虑分区表设计以提高大数据量下的查询性能</p>
 */
@Repository
public interface SpringChatUsageRepo extends JpaRepository<ChatUsageDailyEntity, Object> {

    // =================== 基础查询方法 ===================

    /**
     * 查找特定日期和租户的模型使用记录
     *
     * <p>这是累加操作的基础查询。每次记录使用量时，都需要先查找是否
     * 已存在当天的记录，然后决定是创建新记录还是更新现有记录。</p>
     */
    Optional<ChatUsageDailyEntity> findByStatDateAndTenantIdAndModelCode(
            LocalDate statDate, Long tenantId, String modelCode);

    /**
     * 查询时间范围内的使用记录，按日期排序
     *
     * <p>OrderBy子句确保返回的数据按时间顺序排列，这对生成趋势图表很重要。
     * 前端可以直接使用这些数据绘制时间序列图，无需额外排序。</p>
     */
    List<ChatUsageDailyEntity> findByTenantIdAndStatDateBetweenOrderByStatDate(
            Long tenantId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询租户在指定日期的所有模型使用情况
     */
    List<ChatUsageDailyEntity> findByTenantIdAndStatDate(Long tenantId, LocalDate statDate);

    // =================== 聚合统计查询 ===================

    /**
     * 获取最热门的模型使用情况
     *
     * <p>这个查询展示了如何将聚合结果直接映射到DTO对象。
     * 通过在SQL中计算总Token数，避免了在应用层进行大量数据的聚合运算。</p>
     */
    @Query("""
            SELECT ModelUsageDTO(
                u.modelCode,
                COUNT(u),
                SUM(u.promptTokens + u.completionTokens)
            )
            FROM ChatUsageDailyEntity u
            WHERE u.tenantId = :tenantId
            AND u.statDate >= :since
            GROUP BY u.modelCode
            ORDER BY SUM(u.promptTokens + u.completionTokens) DESC
            """)
    List<ModelUsageDTO> findTopUsedModelsByTenantId(
            @Param("tenantId") Long tenantId,
            @Param("since") LocalDate since,
            @Param("limit") int limit);

    /**
     * 获取月度使用汇总
     *
     * <p>使用DATE_FORMAT函数将日期聚合到月份级别。这种SQL技巧
     * 在数据分析中很常用，能够在数据库层面完成时间维度的聚合。</p>
     */
    @Query("""
            SELECT ChatUsageJpaRepository$MonthlyUsageSummary(
                DATE_FORMAT(u.statDate, '%Y-%m'),
                SUM(u.promptTokens),
                SUM(u.completionTokens),
                SUM(u.costUsd),
                COUNT(DISTINCT u.statDate)
            )
            FROM ChatUsageDailyEntity u
            WHERE u.tenantId = :tenantId
            AND u.statDate >= :sinceMonths
            GROUP BY DATE_FORMAT(u.statDate, '%Y-%m')
            ORDER BY DATE_FORMAT(u.statDate, '%Y-%m') DESC
            """)
    List<ChatUsageJpaRepository.MonthlyUsageSummary> findMonthlyUsageSummary(
            @Param("tenantId") Long tenantId,
            @Param("months") int sinceMonths);

    /**
     * 计算使用量增长趋势
     *
     * <p>这个查询使用了窗口函数LAG来计算日环比增长率。
     * 窗口函数是现代SQL的高级特性，能够在单个查询中完成复杂的分析计算。</p>
     */
    @Query(value = """
            SELECT
                stat_date,
                daily_tokens,
                CASE
                    WHEN prev_tokens > 0 THEN ((daily_tokens - prev_tokens) * 100.0 / prev_tokens)
                    ELSE 0
                END as growth_rate
            FROM (
                SELECT
                    stat_date,
                    (prompt_tokens + completion_tokens) as daily_tokens,
                    LAG(prompt_tokens + completion_tokens) OVER (ORDER BY stat_date) as prev_tokens
                FROM chat_usage_daily
                WHERE tenant_id = :tenantId
                AND stat_date BETWEEN :startDate AND :endDate
            ) t
            ORDER BY stat_date
            """, nativeQuery = true)
    List<Object[]> findUsageGrowthTrendRaw(@Param("tenantId") Long tenantId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    /**
     * 使用默认方法将原生查询结果转换为类型安全的DTO
     *
     * <p>这种模式结合了原生SQL的灵活性和Java类型系统的安全性。
     * 复杂的分析查询用原生SQL编写，然后通过默认方法转换为业务对象。</p>
     */
    default List<ChatUsageJpaRepository.UsageGrowthTrend> findUsageGrowthTrend(
            Long tenantId, LocalDate startDate, LocalDate endDate) {

        return findUsageGrowthTrendRaw(tenantId, startDate, endDate)
                .stream()
                .map(row -> new ChatUsageJpaRepository.UsageGrowthTrend(
                        ((java.sql.Date) row[0]).toLocalDate(),
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).doubleValue()
                ))
                .toList();
    }

    /**
     * 查找异常使用模式
     *
     * <p>异常检测基于统计学方法：计算平均值和标准差，然后找出
     * 偏离正常范围的数据点。这种方法能够自动发现数据中的异常模式。</p>
     */
    @Query("""
            SELECT u FROM ChatUsageDailyEntity u
            WHERE u.tenantId = :tenantId
            AND (u.promptTokens + u.completionTokens) > (
                SELECT AVG(u2.promptTokens + u2.completionTokens) * :threshold
                FROM ChatUsageDailyEntity u2
                WHERE u2.tenantId = :tenantId
            )
            ORDER BY (u.promptTokens + u.completionTokens) DESC
            """)
    List<ChatUsageDailyEntity> findAnomalousUsageByTenantId(
            @Param("tenantId") Long tenantId,
            @Param("threshold") double threshold);

    /**
     * 成本效率分析
     *
     * <p>这个查询计算每个模型的成本效率指标。成本效率不仅考虑绝对成本，
     * 还考虑性能表现，帮助用户选择性价比最高的模型配置。</p>
     */
    @Query("""
            SELECT ChatUsageJpaRepository$CostEfficiencyStats(
                u.modelCode,
                SUM(u.promptTokens + u.completionTokens),
                SUM(u.costUsd),
                AVG(u.costUsd / (u.promptTokens + u.completionTokens)),
                (SUM(u.promptTokens + u.completionTokens) / SUM(u.costUsd))
            )
            FROM ChatUsageDailyEntity u
            WHERE u.tenantId = :tenantId
            AND u.statDate >= :since
            AND (u.promptTokens + u.completionTokens) > 0
            GROUP BY u.modelCode
            ORDER BY (SUM(u.promptTokens + u.completionTokens) / SUM(u.costUsd)) DESC
            """)
    List<ChatUsageJpaRepository.CostEfficiencyStats> findCostEfficiencyStatsByTenantId(
            @Param("tenantId") Long tenantId,
            @Param("since") LocalDate since);

    // =================== 管理和维护查询 ===================

    /**
     * 清理历史数据
     *
     * <p>数据保留策略是企业级应用的重要考虑。过老的使用量数据
     * 可能需要归档或删除，以控制数据库大小和查询性能。</p>
     */
    @Query("DELETE FROM ChatUsageDailyEntity u WHERE u.statDate < :cutoffDate")
    void deleteDataBeforeDate(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * 数据一致性检查
     *
     * <p>定期检查数据一致性能够发现潜在的数据质量问题。
     * 例如，负数的Token使用量显然是错误数据。</p>
     */
    @Query("""
            SELECT COUNT(u) FROM ChatUsageDailyEntity u
            WHERE u.promptTokens < 0
            OR u.completionTokens < 0
            OR u.costUsd < 0
            """)
    long countInconsistentRecords();
}