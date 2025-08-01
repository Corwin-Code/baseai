package com.cloud.baseai.infrastructure.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.validation.annotation.Validated;

import static java.lang.Thread.NORM_PRIORITY;


/**
 * <h2>定时任务调度器的配置属性类</h2>
 *
 * <p>此类封装了线程池任务调度器的所有可配置参数，支持通过外部配置文件进行调整，
 * 无需修改代码即可适应不同的部署环境需求。</p>
 */
@Setter
@Getter
@Validated
@ConfigurationProperties(prefix = "baseai.scheduling")
public class SchedulingProperties {

    // ==================== 主配置属性 ====================

    /**
     * 是否启用定时任务功能。
     *
     * <p>全局开关，可以在不删除定时任务代码的情况下禁用所有定时任务。
     * 这在某些环境（如开发环境）中非常有用。</p>
     */
    private boolean enabled = true;

    /**
     * 线程池配置。
     */
    private Pool pool = new Pool();

    /**
     * 线程配置。
     */
    private Thread thread = new Thread();

    /**
     * 关闭配置。
     */
    private Shutdown shutdown = new Shutdown();

    /**
     * 线程池大小配置。
     *
     * <p>此内部类封装了与线程池大小相关的所有配置项，包括核心线程数、
     * 最大线程数等。这种嵌套结构使配置更加清晰和有组织。</p>
     */
    @Setter
    @Getter
    public static class Pool {

        /**
         * 线程池的核心线程数（同时也是最大线程数）。
         *
         * <p>对于 {@link ThreadPoolTaskScheduler}，
         * 这个值既是核心线程数也是最大线程数。建议根据以下因素设置：</p>
         * <ul>
         *   <li><strong>定时任务数量：</strong> 至少等于同时运行的定时任务数</li>
         *   <li><strong>任务执行时间：</strong> 长时间运行的任务需要更多线程</li>
         *   <li><strong>服务器资源：</strong> 考虑CPU核心数和内存限制</li>
         * </ul>
         */
        @Min(value = 1, message = "线程池大小必须至少为1")
        private int size = 10;

        /**
         * 线程池是否允许核心线程超时。
         *
         * <p>当设置为 {@code true} 时，即使是核心线程在空闲超过 keep-alive 时间后
         * 也会被回收，这有助于在低负载时期节省系统资源。</p>
         */
        private boolean allowCoreThreadTimeout = false;

        /**
         * 线程空闲时的存活时间（秒）。
         *
         * <p>当 {@link #allowCoreThreadTimeout} 为 {@code true} 时生效，
         * 空闲超过此时间的线程将被回收。</p>
         */
        @Positive(message = "线程存活时间必须为正数")
        private int keepAliveSeconds = 60;

    }

    /**
     * 线程配置。
     *
     * <p>封装了线程相关的配置项，如线程名称前缀、优先级等。</p>
     */
    @Setter
    @Getter
    public static class Thread {

        /**
         * 线程名称前缀。
         *
         * <p>为调度器创建的线程设置统一的名称前缀，便于在日志、监控工具中
         * 识别和追踪。线程的完整名称格式为：{prefix}{thread-number}</p>
         *
         * <p><strong>命名建议：</strong></p>
         * <ul>
         *   <li>包含应用名称：如 "MyApp-Scheduler-"</li>
         *   <li>包含环境标识：如 "Prod-Scheduler-"</li>
         *   <li>保持简洁明了：避免过长的前缀</li>
         * </ul>
         */
        @NotBlank(message = "线程名称前缀不能为空")
        private String namePrefix = "BaseAI-Scheduled-";

        /**
         * 线程优先级。
         *
         * <p>设置调度器线程的优先级，范围为 1-10，其中：</p>
         * <ul>
         *   <li>1 = {@link java.lang.Thread#MIN_PRIORITY} （最低优先级）</li>
         *   <li>5 = {@link java.lang.Thread#NORM_PRIORITY} （正常优先级，默认值）</li>
         *   <li>10 = {@link java.lang.Thread#MAX_PRIORITY} （最高优先级）</li>
         * </ul>
         *
         * <p><strong>注意：</strong> 在大多数情况下，建议使用默认优先级（5），
         * 除非有特殊的性能要求。</p>
         */
        @Min(value = 1, message = "线程优先级最小值为1")
        @Max(value = 10, message = "线程优先级最大值为10")
        private int priority = NORM_PRIORITY;

    }

    /**
     * 关闭配置。
     *
     * <p>封装了应用关闭时调度器的行为配置。</p>
     */
    @Setter
    @Getter
    public static class Shutdown {

        /**
         * 是否等待正在执行的任务完成后再关闭调度器。
         *
         * <p>当设置为 {@code true} 时，应用关闭时会等待所有正在执行的定时任务
         * 完成后再关闭线程池，这可以防止数据丢失或业务逻辑中断。</p>
         *
         * <p><strong>建议：</strong></p>
         * <ul>
         *   <li>生产环境：建议设置为 {@code true}</li>
         *   <li>开发环境：可以设置为 {@code false} 以加快重启速度</li>
         * </ul>
         */
        private boolean waitForTasksToComplete = true;

        /**
         * 等待任务完成的最长时间（秒）。
         *
         * <p>当 {@link #waitForTasksToComplete} 为 {@code true} 时生效。
         * 如果在指定时间内任务仍未完成，将强制关闭线程池。</p>
         *
         * <p><strong>设置建议：</strong></p>
         * <ul>
         *   <li>根据最长任务的执行时间设置</li>
         *   <li>一般设置为 30-300 秒</li>
         *   <li>过短可能导致任务被强制中断</li>
         *   <li>过长可能导致应用关闭时间过长</li>
         * </ul>
         */
        @Positive(message = "等待终止时间必须为正数")
        private int awaitTerminationSeconds = 60;

    }
}