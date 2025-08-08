package com.cloud.baseai.infrastructure.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import com.cloud.baseai.infrastructure.config.properties.KnowledgeBaseProperties;
import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.MetadataMode;
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
 * <h2>通义千问自动配置类</h2>
 *
 * <p>基于Spring AI Alibaba框架的通义千问配置类。该配置类负责初始化
 * DashScope API客户端、聊天模型和相关的配置选项。</p>
 *
 * <p><b>配置特点：</b></p>
 * <ul>
 * <li><b>条件装配：</b>只有在启用Qwen服务时才会创建相关Bean</li>
 * <li><b>自定义重试：</b>提供指数退避的重试策略</li>
 * <li><b>灵活配置：</b>支持通过配置文件自定义所有参数</li>
 * <li><b>监控友好：</b>包含详细的日志记录</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(prefix = "baseai.llm.qwen", name = "enabled", havingValue = "true", matchIfMissing = true)
public class QwenAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(QwenAutoConfiguration.class);

    private final KnowledgeBaseProperties kbProps;
    private final LlmProperties llmProps;

    public QwenAutoConfiguration(KnowledgeBaseProperties kbProps, LlmProperties llmProps) {
        this.kbProps = kbProps;
        this.llmProps = llmProps;

        // 配置验证
        validateQwenConfiguration();
    }

    /**
     * 验证通义千问配置
     */
    private void validateQwenConfiguration() {
        LlmProperties.QwenProperties qwenConfig = llmProps.getQwen();

        Assert.hasText(qwenConfig.getApiKey(), "通义千问API密钥不能为空");
        Assert.hasText(qwenConfig.getBaseUrl(), "通义千问BaseURL不能为空");
        Assert.notNull(qwenConfig.getTimeout(), "通义千问超时时间不能为空");
        Assert.isTrue(qwenConfig.getMaxRetries() > 0, "通义千问重试次数必须大于0");
        Assert.notEmpty(qwenConfig.getModels(), "通义千问支持的模型列表不能为空");

        log.info("通义千问配置验证通过: baseUrl={}, models={}",
                qwenConfig.getBaseUrl(), qwenConfig.getModels());
    }

    /**
     * 创建自定义的DashScope API Bean
     *
     * <p>使用项目自定义的配置创建DashScope API客户端，支持自定义超时、
     * 重试策略和请求头配置。</p>
     *
     * @return 配置好的DashScope API实例
     */
    @Bean
    @ConditionalOnMissingBean(DashScopeApi.class)
    public DashScopeApi dashScopeApi() {
        LlmProperties.QwenProperties qwenConfig = llmProps.getQwen();

        log.info("创建DashScope API: baseUrl={}, timeout={}",
                qwenConfig.getBaseUrl(), qwenConfig.getTimeout());

        try {
            // 创建自定义RestTemplate
            final RestTemplate restTemplate = new RestTemplateBuilder()
                    .connectTimeout(qwenConfig.getTimeout())
                    .readTimeout(qwenConfig.getTimeout())
                    .build();

            // 构建RestClient
            RestClient.Builder restClientBuilder = RestClient.builder(restTemplate)
                    .baseUrl(qwenConfig.getBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + qwenConfig.getApiKey())
                    .defaultHeader(HttpHeaders.USER_AGENT, "BaseAI-SpringAI/1.0")
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .defaultHeader("X-DashScope-SSE", "disable"); // 默认禁用SSE

            // 创建DashScope API
            DashScopeApi api = DashScopeApi.builder()
                    .baseUrl(qwenConfig.getBaseUrl())
                    .apiKey(qwenConfig.getApiKey())
                    .restClientBuilder(restClientBuilder)
                    .build();

            log.info("DashScope API创建成功");
            return api;

        } catch (Exception e) {
            log.error("创建DashScope API失败", e);
            throw new IllegalStateException("无法创建DashScope API", e);
        }
    }

    /**
     * 创建自定义的Qwen聊天模型选项
     *
     * <p>使用项目配置的默认参数创建聊天选项，确保与业务需求一致。</p>
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
     * 为嵌入模型创建专用的选项Bean
     *
     * <p>嵌入模型和聊天模型使用不同的配置参数，需要单独的配置Bean。</p>
     *
     * @return 配置好的嵌入模型选项
     */
    @Bean
    @ConditionalOnMissingBean(name = "qwenEmbeddingOptions")
    public DashScopeEmbeddingOptions qwenEmbeddingOptions() {
        // 确定嵌入模型
        String embeddingModel = kbProps.getEmbedding().getDefaultModel();
        Integer dimension = kbProps.getEmbedding().getDimension();

        // 如果配置的默认模型不是通义千问模型，使用通义千问的默认嵌入模型
        if (embeddingModel == null || !embeddingModel.startsWith("text-embedding-v")) {
            embeddingModel = "text-embedding-v2";
        }

        log.info("创建Qwen嵌入模型选项: model={}, dimension={}", embeddingModel, dimension);

        return DashScopeEmbeddingOptions.builder()
                .withModel(embeddingModel)
                .withDimensions(dimension)
                .build();
    }

    /**
     * 创建自定义重试模板
     *
     * <p>为DashScope API调用提供自定义的重试策略，包括指数退避
     * 和智能重试次数控制。</p>
     *
     * @return 配置好的重试模板
     */
    @Primary
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
     * 创建自定义的Qwen聊天模型
     *
     * <p>这是主要的聊天模型Bean，集成了所有自定义配置，
     * 包括API客户端、默认选项和重试策略。</p>
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

        try {
            // 创建聊天模型
            DashScopeChatModel chatModel = DashScopeChatModel.builder()
                    .dashScopeApi(dashScopeApi)
                    .defaultOptions(chatOptions)
                    .retryTemplate(qwenRetryTemplate)
                    .build();

            log.info("Qwen聊天模型创建成功");
            return chatModel;

        } catch (Exception e) {
            log.error("创建Qwen聊天模型失败", e);
            throw new IllegalStateException("无法创建Qwen聊天模型", e);
        }
    }

    /**
     * 创建专用的Qwen嵌入模型
     *
     * <p>专门为QwenEmbeddingService创建的嵌入模型Bean，使用独立的配置和选项。</p>
     *
     * @param dashScopeApi      由Spring注入的DashScope API实例
     * @param embeddingOptions  由Spring注入的嵌入模型选项
     * @param qwenRetryTemplate 由Spring注入的重试模板
     * @return 配置好的嵌入模型
     */
    @Bean(name = "qwenEmbeddingModel")
    @ConditionalOnProperty(prefix = "baseai.llm.qwen", name = "enabled", havingValue = "true")
    @ConditionalOnBean(DashScopeApi.class)
    public DashScopeEmbeddingModel qwenEmbeddingModel(
            DashScopeApi dashScopeApi,
            DashScopeEmbeddingOptions embeddingOptions,
            RetryTemplate qwenRetryTemplate) {

        log.info("创建专用的Qwen嵌入模型");

        try {
            DashScopeEmbeddingModel embeddingModel = new DashScopeEmbeddingModel(
                    dashScopeApi,
                    MetadataMode.ALL,
                    embeddingOptions,
                    qwenRetryTemplate
            );

            log.info("Qwen嵌入模型创建成功: model={}, dimension={}",
                    embeddingOptions.getModel(), embeddingOptions.getDimensions());

            return embeddingModel;

        } catch (Exception e) {
            log.error("创建Qwen嵌入模型失败", e);
            throw new IllegalStateException("无法创建Qwen嵌入模型", e);
        }
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
                .connectTimeout(llmProps.getQwen().getTimeout())
                .readTimeout(llmProps.getQwen().getTimeout())
                .build();
    }
}