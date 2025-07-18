package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.external.llm.ChatCompletionService;
import com.cloud.baseai.infrastructure.external.llm.impl.ClaudeChatCompletionService;
import com.cloud.baseai.infrastructure.external.llm.impl.CompositeChatCompletionService;
import com.cloud.baseai.infrastructure.external.llm.impl.MockChatCompletionService;
import com.cloud.baseai.infrastructure.external.llm.impl.OpenAIChatCompletionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

/**
 * <h2>LLM服务自动配置类</h2>
 *
 * <p>这个配置类负责协调和管理多个LLM提供商的接入。它根据配置自动选择和初始化合适的AI服务提供商。</p>
 *
 * <p><b>多provider架构的设计哲学：</b></p>
 * <p>在企业级AI应用中，依赖单一AI提供商是有风险的。我们采用多provider架构，
 * 让系统具备以下能力：</p>
 * <ul>
 * <li><b>供应商多样化：</b>OpenAI擅长创造性任务，Claude擅长分析推理，可以按需选择</li>
 * <li><b>成本优化：</b>根据任务复杂度和预算选择性价比最优的模型</li>
 * <li><b>容灾备份：</b>主要服务故障时自动切换到备用服务</li>
 * <li><b>A/B测试：</b>同时运行多个模型进行效果对比</li>
 * </ul>
 *
 * <p><b>配置驱动的好处：</b></p>
 * <p>通过Spring的条件注解和配置属性，我们实现了真正的"配置即代码"。
 * 运维人员可以通过简单修改配置文件，在不重新编译代码的情况下：</p>
 * <p>1. 启用或禁用特定的AI提供商</p>
 * <p>2. 调整各个服务的优先级和负载均衡策略</p>
 * <p>3. 配置开发/测试/生产环境的不同AI服务</p>
 */
@Configuration
public class LLMServiceAutoConfiguration {

    /**
     * 主要的ChatCompletionService Bean
     *
     * <p>@Primary注解确保当存在多个ChatCompletionService实现时，
     * 这个Bean会被优先注入。这是策略模式在Spring中的经典应用。</p>
     */
    @Bean
    @Primary
    public ChatCompletionService primaryChatCompletionService(
            LLMServiceProperties properties,
            @Qualifier("openaiChatCompletionService") ChatCompletionService openaiService,
            @Qualifier("claudeChatCompletionService") @Autowired(required = false) ChatCompletionService claudeService,
            @Qualifier("mockChatCompletionService") @Autowired(required = false) ChatCompletionService mockService) {

        return new CompositeChatCompletionService(properties, openaiService, claudeService, mockService);
    }

    /**
     * OpenAI聊天完成服务
     */
    @Bean(name = "openaiChatCompletionService")
    @ConditionalOnProperty(name = "baseai.llm.openai.enabled", havingValue = "true", matchIfMissing = true)
    public ChatCompletionService openaiChatCompletionService(
            @Qualifier("llmRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            LLMServiceProperties properties) {

        return new OpenAIChatCompletionService(
                restTemplate,
                objectMapper,
                properties.getOpenai().getApiKey(),
                properties.getOpenai().getBaseUrl()
        );
    }

    /**
     * Claude聊天完成服务
     */
    @Bean(name = "claudeChatCompletionService")
    @ConditionalOnProperty(name = "baseai.llm.claude.enabled", havingValue = "true")
    public ChatCompletionService claudeChatCompletionService(
            @Qualifier("llmRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            LLMServiceProperties properties) {

        return new ClaudeChatCompletionService(
                restTemplate,
                objectMapper,
                properties.getClaude().getApiKey(),
                properties.getClaude().getBaseUrl()
        );
    }

    /**
     * Mock聊天完成服务（用于开发和测试）
     */
    @Bean(name = "mockChatCompletionService")
    @ConditionalOnProperty(name = "baseai.llm.mock.enabled", havingValue = "true")
    public ChatCompletionService mockChatCompletionService() {
        return new MockChatCompletionService();
    }

    /**
     * LLM专用的RestTemplate
     *
     * <p>为LLM调用配置专门的RestTemplate，设置合适的超时时间和错误处理策略。
     * AI模型的响应时间通常比普通API更长，需要特殊的超时配置。</p>
     */
    @Bean(name = "llmRestTemplate")
    public RestTemplate llmRestTemplate(LLMServiceProperties properties) {
        RestTemplate restTemplate = new RestTemplate();

        // 配置超时时间
        restTemplate.getRequestFactory();
        // 这里需要配置连接超时和读取超时
        // 实际实现需要使用HttpComponentsClientHttpRequestFactory等

        return restTemplate;
    }

    /**
     * LLM服务配置属性
     */
    @Bean
    @ConfigurationProperties(prefix = "baseai.llm")
    public LLMServiceProperties llmServiceProperties() {
        return new LLMServiceProperties();
    }
}