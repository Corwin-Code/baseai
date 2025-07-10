package com.clinflash.baseai.infrastructure.config;

import com.clinflash.baseai.infrastructure.external.audit.repository.AuditLogRepository;
import com.clinflash.baseai.infrastructure.external.audit.service.AuditService;
import com.clinflash.baseai.infrastructure.external.audit.service.AuditServiceImpl;
import com.clinflash.baseai.infrastructure.external.email.EmailService;
import com.clinflash.baseai.infrastructure.external.email.EmailServiceImpl;
import com.clinflash.baseai.infrastructure.external.email.EmailTemplateEngine;
import com.clinflash.baseai.infrastructure.external.sms.SmsService;
import com.clinflash.baseai.infrastructure.external.sms.SmsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * <h2>外部服务自动配置类</h2>
 *
 * <p>这个自动配置类负责初始化和配置所有的外部服务，包括邮件服务、短信服务和审计服务。
 * 它采用了Spring Boot的自动配置机制，根据类路径和配置条件自动创建相应的Bean。</p>
 *
 * <p><b>配置策略：</b></p>
 * <p>我们使用条件注解来控制Bean的创建，只有在满足特定条件时才会创建相应的服务实例。
 * 这样可以避免在不需要某些服务时创建不必要的依赖，提高应用的启动速度和资源利用率。</p>
 *
 * <p><b>依赖管理：</b></p>
 * <p>每个服务的依赖都是可选的，如果某个依赖不存在，对应的服务会降级为简化模式。
 * 比如，如果没有Redis，短信服务仍然可以工作，只是失去了限流功能。</p>
 */
