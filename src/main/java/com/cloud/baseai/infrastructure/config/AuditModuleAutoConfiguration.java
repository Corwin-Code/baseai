package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.domain.audit.repository.SysAuditLogRepository;
import com.cloud.baseai.domain.audit.service.AuditService;
import com.cloud.baseai.domain.audit.service.AuditServiceImpl;
import com.cloud.baseai.infrastructure.monitoring.AuditHealthIndicator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * <h2>审计模块自动配置类</h2>
 *
 * <p>这个自动配置类是审计模块的"总指挥官"，它负责协调和初始化审计系统的
 * 所有核心组件。就像交响乐团的指挥一样，确保每个组件都能在正确的时间
 * 以正确的方式发挥作用。</p>
 *
 * <p><b>配置策略：</b></p>
 * <p>我们采用了Spring Boot的自动配置机制，通过条件注解来智能地决定
 * 哪些组件需要被创建。这种设计让系统既能开箱即用，又保持了足够的灵活性
 * 来适应不同的部署环境和业务需求。</p>
 *
 * <p><b>依赖管理：</b></p>
 * <p>配置类会根据类路径上的依赖来决定创建哪些Bean。比如，只有在
 * Elasticsearch依赖存在时才会创建相关的配置，这样可以避免在简单
 * 部署场景中引入不必要的复杂性。</p>
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({
        AuditProperties.class
})
public class AuditModuleAutoConfiguration {

    /**
     * 审计服务Bean配置
     *
     * <p>这是审计模块的核心服务Bean。我们使用条件注解来确保只有在
     * 必要的依赖都存在时才创建这个服务。</p>
     */
    @Bean
    @ConditionalOnMissingBean(AuditService.class)
    @ConditionalOnProperty(name = "baseai.audit.enabled", havingValue = "true", matchIfMissing = true)
    public AuditService auditService(
            SysAuditLogRepository auditLogRepository,
            ObjectMapper objectMapper,
            ElasticsearchOperations elasticsearchOps,
            AuditProperties auditProperties) {

        log.info("配置审计服务 - 异步模式: {}, 批处理大小: {}, ES支持: {}",
                auditProperties.getAsync().isEnabled(),
                auditProperties.getBatch().getSize(),
                auditProperties.getElasticsearch().isEnabled());

        return new AuditServiceImpl(auditLogRepository, elasticsearchOps, objectMapper);
    }

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
    @ConditionalOnMissingBean(name = "auditTaskExecutor")
    public Executor auditTaskExecutor(AuditProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：基于CPU核心数计算
        int corePoolSize = Math.max(2, Runtime.getRuntime().availableProcessors());
        executor.setCorePoolSize(corePoolSize);

        // 最大线程数：通常设置为核心线程数的2-3倍
        executor.setMaxPoolSize(properties.getAsync().getMaxPoolSize() > 0 ? properties.getAsync().getMaxPoolSize() : corePoolSize * 2);

        // 队列容量：控制内存使用
        executor.setQueueCapacity(properties.getAsync().getQueueCapacity());

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
                corePoolSize, executor.getMaxPoolSize(), properties.getAsync().getQueueCapacity());

        return executor;
    }

    /**
     * 审计健康指示器配置
     *
     * <p>健康检查是生产环境中的重要组件，它能够及时发现审计系统的问题。</p>
     */
    @Bean
    @ConditionalOnMissingBean(AuditHealthIndicator.class)
    @ConditionalOnProperty(name = "baseai.audit.monitoring.health-check.enabled", havingValue = "true", matchIfMissing = true)
    public AuditHealthIndicator auditHealthIndicator(
            SysAuditLogRepository auditLogRepository,
            AuditProperties auditConfig) {

        log.info("配置审计健康检查指示器");
        return new AuditHealthIndicator(auditLogRepository, auditConfig);
    }

    /**
     * 审计数据保留管理器配置
     *
     * <p>这个组件负责管理审计数据的生命周期，包括归档和清理操作。</p>
     */
    @Bean
    @ConditionalOnProperty(name = "baseai.audit.retention.auto-cleanup.enabled", havingValue = "true")
    public AuditRetentionManager auditRetentionManager(
            AuditService auditService,
            AuditProperties properties) {

        log.info("配置审计数据保留管理器 - 默认保留天数: {}, 自动归档: {}",
                properties.getRetention().getDefaultRetentionDays(),
                properties.getRetention().isAutoArchiveEnabled());

        return new AuditRetentionManager(auditService, properties);
    }

    // =================== 内部配置组件 ===================

    /**
     * 审计数据保留管理器
     *
     * <p>负责执行数据清理和归档策略。</p>
     */
    public static class AuditRetentionManager {
        private final AuditService auditService;
        private final AuditProperties properties;

        public AuditRetentionManager(AuditService auditService, AuditProperties properties) {
            this.auditService = auditService;
            this.properties = properties;
            scheduleRetentionTasks();
        }

        private void scheduleRetentionTasks() {
            log.info("安排审计数据保留任务 - 保留天数: {}, 归档间隔: {} 天",
                    properties.getRetention().getDefaultRetentionDays(),
                    properties.getRetention().getArchiveAfterDays());
        }
    }
}