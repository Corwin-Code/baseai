package com.cloud.baseai.domain.chat.service;

import com.cloud.baseai.application.chat.dto.UsageStatisticsDTO;
import com.cloud.baseai.domain.chat.model.ChatUsageDaily;
import com.cloud.baseai.domain.chat.repository.ChatUsageRepository;
import com.cloud.baseai.infrastructure.config.properties.ChatProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <h2>使用量计算领域服务</h2>
 *
 * <p>负责AI对话系统的使用量统计和费用计算。这个服务就像一个精确的会计师，
 * 记录每一次AI调用的成本，为用户提供透明的费用明细。</p>
 */
@Service
public class UsageCalculationService {

    private static final Logger log = LoggerFactory.getLogger(UsageCalculationService.class);

    private final ChatUsageRepository usageRepo;
    private final ChatProperties config;

    // 模型计费标准（每1000个Token的价格，单位：美元）
    private static final Map<String, ModelPricing> MODEL_PRICING = Map.of(
            "gpt-4o", new ModelPricing(0.03, 0.06),
            "gpt-4o-mini", new ModelPricing(0.0015, 0.006),
            "claude-3.5-sonnet", new ModelPricing(0.03, 0.15),
            "claude-3-haiku", new ModelPricing(0.0025, 0.0125),
            "gemini-pro", new ModelPricing(0.0005, 0.0015)
    );

    public UsageCalculationService(ChatUsageRepository usageRepo, ChatProperties config) {
        this.usageRepo = usageRepo;
        this.config = config;
    }

    /**
     * 记录使用量
     *
     * @param tenantId         租户ID
     * @param modelCode        模型代码
     * @param promptTokens     输入Token数
     * @param completionTokens 输出Token数
     * @param cost             费用
     */
    public void recordUsage(Long tenantId, String modelCode,
                            Long promptTokens, Long completionTokens, Double cost) {
        try {
            LocalDate today = LocalDate.now();

            // 查找或创建今日使用记录
            ChatUsageDaily todayUsage = usageRepo.findByStatDateAndTenantIdAndModelCode(today, tenantId, modelCode)
                    .orElse(ChatUsageDaily.create(today, tenantId, modelCode, 0L, 0L, BigDecimal.ZERO));

            // 累加使用量
            todayUsage = todayUsage.addUsage(promptTokens, completionTokens, BigDecimal.valueOf(cost));

            usageRepo.save(todayUsage);

            log.debug("记录使用量: tenantId={}, model={}, promptTokens={}, completionTokens={}, cost={}",
                    tenantId, modelCode, promptTokens, completionTokens, cost);

        } catch (Exception e) {
            log.error("记录使用量失败: tenantId={}, model={}", tenantId, modelCode, e);
        }
    }

    /**
     * 计算费用
     *
     * @param modelCode        模型代码
     * @param promptTokens     输入Token数
     * @param completionTokens 输出Token数
     * @return 计算出的费用（美元）
     */
    public double calculateCost(String modelCode, long promptTokens, long completionTokens) {
        ModelPricing pricing = MODEL_PRICING.get(modelCode);
        if (pricing == null) {
            log.warn("未知模型的计费标准: {}", modelCode);
            // 使用默认计费标准
            pricing = new ModelPricing(0.002, 0.004);
        }

        double promptCost = (promptTokens / 1000.0) * pricing.inputPrice();
        double completionCost = (completionTokens / 1000.0) * pricing.outputPrice();

        return promptCost + completionCost;
    }

