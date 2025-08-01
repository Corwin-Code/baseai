package com.cloud.baseai.infrastructure.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@ConditionalOnProperty(prefix = "baseai.llm.qwen", name = "enabled", havingValue = "true", matchIfMissing = true)
public class QwenAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(QwenAutoConfiguration.class);

    private final LlmProperties llmProps;

    public QwenAutoConfiguration(LlmProperties llmProps) {
        this.llmProps = llmProps;
    }

    /**
     * 创建自定义的DashScope API Bean
     *
     * <p>使用项目自定义的配置创建DashScope API客户端。</p>
     *
     * @return 配置好的DashScope API实例
     */
    @Bean
    @ConditionalOnMissingBean(AnthropicApi.class)
    public DashScopeApi dashScopeApi() {
        LlmProperties.QwenProperties qwenConfig = llmProps.getQwen();

        log.info("创建DashScope API: baseUrl={}, timeout={}",
                qwenConfig.getBaseUrl(), qwenConfig.getTimeout());

        final RestTemplate restTemplate = new RestTemplateBuilder()
                .connectTimeout(qwenConfig.getTimeout())
                .readTimeout(qwenConfig.getTimeout())
                .build();

        // 构建RestClient
        RestClient.Builder restClientBuilder = RestClient.builder(restTemplate)
                .baseUrl(qwenConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + qwenConfig.getApiKey())
                .defaultHeader(HttpHeaders.USER_AGENT, "BaseAI-SpringAI/1.0")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        // 创建Anthropic API
        return DashScopeApi.builder()
                .baseUrl(qwenConfig.getBaseUrl())
                .apiKey(qwenConfig.getApiKey())
                .restClientBuilder(restClientBuilder)
                .build();
    }

    /**
     * 创建自定义的Qwen聊天模型选项
     *
     * <p>使用项目配置的默认参数创建聊天选项。</p>
     *
     * @return 配置好的聊天选项
     */
    @Bean
    @ConditionalOnMissingBean(name = "qwenChatOptions")
    public DashScopeChatOptions qwenChatOptions() {
        LlmProperties.DefaultParametersProperties defaults = llmProps.getDefaultParameters();

        // 如果默认模型不是Qwen模型，使用Qwen的默认模型
        String defaultModel = defaults.getModel();
        if (!defaultModel.startsWith("qwen")) {
            defaultModel = "qwen-plus";
        }

        log.info("创建Qwen聊天选项: model={}, temperature={}, maxTokens={}",
                defaultModel, defaults.getTemperature(), defaults.getMaxTokens());

        return DashScopeChatOptions.builder()
                .withModel(defaultModel)
                .withTemperature(defaults.getTemperature())
                .withMaxToken(defaults.getMaxTokens())
                .withTopP(defaults.getTopP())
                .build();
    }

    /**
     * 创建自定义重试模板
     *
     * <p>为DashScope API调用提供自定义的重试策略。</p>
     *
     * @return 配置好的重试模板
     */
    @Bean(name = "qwenRetryTemplate")
    @ConditionalOnProperty(prefix = "baseai.llm.qwen", name = "max-retries", matchIfMissing = false)
    public RetryTemplate qwenRetryTemplate() {
        int maxRetries = llmProps.getQwen().getMaxRetries();

        log.info("配置Qwen重试策略: maxRetries={}", maxRetries);

        RetryTemplate retryTemplate = new RetryTemplate();

        // 配置重试策略
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxRetries);
        retryTemplate.setRetryPolicy(retryPolicy);

        // 配置退避策略 - 指数退避
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2000); // 初始间隔2秒
        backOffPolicy.setMaxInterval(30000);    // 最大间隔30秒
        backOffPolicy.setMultiplier(2.0);      // 每次重试间隔翻倍
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

    /**
     * 使用默认重试模板（当没有配置自定义重试次数时）
     */
    @Bean(name = "qwenRetryTemplate")
    @ConditionalOnMissingBean(name = "qwenRetryTemplate")
    public RetryTemplate qwenDefaultRetryTemplate() {
        log.info("使用Spring AI默认重试策略 (Qwen)");
        return RetryUtils.DEFAULT_RETRY_TEMPLATE;
    }

    /**
     * 创建自定义的Qwen聊天模型
     *
     * <p>这是主要的聊天模型Bean，集成了所有自定义配置。</p>
     *
     * @param dashScopeApi      自定义的DashScope API
     * @param chatOptions       自定义的聊天选项
     * @param qwenRetryTemplate 重试模板
     * @return 配置好的聊天模型
     */
    @Bean
    @ConditionalOnBean(DashScopeApi.class)
    public DashScopeChatModel dashScopeChatModel(
            DashScopeApi dashScopeApi,
            DashScopeChatOptions chatOptions,
            RetryTemplate qwenRetryTemplate) {

        log.info("创建自定义Qwen聊天模型");

        // 创建聊天模型
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(chatOptions)
                .retryTemplate(qwenRetryTemplate)
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