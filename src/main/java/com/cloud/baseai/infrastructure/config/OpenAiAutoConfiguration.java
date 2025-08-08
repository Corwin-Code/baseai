package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.config.properties.KnowledgeBaseProperties;
import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * <h2>OpenAI大语言模型服务的Spring自动配置类</h2>
 *
 * <p>该配置类负责集成Spring AI框架与自定义的OpenAI LLM配置。
 * * 它覆盖了Spring AI的默认配置，使用项目自定义的配置属性，
 * * 并提供企业级的错误处理、重试机制和监控功能。</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "baseai.llm.openai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenAiAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OpenAiAutoConfiguration.class);

    private final KnowledgeBaseProperties kbProps;
    private final LlmProperties llmProps;

    public OpenAiAutoConfiguration(KnowledgeBaseProperties kbProps, LlmProperties llmProps) {
        this.kbProps = kbProps;
        this.llmProps = llmProps;

        // 配置验证
        validateOpenAiConfiguration();
    }

    /**
     * 验证OpenAI配置参数
     *
     * <p>在Bean创建前验证所有必要的配置参数，确保服务能够正常启动。</p>
     */
    private void validateOpenAiConfiguration() {
        LlmProperties.OpenAiProperties openAiConfig = llmProps.getOpenai();

        Assert.hasText(openAiConfig.getApiKey(), "OpenAI API密钥不能为空");
        Assert.hasText(openAiConfig.getBaseUrl(), "OpenAI BaseURL不能为空");
        Assert.notNull(openAiConfig.getTimeout(), "OpenAI超时时间不能为空");
        Assert.isTrue(openAiConfig.getMaxRetries() > 0, "OpenAI重试次数必须大于0");
        Assert.notEmpty(openAiConfig.getModels(), "OpenAI支持的模型列表不能为空");

        // 验证API密钥格式（OpenAI密钥通常以sk-开头）
        if (!openAiConfig.getApiKey().startsWith("sk-") &&
                !openAiConfig.getApiKey().startsWith("hk-")) { // 支持代理服务密钥
            log.warn("OpenAI API密钥格式可能不正确，请确认密钥有效性");
        }

        log.info("OpenAI配置验证通过: baseUrl={}, models={}",
                openAiConfig.getBaseUrl(), openAiConfig.getModels());
    }

    /**
     * 创建自定义的OpenAI API Bean
     *
     * <p>这个Bean使用项目自定义的配置，而不是Spring AI的默认配置。
     * 支持代理服务器和自定义请求头，包含完善的错误处理。</p>
     *
     * @return 配置好的OpenAI API实例
     */
    @Bean
    @Primary
    public OpenAiApi customOpenAiApi() {
        LlmProperties.OpenAiProperties openAiConfig = llmProps.getOpenai();

        log.info("创建自定义OpenAI API: baseUrl={}, timeout={}",
                openAiConfig.getBaseUrl(), openAiConfig.getTimeout());

        try {
            // 创建自定义RestTemplate
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
                log.debug("添加OpenAI组织ID: {}", openAiConfig.getOrganization());
            }

            // 使用Spring AI的OpenAiApi.Builder来创建实例
            OpenAiApi api = new OpenAiApi.Builder()
                    .baseUrl(openAiConfig.getBaseUrl())
                    .apiKey(openAiConfig.getApiKey())
                    .restClientBuilder(restClientBuilder)
                    .build();

            log.info("OpenAI API创建成功");
            return api;

        } catch (Exception e) {
            log.error("创建OpenAI API失败", e);
            throw new IllegalStateException("无法创建OpenAI API", e);
        }
    }

    /**
     * 创建自定义的聊天模型选项
     *
     * <p>使用项目配置的默认参数创建聊天选项，确保与业务需求一致。</p>
     *
     * @return 配置好的聊天选项
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "openAiChatOptions")
    public OpenAiChatOptions openAiChatOptions() {
        LlmProperties.DefaultParametersProperties defaults = llmProps.getDefaultParameters();

        // 确保使用OpenAI兼容的模型
        String defaultModel = defaults.getModel();
        if (!isOpenAiCompatibleModel(defaultModel)) {
            defaultModel = "gpt-3.5-turbo"; // 使用OpenAI的默认模型
            log.warn("配置的默认模型不兼容OpenAI，使用默认模型: {}", defaultModel);
        }

        log.info("创建OpenAI聊天选项: model={}, temperature={}, maxTokens={}",
                defaultModel, defaults.getTemperature(), defaults.getMaxTokens());

        return OpenAiChatOptions.builder()
                .model(defaultModel)
                .temperature(defaults.getTemperature())
                .maxTokens(defaults.getMaxTokens())
                .topP(defaults.getTopP())
                .frequencyPenalty(defaults.getFrequencyPenalty())
                .presencePenalty(defaults.getPresencePenalty())
                .build();
    }

    /**
     * 检查是否为OpenAI兼容的聊天模型
     */
    private boolean isOpenAiCompatibleModel(String model) {
        if (model == null) return false;
        return model.startsWith("gpt-") ||
                model.startsWith("text-davinci-") ||
                model.startsWith("text-curie-") ||
                model.startsWith("text-babbage-") ||
                model.startsWith("text-ada-");
    }

    /**
     * 为嵌入模型创建专用的选项Bean
     *
     * <p>嵌入模型和聊天模型使用不同的配置参数，需要单独的配置Bean。</p>
     *
     * @return 配置好的嵌入模型选项
     */
    @Bean
    @ConditionalOnMissingBean(name = "openAiEmbeddingOptions")
    public OpenAiEmbeddingOptions openAiEmbeddingOptions() {
        // 确定嵌入模型
        String embeddingModel = kbProps.getEmbedding().getDefaultModel();
        Integer dimension = kbProps.getEmbedding().getDimension();

        // 如果配置的默认模型不是OpenAI模型，使用OpenAI的默认嵌入模型
        if (!isOpenAiEmbeddingModel(embeddingModel)) {
            embeddingModel = "text-embedding-3-small";
        }

        log.info("创建OpenAI嵌入模型选项: model={}, dimension={}", embeddingModel, dimension);

        return OpenAiEmbeddingOptions.builder()
                .model(embeddingModel)
                .dimensions(dimension)
                .build();
    }

    /**
     * 检查是否为OpenAI嵌入模型
     */
    private boolean isOpenAiEmbeddingModel(String model) {
        if (model == null) return false;
        return model.equals("text-embedding-3-small") ||
                model.equals("text-embedding-3-large") ||
                model.equals("text-embedding-ada-002");
    }

    /**
     * 创建自定义重试模板
     *
     * <p>为OpenAI API调用提供自定义的重试策略，包括指数退避
     * 和智能重试次数控制。</p>
     *
     * @return 配置好的重试模板
     */
    @Primary
    @Bean(name = "openAiRetryTemplate")
    @ConditionalOnProperty(prefix = "baseai.llm.openai", name = "max-retries", matchIfMissing = false)
    public RetryTemplate openAiRetryTemplate() {
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
        backOffPolicy.setMultiplier(2.0);       // 每次重试间隔翻倍
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

    /**
     * 使用默认重试模板（当没有配置自定义重试次数时）
     */
    @Bean(name = "openAiRetryTemplate")
    @ConditionalOnMissingBean(name = "openAiRetryTemplate")
    public RetryTemplate openAiDefaultRetryTemplate() {
        log.info("使用Spring AI默认重试策略 (OpenAI)");

        // 创建一个基本的重试模板
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3); // 默认重试3次
        retryTemplate.setRetryPolicy(retryPolicy);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMaxInterval(10000);
        backOffPolicy.setMultiplier(2.0);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

    /**
     * 创建自定义的OpenAI聊天模型
     *
     * <p>这是主要的聊天模型Bean，集成了所有自定义配置，
     * 包括API客户端、默认选项和重试策略。</p>
     *
     * @param openAiApi           自定义的OpenAI API
     * @param chatOptions         自定义的聊天选项
     * @param openAiRetryTemplate 重试模板
     * @return 配置好的聊天模型
     */
    @Bean
    @Primary
    @ConditionalOnBean(OpenAiApi.class)
    public OpenAiChatModel customOpenAiChatModel(
            OpenAiApi openAiApi,
            OpenAiChatOptions chatOptions,
            RetryTemplate openAiRetryTemplate) {

        log.info("创建自定义OpenAI聊天模型");

        try {
            // 使用Builder模式创建聊天模型
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(chatOptions)
                    .retryTemplate(openAiRetryTemplate)
                    .build();

            log.info("OpenAI聊天模型创建成功");
            return chatModel;

        } catch (Exception e) {
            log.error("创建OpenAI聊天模型失败", e);
            throw new IllegalStateException("无法创建OpenAI聊天模型", e);
        }
    }


    /**
     * 创建专用的OpenAI嵌入模型
     *
     * <p>专门为OpenAIEmbeddingService创建的嵌入模型Bean，使用独立的配置和选项。</p>
     *
     * @param openAiApi            由Spring注入的OpenAI API实例
     * @param embeddingOptions     由Spring注入的嵌入模型选项
     * @param openAiRetryTemplate  由Spring注入的重试模板
     * @return 配置好的嵌入模型
     */
    @Bean(name = "openAiEmbeddingModel")
    @ConditionalOnProperty(prefix = "baseai.llm.openai", name = "enabled", havingValue = "true")
    @ConditionalOnBean(OpenAiApi.class)
    public OpenAiEmbeddingModel openAiEmbeddingModel(
            OpenAiApi openAiApi,
            OpenAiEmbeddingOptions embeddingOptions,
            RetryTemplate openAiRetryTemplate) {

        log.info("创建专用的OpenAI嵌入模型");

        try {
            OpenAiEmbeddingModel embeddingModel = new OpenAiEmbeddingModel(
                    openAiApi,
                    MetadataMode.ALL,
                    embeddingOptions,
                    openAiRetryTemplate
            );

            log.info("OpenAI嵌入模型创建成功: model={}, dimension={}",
                    embeddingOptions.getModel(), embeddingOptions.getDimensions());

            return embeddingModel;

        } catch (Exception e) {
            log.error("创建OpenAI嵌入模型失败", e);
            throw new IllegalStateException("无法创建OpenAI嵌入模型", e);
        }
    }

    /**
     * 创建通用的EmbeddingModel Bean（用于自动装配）
     *
     * <p>当没有其他EmbeddingModel Bean时，提供一个默认的OpenAI嵌入模型。
     * 这确保了向后兼容性和自动装配的正确性。</p>
     *
     * @param openAiApi            OpenAI API实例
     * @param embeddingOptions     嵌入模型选项
     * @param openAiRetryTemplate  重试模板
     * @return 通用嵌入模型
     */
    @Bean
    @ConditionalOnProperty(prefix = "baseai.llm.openai", name = "enabled", havingValue = "true")
    @ConditionalOnBean(OpenAiApi.class)
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel defaultEmbeddingModel(
            OpenAiApi openAiApi,
            OpenAiEmbeddingOptions embeddingOptions,
            RetryTemplate openAiRetryTemplate) {

        log.info("创建默认的OpenAI嵌入模型（EmbeddingModel接口）");

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.ALL, embeddingOptions, openAiRetryTemplate);
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

    /**
     * 创建RestTemplate Bean（如果不存在）
     *
     * <p>某些服务可能需要额外的RestTemplate实例</p>
     *
     * @return RestTemplate实例
     */
    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .connectTimeout(llmProps.getOpenai().getTimeout())
                .readTimeout(llmProps.getOpenai().getTimeout())
                .build();
    }
}