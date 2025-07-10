package com.clinflash.baseai.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>流程编排配置</h2>
 *
 * <p>集中管理流程编排系统的所有配置参数，包括执行配置、性能参数、
 * 超时设置等。通过配置文件可以灵活调整系统行为。</p>
 */
@Configuration
@ConfigurationProperties(prefix = "baseai.flow")
@Data
public class FlowConfig {

    /**
     * 执行引擎配置
     */
    private ExecutionConfig execution = new ExecutionConfig();

    /**
     * 构建服务配置
     */
    private BuildConfig build = new BuildConfig();

    /**
     * 性能相关配置
     */
    private PerformanceConfig performance = new PerformanceConfig();

    /**
     * 监控配置
     */
    private MonitoringConfig monitoring = new MonitoringConfig();

    @Data
    public static class ExecutionConfig {
        /**
         * 异步执行线程池大小
         */
        private int asyncPoolSize = 10;

        /**
         * 默认超时时间（分钟）
         */
        private int defaultTimeoutMinutes = 30;

        /**
         * 最大超时时间（分钟）
         */
        private int maxTimeoutMinutes = 240;

        /**
         * 节点执行重试次数
         */
        private int maxRetryCount = 3;

        /**
         * 重试延迟（毫秒）
         */
        private long retryDelayMs = 1000;

        /**
         * 是否启用并行执行
         */
        private boolean enableParallelExecution = true;

        /**
         * 并行执行最大线程数
         */
        private int maxParallelThreads = 5;
    }

    @Data
    public static class BuildConfig {
        /**
         * 是否启用结构验证
         */
        private boolean enableValidation = true;

        /**
         * 是否启用循环检测
         */
        private boolean enableCycleDetection = true;

        /**
         * 最大节点数量
         */
        private int maxNodes = 100;

        /**
         * 最大连接数量
         */
        private int maxEdges = 200;

        /**
         * 快照压缩
         */
        private boolean compressSnapshot = false;
    }

    @Data
    public static class PerformanceConfig {
        /**
         * 异步处理线程池大小
         */
        private int asyncPoolSize = 10;

        /**
         * 批处理大小
         */
        private int batchSize = 50;

        /**
         * 缓存配置
         */
        private CacheConfig cache = new CacheConfig();

        @Data
        public static class CacheConfig {
            /**
             * 是否启用缓存
             */
            private boolean enabled = true;

            /**
             * 快照缓存大小
             */
            private int snapshotCacheSize = 100;

            /**
             * 缓存过期时间（分钟）
             */
            private int cacheExpirationMinutes = 60;
        }
    }

    @Data
    public static class MonitoringConfig {
        /**
         * 是否启用性能监控
         */
        private boolean enableMetrics = true;

        /**
         * 是否启用详细日志
         */
        private boolean enableDetailedLogging = false;

        /**
         * 日志级别
         */
        private String logLevel = "INFO";

        /**
         * 健康检查间隔（秒）
         */
        private int healthCheckIntervalSeconds = 30;
    }
}