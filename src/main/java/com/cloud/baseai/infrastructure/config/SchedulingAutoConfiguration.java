package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.config.base.BaseAutoConfiguration;
import com.cloud.baseai.infrastructure.config.properties.SchedulingProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <h2>应用定时任务功能的综合配置类</h2>
 *
 * <p>这个配置类提供了一个完整的定时任务解决方案，
 * 使用自定义线程池调度器，替代Spring Boot默认的单线程调度器</p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "baseai.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingAutoConfiguration extends BaseAutoConfiguration {

    private final SchedulingProperties schedulingProperties;

    /**
     * 构造函数，注入配置属性。
     *
     * @param schedulingProperties 定时任务配置属性
     */
    public SchedulingAutoConfiguration(SchedulingProperties schedulingProperties) {
        this.schedulingProperties = schedulingProperties;

        // 统一初始化
        initializeConfiguration();
    }

    @Override
    protected String getConfigurationName() {
        return "定时任务调度";
    }

    @Override
    protected String getModuleName() {
        return "SCHEDULING";
    }

    @Override
    protected void validateConfiguration() {
        logInfo("开始验证定时任务配置参数...");

        // 基础配置验证
        validateNotNull(schedulingProperties.getPool(), "线程池配置");
        validateNotNull(schedulingProperties.getThread(), "线程配置");
        validateNotNull(schedulingProperties.getShutdown(), "关闭配置");

        // 线程池配置验证
        SchedulingProperties.Pool pool = schedulingProperties.getPool();
        validateRange(pool.getSize(), 1, 200, "线程池大小");
        validateRange(pool.getKeepAliveSeconds(), 10, 3600, "线程存活时间");

        // 线程配置验证
        SchedulingProperties.Thread thread = schedulingProperties.getThread();
        validateNotBlank(thread.getNamePrefix(), "线程名称前缀");
        validateRange(thread.getPriority(), Thread.MIN_PRIORITY, Thread.MAX_PRIORITY, "线程优先级");

        // 关闭配置验证
        SchedulingProperties.Shutdown shutdown = schedulingProperties.getShutdown();
        validateRange(shutdown.getAwaitTerminationSeconds(), 1, 600, "等待终止时间");

        // 性能建议
        performanceRecommendations();

        logSuccess("定时任务配置验证通过");
    }

    /**
     * 性能建议和最佳实践检查
     */
    private void performanceRecommendations() {
        int poolSize = schedulingProperties.getPool().getSize();
        int cpuCores = Runtime.getRuntime().availableProcessors();

        // 线程池大小建议
        if (poolSize == 1) {
            logWarning("线程池大小为1，如果有并发定时任务可能会相互阻塞");
        } else if (poolSize > cpuCores * 4) {
            logWarning("线程池大小 (%d) 远超CPU核心数 (%d)，可能导致过多的上下文切换",
                    poolSize, cpuCores);
        }

        // 线程名称前缀建议
        String namePrefix = schedulingProperties.getThread().getNamePrefix();
        if (!namePrefix.endsWith("-")) {
            logWarning("建议线程名称前缀以 '-' 结尾，当前: '%s'", namePrefix);
        }

        // 优先级建议
        int priority = schedulingProperties.getThread().getPriority();
        if (priority != Thread.NORM_PRIORITY) {
            logInfo("使用非默认线程优先级: %d (默认: %d)", priority, Thread.NORM_PRIORITY);
        }

        logInfo("系统CPU核心数: %d，建议线程池大小: %d-%d", cpuCores, cpuCores, cpuCores * 2);
    }

    @Override
    protected Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();

        // 基础状态
        summary.put("功能启用", schedulingProperties.isEnabled());

        // 线程池配置
        SchedulingProperties.Pool pool = schedulingProperties.getPool();
        summary.put("线程池大小", pool.getSize());
        summary.put("允许核心线程超时", pool.isAllowCoreThreadTimeout());
        summary.put("线程存活时间(秒)", pool.getKeepAliveSeconds());

        // 线程配置
        SchedulingProperties.Thread thread = schedulingProperties.getThread();
        summary.put("线程名称前缀", thread.getNamePrefix());
        summary.put("线程优先级", thread.getPriority());

        // 关闭配置
        SchedulingProperties.Shutdown shutdown = schedulingProperties.getShutdown();
        summary.put("等待任务完成", shutdown.isWaitForTasksToComplete());
        summary.put("等待终止时间(秒)", shutdown.getAwaitTerminationSeconds());

        // 系统信息
        summary.put("系统CPU核心数", Runtime.getRuntime().availableProcessors());

        return summary;
    }

    /**
     * 创建并配置线程池任务调度器Bean。
     *
     * @return 配置好的ThreadPoolTaskScheduler实例
     * @see ThreadPoolTaskScheduler
     */
    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        logBeanCreation("taskScheduler", "线程池任务调度器");

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        // 基本配置
        SchedulingProperties.Pool pool = schedulingProperties.getPool();
        scheduler.setPoolSize(pool.getSize());
        // 当应用关闭时，是否等待正在执行的任务完成
        scheduler.setWaitForTasksToCompleteOnShutdown(
                schedulingProperties.getShutdown().isWaitForTasksToComplete());
        // 等待任务完成的最长时间
        scheduler.setAwaitTerminationSeconds(
                schedulingProperties.getShutdown().getAwaitTerminationSeconds());


        // 线程配置
        SchedulingProperties.Thread thread = schedulingProperties.getThread();
        // 设置线程优先级
        scheduler.setThreadPriority(thread.getPriority());
        scheduler.setThreadNamePrefix(thread.getNamePrefix());

        // 拒绝策略：当线程池和队列都满时的处理策略
        // CallerRunsPolicy: 由调用线程执行任务，这样可以减缓任务提交速度，确保任务不会丢失
        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 初始化调度器
        scheduler.initialize();

        // 获取底层执行器并设置高级参数
        configureUnderlyingExecutor(scheduler);

        logInfo("调度器配置完成 - 线程池大小: %d, 线程前缀: '%s', 优先级: %d",
                pool.getSize(), thread.getNamePrefix(), thread.getPriority());

        logBeanSuccess("taskScheduler");
        return scheduler;
    }

    /**
     * 配置底层执行器的高级参数
     */
    private void configureUnderlyingExecutor(ThreadPoolTaskScheduler scheduler) {
        // 获取底层的 JDK 执行器，并设置高级参数
        ThreadPoolExecutor executor = scheduler.getScheduledThreadPoolExecutor();
        SchedulingProperties.Pool pool = schedulingProperties.getPool();

        // 设置是否允许核心线程超时，这有助于在低负载时节省资源
        executor.allowCoreThreadTimeOut(pool.isAllowCoreThreadTimeout());

        // 设置线程空闲时间，超过此时间的空闲线程将被回收
        executor.setKeepAliveTime(pool.getKeepAliveSeconds(), TimeUnit.SECONDS);

        logInfo("底层执行器配置 - 核心线程超时: %s, 存活时间: %d秒",
                pool.isAllowCoreThreadTimeout(), pool.getKeepAliveSeconds());
    }

    /**
     * 创建调度器健康检查器。
     *
     * <p>这个健康检查器会监控线程池调度器的状态，并在Spring Boot Actuator
     * 的健康检查端点中报告状态信息。这对于生产环境的监控和故障排查非常重要。</p>
     *
     * <p><strong>健康检查内容包括：</strong></p>
     * <ul>
     *   <li>调度器是否正在运行</li>
     *   <li>当前活跃线程数</li>
     *   <li>线程池大小</li>
     *   <li>已完成任务数量</li>
     * </ul>
     *
     * <p><strong>访问方式：</strong></p>
     * <pre>GET /actuator/health/schedulerHealth</pre>
     *
     * @param taskScheduler 任务调度器实例
     * @return HealthIndicator实例
     */
    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    public HealthIndicator schedulerHealthIndicator(ThreadPoolTaskScheduler taskScheduler) {
        logBeanCreation("schedulerHealthIndicator", "调度器健康检查指示器");

        HealthIndicator indicator = new SchedulerHealthIndicator(taskScheduler);
        logBeanSuccess("schedulerHealthIndicator");

        return indicator;
    }

    /**
     * 为调度器配置Micrometer指标收集。
     *
     * <p>这个方法将调度器的线程池绑定到Micrometer指标注册表，
     * 自动收集和暴露以下指标：</p>
     * <ul>
     *   <li><strong>executor.pool.size：</strong> 线程池当前大小</li>
     *   <li><strong>executor.active：</strong> 当前活跃线程数</li>
     *   <li><strong>executor.completed：</strong> 已完成任务总数</li>
     *   <li><strong>executor.queue.size：</strong> 队列中等待的任务数</li>
     * </ul>
     *
     * <p><strong>指标标签：</strong></p>
     * <ul>
     *   <li><strong>name：</strong> "scheduler" - 便于区分不同的线程池</li>
     * </ul>
     *
     * <p>这些指标可以通过Prometheus、Grafana等监控工具进行可视化和告警。</p>
     *
     * @param taskScheduler 任务调度器实例
     * @param meterRegistry Micrometer指标注册表
     * @return ExecutorServiceMetrics实例，如果MeterRegistry不可用则返回null
     */
    @Bean
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnBean(MeterRegistry.class)
    public ExecutorServiceMetrics schedulerMetrics(ThreadPoolTaskScheduler taskScheduler,
                                                   MeterRegistry meterRegistry) {

        logBeanCreation("schedulerMetrics", "调度器Micrometer指标收集器");

        if (meterRegistry == null) {
            logWarning("MeterRegistry不可用，跳过调度器指标配置");
            return null;
        }

        // 获取底层的ScheduledExecutorService进行指标绑定
        ScheduledExecutorService executorService = taskScheduler.getScheduledExecutor();

        ExecutorServiceMetrics metrics = new ExecutorServiceMetrics(
                executorService,                    // 需要监控的 ExecutorService
                "scheduler",                        // 指标中执行器的名称
                Tags.of("component", "scheduling")  // 额外的标签
        );

        // 将指标绑定到注册表
        metrics.bindTo(meterRegistry);

        logInfo("已配置指标收集 - 指标名称: 'executor', 标签: component=scheduling");
        logBeanSuccess("schedulerMetrics");

        return metrics;
    }

    /**
     * 应用关闭时的清理工作。
     *
     * <p>虽然ThreadPoolTaskScheduler自身会在Spring容器销毁时进行清理，
     * 但我们在这里添加额外的日志记录，以便更好地跟踪应用的关闭过程。</p>
     */
    @PreDestroy
    public void destroy() {
        logInfo("开始执行定时任务配置清理...");

        // Spring会自动处理ThreadPoolTaskScheduler的优雅关闭
        // 这里主要用于日志记录和额外的清理工作

        logSuccess("定时任务配置清理完成");
    }

    /**
     * 调度器健康检查实现类。
     *
     * <p>实现Spring Boot Actuator的HealthIndicator接口，
     * 定期检查调度器的运行状态并报告详细信息。</p>
     */
    private record SchedulerHealthIndicator(ThreadPoolTaskScheduler taskScheduler) implements HealthIndicator {

        /**
         * 执行健康检查并返回结果。
         *
         * @return Health对象，包含状态和详细信息
         */
        @Override
        public Health health() {
            try {
                // 检查调度器是否初始化和运行
                if (taskScheduler.getScheduledExecutor() == null) {
                    return Health.down()
                            .withDetail("reason", "调度器尚未初始化")
                            .withDetail("status", "NOT_INITIALIZED")
                            .build();
                }

                if (taskScheduler.getScheduledExecutor().isShutdown()) {
                    return Health.down()
                            .withDetail("reason", "调度器已关闭")
                            .withDetail("status", "SHUTDOWN")
                            .build();
                }

                // 获取线程池状态信息
                ThreadPoolExecutor executor = (ThreadPoolExecutor) taskScheduler.getScheduledExecutor();

                return buildHealthStatus(executor);

            } catch (Exception e) {
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .withDetail("status", "ERROR")
                        .withException(e)
                        .build();
            }
        }

        /**
         * 构建健康状态
         */
        private Health buildHealthStatus(ThreadPoolExecutor executor) {
            int poolSize = executor.getPoolSize();
            int activeCount = executor.getActiveCount();
            long completedTaskCount = executor.getCompletedTaskCount();
            long taskCount = executor.getTaskCount();
            int queueSize = executor.getQueue().size();

            // 构建健康状态响应
            Health.Builder healthBuilder = Health.up()
                    .withDetail("status", "RUNNING")
                    .withDetail("poolSize", poolSize)
                    .withDetail("activeThreads", activeCount)
                    .withDetail("completedTasks", completedTaskCount)
                    .withDetail("totalTasks", taskCount)
                    .withDetail("queueSize", queueSize)
                    .withDetail("threadNamePrefix", taskScheduler.getThreadNamePrefix());

            // 检查是否有线程池压力过大的情况，计算利用率
            if (poolSize > 0) {
                double utilizationRate = (double) activeCount / poolSize;
                healthBuilder.withDetail("utilizationRate", String.format("%.2f%%", utilizationRate * 100));

                // 如果利用率过高，发出警告但不标记为DOWN
                if (utilizationRate > 0.9) {
                    healthBuilder.withDetail("warning", "线程池利用率较高，可能需要增加线程数");
                    healthBuilder.withDetail("recommendation", "考虑增加线程池大小或检查任务执行效率");
                }
            }

            // 队列状态检查
            if (queueSize > 0) {
                healthBuilder.withDetail("info", String.format("队列中有 %d 个待执行任务", queueSize));
            }

            // 任务执行统计
            if (taskCount > 0) {
                double completionRate = (double) completedTaskCount / taskCount;
                healthBuilder.withDetail("completionRate", String.format("%.2f%%", completionRate * 100));
            }

            return healthBuilder.build();
        }
    }
}