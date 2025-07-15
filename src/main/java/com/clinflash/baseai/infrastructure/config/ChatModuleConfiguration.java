package com.clinflash.baseai.infrastructure.config;

import com.clinflash.baseai.application.flow.service.FlowOrchestrationAppService;
import com.clinflash.baseai.application.kb.service.KnowledgeBaseAppService;
import com.clinflash.baseai.application.mcp.service.McpApplicationService;
import com.clinflash.baseai.infrastructure.integration.ChatIntegrationService;
import com.clinflash.baseai.infrastructure.monitoring.ChatMetricsCollector;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;

/**
 * <h2>对话模块完整配置</h2>
 *
 * <p>这是对话模块的总配置类，它把所有相关的配置类整合在一起，
 * 确保整个模块能够作为一个完整的、可独立运行的系统来工作。</p>
 *
 * <p><b>配置的艺术：</b></p>
 * <p>好的配置就像交响乐的指挥，它不创造音乐，但决定了音乐的和谐。
 * 在微服务架构中，配置管理更是关键：</p>
 * <ul>
 * <li><b>环境隔离：</b>开发、测试、生产环境使用不同的配置</li>
 * <li><b>安全管理：</b>敏感信息如API密钥的安全存储和使用</li>
 * <li><b>性能调优：</b>根据实际负载调整线程池、超时等参数</li>
 * <li><b>功能开关：</b>通过配置快速启用或禁用特定功能</li>
 * </ul>
 */
@Configuration
@EnableAsync
@Import({
        LLMServiceAutoConfiguration.class,
        ChatConfig.class
})
public class ChatModuleConfiguration {

    /**
     * 配置专用于Chat模块的RestTemplate
     *
     * <p>不同的业务模块可能需要不同的HTTP客户端配置。
     * Chat模块需要处理可能较长的AI响应时间，所以配置了更长的超时。</p>
     */
    @Bean(name = "chatRestTemplate")
    public RestTemplate chatRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(10))   // 连接超时
                .readTimeout(Duration.ofMinutes(3))       // 读取超时，AI响应可能较慢
                .build();
    }

    /**
     * Chat模块异步任务执行器
     *
     * <p>异步执行对于提升用户体验至关重要。例如：</p>
     * <p>1. 流式响应的后台处理</p>
     * <p>2. 批量数据处理任务</p>
     * <p>3. 指标统计和日志记录</p>
     * <p>4. 缓存预热和数据同步</p>
     */
    @Bean(name = "chatAsyncExecutor")
    public Executor chatAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);          // 核心线程数
        executor.setMaxPoolSize(20);          // 最大线程数
        executor.setQueueCapacity(100);       // 队列容量
        executor.setThreadNamePrefix("chat-async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Chat模块指标收集器
     */
    @Bean
    public ChatMetricsCollector chatMetricsCollector(MeterRegistry meterRegistry) {
        return new ChatMetricsCollector(meterRegistry);
    }

    /**
     * Chat集成服务
     */
    @Bean
    public ChatIntegrationService chatIntegrationService(
            KnowledgeBaseAppService kbService,
            McpApplicationService mcpService,
            FlowOrchestrationAppService flowService) {

        return new ChatIntegrationService(kbService, mcpService, flowService);
    }
}