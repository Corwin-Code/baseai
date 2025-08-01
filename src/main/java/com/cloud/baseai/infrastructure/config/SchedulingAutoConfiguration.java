package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.config.properties.SchedulingProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

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
public class SchedulingAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SchedulingAutoConfiguration.class);

    private final SchedulingProperties schedulingProperties;

    /**
     * 构造函数，注入配置属性。
     *
     * @param schedulingProperties 定时任务配置属性
     */
    public SchedulingAutoConfiguration(SchedulingProperties schedulingProperties) {
        this.schedulingProperties = schedulingProperties;
        logger.info("初始化定时任务配置: {}", schedulingProperties);
    }

    /**
     * 创建并配置线程池任务调度器Bean。
     *
     * @return 配置好的ThreadPoolTaskScheduler实例
     * @see ThreadPoolTaskScheduler
     */
    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        logger.info("创建ThreadPoolTaskScheduler，线程池大小: {}", schedulingProperties.getPool().getSize());

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(schedulingProperties.getPool().getSize());
        scheduler.setThreadNamePrefix(schedulingProperties.getThread().getNamePrefix());
        // 当应用关闭时，是否等待正在执行的任务完成
        scheduler.setWaitForTasksToCompleteOnShutdown(
                schedulingProperties.getShutdown().isWaitForTasksToComplete()
        );
        // 等待任务完成的最长时间
        scheduler.setAwaitTerminationSeconds(
                schedulingProperties.getShutdown().getAwaitTerminationSeconds()
        );
        // 设置线程优先级
        scheduler.setThreadPriority(schedulingProperties.getThread().getPriority());
        // 当线程池和队列都满时的处理策略
        // CallerRunsPolicy: 由调用线程执行任务，这样可以减缓任务提交速度
        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 初始化调度器
        scheduler.initialize();

        // 获取底层的 JDK 执行器，并设置高级参数
        ThreadPoolExecutor executor = scheduler.getScheduledThreadPoolExecutor();
        // 设置是否允许核心线程超时，这有助于在低负载时节省资源
        executor.allowCoreThreadTimeOut(schedulingProperties.getPool().isAllowCoreThreadTimeout());
        // 设置线程空闲时间，超过此时间的空闲线程将被回收
        executor.setKeepAliveTime(schedulingProperties.getPool().getKeepAliveSeconds(), TimeUnit.SECONDS);

        logger.info("ThreadPoolTaskScheduler创建完成 - 线程池大小: {}, 线程前缀: '{}', 优雅关闭: {}",
                schedulingProperties.getPool().getSize(),
                schedulingProperties.getThread().getNamePrefix(),
                schedulingProperties.getShutdown().isWaitForTasksToComplete());

        return scheduler;
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
    public HealthIndicator schedulerHealthIndicator(ThreadPoolTaskScheduler taskScheduler) {
        return new SchedulerHealthIndicator(taskScheduler);
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
    public ExecutorServiceMetrics schedulerMetrics(ThreadPoolTaskScheduler taskScheduler, MeterRegistry meterRegistry) {

        if (meterRegistry == null) {
            logger.warn("MeterRegistry不可用，跳过调度器指标配置");
            return null;
        }

        logger.info("配置调度器指标收集");

        // 获取底层的ScheduledExecutorService进行指标绑定
        ScheduledExecutorService executorService = taskScheduler.getScheduledExecutor();

        return new ExecutorServiceMetrics(
                executorService, // 需要监控的 ExecutorService
                "scheduler",  // 指标中执行器的名称
                Tags.of("component", "scheduling") // 额外的标签
        );
    }

    /**
     * 应用关闭时的清理工作。
     *
     * <p>虽然ThreadPoolTaskScheduler自身会在Spring容器销毁时进行清理，
     * 但我们在这里添加额外的日志记录，以便更好地跟踪应用的关闭过程。</p>
     */
    @PreDestroy
    public void destroy() {
        logger.info("定时任务配置正在关闭...");
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
                            .build();
                }

                if (taskScheduler.getScheduledExecutor().isShutdown()) {
                    return Health.down()
                            .withDetail("reason", "调度器已关闭")
                            .build();
                }

                // 获取线程池状态信息
                ThreadPoolExecutor executor = (ThreadPoolExecutor) taskScheduler.getScheduledExecutor();

                int poolSize = executor.getPoolSize();
                int activeCount = executor.getActiveCount();
                long completedTaskCount = executor.getCompletedTaskCount();
                long taskCount = executor.getTaskCount();
                int queueSize = executor.getQueue().size();

                // 构建健康状态响应
                Health.Builder healthBuilder = Health.up()
                        .withDetail("poolSize", poolSize)
                        .withDetail("activeThreads", activeCount)
                        .withDetail("completedTasks", completedTaskCount)
                        .withDetail("totalTasks", taskCount)
                        .withDetail("queueSize", queueSize)
                        .withDetail("threadNamePrefix", taskScheduler.getThreadNamePrefix());

                // 检查是否有线程池压力过大的情况
                if (poolSize > 0) {
                    double utilizationRate = (double) activeCount / poolSize;
                    healthBuilder.withDetail("utilizationRate", String.format("%.2f%%", utilizationRate * 100));

                    // 如果利用率过高，发出警告但不标记为DOWN
                    if (utilizationRate > 0.9) {
                        healthBuilder.withDetail("warning", "线程池利用率较高，可能需要增加线程数");
                    }
                }

                return healthBuilder.build();

            } catch (Exception e) {
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .withException(e)
                        .build();
            }
        }
    }
}