package com.clinflash.baseai.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>MCP模块配置</h2>
 *
 * <p>MCP系统的核心配置项，包括工具执行、安全控制、性能调优等各个方面的参数。
 * 这些配置让系统能够根据不同的部署环境和业务需求进行灵活调整。</p>
 *
 * <p><b>配置分类：</b></p>
 * <ul>
 * <li><b>执行配置：</b>控制工具执行的超时、重试、并发等参数</li>
 * <li><b>安全配置：</b>设置权限检查、配额限制、访问控制等规则</li>
 * <li><b>监控配置：</b>定义日志保留、指标收集、告警阈值等策略</li>
 * </ul>
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "baseai.mcp")
public class McpConfig {

    /**
     * 工具执行配置
     */
    private ExecutionConfig execution = new ExecutionConfig();

    /**
     * 安全配置
     */
    private SecurityConfig security = new SecurityConfig();

    /**
     * 监控配置
     */
    private MonitoringConfig monitoring = new MonitoringConfig();

    /**
     * HTTP客户端配置
     */
    private HttpClientConfig httpClient = new HttpClientConfig();

    // =================== 工具执行配置 ===================

    @Setter
    @Getter
    public static class ExecutionConfig {
        /**
         * 默认超时时间（秒）
         */
        private int defaultTimeoutSeconds = 30;

        /**
         * 最大超时时间（秒）
         */
        private int maxTimeoutSeconds = 300;

        /**
         * 异步执行线程池大小
         */
        private int asyncPoolSize = 10;

        /**
         * 最大并发执行数
         */
        private int maxConcurrentExecutions = 100;

        /**
         * 默认重试次数
         */
        private int defaultRetryCount = 3;

        /**
         * 重试间隔（毫秒）
         */
        private long retryIntervalMs = 1000;

        /**
         * 是否启用执行缓存
         */
        private boolean enableCache = true;

        /**
         * 缓存过期时间（秒）
         */
        private int cacheExpirationSeconds = 300;

    }

    // =================== 安全配置 ===================

    @Setter
    @Getter
    public static class SecurityConfig {
        /**
         * 是否启用配额检查
         */
        private boolean enableQuotaCheck = true;

        /**
         * 默认配额限制
         */
        private int defaultQuotaLimit = 1000;

        /**
         * 是否启用IP限制
         */
        private boolean enableIpRestriction = false;

        /**
         * 允许的IP地址列表
         */
        private String[] allowedIps = {};

        /**
         * API密钥过期时间（天）
         */
        private int apiKeyExpirationDays = 365;

        /**
         * 是否启用请求签名验证
         */
        private boolean enableSignatureVerification = false;

        /**
         * 工具执行沙箱配置
         */
        private boolean enableSandbox = true;

        /**
         * 沙箱内存限制（MB）
         */
        private int sandboxMemoryLimitMb = 256;

        /**
         * 沙箱CPU时间限制（秒）
         */
        private int sandboxCpuTimeLimitSeconds = 10;

    }

    // =================== 监控配置 ===================

    @Setter
    @Getter
    public static class MonitoringConfig {
        /**
         * 是否启用调用日志记录
         */
        private boolean enableCallLogging = true;

        /**
         * 日志保留天数
         */
        private int logRetentionDays = 90;

        /**
         * 是否启用性能指标收集
         */
        private boolean enableMetrics = true;

        /**
         * 指标收集间隔（秒）
         */
        private int metricsIntervalSeconds = 60;

        /**
         * 是否启用告警
         */
        private boolean enableAlerts = true;

        /**
         * 错误率告警阈值（百分比）
         */
        private double errorRateThreshold = 5.0;

        /**
         * 响应时间告警阈值（毫秒）
         */
        private long latencyThresholdMs = 5000;

        /**
         * 配额使用率告警阈值（百分比）
         */
        private double quotaUsageThreshold = 80.0;

    }

    // =================== HTTP客户端配置 ===================

    @Setter
    @Getter
    public static class HttpClientConfig {
        /**
         * 连接超时时间（毫秒）
         */
        private int connectionTimeoutMs = 5000;

        /**
         * 读取超时时间（毫秒）
         */
        private int readTimeoutMs = 30000;

        /**
         * 最大连接数
         */
        private int maxConnections = 100;

        /**
         * 每个路由的最大连接数
         */
        private int maxConnectionsPerRoute = 20;

        /**
         * 是否启用重试
         */
        private boolean enableRetry = true;

        /**
         * 最大重试次数
         */
        private int maxRetries = 3;

        /**
         * 是否跟踪重定向
         */
        private boolean followRedirects = true;

        /**
         * 用户代理字符串
         */
        private String userAgent = "BaseAI-MCP/1.0";

    }
}