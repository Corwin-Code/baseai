package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.cache.service.BaseRedisService;
import com.cloud.baseai.infrastructure.cache.service.impl.BaseRedisServiceImpl;
import com.cloud.baseai.infrastructure.config.base.BaseAutoConfiguration;
import com.cloud.baseai.infrastructure.config.properties.CacheProperties;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * <h2>Redis自动配置类</h2>
 *
 * <p>该配置类负责创建和配置Redis相关的Bean，包括RedisTemplate的序列化配置，
 * 确保数据在Redis中的存储和读取能够正确处理Java对象和JSON之间的转换。</p>
 *
 * <p><b>配置特点：</b></p>
 * <ul>
 * <li><b>JSON序列化：</b>使用Jackson进行值的序列化，提高可读性</li>
 * <li><b>字符串序列化：</b>键使用字符串序列化，确保键的可读性</li>
 * <li><b>类型安全：</b>配置了泛型类型，确保类型安全</li>
 * <li><b>兼容性：</b>支持多种Java对象类型的序列化</li>
 * </ul>
 */
@Configuration
@ConditionalOnClass(RedisConnectionFactory.class)
public class RedisAutoConfiguration extends BaseAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RedisAutoConfiguration.class);

    private final CacheProperties cacheProperties;

    public RedisAutoConfiguration(CacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;

        // 统一初始化
        initializeConfiguration();
    }

    @Override
    protected String getConfigurationName() {
        return "Redis缓存服务";
    }

    @Override
    protected String getModuleName() {
        return "REDIS";
    }

    @Override
    protected void validateConfiguration() {
        logInfo("开始验证Redis配置...");

        // 基础配置验证
        validateNotNull(cacheProperties, "缓存配置属性");
        validateNotNull(cacheProperties.getRedis(), "Redis配置");

        // Redis配置验证
        CacheProperties.RedisProperties redis = cacheProperties.getRedis();
        validateRange(redis.getDefaultTtlSeconds(), 1, 86400 * 30, "默认TTL");
        validateRange(redis.getMaxTtlSeconds(), redis.getDefaultTtlSeconds(), 86400 * 365, "最大TTL");
        validateNotBlank(redis.getKeyPrefix(), "键前缀");
        validateRange(redis.getCompressionThresholdBytes(), 100, 1024 * 1024, "压缩阈值");
        validateRange(redis.getMaxKeyLength(), 50, 1000, "最大键长度");

        // 应用级缓存配置验证
        CacheProperties.ApplicationProperties app = cacheProperties.getApplication();
        validateRange(app.getConfigCacheTtlSeconds(), 60, 3600, "配置缓存TTL");
        validateRange(app.getUserSessionCacheTtlSeconds(), 300, 86400, "会话缓存TTL");
        validateRange(app.getApiResponseCacheTtlSeconds(), 10, 3600, "API响应缓存TTL");
        validateRange(app.getMaxCacheSizeMb(), 64, 4096, "最大缓存大小");

        // 性能建议
        performanceRecommendations();

        logSuccess("Enhanced Redis配置验证通过");
    }

    /**
     * 性能建议
     */
    private void performanceRecommendations() {
        CacheProperties.RedisProperties redis = cacheProperties.getRedis();

        // TTL建议
        if (redis.getDefaultTtlSeconds() > 86400) {
            logWarning("默认TTL超过1天 (%d秒)，可能导致内存占用过高", redis.getDefaultTtlSeconds());
        }

        // 压缩建议
        if (!redis.getEnableCompression()) {
            logInfo("未启用压缩，大对象可能占用较多内存");
        } else if (redis.getCompressionThresholdBytes() < 512) {
            logWarning("压缩阈值较小 (%d字节)，可能影响性能", redis.getCompressionThresholdBytes());
        }

        // 键前缀建议
        if (redis.getKeyPrefix().length() > 20) {
            logWarning("键前缀较长 (%s)，建议简化以节省内存", redis.getKeyPrefix());
        }

        logInfo("Redis性能建议检查完成");
    }

    @Override
    protected Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();

        // Redis配置摘要
        CacheProperties.RedisProperties redis = cacheProperties.getRedis();
        summary.put("默认TTL(秒)", redis.getDefaultTtlSeconds());
        summary.put("最大TTL(秒)", redis.getMaxTtlSeconds());
        summary.put("键前缀", redis.getKeyPrefix());
        summary.put("启用压缩", redis.getEnableCompression());
        summary.put("压缩阈值(字节)", redis.getCompressionThresholdBytes());
        summary.put("最大键长度", redis.getMaxKeyLength());

        // 应用缓存配置摘要
        CacheProperties.ApplicationProperties app = cacheProperties.getApplication();
        summary.put("配置缓存TTL(秒)", app.getConfigCacheTtlSeconds());
        summary.put("会话缓存TTL(秒)", app.getUserSessionCacheTtlSeconds());
        summary.put("API响应缓存TTL(秒)", app.getApiResponseCacheTtlSeconds());
        summary.put("启用缓存预热", app.getEnableCacheWarming());
        summary.put("最大缓存大小(MB)", app.getMaxCacheSizeMb());
        summary.put("缓存淘汰策略", app.getEvictionPolicy());

        return summary;
    }

    /**
     * 创建优化的ObjectMapper用于Redis序列化
     *
     * <p>配置了Java时间模块支持和优化的序列化设置，确保所有对象类型
     * 都能正确序列化和反序列化。</p>
     *
     * @return 优化的ObjectMapper实例
     */
    @Bean(name = "redisObjectMapper")
    @ConditionalOnMissingBean(name = "redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        logBeanCreation("redisObjectMapper", "Redis专用JSON序列化器");

        ObjectMapper objectMapper = new ObjectMapper();

        // 注册Java时间模块
        objectMapper.registerModule(new JavaTimeModule());

        // 设置可见性：允许访问所有字段
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 启用默认类型信息，确保反序列化时类型正确
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        // 配置时间格式
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        logInfo("Redis ObjectMapper配置 - 支持Java时间类型, 启用类型信息");
        logBeanSuccess("redisObjectMapper");

        return objectMapper;
    }

    /**
     * 创建主要的RedisTemplate Bean
     *
     * <p>使用优化的序列化器和配置，支持复杂对象存储和类型安全操作。</p>
     *
     * @param connectionFactory Redis连接工厂
     * @param redisObjectMapper Redis专用ObjectMapper
     * @return 配置好的RedisTemplate实例
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {

        logBeanCreation("redisTemplate", "RedisTemplate主Bean");

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 创建字符串序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        // 创建JSON序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        // 配置键值序列化器
        template.setKeySerializer(stringSerializer);           // key序列化
        template.setValueSerializer(jsonSerializer);           // value序列化
        template.setHashKeySerializer(stringSerializer);       // hash key序列化
        template.setHashValueSerializer(jsonSerializer);       // hash value序列化

        // 启用默认序列化器
        template.setDefaultSerializer(jsonSerializer);
        template.setEnableDefaultSerializer(true);

        // 启用事务支持
        template.setEnableTransactionSupport(true);

        // 初始化模板
        template.afterPropertiesSet();

        logInfo("RedisTemplate配置完成 - 支持事务, 优化序列化");
        logBeanSuccess("redisTemplate");

        return template;
    }

    /**
     * 创建缓存优化的RedisTemplate
     *
     * <p>专门用于缓存场景的优化版本，使用更紧凑的序列化设置。</p>
     *
     * @param connectionFactory Redis连接工厂
     * @param redisObjectMapper Redis专用ObjectMapper
     * @return 缓存优化的RedisTemplate
     */
    @Bean("cacheRedisTemplate")
    @ConditionalOnMissingBean(name = "cacheRedisTemplate")
    public RedisTemplate<String, Object> cacheRedisTemplate(
            RedisConnectionFactory connectionFactory,
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {

        logBeanCreation("cacheRedisTemplate", "缓存优化RedisTemplate");

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 为缓存场景创建优化的序列化器（不包含类型信息）
        ObjectMapper cacheMapper = redisObjectMapper.copy();
        cacheMapper.deactivateDefaultTyping();

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer cacheSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(cacheSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(cacheSerializer);
        template.setDefaultSerializer(cacheSerializer);

        template.afterPropertiesSet();

        logInfo("缓存优化RedisTemplate配置完成");
        logBeanSuccess("cacheRedisTemplate");

        return template;
    }

    /**
     * 创建Redis消息监听器容器
     *
     * <p>用于支持Redis发布订阅功能，提供异步消息处理能力。</p>
     *
     * @param connectionFactory Redis连接工厂
     * @return 消息监听器容器
     */
    @Bean
    @ConditionalOnMissingBean(RedisMessageListenerContainer.class)
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            @Qualifier("redisMessageAsyncExecutor") AsyncTaskExecutor taskExecutor) {
        logBeanCreation("RedisMessageListenerContainer", "Redis消息监听器容器");

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 配置任务执行器
        container.setTaskExecutor(taskExecutor);

        // 配置错误处理器
        container.setErrorHandler(error ->
                log.error("Redis消息监听器异常: {}", error.getMessage(), error));

        logInfo("Redis消息监听器容器配置完成");
        logBeanSuccess("RedisMessageListenerContainer");

        return container;
    }

    /**
     * 创建统一的Redis服务Bean
     *
     * <p>这是整个Redis服务层的核心Bean，提供所有Redis操作的统一入口。</p>
     *
     * @param redisTemplate     主要的RedisTemplate
     * @param redisObjectMapper Redis专用ObjectMapper
     * @param connectionFactory Redis连接工厂
     * @param meterRegistry     指标注册表
     * @return BaseRedisService实例
     */
    @Bean
    @ConditionalOnBean({RedisTemplate.class, MeterRegistry.class})
    public BaseRedisService baseRedisService(
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper redisObjectMapper,
            RedisConnectionFactory connectionFactory,
            MeterRegistry meterRegistry) {

        logBeanCreation("BaseRedisService", "统一Redis服务");

        BaseRedisService redisService = new BaseRedisServiceImpl(
                redisTemplate,
                cacheProperties,
                redisObjectMapper,
                connectionFactory,
                meterRegistry);

        logInfo("统一Redis服务配置完成 - 支持分布式锁, 发布订阅, 缓存管理");
        logBeanSuccess("BaseRedisService");
        logSuccess("Redis服务已就绪，提供企业级缓存解决方案");

        return redisService;
    }

    /**
     * Redis健康检查指示器
     *
     * <p>监控Redis连接状态和基本性能指标，集成到Spring Boot Actuator。</p>
     *
     * @param baseRedisService Redis服务实例
     * @return 健康检查指示器
     */
    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnBean(BaseRedisService.class)
    @ConditionalOnProperty(name = "management.health.redis.enabled", havingValue = "true", matchIfMissing = true)
    public HealthIndicator redisHealthIndicator(BaseRedisService baseRedisService) {
        logBeanCreation("redisHealthIndicator", "Redis健康检查指示器");

        HealthIndicator indicator = new RedisHealthIndicator(baseRedisService);
        logBeanSuccess("redisHealthIndicator");

        return indicator;
    }

    /**
     * Redis缓存预热任务
     *
     * <p>系统启动时和定期执行的缓存预热任务，提升系统性能。</p>
     *
     * @param baseRedisService Redis服务实例
     * @return 缓存预热管理器
     */
    @Bean
    @ConditionalOnProperty(prefix = "baseai.cache.application", name = "enable-cache-warming", havingValue = "true")
    @ConditionalOnBean(BaseRedisService.class)
    public CacheWarmupManager cacheWarmupManager(BaseRedisService baseRedisService) {
        logBeanCreation("CacheWarmupManager", "缓存预热管理器");

        CacheWarmupManager manager = new CacheWarmupManager(baseRedisService, cacheProperties);
        logBeanSuccess("CacheWarmupManager");

        return manager;
    }

    // =================== 内部辅助类 ===================

    /**
     * Redis健康检查实现
     */
    private static class RedisHealthIndicator implements HealthIndicator {
        private final BaseRedisService redisService;

        public RedisHealthIndicator(BaseRedisService redisService) {
            this.redisService = redisService;
        }

        @Override
        public Health health() {
            try {
                // 测试连接
                boolean pingResult = redisService.ping();
                if (!pingResult) {
                    return Health.down()
                            .withDetail("reason", "Ping失败")
                            .withDetail("status", "CONNECTION_FAILED")
                            .build();
                }

                // 获取基本信息
                long dbSize = redisService.getDbSize();
                BaseRedisService.CacheStats stats = redisService.getCacheStats();

                // 构建健康状态
                Health.Builder healthBuilder = Health.up()
                        .withDetail("status", "UP")
                        .withDetail("ping", "PONG")
                        .withDetail("dbSize", dbSize)
                        .withDetail("hitCount", stats.getHitCount())
                        .withDetail("missCount", stats.getMissCount())
                        .withDetail("hitRate", String.format("%.2f%%", stats.getHitRate() * 100));

                // 检查性能指标
                double hitRate = stats.getHitRate();
                if (hitRate < 0.8 && stats.getHitCount() + stats.getMissCount() > 1000) {
                    healthBuilder.withDetail("warning", "缓存命中率较低，可能需要优化缓存策略");
                }

                return healthBuilder.build();

            } catch (Exception e) {
                return Health.down()
                        .withDetail("error", e.getMessage())
                        .withDetail("status", "ERROR")
                        .withException(e)
                        .build();
            }
        }
    }

    /**
     * 缓存预热管理器
     */
    private static class CacheWarmupManager {
        private final BaseRedisService redisService;
        private final CacheProperties cacheProperties;

        public CacheWarmupManager(BaseRedisService redisService, CacheProperties cacheProperties) {
            this.redisService = redisService;
            this.cacheProperties = cacheProperties;
        }

        /**
         * 执行缓存预热
         */
//        @Scheduled(cron = "${baseai.cache.application.cache-warming-schedule:0 0 */6 * * *}")
        public void performCacheWarmup() {
            if (!cacheProperties.getApplication().getEnableCacheWarming()) {
                return;
            }

            try {
                // 预热常用配置
                redisService.warmUpCache(() -> {
                    Map<String, Object> warmupData = new HashMap<>();
                    warmupData.put("system:status", "running");
                    warmupData.put("system:version", "1.0.0");
                    warmupData.put("cache:warmup:time", System.currentTimeMillis());
                    return warmupData;
                });

                log.info("缓存预热完成");
            } catch (Exception e) {
                log.error("缓存预热失败: {}", e.getMessage());
            }
        }
    }
}