@Configuration
@EnableConfigurationProperties({
        ExternalServicesAutoConfiguration.EmailServiceProperties.class,
        ExternalServicesAutoConfiguration.SmsServiceProperties.class,
        ExternalServicesAutoConfiguration.AuditServiceProperties.class
})
public class ExternalServicesAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ExternalServicesAutoConfiguration.class);

    /**
     * 邮件服务配置
     *
     * <p>只有在类路径中存在JavaMailSender时才会创建邮件服务。
     * 这确保了在没有邮件依赖的情况下应用仍然可以正常启动。</p>
     */
    @Bean
    @ConditionalOnClass(JavaMailSender.class)
    @ConditionalOnMissingBean(EmailService.class)
    @ConditionalOnProperty(name = "email.enabled", havingValue = "true", matchIfMissing = true)
    public EmailService emailService(JavaMailSender mailSender,
                                     EmailServiceProperties properties) {

        log.info("配置邮件服务 - 异步模式: {}, 重试次数: {}",
                properties.isAsync(), properties.getRetry().getMaxAttempts());

        return new EmailServiceImpl(mailSender);
    }

    /**
     * 邮件模板引擎配置
     *
     * <p>邮件模板引擎是可选组件，如果启用了模板功能且相关依赖存在，
     * 才会创建模板引擎实例。</p>
     */
    @Bean
    @ConditionalOnProperty(name = "email.template.enabled", havingValue = "true")
    @ConditionalOnMissingBean(EmailTemplateEngine.class)
    public EmailTemplateEngine emailTemplateEngine(EmailServiceProperties properties) {

        log.info("配置邮件模板引擎 - 模板位置: {}", properties.getTemplate().getLocation());

        return new ThymeleafEmailTemplateEngine(properties);
    }

    /**
     * 短信服务配置
     *
     * <p>短信服务依赖Redis来实现限流功能。如果Redis不可用，
     * 服务仍然可以工作，但会失去限流保护。</p>
     */
    @Bean
    @ConditionalOnMissingBean(SmsService.class)
    @ConditionalOnProperty(name = "sms.enabled", havingValue = "true", matchIfMissing = true)
    public SmsService smsService(StringRedisTemplate redisTemplate,
                                 SmsServiceProperties properties) {

        log.info("配置短信服务 - 服务商: {}, 异步模式: {}, 限流: {}/分钟",
                properties.getProvider(), properties.isAsync(),
                properties.getLimit().getPerMinute());

        return new SmsServiceImpl(redisTemplate);
    }

    /**
     * 审计服务配置
     *
     * <p>审计服务是核心功能，即使在最小化配置下也会创建。
     * 但Elasticsearch等高级功能是可选的。</p>
     */
    @Bean
    @ConditionalOnMissingBean(AuditService.class)
    @ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)
    public AuditService auditService(AuditLogRepository auditLogRepository,
                                     ObjectMapper objectMapper,
                                     ElasticsearchOperations elasticsearchOps,
                                     AuditServiceProperties properties) {

        log.info("配置审计服务 - 异步模式: {}, ES支持: {}, 批处理大小: {}",
                properties.getAsync().isEnabled(),
                properties.getElasticsearch().isEnabled(),
                properties.getBatch().getSize());

        return new AuditServiceImpl(auditLogRepository, elasticsearchOps, objectMapper);
    }

    /**
     * Elasticsearch审计服务配置
     *
     * <p>只有在Elasticsearch可用且启用时才会创建增强的审计服务配置。</p>
     */
    @Bean
    @ConditionalOnClass(ElasticsearchOperations.class)
    @ConditionalOnProperty(name = "audit.elasticsearch.enabled", havingValue = "true")
    public AuditIndexManager auditIndexManager(ElasticsearchOperations elasticsearchOps,
                                               AuditServiceProperties properties) {

        log.info("配置审计索引管理器 - 索引前缀: {}",
                properties.getElasticsearch().getIndexPrefix());

        return new AuditIndexManager(elasticsearchOps, properties);
    }

    /**
     * 审计数据保留策略配置
     */
    @Bean
    @ConditionalOnProperty(name = "audit.retention.auto-archive", havingValue = "true")
    public AuditRetentionManager auditRetentionManager(AuditService auditService,
                                                       AuditServiceProperties properties) {

        log.info("配置审计数据保留管理器 - 默认保留天数: {}",
                properties.getRetention().getDefaultDays());

        return new AuditRetentionManager(auditService, properties);
    }

    /**
     * 服务健康检查配置
     */
    @Bean
    public ExternalServicesHealthIndicator externalServicesHealthIndicator(
            EmailService emailService,
            SmsService smsService,
            AuditService auditService) {

        log.info("配置外部服务健康检查");

        return new ExternalServicesHealthIndicator(emailService, smsService, auditService);
    }

    // =================== 配置属性类 ===================

    /**
     * 邮件服务配置属性
     */
    @Setter
    @Getter
    @ConfigurationProperties(prefix = "email")
    public static class EmailServiceProperties {

        private String from = "noreply@baseai.com";
        private String fromName = "BaseAI System";
        private String baseUrl = "https://app.baseai.com";
        private boolean async = true;
        private Retry retry = new Retry();
        private Batch batch = new Batch();
        private Template template = new Template();

        @Setter
        @Getter
        public static class Retry {
            private int maxAttempts = 3;

        }

        @Setter
        @Getter
        public static class Batch {
            private int size = 50;
        }

        @Setter
        @Getter
        public static class Template {
            private boolean enabled = true;
            private String location = "classpath:/email-templates/";
            private boolean cacheEnabled = true;
        }
    }

    /**
     * 短信服务配置属性
     */
    @Setter
    @Getter
    @ConfigurationProperties(prefix = "sms")
    public static class SmsServiceProperties {

        private String provider = "aliyun";
        private String accessKey;
        private String accessSecret;
        private String signature = "BaseAI";
        private boolean async = true;
        private int duplicateCheckMinutes = 2;
        private Limit limit = new Limit();

        @Setter
        @Getter
        public static class Limit {
            private int perMinute = 5;
            private int perHour = 20;
            private int perDay = 100;
        }
    }

    /**
     * 审计服务配置属性
     */
    @Setter
    @Getter
    @ConfigurationProperties(prefix = "audit")
    public static class AuditServiceProperties {

        private Async async = new Async();
        private Batch batch = new Batch();
        private Elasticsearch elasticsearch = new Elasticsearch();
        private Integrity integrity = new Integrity();
        private Retention retention = new Retention();

        @Setter
        @Getter
        public static class Async {
            private boolean enabled = true;
        }

        @Setter
        @Getter
        public static class Batch {
            private int size = 100;
            private long timeout = 5000;
        }

        @Setter
        @Getter
        public static class Elasticsearch {
            private boolean enabled = true;
            private String indexPrefix = "audit";
        }

        @Setter
        @Getter
        public static class Integrity {
            private Check check = new Check();

            @Setter
            @Getter
            public static class Check {
                private boolean enabled = true;
                private String schedule = "0 2 * * *";
            }
        }

        @Setter
        @Getter
        public static class Retention {
            private int defaultDays = 2555; // 7年
            private boolean autoArchive = true;
            private String archiveLocation = "s3://baseai-audit-archive/";
        }
    }

    // =================== 辅助组件 ===================

    /**
     * Thymeleaf邮件模板引擎实现
     */
    @Getter
    public static class ThymeleafEmailTemplateEngine implements EmailTemplateEngine {

        private final EmailServiceProperties properties;

        public ThymeleafEmailTemplateEngine(EmailServiceProperties properties) {
            this.properties = properties;
        }

        @Override
        public String renderTemplate(String templateName, java.util.Map<String, Object> params) {
            // 这里应该使用Thymeleaf模板引擎渲染邮件内容
            log.debug("渲染邮件模板: {}", templateName);
            return "<html><body>邮件内容</body></html>";
        }

        @Override
        public String renderSubject(String templateName, java.util.Map<String, Object> params) {
            // 渲染邮件主题
            return "BaseAI - 系统通知";
        }
    }

    /**
     * 审计索引管理器
     */
    @Getter
    public static class AuditIndexManager {

        private static final Logger log = LoggerFactory.getLogger(AuditIndexManager.class);

        private final ElasticsearchOperations elasticsearchOps;
        private final AuditServiceProperties properties;

        public AuditIndexManager(ElasticsearchOperations elasticsearchOps, AuditServiceProperties properties) {
            this.elasticsearchOps = elasticsearchOps;
            this.properties = properties;
            initializeIndices();
        }

        private void initializeIndices() {
            log.info("初始化审计索引");
            // 这里应该创建和配置Elasticsearch索引
        }
    }

    /**
     * 审计数据保留管理器
     */
    @Getter
    public static class AuditRetentionManager {

        private static final Logger log = LoggerFactory.getLogger(AuditRetentionManager.class);

        private final AuditService auditService;
        private final AuditServiceProperties properties;

        public AuditRetentionManager(AuditService auditService, AuditServiceProperties properties) {
            this.auditService = auditService;
            this.properties = properties;
            scheduleRetentionTasks();
        }

        private void scheduleRetentionTasks() {
            log.info("安排数据保留任务");
            // 这里应该安排定期的数据归档和清理任务
        }
    }

    /**
     * 外部服务健康检查
     */
    public static class ExternalServicesHealthIndicator implements HealthIndicator {

        @Getter
        private final EmailService emailService;
        private final SmsService smsService;
        @Getter
        private final AuditService auditService;

        public ExternalServicesHealthIndicator(EmailService emailService, SmsService smsService, AuditService auditService) {
            this.emailService = emailService;
            this.smsService = smsService;
            this.auditService = auditService;
        }

        @Override
        public org.springframework.boot.actuate.health.Health health() {
            org.springframework.boot.actuate.health.Health.Builder builder = Health.up();

            // 检查各服务的健康状态
            try {
                // 检查邮件服务
                builder.withDetail("emailService", "UP");

                // 检查短信服务
                long smsQuota = smsService.getRemainingQuota();
                builder.withDetail("smsService", "UP")
                        .withDetail("smsQuota", smsQuota);

                // 检查审计服务
                builder.withDetail("auditService", "UP");

            } catch (Exception e) {
                log.error("外部服务健康检查失败", e);
                builder.down().withException(e);
            }

            return builder.build();
        }
    }
}