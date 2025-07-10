package com.clinflash.baseai.infrastructure.config;

import com.clinflash.baseai.domain.mcp.service.HttpToolExecutionService;
import com.clinflash.baseai.domain.mcp.service.ToolExecutionService;
import com.clinflash.baseai.infrastructure.monitoring.McpMonitoringManager;
import com.clinflash.baseai.infrastructure.security.McpSecurityManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <h2>MCP自动配置类</h2>
 *
 * <p>这个配置类负责创建MCP系统运行所需的所有Bean实例。
 * 采用了自动配置的模式，让用户能够轻松集成MCP功能到自己的应用中。</p>
 *
 * <p><b>配置策略：</b></p>
 * <p>我们使用了条件注解，只有在用户没有提供自定义Bean的情况下，
 * 才会创建默认的实现。这样既保证了开箱即用，又保持了足够的灵活性。</p>
 */
@Configuration
public class McpAutoConfiguration {

    private final McpConfig mcpConfig;

    public McpAutoConfiguration(McpConfig mcpConfig) {
        this.mcpConfig = mcpConfig;
    }

    /**
     * 配置HTTP客户端
     *
     * <p>这个RestTemplate专门用于MCP工具的HTTP调用，
     * 配置了超时、连接池等参数来保证性能和稳定性。</p>
     */
    @Bean("mcpRestTemplate")
    @ConditionalOnMissingBean(name = "mcpRestTemplate")
    public RestTemplate mcpRestTemplate(McpConfig mcpConfig) {
        // 配置HTTP客户端
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(mcpConfig.getHttpClient().getConnectionTimeoutMs()))
                .setConnectTimeout(Timeout.ofMilliseconds(mcpConfig.getHttpClient().getConnectionTimeoutMs()))
                .setResponseTimeout(Timeout.ofMilliseconds(mcpConfig.getHttpClient().getReadTimeoutMs()))
                .build();

        // 配置连接池
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(mcpConfig.getHttpClient().getMaxConnections())
                .setMaxConnPerRoute(mcpConfig.getHttpClient().getMaxConnectionsPerRoute())
                .build();

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager)
                .setUserAgent(mcpConfig.getHttpClient().getUserAgent())
                .build();

        // 接收 HttpClient 的实例
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(factory);
    }

    /**
     * 配置工具执行服务
     *
     * <p>这是MCP系统的核心执行引擎，负责调用各种类型的工具。
     * 默认实现主要支持HTTP工具，可以通过实现ToolExecutionService接口来扩展其他类型。</p>
     */
    @Bean
    @ConditionalOnMissingBean(ToolExecutionService.class)
    public ToolExecutionService toolExecutionService(RestTemplate mcpRestTemplate, ObjectMapper objectMapper) {
        return new HttpToolExecutionService(mcpRestTemplate, objectMapper);
    }

    /**
     * 配置异步执行器
     *
     * <p>用于异步执行工具调用，避免阻塞主线程。
     * 线程池大小可以通过配置文件调整。</p>
     */
    @Bean("mcpAsyncExecutor")
    @ConditionalOnMissingBean(name = "mcpAsyncExecutor")
    public ExecutorService mcpAsyncExecutor() {
        return Executors.newFixedThreadPool(mcpConfig.getExecution().getAsyncPoolSize());
    }

    /**
     * 配置MCP安全管理器
     *
     * <p>负责权限检查、配额验证、访问控制等安全相关功能。</p>
     */
    @Bean
    @ConditionalOnMissingBean(McpSecurityManager.class)
    public McpSecurityManager mcpSecurityManager() {
        return new McpSecurityManager(mcpConfig.getSecurity());
    }

    /**
     * 配置MCP监控管理器
     *
     * <p>负责收集工具调用的性能指标、生成告警等监控功能。</p>
     */
    @Bean
    @ConditionalOnMissingBean(McpMonitoringManager.class)
    public McpMonitoringManager mcpMonitoringManager() {
        return new McpMonitoringManager(mcpConfig.getMonitoring());
    }
}