package com.cloud.baseai.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <h2>缓存配置属性类</h2>
 *
 * <p>该类管理应用的缓存策略，包括Redis缓存和应用级缓存的配置，
 * 提供灵活的缓存管理和性能优化功能。</p>
 */
@Data
@ConfigurationProperties(prefix = "baseai.cache")
public class CacheProperties {

    /**
     * Redis缓存配置
     */
    private RedisProperties redis = new RedisProperties();

    /**
     * 应用级缓存配置
     */
    private ApplicationProperties application = new ApplicationProperties();

    /**
     * Redis缓存配置内部类
     */
    @Data
    public static class RedisProperties {
        /**
         * 默认过期时间（秒）
         */
        private Integer defaultTtlSeconds = 3600;

        /**
         * 最大过期时间（秒）
         */
        private Integer maxTtlSeconds = 86400;

        /**
         * 键名前缀
         */
        private String keyPrefix = "baseai:";

        /**
         * 是否启用压缩
         */
        private Boolean enableCompression = true;

        /**
         * 压缩阈值（字节）
         */
        private Integer compressionThresholdBytes = 1024;

        /**
         * 最大键名长度
         */
        private Integer maxKeyLength = 250;
    }

    /**
     * 应用级缓存配置内部类
     */
    @Data
    public static class ApplicationProperties {
        /**
         * 配置缓存过期时间（秒）
         */
        private Integer configCacheTtlSeconds = 300;

        /**
         * 用户会话缓存过期时间（秒）
         */
        private Integer userSessionCacheTtlSeconds = 1800;

        /**
         * API响应缓存过期时间（秒）
         */
        private Integer apiResponseCacheTtlSeconds = 60;

        /**
         * 静态内容缓存过期时间（秒）
         */
        private Integer staticContentCacheTtlSeconds = 3600;

        /**
         * 是否启用缓存预热
         */
        private Boolean enableCacheWarming = true;

        /**
         * 缓存预热调度表达式（每6小时）
         */
        private String cacheWarmingSchedule = "0 0 */6 * * *";

        /**
         * 最大缓存大小（MB）
         */
        private Integer maxCacheSizeMb = 512;

        /**
         * 缓存淘汰策略：LRU、LFU、FIFO
         */
        private String evictionPolicy = "LRU";
    }
}