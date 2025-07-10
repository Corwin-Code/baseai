package com.clinflash.baseai.infrastructure.repository.chat;

import com.clinflash.baseai.application.chat.dto.ModelUsageDTO;
import com.clinflash.baseai.domain.chat.model.ChatUsageDaily;
import com.clinflash.baseai.domain.chat.repository.ChatUsageRepository;
import com.clinflash.baseai.infrastructure.persistence.chat.entity.ChatUsageDailyEntity;
import com.clinflash.baseai.infrastructure.persistence.chat.mapper.ChatMapper;
import com.clinflash.baseai.infrastructure.repository.chat.spring.SpringChatUsageRepo;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>对话使用量仓储实现</h2>
 *
 * <p>在SaaS业务模式中，使用量统计是核心业务逻辑之一。这个仓储就像企业的财务记账系统，
 * 精确记录每一笔AI服务的消耗，为计费、预算、容量规划提供数据基础。</p>
 *
 * <p><b>业务价值分析：</b></p>
 * <p>想象一下传统的电力公司如何计费：他们精确记录每户每天的用电量，月底汇总计费。
 * 我们的AI平台也是如此，但计量单位是Token而不是千瓦时。每次AI调用的Token消耗
 * 都需要准确记录，这不仅用于计费，还能帮助用户了解使用模式，优化成本。</p>
 *
 * <p><b>聚合设计模式：</b></p>
 * <p>使用量采用按日聚合的设计模式。这种设计的好处是：</p>
 * <p>1. <strong>存储效率：</strong>避免为每次调用创建单独记录</p>
 * <p>2. <strong>查询性能：</strong>统计查询只需扫描较少的记录</p>
 * <p>3. <strong>业务对齐：</strong>符合按日计费的业务需求</p>
 * <p>4. <strong>数据一致性：</strong>通过原子操作确保计数准确</p>
 */
@Repository
public class ChatUsageJpaRepository implements ChatUsageRepository {

    private final SpringChatUsageRepo springRepo;
    private final ChatMapper mapper;

    /**
     * 依赖注入构造函数
     *
     * <p>仓储模式的一个重要特点是它完全隔离了数据访问技术的细节。
     * 业务层只需要知道"保存使用量记录"这个概念，而不需要关心是存储在
     * MySQL、PostgreSQL还是NoSQL数据库中。</p>
     */
    public ChatUsageJpaRepository(SpringChatUsageRepo springRepo, ChatMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public ChatUsageDaily save(ChatUsageDaily usage) {
        // 使用量记录通常涉及累加操作，而不是简单的新增
        // 这里实现了"upsert"语义：存在则更新，不存在则创建
        ChatUsageDailyEntity entity = mapper.toEntity(usage);
        ChatUsageDailyEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ChatUsageDaily> findByStatDateAndTenantIdAndModelCode(
            LocalDate statDate, Long tenantId, String modelCode) {

        // 查找特定日期、租户、模型的使用记录
        // 这个方法是累加逻辑的基础：先查找现有记录，再决定创建还是更新
        return springRepo.findByStatDateAndTenantIdAndModelCode(statDate, tenantId, modelCode)
                .map(mapper::toDomain);
    }

    @Override
    public List<ChatUsageDaily> findByTenantIdAndStatDateBetween(
            Long tenantId, LocalDate startDate, LocalDate endDate) {

        // 查询时间范围内的使用记录，这是生成账单和报表的基础
        // Between查询在数据库中通常能利用索引，性能较好
        List<ChatUsageDailyEntity> entities = springRepo
                .findByTenantIdAndStatDateBetweenOrderByStatDate(tenantId, startDate, endDate);
        return mapper.toUsageDomainList(entities);
    }

    @Override
    public List<ModelUsageDTO> getTopUsedModels(
            Long tenantId, OffsetDateTime since, int limit) {

        // 获取最热门的模型使用情况
        // 这种统计信息帮助了解用户偏好，指导产品策略
        return springRepo.findTopUsedModelsByTenantId(tenantId, since.toLocalDate(), limit);
    }

    // =================== 扩展的业务方法 ===================

    /**
     * 获取租户的月度使用汇总
     *
     * <p>月度汇总是企业客户最关心的指标。这个方法展示了如何将日度数据
     * 聚合为月度视图，满足不同粒度的业务需求。</p>
     */
    public List<MonthlyUsageSummary> getMonthlyUsageSummary(Long tenantId, int months) {
        return springRepo.findMonthlyUsageSummary(tenantId, months);
    }

    /**
     * 获取使用量增长趋势
     *
     * <p>趋势分析对产品运营很重要。通过分析使用量的增长模式，
     * 可以预测容量需求，制定扩容计划。</p>
     */
    public List<UsageGrowthTrend> getUsageGrowthTrend(Long tenantId, LocalDate startDate, LocalDate endDate) {
        return springRepo.findUsageGrowthTrend(tenantId, startDate, endDate);
    }

    /**
     * 查找异常使用模式
     *
     * <p>异常检测可以帮助发现系统问题或潜在的滥用行为。
     * 例如，某天的使用量突然暴增可能意味着出现了死循环调用。</p>
     */
    public List<ChatUsageDaily> findAnomalousUsage(Long tenantId, double threshold) {
        return springRepo.findAnomalousUsageByTenantId(tenantId, threshold)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * 获取成本效率分析
     *
     * <p>这个分析帮助用户了解不同模型的成本效率，
     * 指导他们选择最适合的模型配置。</p>
     */
    public List<CostEfficiencyStats> getCostEfficiencyAnalysis(Long tenantId, LocalDate since) {
        return springRepo.findCostEfficiencyStatsByTenantId(tenantId, since);
    }

    // =================== 内部数据传输对象 ===================

    /**
     * 月度使用汇总
     */
    public record MonthlyUsageSummary(
            String month,                    // 格式: YYYY-MM
            Long totalPromptTokens,
            Long totalCompletionTokens,
            java.math.BigDecimal totalCost,
            Integer activeDays              // 该月有使用记录的天数
    ) {
    }

    /**
     * 使用量增长趋势
     */
    public record UsageGrowthTrend(
            LocalDate date,
            Long dailyTokens,
            Double growthRate               // 相比前一天的增长率
    ) {
    }

    /**
     * 成本效率统计
     */
    public record CostEfficiencyStats(
            String modelCode,
            Long totalTokens,
            java.math.BigDecimal totalCost,
            java.math.BigDecimal costPerToken,  // 每Token成本
            Double efficiency               // 效率评分(基于成本和性能)
    ) {
    }
}