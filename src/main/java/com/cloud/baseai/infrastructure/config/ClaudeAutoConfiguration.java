package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnProperty(prefix = "baseai.llm.claude", name = "enabled", havingValue = "true")
public class ClaudeAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAutoConfiguration.class);

    private final LlmProperties llmProps;

    public ClaudeAutoConfiguration(LlmProperties llmProps) {
        this.llmProps = llmProps;
    }

    /**
     * 创建自定义的Anthropic API Bean
     *
     * <p>使用项目自定义的配置创建Anthropic API客户端。</p>
     *
     * @return 配置好的Anthropic API实例
     */
    @Bean
    @ConditionalOnMissingBean(AnthropicApi.class)
    public AnthropicApi anthropicApi() {
        LlmProperties.ClaudeProperties claudeConfig = llmProps.getClaude();

        log.info("创建Anthropic API: baseUrl={}, timeout={}",
                claudeConfig.getBaseUrl(), claudeConfig.getTimeout());

        final RestTemplate restTemplate = new RestTemplateBuilder()
                .connectTimeout(claudeConfig.getTimeout())
                .readTimeout(claudeConfig.getTimeout())
                .build();

        // 构建RestClient
        RestClient.Builder restClientBuilder = RestClient.builder(restTemplate)
                .baseUrl(claudeConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + claudeConfig.getApiKey())
                .defaultHeader(HttpHeaders.USER_AGENT, "BaseAI-SpringAI/1.0")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        // 创建Anthropic API
        return AnthropicApi.builder()
                .baseUrl(claudeConfig.getBaseUrl())
                .apiKey(claudeConfig.getApiKey())
                .restClientBuilder(restClientBuilder)
                .build();
    }

    /**
     * 创建自定义的Claude聊天模型选项
     *
     * <p>使用项目配置的默认参数创建聊天选项。</p>
     *
     * @return 配置好的聊天选项
     */
    @Bean
    @ConditionalOnMissingBean(name = "claudeChatOptions")
    public AnthropicChatOptions claudeChatOptions() {
        LlmProperties.DefaultParametersProperties defaults = llmProps.getDefaultParameters();

        // 如果默认模型不是Claude模型，使用Claude的默认模型
        String defaultModel = defaults.getModel();
        if (!defaultModel.startsWith("claude")) {
            defaultModel = "claude-3-sonnet-20240229";
        }

        log.info("创建Claude聊天选项: model={}, temperature={}, maxTokens={}",
                defaultModel, defaults.getTemperature(), defaults.getMaxTokens());

        return AnthropicChatOptions.builder()
                .model(defaultModel)
                .temperature(defaults.getTemperature())
                .maxTokens(defaults.getMaxTokens())
                .topP(defaults.getTopP())
                .build();
    }

    /**
     * 创建自定义重试模板
     *
     * <p>为Claude API调用提供自定义的重试策略。</p>
     *
     * @return 配置好的重试模板
     */
    @Bean(name = "claudeRetryTemplate")
    @ConditionalOnProperty(prefix = "baseai.llm.claude", name = "max-retries", matchIfMissing = false)
    public RetryTemplate claudeRetryTemplate() {
        int maxRetries = llmProps.getClaude().getMaxRetries();

        log.info("配置Claude重试策略: maxRetries={}", maxRetries);

        RetryTemplate retryTemplate = new RetryTemplate();

        // 配置重试策略
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxRetries);
        retryTemplate.setRetryPolicy(retryPolicy);

        // 配置退避策略 - 指数退避
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2000); // 初始间隔2秒（Claude的速率限制较严格）
        backOffPolicy.setMaxInterval(30000);    // 最大间隔30秒
        backOffPolicy.setMultiplier(2.0);      // 每次重试间隔翻倍
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

    /**
     * 使用默认重试模板（当没有配置自定义重试次数时）
     */
    @Bean(name = "claudeRetryTemplate")
    @ConditionalOnMissingBean(name = "claudeRetryTemplate")
    public RetryTemplate claudeDefaultRetryTemplate() {
        log.info("使用Spring AI默认重试策略 (Claude)");
        return RetryUtils.DEFAULT_RETRY_TEMPLATE;
    }

    /**
     * 创建自定义的Claude聊天模型
     *
     * <p>这是主要的聊天模型Bean，集成了所有自定义配置。</p>
     *
     * @param anthropicApi        自定义的Anthropic API
     * @param chatOptions         自定义的聊天选项
     * @param claudeRetryTemplate 重试模板
     * @return 配置好的聊天模型
     */
    @Bean
    @ConditionalOnBean(AnthropicApi.class)
    public AnthropicChatModel anthropicChatModel(
            AnthropicApi anthropicApi,
            AnthropicChatOptions chatOptions,
            RetryTemplate claudeRetryTemplate) {

        log.info("创建自定义Claude聊天模型");

        // 创建聊天模型
        return AnthropicChatModel.builder()
                .anthropicApi(anthropicApi)
                .defaultOptions(chatOptions)
                .retryTemplate(claudeRetryTemplate)
                .build();
    }

    /**
     * 创建ObjectMapper Bean（如果不存在）
     *
     * @return ObjectMapper实例
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}