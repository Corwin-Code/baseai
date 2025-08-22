package com.cloud.baseai.infrastructure.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import com.cloud.baseai.infrastructure.config.base.BaseAutoConfiguration;
import com.cloud.baseai.infrastructure.config.properties.KnowledgeBaseProperties;
import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.MetadataMode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * <h2>通义千问自动配置类</h2>
 *
 * <p>基于Spring AI Alibaba框架的通义千问配置类。该配置类负责初始化
 * DashScope API客户端、聊天模型和相关的配置选项。</p>
 *
 * <p><b>通义千问特色：</b></p>
 * <ul>
 * <li><b>中文优化：</b>针对中文场景深度优化，理解更准确</li>
 * <li><b>多模态能力：</b>支持文本、图像、音频等多种模态输入</li>
 * <li><b>代码能力：</b>强大的代码生成和理解能力</li>
 * <li><b>数学推理：</b>在数学问题求解方面表现优异</li>
 * <li><b>长文本：</b>支持超长上下文理解</li>
 * </ul>
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
@ConditionalOnClass(DashScopeApi.class)
public class QwenAutoConfiguration extends BaseAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(QwenAutoConfiguration.class);

    private final KnowledgeBaseProperties kbProps;
    private final LlmProperties llmProps;
    private final LlmProperties.QwenProperties qwenConfig;

    public QwenAutoConfiguration(KnowledgeBaseProperties kbProps, LlmProperties llmProps) {
        this.kbProps = kbProps;
        this.llmProps = llmProps;
        this.qwenConfig = llmProps.getQwen();

        // 统一初始化
        initializeConfiguration();
    }

    @Override
    protected String getConfigurationName() {
        return "通义千问语言模型服务";
    }

    @Override
    protected String getModuleName() {
        return "QWEN";
    }

    /**
     * 验证通义千问配置
     */
    @Override
    protected void validateConfiguration() {
        logInfo("开始验证通义千问配置参数...");

        // 基础配置验证
        validateNotBlank(qwenConfig.getApiKey(), "通义千问API密钥");
        validateNotBlank(qwenConfig.getBaseUrl(), "通义千问BaseURL");
        validateNotNull(qwenConfig.getTimeout(), "通义千问超时时间");
        validatePositive(qwenConfig.getMaxRetries(), "通义千问重试次数");
        validateNotEmpty(qwenConfig.getModels(), "通义千问支持的模型列表");

        // URL格式验证
        validateUrl(qwenConfig.getBaseUrl(), "通义千问BaseURL");

        // API密钥格式验证
        validateApiKeyFormat();

        // BaseURL有效性验证
        validateBaseUrlFormat();

        // 超时时间验证
        validateTimeout(qwenConfig.getTimeout(), "请求超时时间");
        if (qwenConfig.getTimeout().toMinutes() > 10) {
            logWarning("超时时间设置过长 (%s)，通义千问推荐不超过5分钟", qwenConfig.getTimeout());
        }

        // 重试次数验证
        validateRange(qwenConfig.getMaxRetries(), 1, 10, "最大重试次数");
        if (qwenConfig.getMaxRetries() > 5) {
            logWarning("重试次数较高 (%d)，可能增加API调用成本", qwenConfig.getMaxRetries());
        }

        // 模型列表验证
        validateModelList();

        // 默认参数验证
        validateDefaultParameters();

        logSuccess("通义千问配置验证通过");
    }

    /**
     * 验证API密钥格式
     */
    private void validateApiKeyFormat() {
        String apiKey = qwenConfig.getApiKey();

        if (apiKey.startsWith("sk-")) {
            logInfo("检测到标准DashScope API密钥格式");
        } else {
            logWarning("API密钥格式可能不正确，DashScope标准格式通常以特定前缀开头");
        }

        if (apiKey.length() < 20) {
            logWarning("API密钥长度过短 (%d字符)，可能无效", apiKey.length());
        } else if (apiKey.length() > 200) {
            logWarning("API密钥长度过长 (%d字符)，请检查是否正确", apiKey.length());
        }
    }

    /**
     * 验证BaseURL格式
     */
    private void validateBaseUrlFormat() {
        String baseUrl = qwenConfig.getBaseUrl();

        if (baseUrl.contains("dashscope.aliyuncs.com")) {
            logInfo("使用阿里云DashScope官方API地址");
        } else {
            logWarning("BaseURL可能不是阿里云DashScope官方地址: %s", baseUrl);
        }

        if (baseUrl.endsWith("/")) {
            logWarning("BaseURL不应以 '/' 结尾，建议移除: %s", baseUrl);
        }

        // 检查是否使用HTTPS
        if (!baseUrl.startsWith("https://")) {
            logWarning("建议使用HTTPS协议以确保API调用安全");
        }
    }

    /**
     * 验证模型列表
     */
    private void validateModelList() {
        logInfo("验证通义千问模型列表...");

        List<String> supportedChatModels = List.of(
                "qwen-plus", "qwen-turbo", "qwen-max", "qwen-max-longcontext",
                "qwen-coder", "qwen-math", "qwen-vl-plus", "qwen-vl-max"
        );

        List<String> supportedEmbeddingModels = List.of(
                "text-embedding-v2", "text-embedding-v3"
        );

        // 验证聊天模型
        List<String> invalidChatModels = qwenConfig.getModels().stream()
                .filter(model -> !supportedChatModels.contains(model) && !supportedEmbeddingModels.contains(model))
                .toList();

        if (!invalidChatModels.isEmpty()) {
            logWarning("发现可能不支持的模型: %s", invalidChatModels);
        }

        // 检查模型功能组合
        boolean hasChatModel = qwenConfig.getModels().stream().anyMatch(supportedChatModels::contains);
        boolean hasEmbeddingModel = qwenConfig.getModels().stream().anyMatch(supportedEmbeddingModels::contains);
        boolean hasVisionModel = qwenConfig.getModels().stream().anyMatch(model -> model.contains("vl"));
        boolean hasCodeModel = qwenConfig.getModels().stream().anyMatch(model -> model.contains("coder"));
        boolean hasMathModel = qwenConfig.getModels().stream().anyMatch(model -> model.contains("math"));

        logInfo("模型功能分析:");
        logInfo("  聊天模型: %s", hasChatModel ? "已配置" : "未配置");
        logInfo("  嵌入模型: %s", hasEmbeddingModel ? "已配置" : "未配置");
        logInfo("  视觉模型: %s", hasVisionModel ? "已配置" : "未配置");
        logInfo("  代码模型: %s", hasCodeModel ? "已配置" : "未配置");
        logInfo("  数学模型: %s", hasMathModel ? "已配置" : "未配置");

        if (hasChatModel && hasEmbeddingModel) {
            logInfo("✓ 配置了完整的聊天和嵌入功能");
        } else if (hasChatModel) {
            logInfo("仅配置了聊天功能");
        } else if (hasEmbeddingModel) {
            logInfo("仅配置了嵌入功能");
        } else {
            logWarning("未识别到标准的聊天或嵌入模型");
        }
    }

    /**
     * 验证默认参数
     */
    private void validateDefaultParameters() {
        LlmProperties.DefaultParametersProperties defaults = llmProps.getDefaultParameters();

        // 验证默认模型
        String defaultModel = defaults.getModel();
        if (!isQwenCompatibleModel(defaultModel)) {
            logWarning("默认模型 '%s' 不是通义千问模型，聊天功能可能无法正常工作", defaultModel);
        }

        // 温度参数验证 (通义千问支持0-2)
        if (defaults.getTemperature() != null) {
            validateRange(defaults.getTemperature().intValue(), 0, 2, "温度参数");
            if (defaults.getTemperature() > 1.5) {
                logWarning("温度参数较高 (%.2f)，回答可能过于随机", defaults.getTemperature());
            }
        }

        // 最大令牌数验证
        if (defaults.getMaxTokens() != null) {
            validateRange(defaults.getMaxTokens(), 1, 100000, "最大令牌数");

            // 根据模型给出建议
            if (defaultModel != null) {
                int recommendedMaxTokens = getRecommendedMaxTokens(defaultModel);
                if (defaults.getMaxTokens() > recommendedMaxTokens) {
                    logWarning("最大令牌数 (%d) 超过模型 '%s' 的建议值 (%d)",
                            defaults.getMaxTokens(), defaultModel, recommendedMaxTokens);
                }
            }
        }

        // Top-p参数验证
        if (defaults.getTopP() != null) {
            validateRange(defaults.getTopP().intValue(), 0, 1, "Top-p参数");
        }
    }

    /**
     * 检查是否为通义千问兼容的模型
     */
    private boolean isQwenCompatibleModel(String model) {
        if (model == null) return false;
        return model.startsWith("qwen");
    }

    /**
     * 获取模型推荐的最大令牌数
     */
    private int getRecommendedMaxTokens(String model) {
        return switch (model) {
            case "qwen-max" -> 6000;
            case "qwen-max-longcontext" -> 28000;
            case "qwen-plus" -> 6000;
            case "qwen-turbo" -> 1500;
            default -> 2000;
        };
    }

    @Override
    protected Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("启用状态", qwenConfig.getEnabled());
        summary.put("服务地址", qwenConfig.getBaseUrl());
        summary.put("API密钥", qwenConfig.getApiKey());
        summary.put("超时时间", qwenConfig.getTimeout());
        summary.put("最大重试次数", qwenConfig.getMaxRetries());
        summary.put("支持的模型数量", qwenConfig.getModels().size());
        summary.put("支持的模型", qwenConfig.getModels());

        // 默认参数
        LlmProperties.DefaultParametersProperties defaults = llmProps.getDefaultParameters();
        summary.put("默认模型", defaults.getModel());
        summary.put("默认温度", defaults.getTemperature());
        summary.put("默认最大令牌数", defaults.getMaxTokens());

        // 特殊功能
        boolean hasVisionModel = qwenConfig.getModels().stream().anyMatch(model -> model.contains("vl"));
        boolean hasCodeModel = qwenConfig.getModels().stream().anyMatch(model -> model.contains("coder"));
        boolean hasMathModel = qwenConfig.getModels().stream().anyMatch(model -> model.contains("math"));

        summary.put("多模态支持", hasVisionModel ? "启用" : "禁用");
        summary.put("代码专用模型", hasCodeModel ? "启用" : "禁用");
        summary.put("数学专用模型", hasMathModel ? "启用" : "禁用");

        return summary;
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
        logBeanCreation("DashScopeApi", "阿里云DashScope API客户端");

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

            logInfo("DashScope API配置完成");
            logBeanSuccess("DashScopeApi");
            return api;

        } catch (Exception e) {
            String errorMsg = String.format("创建DashScopeApi失败: %s", e.getMessage());
            log.error("❌ [{}] {}", getModuleName(), errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
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
        logBeanCreation("DashScopeChatOptions", "通义千问聊天模型选项");

        LlmProperties.DefaultParametersProperties defaults = llmProps.getDefaultParameters();

        // 如果默认模型不是Qwen模型，使用Qwen的默认模型
        String defaultModel = defaults.getModel();
        if (!isQwenCompatibleModel(defaultModel)) {
            defaultModel = "qwen-plus";
            logWarning("配置的默认模型不兼容通义千问，使用默认模型: %s", defaultModel);
        }

        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withModel(defaultModel)
                .withTemperature(defaults.getTemperature())
                .withMaxToken(defaults.getMaxTokens())
                .withTopP(defaults.getTopP())
                .build();

        logInfo("聊天选项配置 - 模型: %s, 温度: %s, 最大令牌: %s",
                defaultModel, defaults.getTemperature(), defaults.getMaxTokens());

        logBeanSuccess("DashScopeChatOptions");
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
    @ConditionalOnMissingBean(name = "qwenEmbeddingOptions")
    public DashScopeEmbeddingOptions qwenEmbeddingOptions() {
        logBeanCreation("DashScopeEmbeddingOptions", "通义千问嵌入模型选项");

        // 确定嵌入模型
        String embeddingModel = kbProps.getEmbedding().getDefaultModel();
        Integer dimension = kbProps.getEmbedding().getDimension();

        // 如果配置的默认模型不是通义千问嵌入模型，使用通义千问的默认嵌入模型
        if (!isQwenEmbeddingModel(embeddingModel)) {
            embeddingModel = "text-embedding-v2";
            logWarning("配置的嵌入模型不兼容通义千问，使用默认模型: %s", embeddingModel);
        }

        DashScopeEmbeddingOptions options = DashScopeEmbeddingOptions.builder()
                .withModel(embeddingModel)
                .withDimensions(dimension)
                .build();

        logInfo("嵌入选项配置 - 模型: %s, 维度: %s", embeddingModel, dimension);
        logBeanSuccess("DashScopeEmbeddingOptions");

        return options;
    }

    /**
     * 检查是否为通义千问嵌入模型
     */
    private boolean isQwenEmbeddingModel(String model) {
        if (model == null) return false;
        return model.equals("text-embedding-v2") || model.equals("text-embedding-v3");
    }

    /**
     * 创建自定义重试模板
     *
     * <p>为DashScope API调用提供自定义的重试策略，包括指数退避
     * 和智能重试次数控制。</p>
     *
     * @return 配置好的重试模板
     */
    @Bean(name = "qwenRetryTemplate")
    @ConditionalOnProperty(prefix = "baseai.llm.qwen", name = "max-retries", matchIfMissing = false)
    public RetryTemplate qwenRetryTemplate() {
        logBeanCreation("QwenRetryTemplate", "通义千问重试策略模板");

        int maxRetries = qwenConfig.getMaxRetries();

        RetryTemplate retryTemplate = new RetryTemplate();

        // 配置重试策略
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxRetries);
        retryTemplate.setRetryPolicy(retryPolicy);

        // 配置退避策略 - 通义千问建议较长的重试间隔
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2000); // 初始间隔2秒
        backOffPolicy.setMaxInterval(30000);    // 最大间隔30秒
        backOffPolicy.setMultiplier(2.0);      // 每次重试间隔翻倍
        retryTemplate.setBackOffPolicy(backOffPolicy);

        logInfo("重试策略配置 - 最大重试: %d次, 初始间隔: 2秒, 最大间隔: 30秒", maxRetries);
        logBeanSuccess("QwenRetryTemplate");

        return retryTemplate;
    }

    /**
     * 使用默认重试模板（当没有配置自定义重试次数时）
     */
    @Bean(name = "qwenRetryTemplate")
    @ConditionalOnMissingBean(name = "qwenRetryTemplate")
    public RetryTemplate qwenDefaultRetryTemplate() {
        logBeanCreation("QwenDefaultRetryTemplate", "通义千问默认重试策略");

        // 创建一个基本的重试模板
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3); // 默认重试3次
        retryTemplate.setRetryPolicy(retryPolicy);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2000); // 通义千问建议较长的初始间隔
        backOffPolicy.setMaxInterval(30000);
        backOffPolicy.setMultiplier(2.0);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        logInfo("使用默认重试策略 - 最大重试: 3次");
        logBeanSuccess("QwenDefaultRetryTemplate");

        return retryTemplate;
    }

    /**
     * 创建自定义的Qwen聊天模型
     *
     * <p>这是主要的聊天模型Bean，集成了所有自定义配置，
     * 包括API客户端、默认选项和重试策略。</p>
     *
     * @param dashScopeApi  自定义的DashScope API
     * @param chatOptions   自定义的聊天选项
     * @param retryTemplate 重试模板
     * @return 配置好的聊天模型
     */
    @Bean(name = "qwenChatModel")
    @ConditionalOnProperty(prefix = "baseai.llm.qwen", name = "enabled", havingValue = "true")
    @ConditionalOnBean(DashScopeApi.class)
    public DashScopeChatModel qwenChatModel(
            DashScopeApi dashScopeApi,
            DashScopeChatOptions chatOptions,
            @Qualifier("qwenRetryTemplate") RetryTemplate retryTemplate) {

        logBeanCreation("DashScopeChatModel", "通义千问聊天模型主Bean");

        try {
            // 创建聊天模型
            DashScopeChatModel chatModel = DashScopeChatModel.builder()
                    .dashScopeApi(dashScopeApi)
                    .defaultOptions(chatOptions)
                    .retryTemplate(retryTemplate)
                    .build();

            logBeanSuccess("DashScopeChatModel");
            logSuccess("通义千问聊天服务已就绪，可以开始中文对话");

            return chatModel;

        } catch (Exception e) {
            String errorMsg = String.format("创建DashScopeChatModel失败: %s", e.getMessage());
            log.error("❌ [{}] {}", getModuleName(), errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    /**
     * 创建专用的Qwen嵌入模型
     *
     * <p>专门为QwenEmbeddingService创建的嵌入模型Bean，使用独立的配置和选项。</p>
     *
     * @param dashScopeApi     由Spring注入的DashScope API实例
     * @param embeddingOptions 由Spring注入的嵌入模型选项
     * @param retryTemplate    由Spring注入的重试模板
     * @return 配置好的嵌入模型
     */
    @Bean(name = "qwenEmbeddingModel")
    @ConditionalOnProperty(prefix = "baseai.llm.qwen", name = "enabled", havingValue = "true")
    @ConditionalOnBean(DashScopeApi.class)
    public DashScopeEmbeddingModel qwenEmbeddingModel(
            DashScopeApi dashScopeApi,
            DashScopeEmbeddingOptions embeddingOptions,
            @Qualifier("qwenRetryTemplate") RetryTemplate retryTemplate) {

        logBeanCreation("QwenEmbeddingModel", "通义千问嵌入模型专用Bean");

        try {
            DashScopeEmbeddingModel embeddingModel = new DashScopeEmbeddingModel(
                    dashScopeApi,
                    MetadataMode.ALL,
                    embeddingOptions,
                    retryTemplate
            );

            logInfo("嵌入模型配置完成 - 模型: %s, 维度: %s",
                    embeddingOptions.getModel(), embeddingOptions.getDimensions());

            logBeanSuccess("QwenEmbeddingModel");
            return embeddingModel;

        } catch (Exception e) {
            String errorMsg = String.format("创建QwenEmbeddingModel失败: %s", e.getMessage());
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
                .connectTimeout(qwenConfig.getTimeout())
                .readTimeout(qwenConfig.getTimeout())
                .build();
    }
}