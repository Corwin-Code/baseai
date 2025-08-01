package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * <h2>大语言模型（LLM）服务的Spring配置类</h2>
 *
 * <p>这个配置类负责集成Spring AI框架与自定义的LLM配置。
 * 它覆盖了Spring AI的默认配置，使用项目自定义的配置属性。</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "baseai.llm.openai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenAiAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OpenAiAutoConfiguration.class);

    private final LlmProperties llmProps;

    public OpenAiAutoConfiguration(LlmProperties llmProps) {
        this.llmProps = llmProps;
    }

    /**
     * 创建自定义的OpenAI API Bean
     *
     * <p>这个Bean使用项目自定义的配置，而不是Spring AI的默认配置。
     * 支持代理服务器和自定义请求头。</p>
     *
     * @return 配置好的OpenAI API实例
     */
    @Bean
    @Primary
    public OpenAiApi customOpenAiApi() {
        LlmProperties.OpenAiProperties openAiConfig = llmProps.getOpenai();

        log.info("创建自定义OpenAI API: baseUrl={}, timeout={}",
                openAiConfig.getBaseUrl(), openAiConfig.getTimeout());

        final RestTemplate restTemplate = new RestTemplateBuilder()
                .connectTimeout(openAiConfig.getTimeout())
                .readTimeout(openAiConfig.getTimeout())
                .build();

        // 构建RestClient
        RestClient.Builder restClientBuilder = RestClient.builder(restTemplate)
                .baseUrl(openAiConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiConfig.getApiKey())
                .defaultHeader(HttpHeaders.USER_AGENT, "BaseAI-SpringAI/1.0")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        // 添加组织ID（如果配置了）
        if (openAiConfig.getOrganization() != null && !openAiConfig.getOrganization().isEmpty()) {
            restClientBuilder.defaultHeader("OpenAI-Organization", openAiConfig.getOrganization());
        }

        // 使用Spring AI的OpenAiApi.Builder来创建实例
        return new OpenAiApi.Builder()
                .baseUrl(openAiConfig.getBaseUrl())
                .apiKey(openAiConfig.getApiKey())
                .restClientBuilder(restClientBuilder)
                .build();
    }

    /**
     * 创建自定义的聊天模型选项
     *
     * <p>使用项目配置的默认参数创建聊天选项。</p>
     *
     * @return 配置好的聊天选项
     */
    @Bean
    @Primary
    public OpenAiChatOptions customOpenAiChatOptions() {
        LlmProperties.DefaultParametersProperties defaults = llmProps.getDefaultParameters();

        log.info("创建OpenAI聊天选项: model={}, temperature={}, maxTokens={}",
                defaults.getModel(), defaults.getTemperature(), defaults.getMaxTokens());

        return OpenAiChatOptions.builder()
                .model(defaults.getModel())
                .temperature(defaults.getTemperature())
                .maxTokens(defaults.getMaxTokens())
                .topP(defaults.getTopP())
                .frequencyPenalty(defaults.getFrequencyPenalty())
                .presencePenalty(defaults.getPresencePenalty())
                .build();
    }

    /**
     * 创建自定义重试模板
     *
     * <p>提供真正的自定义重试策略，而不是仅使用默认配置。</p>
     *
     * @return 配置好的重试模板
     */
    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "baseai.llm.openai", name = "max-retries", matchIfMissing = false)
    public RetryTemplate customRetryTemplate() {
        int maxRetries = llmProps.getOpenai().getMaxRetries();

        log.info("配置自定义OpenAI重试策略: maxRetries={}", maxRetries);

        RetryTemplate retryTemplate = new RetryTemplate();

        // 配置重试策略
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxRetries);
        retryTemplate.setRetryPolicy(retryPolicy);

        // 配置退避策略 - 指数退避
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000); // 初始间隔1秒
        backOffPolicy.setMaxInterval(10000);    // 最大间隔10秒
        backOffPolicy.setMultiplier(2.0);      // 每次重试间隔翻倍
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

    /**
     * 使用默认重试模板（当没有配置自定义重试次数时）
     */
    @Bean
    @ConditionalOnMissingBean(RetryTemplate.class)
    public RetryTemplate defaultRetryTemplate() {
        log.info("使用Spring AI默认重试策略");
        return RetryUtils.DEFAULT_RETRY_TEMPLATE;
    }

    /**
     * 创建自定义的OpenAI聊天模型
     *
     * <p>这是主要的聊天模型Bean，集成了所有自定义配置。</p>
     *
     * @param openAiApi     自定义的OpenAI API
     * @param chatOptions   自定义的聊天选项
     * @param retryTemplate 重试模板
     * @return 配置好的聊天模型
     */
    @Bean
    @Primary
    public OpenAiChatModel customOpenAiChatModel(
            OpenAiApi openAiApi,
            OpenAiChatOptions chatOptions,
            RetryTemplate retryTemplate) {

        log.info("创建自定义OpenAI聊天模型");

        // 使用Builder模式创建聊天模型
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(chatOptions)
                .retryTemplate(retryTemplate)
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