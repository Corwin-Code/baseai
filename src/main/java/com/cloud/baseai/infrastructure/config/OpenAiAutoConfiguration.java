package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.config.base.BaseAutoConfiguration;
import com.cloud.baseai.infrastructure.config.properties.KnowledgeBaseProperties;
import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.cloud.baseai.infrastructure.utils.AuditUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>OpenAI大语言模型服务的Spring自动配置类</h2>
 *
 * <p>该配置类负责集成Spring AI框架与自定义的OpenAI LLM配置。
 * * 它覆盖了Spring AI的默认配置，使用项目自定义的配置属性，
 * * 并提供企业级的错误处理、重试机制和监控功能。</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "baseai.llm.openai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenAiAutoConfiguration extends BaseAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OpenAiAutoConfiguration.class);

    private final KnowledgeBaseProperties kbProps;
    private final LlmProperties llmProps;
    private final LlmProperties.OpenAiProperties openAiConfig;

    public OpenAiAutoConfiguration(KnowledgeBaseProperties kbProps, LlmProperties llmProps) {
        this.kbProps = kbProps;
        this.llmProps = llmProps;
        this.openAiConfig = llmProps.getOpenai();

        // 统一初始化
        initializeConfiguration();
    }

    @Override
    protected String getConfigurationName() {
        return "OpenAI语言模型服务";
    }

    @Override
    protected String getModuleName() {
        return "OPENAI";
    }

    /**
     * 验证OpenAI配置参数
     *
     * <p>在Bean创建前验证所有必要的配置参数，确保服务能够正常启动。</p>
     */
    @Override
    protected void validateConfiguration() {
        logInfo("开始验证OpenAI配置参数...");

        // 基础配置验证
        validateNotBlank(openAiConfig.getApiKey(), "OpenAI API密钥");
        validateNotBlank(openAiConfig.getBaseUrl(), "OpenAI BaseURL");
        validateNotNull(openAiConfig.getTimeout(), "OpenAI超时时间");
        validatePositive(openAiConfig.getMaxRetries(), "OpenAI重试次数");
        validateNotEmpty(openAiConfig.getModels(), "OpenAI支持的模型列表");

        // URL格式验证
        validateUrl(openAiConfig.getBaseUrl(), "OpenAI BaseURL");

        // API密钥格式验证
        validateApiKeyFormat();

        // BaseURL有效性验证
        validateBaseUrlFormat();

        // 超时时间验证
        validateTimeout(openAiConfig.getTimeout(), "请求超时时间");
        if (openAiConfig.getTimeout().toMinutes() > 10) {
            logWarning("超时时间设置过长 (%s)，建议不超过5分钟", openAiConfig.getTimeout());
        }

        // 重试次数验证
        validateRange(openAiConfig.getMaxRetries(), 1, 10, "最大重试次数");
        if (openAiConfig.getMaxRetries() > 5) {
            logWarning("重试次数较高 (%d)，可能增加API调用成本", openAiConfig.getMaxRetries());
        }

        // 模型列表验证
        validateModelList();

        // 组织ID验证（可选）
        validateOrganizationId();

        // 默认参数验证
        validateDefaultParameters();

        logSuccess("所有配置验证通过");
    }

    /**
     * 验证API密钥格式
     */
    private void validateApiKeyFormat() {
        String apiKey = openAiConfig.getApiKey();

        if (apiKey.startsWith("sk-")) {
            logInfo("检测到标准OpenAI API密钥格式");
        } else if (apiKey.startsWith("hk-")) {
            logInfo("检测到第三方代理服务API密钥格式");
        } else {
            logWarning("API密钥格式可能不正确，标准格式应以 'sk-' 开头");
        }

        if (apiKey.length() < 20) {
            logWarning("API密钥长度过短 (%d字符)，可能无效", apiKey.length());
        }
    }

    /**
     * 验证BaseURL格式
     */
    private void validateBaseUrlFormat() {
        String baseUrl = openAiConfig.getBaseUrl();

        if (baseUrl.contains("api.openai.com")) {
            logInfo("使用OpenAI官方API地址");
        } else if (baseUrl.contains("openai-hk.com") || baseUrl.contains("api2d.com")) {
            logInfo("使用第三方代理服务地址");
        } else {
            logWarning("BaseURL可能不是已知的OpenAI服务地址: %s", baseUrl);
        }

        if (baseUrl.endsWith("/")) {
            logWarning("BaseURL不应以 '/' 结尾，建议移除: %s", baseUrl);
        }
    }

    /**
     * 验证模型列表
     */
    private void validateModelList() {
        List<String> supportedChatModels = List.of(
                "gpt-4", "gpt-4-turbo", "gpt-4-turbo-preview", "gpt-4-0125-preview", "gpt-4-1106-preview",
                "gpt-3.5-turbo", "gpt-3.5-turbo-0125", "gpt-3.5-turbo-1106", "gpt-3.5-turbo-16k"
        );

        List<String> supportedEmbeddingModels = List.of(
                "text-embedding-3-small", "text-embedding-3-large", "text-embedding-ada-002"
        );

        for (String model : openAiConfig.getModels()) {
            if (!supportedChatModels.contains(model) && !supportedEmbeddingModels.contains(model)) {
                logWarning("模型 '%s' 可能不受支持或已废弃", model);
            }
        }

        // 检查是否同时配置了聊天和嵌入模型
        boolean hasChatModel = openAiConfig.getModels().stream().anyMatch(supportedChatModels::contains);
        boolean hasEmbeddingModel = openAiConfig.getModels().stream().anyMatch(supportedEmbeddingModels::contains);

        if (hasChatModel && hasEmbeddingModel) {
            logInfo("配置了聊天模型和嵌入模型，支持完整功能");
        } else if (hasChatModel) {
            logInfo("仅配置了聊天模型");
        } else if (hasEmbeddingModel) {
            logInfo("仅配置了嵌入模型");
        }
    }

    /**
     * 验证组织ID
     */
    private void validateOrganizationId() {
        String organization = openAiConfig.getOrganization();
        if (StringUtils.hasText(organization)) {
            if (organization.startsWith("org-")) {
                logInfo("组织ID格式正确: %s", organization);
            } else {
                logWarning("组织ID格式可能不正确，标准格式应以 'org-' 开头");
            }
        } else {
            logInfo("未配置组织ID，将使用默认组织");
        }
    }

    /**
     * 验证默认参数
     */
    private void validateDefaultParameters() {
        LlmProperties.DefaultParametersProperties defaults = llmProps.getDefaultParameters();

        // 验证默认模型
        String defaultModel = defaults.getModel();
        if (!isOpenAiCompatibleModel(defaultModel)) {
            logWarning("默认模型 '%s' 不是OpenAI模型，聊天功能可能无法正常工作", defaultModel);
        }

        // 温度参数验证
        if (defaults.getTemperature() != null) {
            validateRange(defaults.getTemperature().intValue(), 0, 2, "温度参数");
            if (defaults.getTemperature() > 1.5) {
                logWarning("温度参数较高 (%.2f)，回答可能过于随机", defaults.getTemperature());
            }
        }

        // 最大令牌数验证
        if (defaults.getMaxTokens() != null) {
            validateRange(defaults.getMaxTokens(), 1, 100000, "最大令牌数");
            if (defaults.getMaxTokens() > 4000) {
                logWarning("最大令牌数较高 (%d)，可能增加API调用成本", defaults.getMaxTokens());
            }
        }

        // 其他参数验证
        if (defaults.getTopP() != null) {
            validateRange(defaults.getTopP().intValue(), 0, 1, "Top-p参数");
        }
        if (defaults.getFrequencyPenalty() != null) {
            validateRange(defaults.getFrequencyPenalty().intValue(), -2, 2, "频率惩罚参数");
        }
        if (defaults.getPresencePenalty() != null) {
            validateRange(defaults.getPresencePenalty().intValue(), -2, 2, "存在惩罚参数");
        }
    }

    /**
     * 检查是否为OpenAI兼容的聊天模型
     */
    private boolean isOpenAiCompatibleModel(String model) {
        if (model == null) return false;
        return model.startsWith("gpt-") || model.startsWith("text-davinci-") ||
                model.startsWith("text-curie-") || model.startsWith("text-babbage-") ||
                model.startsWith("text-ada-");
    }

    @Override
    protected Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("启用状态", openAiConfig.getEnabled());
        summary.put("服务地址", AuditUtils.maskUrl(openAiConfig.getBaseUrl()));
        summary.put("API密钥", AuditUtils.maskSecretKeepingPrefix(openAiConfig.getApiKey()));
        summary.put("组织ID", StringUtils.hasText(openAiConfig.getOrganization()) ?
                openAiConfig.getOrganization() : "未配置");
        summary.put("超时时间", openAiConfig.getTimeout());
        summary.put("最大重试次数", openAiConfig.getMaxRetries());
        summary.put("支持的模型数量", openAiConfig.getModels().size());
        summary.put("支持的模型", openAiConfig.getModels());

        // 默认参数
        LlmProperties.DefaultParametersProperties defaults = llmProps.getDefaultParameters();
        summary.put("默认模型", defaults.getModel());
        summary.put("默认温度", defaults.getTemperature());
        summary.put("默认最大令牌数", defaults.getMaxTokens());

        return summary;
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
        logBeanCreation("OpenAiApi", "OpenAI API客户端");

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
            if (StringUtils.hasText(openAiConfig.getOrganization())) {
                restClientBuilder.defaultHeader("OpenAI-Organization", openAiConfig.getOrganization());
                logInfo("已配置组织ID请求头");
            }

            // 使用Spring AI的OpenAiApi.Builder来创建实例
            OpenAiApi api = new OpenAiApi.Builder()
                    .baseUrl(openAiConfig.getBaseUrl())
                    .apiKey(openAiConfig.getApiKey())
                    .restClientBuilder(restClientBuilder)
                    .build();

            logBeanSuccess("OpenAiApi");
            return api;

        } catch (Exception e) {
            String errorMsg = String.format("创建OpenAiApi失败: %s", e.getMessage());
            log.error("❌ [{}] {}", getModuleName(), errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
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
        logBeanCreation("OpenAiChatOptions", "OpenAI聊天模型选项");

        LlmProperties.DefaultParametersProperties defaults = llmProps.getDefaultParameters();

        // 确保使用OpenAI兼容的模型
        String defaultModel = defaults.getModel();
        if (!isOpenAiCompatibleModel(defaultModel)) {
            defaultModel = "gpt-3.5-turbo"; // 使用OpenAI的默认模型
            logWarning("配置的默认模型不兼容OpenAI，使用默认模型: %s", defaultModel);
        }

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(defaultModel)
                .temperature(defaults.getTemperature())
                .maxTokens(defaults.getMaxTokens())
                .topP(defaults.getTopP())
                .frequencyPenalty(defaults.getFrequencyPenalty())
                .presencePenalty(defaults.getPresencePenalty())
                .build();

        logInfo("聊天选项配置 - 模型: %s, 温度: %s, 最大令牌: %s",
                defaultModel, defaults.getTemperature(), defaults.getMaxTokens());

        logBeanSuccess("OpenAiChatOptions");
        return options;
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
        logBeanCreation("OpenAiEmbeddingOptions", "OpenAI嵌入模型选项");

        // 确定嵌入模型
        String embeddingModel = kbProps.getEmbedding().getDefaultModel();
        Integer dimension = kbProps.getEmbedding().getDimension();

        // 如果配置的默认模型不是OpenAI模型，使用OpenAI的默认嵌入模型
        if (!isOpenAiEmbeddingModel(embeddingModel)) {
            embeddingModel = "text-embedding-3-small";
            logWarning("配置的嵌入模型不兼容OpenAI，使用默认模型: %s", embeddingModel);
        }

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(embeddingModel)
                .dimensions(dimension)
                .build();

        logInfo("嵌入选项配置 - 模型: %s, 维度: %s", embeddingModel, dimension);
        logBeanSuccess("OpenAiEmbeddingOptions");

        return options;
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
    @Bean(name = "openAiRetryTemplate")
    @ConditionalOnProperty(prefix = "baseai.llm.openai", name = "max-retries", matchIfMissing = false)
    public RetryTemplate openAiRetryTemplate() {
        logBeanCreation("OpenAiRetryTemplate", "OpenAI重试策略模板");

        int maxRetries = llmProps.getOpenai().getMaxRetries();

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

        logInfo("重试策略配置 - 最大重试: %d次, 初始间隔: 1秒, 最大间隔: 10秒", maxRetries);
        logBeanSuccess("OpenAiRetryTemplate");

        return retryTemplate;
    }

    /**
     * 使用默认重试模板（当没有配置自定义重试次数时）
     */
    @Bean(name = "openAiRetryTemplate")
    @ConditionalOnMissingBean(name = "openAiRetryTemplate")
    public RetryTemplate openAiDefaultRetryTemplate() {
        logBeanCreation("OpenAiDefaultRetryTemplate", "OpenAI默认重试策略");

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

        logInfo("使用默认重试策略 - 最大重试: 3次");
        logBeanSuccess("OpenAiDefaultRetryTemplate");

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
            @Qualifier("openAiRetryTemplate") RetryTemplate openAiRetryTemplate) {

        logBeanCreation("OpenAiChatModel", "OpenAI聊天模型主Bean");

        try {
            // 使用Builder模式创建聊天模型
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(chatOptions)
                    .retryTemplate(openAiRetryTemplate)
                    .build();

            logBeanSuccess("OpenAiChatModel");
            logSuccess("OpenAI聊天服务已就绪，可以开始对话");

            return chatModel;

        } catch (Exception e) {
            String errorMsg = String.format("创建OpenAiChatModel失败: %s", e.getMessage());
            log.error("❌ [{}] {}", getModuleName(), errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }


    /**
     * 创建专用的OpenAI嵌入模型
     *
     * <p>专门为OpenAIEmbeddingService创建的嵌入模型Bean，使用独立的配置和选项。</p>
     *
     * @param openAiApi           由Spring注入的OpenAI API实例
     * @param embeddingOptions    由Spring注入的嵌入模型选项
     * @param openAiRetryTemplate 由Spring注入的重试模板
     * @return 配置好的嵌入模型
     */
    @Bean(name = "openAiEmbeddingModel")
    @ConditionalOnProperty(prefix = "baseai.llm.openai", name = "enabled", havingValue = "true")
    @ConditionalOnBean(OpenAiApi.class)
    public OpenAiEmbeddingModel openAiEmbeddingModel(
            OpenAiApi openAiApi,
            OpenAiEmbeddingOptions embeddingOptions,
            @Qualifier("openAiRetryTemplate") RetryTemplate openAiRetryTemplate) {

        logBeanCreation("OpenAiEmbeddingModel", "OpenAI嵌入模型专用Bean");

        try {
            OpenAiEmbeddingModel embeddingModel = new OpenAiEmbeddingModel(
                    openAiApi,
                    MetadataMode.ALL,
                    embeddingOptions,
                    openAiRetryTemplate
            );

            logInfo("嵌入模型配置完成 - 模型: %s, 维度: %s",
                    embeddingOptions.getModel(), embeddingOptions.getDimensions());

            logBeanSuccess("OpenAiEmbeddingModel");
            return embeddingModel;

        } catch (Exception e) {
            String errorMsg = String.format("创建OpenAiEmbeddingModel失败: %s", e.getMessage());
            log.error("❌ [{}] {}", getModuleName(), errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
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
        logBeanCreation("ObjectMapper", "JSON对象映射器");
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
        logBeanCreation("RestTemplate", "REST客户端模板");
        return new RestTemplateBuilder()
                .connectTimeout(openAiConfig.getTimeout())
                .readTimeout(openAiConfig.getTimeout())
                .build();
    }
}