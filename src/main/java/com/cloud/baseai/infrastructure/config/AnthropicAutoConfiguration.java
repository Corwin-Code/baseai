package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.config.base.BaseAutoConfiguration;
import com.cloud.baseai.infrastructure.config.properties.KnowledgeBaseProperties;
import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>Anthropic大语言模型服务的Spring自动配置类</h2>
 *
 * <p>该配置类负责集成Spring AI框架与Anthropic Claude模型配置。
 * 使用Spring AI提供的官方Anthropic集成，确保与其他LLM服务的一致性，
 * 并提供企业级的错误处理、重试机制和监控功能。</p>
 *
 * <p><b>Anthropic Claude特色：</b></p>
 * <ul>
 * <li><b>强推理能力：</b>Claude在复杂推理和分析任务上表现卓越</li>
 * <li><b>安全性：</b>内置的安全机制，减少有害内容生成</li>
 * <li><b>长文本处理：</b>支持超长文本的理解和生成，上下文可达200K tokens</li>
 * <li><b>编程能力：</b>在代码生成和理解方面有优异表现</li>
 * <li><b>指令遵循：</b>对复杂指令的理解和执行能力强</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(prefix = "baseai.llm.anthropic", name = "enabled", havingValue = "true", matchIfMissing = false)
public class AnthropicAutoConfiguration extends BaseAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AnthropicAutoConfiguration.class);

    private final KnowledgeBaseProperties kbProps;
    private final LlmProperties llmProps;
    private final LlmProperties.AnthropicProperties anthropicConfig;

    public AnthropicAutoConfiguration(KnowledgeBaseProperties kbProps, LlmProperties llmProps) {
        this.kbProps = kbProps;
        this.llmProps = llmProps;
        this.anthropicConfig = llmProps.getAnthropic();

        // 统一初始化
        initializeConfiguration();
    }

    @Override
    protected String getConfigurationName() {
        return "Anthropic Claude AI";
    }

    @Override
    protected String getModuleName() {
        return "ANTHROPIC";
    }

    /**
     * 验证Anthropic配置参数
     *
     * <p>在Bean创建前验证所有必要的配置参数，确保服务能够正常启动。
     * Anthropic的API密钥格式和认证方式需要特别验证。</p>
     */
    @Override
    protected void validateConfiguration() {
        logInfo("开始验证Anthropic配置参数...");

        // 基础配置验证
        validateNotBlank(anthropicConfig.getApiKey(), "Anthropic API密钥");
        validateNotBlank(anthropicConfig.getBaseUrl(), "Anthropic BaseURL");
        validateNotNull(anthropicConfig.getTimeout(), "Anthropic超时时间");
        validatePositive(anthropicConfig.getMaxRetries(), "Anthropic重试次数");
        validateNotEmpty(anthropicConfig.getModels(), "Anthropic支持的模型列表");

        // URL格式验证
        validateUrl(anthropicConfig.getBaseUrl(), "Anthropic BaseURL");

        // API密钥格式验证
        if (!anthropicConfig.getApiKey().startsWith("sk-ant-")) {
            logWarning("API密钥格式可能不正确，标准格式应以 'sk-ant-' 开头");
        }

        // BaseURL验证
        if (!anthropicConfig.getBaseUrl().contains("anthropic.com")) {
            logWarning("BaseURL可能不是Anthropic官方地址，请确认配置正确");
        }

        // 超时时间验证
        validateTimeout(anthropicConfig.getTimeout(), "请求超时时间");
        if (anthropicConfig.getTimeout().toMinutes() > 5) {
            logWarning("超时时间设置过长 (%s)，可能影响用户体验", anthropicConfig.getTimeout());
        }

        // 重试次数验证
        validateRange(anthropicConfig.getMaxRetries(), 1, 10, "最大重试次数");

        // 模型列表验证
        validateModelList();

        // 默认参数验证
        validateDefaultParameters();

        logSuccess("所有配置验证通过");
    }

    /**
     * 验证模型列表
     */
    private void validateModelList() {
        List<String> supportedModels = List.of(
                "claude-3-haiku-20240307",
                "claude-3-sonnet-20240229",
                "claude-3-opus-20240229",
                "claude-3-5-sonnet-20241022",
                "claude-3-5-sonnet-20240620"
        );

        for (String model : anthropicConfig.getModels()) {
            if (!supportedModels.contains(model)) {
                logWarning("模型 '%s' 可能不受支持，支持的模型: %s", model, supportedModels);
            }
        }
    }

    /**
     * 验证默认参数
     */
    private void validateDefaultParameters() {
        LlmProperties.DefaultParametersProperties defaults = llmProps.getDefaultParameters();

        // 温度参数验证
        if (defaults.getTemperature() != null) {
            validateRange(defaults.getTemperature().intValue(), 0, 1, "温度参数");
        }

        // 最大令牌数验证
        if (defaults.getMaxTokens() != null) {
            validateRange(defaults.getMaxTokens(), 1, 100000, "最大令牌数");
        }

        // Top-p参数验证
        if (defaults.getTopP() != null) {
            validateRange(defaults.getTopP().intValue(), 0, 1, "Top-p参数");
        }
    }

    @Override
    protected Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("启用状态", anthropicConfig.getEnabled());
        summary.put("服务地址", anthropicConfig.getBaseUrl());
        summary.put("API密钥", anthropicConfig.getApiKey());
        summary.put("超时时间", anthropicConfig.getTimeout());
        summary.put("最大重试次数", anthropicConfig.getMaxRetries());
        summary.put("支持的模型数量", anthropicConfig.getModels().size());
        summary.put("支持的模型", anthropicConfig.getModels());

        // 默认参数
        LlmProperties.DefaultParametersProperties defaults = llmProps.getDefaultParameters();
        summary.put("默认模型", defaults.getModel());
        summary.put("默认温度", defaults.getTemperature());
        summary.put("默认最大令牌数", defaults.getMaxTokens());

        return summary;
    }

    /**
     * 创建自定义的Anthropic API Bean
     *
     * <p>使用项目自定义的配置创建Anthropic API客户端，支持自定义超时、
     * 重试策略和请求头配置。</p>
     *
     * @return 配置好的Anthropic API实例
     */
    @Bean
    @ConditionalOnMissingBean(AnthropicApi.class)
    public AnthropicApi customAnthropicApi() {
        logBeanCreation("AnthropicApi", "Anthropic API客户端");

        try {
            // 创建自定义RestTemplate
            final RestTemplate restTemplate = new RestTemplateBuilder()
                    .connectTimeout(anthropicConfig.getTimeout())
                    .readTimeout(anthropicConfig.getTimeout())
                    .build();

            // 构建RestClient
            RestClient.Builder restClientBuilder = RestClient.builder(restTemplate)
                    .baseUrl(anthropicConfig.getBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + anthropicConfig.getApiKey())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .defaultHeader(HttpHeaders.USER_AGENT, "BaseAI-SpringAI/2.0");

            // 使用Spring AI的AnthropicApi.Builder来创建实例
            AnthropicApi api = new AnthropicApi.Builder()
                    .baseUrl(anthropicConfig.getBaseUrl())
                    .apiKey(anthropicConfig.getApiKey())
                    .restClientBuilder(restClientBuilder)
                    .build();

            logBeanSuccess("AnthropicApi");
            return api;

        } catch (Exception e) {
            String errorMsg = String.format("创建AnthropicApi失败: %s", e.getMessage());
            log.error("❌ [{}] {}", getModuleName(), errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    /**
     * 创建自定义的Anthropic聊天模型选项
     *
     * <p>使用项目配置的默认参数创建聊天选项，确保与业务需求一致。</p>
     *
     * @return 配置好的聊天选项
     */
    @Bean
    @ConditionalOnMissingBean(name = "anthropicChatOptions")
    public AnthropicChatOptions anthropicChatOptions() {
        logBeanCreation("AnthropicChatOptions", "Anthropic聊天模型选项");

        LlmProperties.DefaultParametersProperties defaults = llmProps.getDefaultParameters();

        // 确保使用Anthropic兼容的模型
        String defaultModel = defaults.getModel();
        if (defaultModel == null || !defaultModel.startsWith("claude-")) {
            defaultModel = "claude-3-sonnet-20240229"; // 使用Anthropic的默认模型
            logWarning("配置的默认模型不兼容Anthropic，使用默认模型: %s", defaultModel);
        }

        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(defaultModel)
                .temperature(defaults.getTemperature())
                .maxTokens(defaults.getMaxTokens())
                .topP(defaults.getTopP())
                .build();

        logInfo("聊天选项配置 - 模型: %s, 温度: %s, 最大令牌: %s",
                defaultModel, defaults.getTemperature(), defaults.getMaxTokens());

        logBeanSuccess("AnthropicChatOptions");
        return options;
    }

    /**
     * 创建自定义重试模板
     *
     * <p>为Anthropic API调用提供自定义的重试策略，包括指数退避
     * 和智能重试次数控制。Anthropic API可能有不同的限流策略，
     * 需要适当调整重试间隔和次数。</p>
     *
     * @return 配置好的重试模板
     */
    @Primary
    @Bean(name = "anthropicRetryTemplate")
    @ConditionalOnProperty(prefix = "baseai.llm.anthropic", name = "max-retries", matchIfMissing = false)
    public RetryTemplate anthropicRetryTemplate() {
        logBeanCreation("AnthropicRetryTemplate", "Anthropic重试策略模板");

        int maxRetries = anthropicConfig.getMaxRetries();

        RetryTemplate retryTemplate = new RetryTemplate();

        // 配置重试策略
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxRetries);
        retryTemplate.setRetryPolicy(retryPolicy);

        // 配置退避策略 - Anthropic可能需要更长的退避时间
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2000); // 初始间隔2秒
        backOffPolicy.setMaxInterval(30000);    // 最大间隔30秒
        backOffPolicy.setMultiplier(2.0);       // 每次重试间隔翻倍
        retryTemplate.setBackOffPolicy(backOffPolicy);

        logInfo("重试策略配置 - 最大重试: %d次, 初始间隔: 2秒, 最大间隔: 30秒", maxRetries);
        logBeanSuccess("AnthropicRetryTemplate");

        return retryTemplate;
    }

    /**
     * 使用默认重试模板（当没有配置自定义重试次数时）
     */
    @Bean(name = "anthropicRetryTemplate")
    @ConditionalOnMissingBean(name = "anthropicRetryTemplate")
    public RetryTemplate anthropicDefaultRetryTemplate() {
        logBeanCreation("AnthropicDefaultRetryTemplate", "Anthropic默认重试策略");

        // 创建一个基本的重试模板
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(2); // Anthropic默认重试2次，避免过度消耗配额
        retryTemplate.setRetryPolicy(retryPolicy);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2000); // Anthropic建议较长的初始间隔
        backOffPolicy.setMaxInterval(30000);
        backOffPolicy.setMultiplier(2.0);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        logInfo("使用默认重试策略 - 最大重试: 2次");
        logBeanSuccess("AnthropicDefaultRetryTemplate");

        return retryTemplate;
    }

    /**
     * 创建自定义的Anthropic聊天模型
     *
     * <p>这是主要的聊天模型Bean，集成了所有自定义配置，
     * 包括API客户端、默认选项和重试策略。</p>
     *
     * @param anthropicApi           自定义的Anthropic API
     * @param chatOptions            自定义的聊天选项
     * @param anthropicRetryTemplate 重试模板
     * @return 配置好的聊天模型
     */
    @Bean
    @ConditionalOnBean(AnthropicApi.class)
    public AnthropicChatModel customAnthropicChatModel(
            AnthropicApi anthropicApi,
            AnthropicChatOptions chatOptions,
            RetryTemplate anthropicRetryTemplate) {

        logBeanCreation("AnthropicChatModel", "Anthropic聊天模型主Bean");

        try {
            // 使用Builder模式创建聊天模型
            AnthropicChatModel chatModel = AnthropicChatModel.builder()
                    .anthropicApi(anthropicApi)
                    .defaultOptions(chatOptions)
                    .retryTemplate(anthropicRetryTemplate)
                    .build();

            logBeanSuccess("AnthropicChatModel");
            logSuccess("Anthropic聊天服务已就绪，可以开始对话");

            return chatModel;

        } catch (Exception e) {
            String errorMsg = String.format("创建AnthropicChatModel失败: %s", e.getMessage());
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
                .connectTimeout(llmProps.getAnthropic().getTimeout())
                .readTimeout(llmProps.getAnthropic().getTimeout())
                .build();
    }
}