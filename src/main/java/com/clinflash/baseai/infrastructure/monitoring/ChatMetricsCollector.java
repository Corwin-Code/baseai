package com.clinflash.baseai.infrastructure.monitoring;

import com.clinflash.baseai.application.chat.service.ChatApplicationService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <h2>对话指标收集器</h2>
 *
 * <p>指标收集是现代微服务架构的重要组成部分。就像汽车的仪表盘显示速度、油耗、
 * 温度等关键指标一样，我们的系统也需要收集和展示各种业务和技术指标。</p>
 *
 * <p><b>为什么需要指标收集：</b></p>
 * <p>1. <strong>性能监控：</strong>实时了解系统的响应时间、吞吐量等性能指标</p>
 * <p>2. <strong>业务洞察：</strong>分析用户行为模式，指导产品决策</p>
 * <p>3. <strong>容量规划：</strong>基于历史数据预测未来的资源需求</p>
 * <p>4. <strong>故障诊断：</strong>通过指标异常快速定位问题根因</p>
 * <p>5. <strong>成本优化：</strong>了解资源使用效率，优化成本结构</p>
 */
@Component
public class ChatMetricsCollector implements ChatApplicationService.MetricsService {

    private final MeterRegistry meterRegistry;

    // 预定义的指标
    private final Counter messagesSentCounter;
    private final Counter messagesReceivedCounter;
    private final Counter errorsCounter;
    private final Timer responseTimeTimer;
    private final Counter tokensUsedCounter;

    // 动态指标缓存
    private final ConcurrentMap<String, Counter> dynamicCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> dynamicTimers = new ConcurrentHashMap<>();

    public ChatMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 初始化核心指标
        this.messagesSentCounter = Counter.builder("chat.messages.sent")
                .description("发送的消息总数")
                .register(meterRegistry);

        this.messagesReceivedCounter = Counter.builder("chat.messages.received")
                .description("接收的消息总数")
                .register(meterRegistry);

        this.errorsCounter = Counter.builder("chat.errors")
                .description("对话错误总数")
                .register(meterRegistry);

        this.responseTimeTimer = Timer.builder("chat.response.time")
                .description("对话响应时间")
                .register(meterRegistry);

        this.tokensUsedCounter = Counter.builder("chat.tokens.used")
                .description("使用的Token总数")
                .register(meterRegistry);
    }

    @Override
    public void recordOperation(String operation, long durationMs, boolean success) {
        // 记录操作耗时
        Timer operationTimer = dynamicTimers.computeIfAbsent(
                "chat.operation." + operation,
                key -> Timer.builder(key)
                        .description("操作 " + operation + " 的执行时间")
                        .register(meterRegistry)
        );
        operationTimer.record(Duration.ofMillis(durationMs));

        // 记录操作结果
        String resultTag = success ? "success" : "failure";
        Counter operationCounter = dynamicCounters.computeIfAbsent(
                "chat.operation." + operation + "." + resultTag,
                key -> Counter.builder("chat.operation.result")
                        .description("操作结果计数")
                        .tag("operation", operation)
                        .tag("result", resultTag)
                        .register(meterRegistry)
        );
        operationCounter.increment();
    }

    @Override
    public void recordFeedback(Long messageId, Integer rating, String comment) {
        // 记录用户反馈
        Counter feedbackCounter = dynamicCounters.computeIfAbsent(
                "chat.feedback.rating." + rating,
                key -> Counter.builder("chat.feedback")
                        .description("用户反馈统计")
                        .tag("rating", rating.toString())
                        .register(meterRegistry)
        );
        feedbackCounter.increment();
    }

    /**
     * 记录消息发送
     */
    public void recordMessageSent(String modelCode, int tokenCount) {
        messagesSentCounter.increment();

        // 按模型统计
        Counter modelCounter = dynamicCounters.computeIfAbsent(
                "chat.messages.sent.by.model." + modelCode,
                key -> Counter.builder("chat.messages.by.model")
                        .description("按模型统计的消息数")
                        .tag("model", modelCode)
                        .tag("direction", "sent")
                        .register(meterRegistry)
        );
        modelCounter.increment();

        // 记录Token使用
        tokensUsedCounter.increment(tokenCount);
    }

    /**
     * 记录消息接收
     */
    public void recordMessageReceived(String modelCode, int tokenCount, long responseTimeMs) {
        messagesReceivedCounter.increment();
        responseTimeTimer.record(Duration.ofMillis(responseTimeMs));

        // 按模型统计响应时间
        Timer modelResponseTimer = dynamicTimers.computeIfAbsent(
                "chat.response.time.by.model." + modelCode,
                key -> Timer.builder("chat.response.time.by.model")
                        .description("按模型统计的响应时间")
                        .tag("model", modelCode)
                        .register(meterRegistry)
        );
        modelResponseTimer.record(Duration.ofMillis(responseTimeMs));
    }

    /**
     * 记录错误
     */
    public void recordError(String errorType, String modelCode) {
        errorsCounter.increment();

        // 按错误类型统计
        Counter errorTypeCounter = dynamicCounters.computeIfAbsent(
                "chat.errors.by.type." + errorType,
                key -> Counter.builder("chat.errors.by.type")
                        .description("按错误类型统计的错误数")
                        .tag("error_type", errorType)
                        .register(meterRegistry)
        );
        errorTypeCounter.increment();

        // 按模型统计错误
        if (modelCode != null) {
            Counter modelErrorCounter = dynamicCounters.computeIfAbsent(
                    "chat.errors.by.model." + modelCode,
                    key -> Counter.builder("chat.errors.by.model")
                            .description("按模型统计的错误数")
                            .tag("model", modelCode)
                            .register(meterRegistry)
            );
            modelErrorCounter.increment();
        }
    }

    /**
     * 记录费用
     */
    public void recordCost(String modelCode, double costUsd) {
        Counter costCounter = dynamicCounters.computeIfAbsent(
                "chat.cost.total",
                key -> Counter.builder(key)
                        .description("总费用统计")
                        .register(meterRegistry)
        );
        costCounter.increment(costUsd);

        // 按模型统计费用
        Counter modelCostCounter = dynamicCounters.computeIfAbsent(
                "chat.cost.by.model." + modelCode,
                key -> Counter.builder("chat.cost.by.model")
                        .description("按模型统计的费用")
                        .tag("model", modelCode)
                        .register(meterRegistry)
        );
        modelCostCounter.increment(costUsd);
    }

    /**
     * 记录并发用户数
     */
    public void recordConcurrentUsers(int userCount) {
        meterRegistry.gauge("chat.concurrent.users", userCount);
    }

    /**
     * 记录活跃对话数
     */
    public void recordActiveThreads(int threadCount) {
        meterRegistry.gauge("chat.active.threads", threadCount);
    }
}