    /**
     * 获取使用量统计
     *
     * @param tenantId 租户ID
     * @param period   统计周期
     * @param groupBy  分组方式
     * @return 使用量统计
     */
    public UsageStatisticsDTO getUsageStatistics(Long tenantId, String period, String groupBy) {
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = calculateStartDate(endDate, period);

            // 获取时间范围内的使用记录
            List<ChatUsageDaily> usageRecords = usageRepo.findByTenantIdAndStatDateBetween(
                    tenantId, startDate, endDate);

            // 按日期分组统计
            List<UsageStatisticsDTO.DailyUsageDTO> dailyUsage = usageRecords.stream()
                    .collect(Collectors.groupingBy(ChatUsageDaily::statDate))
                    .entrySet().stream()
                    .map(entry -> {
                        LocalDate date = entry.getKey();
                        List<ChatUsageDaily> dayRecords = entry.getValue();

                        long totalPromptTokens = dayRecords.stream()
                                .mapToLong(ChatUsageDaily::promptTokens)
                                .sum();
                        long totalCompletionTokens = dayRecords.stream()
                                .mapToLong(ChatUsageDaily::completionTokens)
                                .sum();
                        double totalCost = dayRecords.stream()
                                .mapToDouble(record -> record.costUsd().doubleValue())
                                .sum();

                        return new UsageStatisticsDTO.DailyUsageDTO(
                                date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                totalPromptTokens,
                                totalCompletionTokens,
                                totalCost
                        );
                    })
                    .sorted((a, b) -> a.date().compareTo(b.date()))
                    .collect(Collectors.toList());

            // 计算总计
            long totalPromptTokens = usageRecords.stream()
                    .mapToLong(ChatUsageDaily::promptTokens)
                    .sum();
            long totalCompletionTokens = usageRecords.stream()
                    .mapToLong(ChatUsageDaily::completionTokens)
                    .sum();
            double totalCost = usageRecords.stream()
                    .mapToDouble(record -> record.costUsd().doubleValue())
                    .sum();

            UsageStatisticsDTO.UsageDetail totalUsage = new UsageStatisticsDTO.UsageDetail(
                    totalPromptTokens, totalCompletionTokens, totalCost
            );

            // 按模型分组统计
            List<UsageStatisticsDTO.ModelUsageBreakdown> modelBreakdown = usageRecords.stream()
                    .collect(Collectors.groupingBy(ChatUsageDaily::modelCode))
                    .entrySet().stream()
                    .map(entry -> {
                        String modelCode = entry.getKey();
                        List<ChatUsageDaily> modelRecords = entry.getValue();

                        long modelPromptTokens = modelRecords.stream()
                                .mapToLong(ChatUsageDaily::promptTokens)
                                .sum();
                        long modelCompletionTokens = modelRecords.stream()
                                .mapToLong(ChatUsageDaily::completionTokens)
                                .sum();
                        double modelCost = modelRecords.stream()
                                .mapToDouble(record -> record.costUsd().doubleValue())
                                .sum();

                        return new UsageStatisticsDTO.ModelUsageBreakdown(
                                modelCode,
                                modelPromptTokens,
                                modelCompletionTokens,
                                modelCost,
                                modelRecords.size()
                        );
                    })
                    .sorted((a, b) -> Double.compare(b.cost(), a.cost()))
                    .collect(Collectors.toList());

            return new UsageStatisticsDTO(
                    tenantId,
                    period,
                    dailyUsage,
                    totalUsage,
                    modelBreakdown
            );

        } catch (Exception e) {
            log.error("获取使用量统计失败: tenantId={}, period={}", tenantId, period, e);
            throw new RuntimeException("获取使用量统计失败", e);
        }
    }

    /**
     * 计算周期开始日期
     */
    private LocalDate calculateStartDate(LocalDate endDate, String period) {
        if (period == null) {
            period = "30d";
        }

        return switch (period.toLowerCase()) {
            case "1d", "day" -> endDate.minusDays(1);
            case "7d", "week" -> endDate.minusDays(7);
            case "30d", "month" -> endDate.minusDays(30);
            case "90d", "quarter" -> endDate.minusDays(90);
            case "365d", "year" -> endDate.minusDays(365);
            default -> endDate.minusDays(30);
        };
    }

    /**
     * 模型计费标准
     */
    public record ModelPricing(double inputPrice, double outputPrice) {
    }
}