package com.clinflash.baseai.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * <h2>审计模块配置类</h2>
 *
 * <p>这个配置类就像是审计系统的"控制面板"，它定义了审计系统的各种参数和行为。
 * 想象一下，如果审计系统是一台精密的仪器，那么这个配置类就是它的设置界面，
 * 让我们可以根据不同的业务需求和环境来调整系统的运行方式。</p>
 *
 * <p><b>配置职责：</b></p>
 * <ul>
 * <li><b>性能调优：</b>配置线程池大小、批处理参数等性能相关设置</li>
 * <li><b>功能开关：</b>控制哪些功能启用，哪些功能禁用</li>
 * <li><b>存储策略：</b>定义数据保留期限、归档策略等</li>
 * <li><b>安全设置：</b>配置敏感信息过滤、访问控制等安全相关参数</li>
 * </ul>
 */
@Setter
@Getter
@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "baseai.audit")
public class AuditConfig {

    private static final Logger log = LoggerFactory.getLogger(AuditConfig.class);

    /**
     * 异步处理配置
     * <p>控制审计日志的异步写入行为，确保不影响主业务流程的性能</p>
     */
    private AsyncConfig async = new AsyncConfig();

    /**
     * 数据保留配置
     * <p>定义审计数据的生命周期管理策略</p>
     */
    private RetentionConfig retention = new RetentionConfig();

    /**
     * 安全过滤配置
     * <p>控制敏感信息的记录和显示</p>
     */
    private SecurityConfig security = new SecurityConfig();

    /**
     * 性能监控配置
     * <p>控制性能指标的收集和报告</p>
     */
    private MonitoringConfig monitoring = new MonitoringConfig();

    /**
     * 创建审计专用的异步执行器
     *
     * <p>这个执行器专门用于处理审计相关的异步任务。我们为它配置了合适的
     * 线程池大小和队列容量，确保在高并发场景下也能稳定运行。</p>
     *
     * <p><b>设计考虑：</b></p>
     * <p>审计任务通常是I/O密集型的（写数据库、发送通知等），所以我们设置了
     * 相对较大的线程池。同时，为了防止内存溢出，我们限制了队列大小，
     * 当队列满时会使用调用者线程执行任务，这样可以起到自然的背压作用。</p>
     */
    @Bean("auditTaskExecutor")
    public Executor auditTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：基于CPU核心数计算
        int corePoolSize = Math.max(2, Runtime.getRuntime().availableProcessors());
        executor.setCorePoolSize(corePoolSize);

        // 最大线程数：通常设置为核心线程数的2-3倍
        executor.setMaxPoolSize(async.maxPoolSize > 0 ? async.maxPoolSize : corePoolSize * 2);

        // 队列容量：控制内存使用
        executor.setQueueCapacity(async.queueCapacity);

        // 线程名前缀：便于日志追踪和问题排查
        executor.setThreadNamePrefix("audit-");

        // 拒绝策略：队列满时使用调用者线程执行
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        // 允许核心线程超时：在空闲时释放资源
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);

        // 优雅关闭：等待任务完成后再关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("审计异步执行器初始化完成: 核心线程={}, 最大线程={}, 队列容量={}",
                corePoolSize, executor.getMaxPoolSize(), async.queueCapacity);

        return executor;
    }

    // =================== 配置属性类 ===================

    /**
     * <h3>异步处理配置</h3>
     *
     * <p>这个内部类定义了异步处理相关的所有配置参数。异步处理是审计系统
     * 性能优化的关键，它让审计记录不会阻塞主业务流程。</p>
     */
    @Setter
    @Getter
    public static class AsyncConfig {

        /**
         * 是否启用异步处理
         * <p>默认启用。在某些特殊情况下（如调试、测试），可能需要禁用异步处理</p>
         */
        private boolean enabled = true;

        /**
         * 最大线程池大小
         * <p>控制同时处理审计任务的最大线程数</p>
         */
        private int maxPoolSize = 20;

        /**
         * 队列容量
         * <p>控制等待处理的任务队列大小，防止内存溢出</p>
         */
        private int queueCapacity = 1000;

        /**
         * 批处理大小
         * <p>控制批量写入数据库时的批次大小，影响性能和内存使用</p>
         */
        private int batchSize = 100;

        /**
         * 批处理超时时间（毫秒）
         * <p>控制批处理的最大等待时间，确保数据及时写入</p>
         */
        private long batchTimeoutMs = 5000;

    }

    /**
     * <h3>数据保留配置</h3>
     *
     * <p>审计数据通常需要长期保存以满足合规要求，但也需要控制存储成本。
     * 这个配置类定义了数据生命周期管理的策略。</p>
     */
    @Setter
    @Getter
    public static class RetentionConfig {

        /**
         * 默认保留天数
         * <p>通常设置为7年（2555天），满足大多数行业的合规要求</p>
         */
        private int defaultRetentionDays = 2555; // 7年

        /**
         * 是否启用自动归档
         * <p>将旧数据移动到成本更低的存储介质</p>
         */
        private boolean autoArchiveEnabled = true;

        /**
         * 归档阈值天数
         * <p>超过这个天数的数据会被归档</p>
         */
        private int archiveAfterDays = 365; // 1年

        /**
         * 是否启用自动清理
         * <p>彻底删除超过保留期限的数据</p>
         */
        private boolean autoCleanupEnabled = false;

        /**
         * 数据压缩级别
         * <p>控制归档数据的压缩程度，平衡存储空间和访问性能</p>
         */
        private int compressionLevel = 6;

    }

    /**
     * <h3>安全过滤配置</h3>
     *
     * <p>在记录审计信息时，我们需要特别注意不能记录敏感信息，如密码、
     * 信用卡号等。这个配置类定义了敏感信息的过滤规则。</p>
     */
    @Setter
    @Getter
    public static class SecurityConfig {

        /**
         * 是否启用敏感信息过滤
         */
        private boolean sensitiveDataFilterEnabled = true;

        /**
         * 需要过滤的字段名模式
         * <p>使用正则表达式匹配需要过滤的字段名</p>
         */
        private String[] sensitiveFieldPatterns = {
                "password", "pwd", "secret", "token", "key", "credit.*card", "ssn", "phone"
        };

        /**
         * 是否记录IP地址
         * <p>某些隐私要求严格的场景可能需要禁用IP记录</p>
         */
        private boolean recordIpAddress = true;

        /**
         * 是否记录用户代理信息
         */
        private boolean recordUserAgent = true;

        /**
         * 数据脱敏级别
         * <p>控制敏感数据的脱敏程度：NONE, PARTIAL, FULL</p>
         */
        private String maskingLevel = "PARTIAL";

    }

    /**
     * <h3>性能监控配置</h3>
     *
     * <p>为了确保审计系统本身不会成为性能瓶颈，我们需要监控审计系统的
     * 各项性能指标，如处理延迟、队列长度、错误率等。</p>
     */
    @Setter
    @Getter
    public static class MonitoringConfig {

        /**
         * 是否启用性能监控
         */
        private boolean enabled = true;

        /**
         * 指标收集间隔（秒）
         */
        private int metricsCollectionInterval = 60;

        /**
         * 慢操作阈值（毫秒）
         * <p>超过这个阈值的操作会被标记为慢操作</p>
         */
        private long slowOperationThreshold = 1000;

        /**
         * 是否启用JVM指标监控
         */
        private boolean jvmMetricsEnabled = true;

        /**
         * 是否启用数据库指标监控
         */
        private boolean databaseMetricsEnabled = true;

    }

}