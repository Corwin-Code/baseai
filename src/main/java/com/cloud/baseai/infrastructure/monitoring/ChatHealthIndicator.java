package com.cloud.baseai.infrastructure.monitoring;

import com.cloud.baseai.domain.chat.repository.ChatMessageRepository;
import com.cloud.baseai.domain.chat.repository.ChatThreadRepository;
import com.cloud.baseai.domain.chat.repository.ChatUsageRepository;
import com.cloud.baseai.infrastructure.external.llm.factory.ChatModelFactory;
import com.cloud.baseai.infrastructure.external.llm.service.ChatCompletionService;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * <h2>对话系统健康指示器</h2>
 *
 * <p>在生产环境中，系统健康监控就像医生给病人体检一样重要。这个健康指示器
 * 会定期检查对话系统的各个关键组件，确保服务能够正常运行。</p>
 *
 * <p><b>监控的重要性：</b></p>
 * <p>想象一下，如果你的在线客服系统在客户最需要帮助的时候突然失效，
 * 这不仅会影响用户体验，还可能造成业务损失。通过全面的健康监控，
 * 我们可以：</p>
 * <ul>
 * <li><b>预防性维护：</b>在问题影响用户之前就发现并解决</li>
 * <li><b>快速故障排除：</b>精确定位问题源头，缩短恢复时间</li>
 * <li><b>容量规划：</b>基于监控数据制定合理的扩容计划</li>
 * <li><b>SLA保障：</b>确保服务水平协议的可达成性</li>
 * </ul>
 *
 * <p><b>健康检查的层次：</b></p>
 * <p>我们采用分层健康检查模式：</p>
 * <p>1. <strong>基础设施层：</strong>数据库连接、网络状态</p>
 * <p>2. <strong>服务层：</strong>LLM提供商可用性、响应时间</p>
 * <p>3. <strong>业务层：</strong>核心功能完整性、数据一致性</p>
 * <p>4. <strong>用户层：</strong>端到端用户体验</p>
 */
