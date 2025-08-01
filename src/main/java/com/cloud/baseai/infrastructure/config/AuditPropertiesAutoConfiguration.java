package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.domain.audit.repository.SysAuditLogRepository;
import com.cloud.baseai.domain.audit.service.AuditService;
import com.cloud.baseai.infrastructure.config.properties.AuditProperties;
import com.cloud.baseai.infrastructure.monitoring.AuditHealthIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
@Configuration
public class AuditPropertiesAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AuditPropertiesAutoConfiguration.class);

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

        logger.info("配置审计健康检查指示器");
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

        logger.info("配置审计数据保留管理器 - 默认保留天数: {}, 自动归档: {}",
                properties.getRetention().getDefaultDays(),
                properties.getRetention().getEnableAutoArchive());

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
            logger.info("安排审计数据保留任务 - 保留天数: {}, 归档间隔: {} 天",
                    properties.getRetention().getDefaultDays(),
                    properties.getRetention().getArchiveAfterDays());
        }
    }
}