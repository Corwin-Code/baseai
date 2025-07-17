package com.cloud.baseai.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * <h1>系统管理模块配置类</h1>
 *
 * <p>这个配置类承担着系统管理模块的"总指挥"角色。
 * 它集中管理着系统监控、任务调度、设置管理等核心功能的运行参数。</p>
 *
 * <p><b>设计哲学：</b></p>
 * <p>优秀的配置管理应该遵循"合理默认值 + 灵活调节"的原则。系统在大多数情况下
 * 应该能够使用默认配置正常运行，同时在需要优化性能或适应特殊环境时，
 * 管理员可以通过简单的配置修改来达到目标。</p>
 */
@Setter
@Getter
@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "baseai.system")
public class SystemProperties {

    /**
     * 任务管理配置
     */
    private TaskManagement taskManagement = new TaskManagement();

    /**
     * 健康检查配置
     */
    private HealthCheck healthCheck = new HealthCheck();

    /**
     * 监控配置
     */
    private Monitoring monitoring = new Monitoring();

    /**
     * 安全配置
     */
    private Security security = new Security();

    /**
     * 系统任务调度器
     *
     * <p>这个调度器就像是系统的"心跳"，它定期执行各种维护任务，
     * 比如清理过期数据、生成统计报告、检查系统健康状态等。</p>
     *
     * <p>线程池的大小需要仔细平衡：太小会导致任务积压，太大会消耗过多资源。
     * 我们选择较小的线程池是因为系统任务通常不是时间敏感的，
     * 而且我们希望避免对主业务流程造成资源竞争。</p>
     */
    @Bean("systemTaskScheduler")
    public ThreadPoolTaskScheduler systemTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        // 设置核心线程数：足够处理常规的定时任务
        scheduler.setPoolSize(3);

        // 设置线程名称前缀：便于日志追踪和问题排查
        scheduler.setThreadNamePrefix("SystemTask-");

        // 等待任务完成再关闭：确保系统优雅退出
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);

        // 设置线程优先级：系统任务优先级稍低，避免影响用户请求
        scheduler.setThreadPriority(Thread.NORM_PRIORITY - 1);

        scheduler.initialize();
        return scheduler;
    }

    /**
     * 监控数据收集执行器
     *
     * <p>监控系统需要持续收集各种指标数据，这个专用的执行器确保
     * 监控活动不会干扰正常的业务处理。就像医院的监护设备，
     * 它需要持续工作但不能影响医生的正常治疗。</p>
     */
    @Bean("monitoringExecutor")
    public ThreadPoolTaskScheduler monitoringExecutor() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("Monitoring-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(15);
        scheduler.initialize();
        return scheduler;
    }

    /**
     * 任务管理配置内部类
     */
    @Setter
    @Getter
    public static class TaskManagement {
        /**
         * 任务最大重试次数
         */
        private int maxRetryCount = 3;

        /**
         * 任务超时时间（秒）
         */
        private int taskTimeoutSeconds = 300;

        /**
         * 批量处理大小
         */
        private int batchSize = 100;

        /**
         * 失败任务重试间隔（分钟）
         */
        private int retryIntervalMinutes = 5;

        /**
         * 已完成任务保留天数
         */
        private int completedTaskRetentionDays = 7;

    }

    /**
     * 健康检查配置内部类
     */
    @Setter
    @Getter
    public static class HealthCheck {
        /**
         * 健康检查间隔（秒）
         */
        private int intervalSeconds = 30;

        /**
         * 检查超时时间（毫秒）
         */
        private int timeoutMs = 5000;

        /**
         * 失败阈值：连续失败多少次认为组件不健康
         */
        private int failureThreshold = 3;

        /**
         * 是否启用详细检查
         */
        private boolean detailedCheckEnabled = true;

    }

    /**
     * 监控配置内部类
     */
    @Setter
    @Getter
    public static class Monitoring {
        /**
         * 指标收集间隔（分钟）
         */
        private int metricsCollectionIntervalMinutes = 5;

        /**
         * 性能数据保留天数
         */
        private int performanceDataRetentionDays = 30;

        /**
         * 是否启用慢查询监控
         */
        private boolean slowQueryMonitoringEnabled = true;

        /**
         * 慢查询阈值（毫秒）
         */
        private long slowQueryThresholdMs = 1000;

    }

    /**
     * 安全配置内部类
     */
    @Setter
    @Getter
    public static class Security {
        /**
         * 敏感配置掩码字符
         */
        private String maskCharacter = "*";

        /**
         * 审计日志保留天数
         */
        private int auditLogRetentionDays = 90;

        /**
         * 是否启用配置变更审计
         */
        private boolean configChangeAuditEnabled = true;

        /**
         * 系统管理员会话超时时间（分钟）
         */
        private int adminSessionTimeoutMinutes = 60;

    }
}