@Component("chatHealthIndicator")
public class ChatHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(ChatHealthIndicator.class);

    private final ChatModelFactory chatModelFactory;
    private final ChatThreadRepository threadRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatUsageRepository usageRepository;

    // 健康检查阈值配置
    private static final long MAX_RESPONSE_TIME_MS = 5000;
    private static final int MAX_ERROR_RATE_PERCENT = 10;
    private static final long DATABASE_TIMEOUT_MS = 3000;

    public ChatHealthIndicator(
            ChatModelFactory chatModelFactory,
            ChatThreadRepository threadRepository,
            ChatMessageRepository messageRepository,
            ChatUsageRepository usageRepository) {

        this.chatModelFactory = chatModelFactory;
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.usageRepository = usageRepository;
    }

    @Override
    public Health health() {
        long startTime = System.currentTimeMillis();

        try {
            Health.Builder builder = Health.up();
            Map<String, Object> details = new HashMap<>();

            // 1. 检查LLM服务健康状态
            HealthCheckResult llmHealth = checkLLMServiceHealth();
            details.put("llm_service", llmHealth.toMap());

            // 2. 检查数据库连接和性能
            HealthCheckResult dbHealth = checkDatabaseHealth();
            details.put("database", dbHealth.toMap());

            // 3. 检查业务功能完整性
            HealthCheckResult businessHealth = checkBusinessHealth();
            details.put("business_logic", businessHealth.toMap());

            // 4. 计算总体健康状态
            boolean overallHealthy = llmHealth.isHealthy() &&
                    dbHealth.isHealthy() &&
                    businessHealth.isHealthy();

            // 5. 添加响应时间信息
            long totalTime = System.currentTimeMillis() - startTime;
            details.put("response_time_ms", totalTime);
            details.put("timestamp", OffsetDateTime.now().toString());

            if (overallHealthy) {
                return builder.withDetails(details).build();
            } else {
                return builder.down().withDetails(details).build();
            }

        } catch (Exception e) {
            log.error("健康检查执行失败", e);
            return Health.down()
                    .withException(e)
                    .withDetail("error", "健康检查执行异常")
                    .withDetail("timestamp", OffsetDateTime.now().toString())
                    .build();
        }
    }

    /**
     * 检查LLM服务健康状态
     *
     * <p>LLM服务是对话系统的核心，我们需要验证：</p>
     * <p>1. 服务是否可达</p>
     * <p>2. 响应时间是否在可接受范围内</p>
     * <p>3. 是否能够正常生成回复</p>
     */
    private HealthCheckResult checkLLMServiceHealth() {
        try {
            long startTime = System.currentTimeMillis();

            // 使用CompletableFuture实现超时控制
            CompletableFuture<Boolean> healthCheck = CompletableFuture.supplyAsync(() -> {
                try {
                    ChatCompletionService service = chatModelFactory.getDefaultService();
                    return service.isHealthy();
                } catch (Exception e) {
                    log.warn("LLM服务健康检查异常", e);
                    return false;
                }
            });

            Boolean isHealthy = healthCheck.get(MAX_RESPONSE_TIME_MS, TimeUnit.MILLISECONDS);
            long responseTime = System.currentTimeMillis() - startTime;

            if (isHealthy && responseTime <= MAX_RESPONSE_TIME_MS) {
                return HealthCheckResult.healthy("LLM服务正常")
                        .withDetail("response_time_ms", responseTime)
                        .withDetail("supported_models", chatModelFactory.getAllSupportedModels().size());
            } else {
                String reason = !isHealthy ? "LLM服务不可用" : "响应时间过长: " + responseTime + "ms";
                return HealthCheckResult.unhealthy(reason)
                        .withDetail("response_time_ms", responseTime);
            }

        } catch (Exception e) {
            log.error("LLM服务健康检查失败", e);
            return HealthCheckResult.unhealthy("LLM服务检查异常: " + e.getMessage());
        }
    }

    /**
     * 检查数据库健康状态
     *
     * <p>数据库是系统的基石，我们需要确保：</p>
     * <p>1. 连接池状态正常</p>
     * <p>2. 查询响应时间合理</p>
     * <p>3. 数据一致性没有问题</p>
     */
    private HealthCheckResult checkDatabaseHealth() {
        try {
            long startTime = System.currentTimeMillis();

            // 1. 检查基本连接性
            CompletableFuture<Long> countCheck = CompletableFuture.supplyAsync(() -> {
                try {
                    return threadRepository.count();
                } catch (Exception e) {
                    log.warn("数据库连接检查异常", e);
                    throw new RuntimeException("数据库连接失败", e);
                }
            });

            Long threadCount = countCheck.get(DATABASE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            long dbResponseTime = System.currentTimeMillis() - startTime;

            // 2. 检查最近的数据活动
            OffsetDateTime recentTime = OffsetDateTime.now().minusHours(1);
            int recentMessages = messageRepository.countByTenantIdSince(1L, recentTime);

            if (dbResponseTime <= DATABASE_TIMEOUT_MS) {
                return HealthCheckResult.healthy("数据库连接正常")
                        .withDetail("response_time_ms", dbResponseTime)
                        .withDetail("total_threads", threadCount)
                        .withDetail("recent_messages", recentMessages);
            } else {
                return HealthCheckResult.unhealthy("数据库响应时间过长: " + dbResponseTime + "ms")
                        .withDetail("response_time_ms", dbResponseTime);
            }

        } catch (Exception e) {
            log.error("数据库健康检查失败", e);
            return HealthCheckResult.unhealthy("数据库检查异常: " + e.getMessage());
        }
    }

    /**
     * 检查业务功能健康状态
     *
     * <p>业务层面的健康检查关注的是系统的核心功能是否正常：</p>
     * <p>1. 新对话创建功能</p>
     * <p>2. 消息发送和接收功能</p>
     * <p>3. 数据统计功能</p>
     */
    private HealthCheckResult checkBusinessHealth() {
        try {
            Map<String, Object> businessDetails = new HashMap<>();

            // 1. 检查最近的错误率
            OffsetDateTime since = OffsetDateTime.now().minusMinutes(30);
            // 这里需要从监控系统或日志中获取错误率数据
            // 为了示例，我们假设错误率为2%
            double errorRate = calculateRecentErrorRate(since);
            businessDetails.put("error_rate_percent", errorRate);

            // 2. 检查平均响应时间
            Double avgResponseTime = messageRepository.getAverageResponseTime(1L, since);
            businessDetails.put("avg_response_time_ms", avgResponseTime);

            // 3. 检查数据一致性
            boolean dataConsistent = checkDataConsistency();
            businessDetails.put("data_consistent", dataConsistent);

            // 4. 检查系统负载
            SystemLoadInfo loadInfo = getSystemLoadInfo();
            businessDetails.put("system_load", loadInfo.toMap());

            boolean businessHealthy = errorRate < MAX_ERROR_RATE_PERCENT &&
                    dataConsistent &&
                    loadInfo.isAcceptable();

            if (businessHealthy) {
                return HealthCheckResult.healthy("业务功能正常").withDetails(businessDetails);
            } else {
                String reason = String.format("业务健康检查失败: 错误率=%.2f%%, 数据一致性=%s",
                        errorRate, dataConsistent);
                return HealthCheckResult.unhealthy(reason).withDetails(businessDetails);
            }

        } catch (Exception e) {
            log.error("业务健康检查失败", e);
            return HealthCheckResult.unhealthy("业务检查异常: " + e.getMessage());
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 计算最近的错误率
     */
    private double calculateRecentErrorRate(OffsetDateTime since) {
        // 这里应该从实际的监控系统获取数据
        // 为了示例，返回一个模拟值
        return 2.5; // 2.5%错误率
    }

    /**
     * 检查数据一致性
     */
    private boolean checkDataConsistency() {
        try {
            // 1. 检查消息和线程的关联关系
            // 2. 检查使用量统计的准确性
            // 3. 检查引用关系的完整性

            // 为了示例，这里返回true
            // 实际实现应该包含具体的一致性检查逻辑
            return true;

        } catch (Exception e) {
            log.warn("数据一致性检查异常", e);
            return false;
        }
    }

    /**
     * 获取系统负载信息
     */
    private SystemLoadInfo getSystemLoadInfo() {
        // 获取JVM内存使用情况
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsage = (double) usedMemory / totalMemory * 100;

        // 获取线程数
        int threadCount = Thread.activeCount();

        return new SystemLoadInfo(memoryUsage, threadCount);
    }

    // =================== 内部数据结构 ===================

    /**
     * 健康检查结果
     */
    public static class HealthCheckResult {
        @Getter
        private final boolean healthy;
        private final String message;
        private final Map<String, Object> details;

        private HealthCheckResult(boolean healthy, String message) {
            this.healthy = healthy;
            this.message = message;
            this.details = new HashMap<>();
        }

        public static HealthCheckResult healthy(String message) {
            return new HealthCheckResult(true, message);
        }

        public static HealthCheckResult unhealthy(String message) {
            return new HealthCheckResult(false, message);
        }

        public HealthCheckResult withDetail(String key, Object value) {
            this.details.put(key, value);
            return this;
        }

        public HealthCheckResult withDetails(Map<String, Object> details) {
            this.details.putAll(details);
            return this;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> result = new HashMap<>();
            result.put("status", healthy ? "UP" : "DOWN");
            result.put("message", message);
            result.putAll(details);
            return result;
        }
    }

    /**
     * 系统负载信息
     */
    private record SystemLoadInfo(
            double memoryUsagePercent,
            int activeThreadCount
    ) {
        public boolean isAcceptable() {
            return memoryUsagePercent < 85.0 && activeThreadCount < 1000;
        }

        public Map<String, Object> toMap() {
            return Map.of(
                    "memory_usage_percent", memoryUsagePercent,
                    "active_threads", activeThreadCount,
                    "acceptable", isAcceptable()
            );
        }
    }
}