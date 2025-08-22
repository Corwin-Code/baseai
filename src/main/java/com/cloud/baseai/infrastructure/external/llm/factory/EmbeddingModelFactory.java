package com.cloud.baseai.infrastructure.external.llm.factory;

import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.cloud.baseai.infrastructure.exception.ChatException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.external.llm.model.EmbeddingRecommendation;
import com.cloud.baseai.infrastructure.external.llm.model.EmbeddingResult;
import com.cloud.baseai.infrastructure.external.llm.model.ModelInfo;
import com.cloud.baseai.infrastructure.external.llm.model.ServiceRegistrationResult;
import com.cloud.baseai.infrastructure.external.llm.service.EmbeddingService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.cloud.baseai.infrastructure.utils.KbUtils.detectLanguage;

/**
 * <h2>嵌入模型工厂</h2>
 *
 * <p>统一管理多个嵌入服务提供商的工厂类，提供智能路由、故障转移和负载均衡功能。
 * 与ChatModelFactory设计理念一致，既是工厂又是统一的服务入口。</p>
 *
 * <p><b>支持的提供商：</b></p>
 * <ul>
 * <li><b>OpenAI：</b>text-embedding-3-small, text-embedding-3-large, text-embedding-ada-002</li>
 * <li><b>通义千问：</b>text-embedding-v2, text-embedding-v3</li>
 * <li><b>Claude：</b>当前不支持嵌入，保留架构完整性</li>
 * </ul>
 *
 * <p><b>智能选择策略：</b></p>
 * <ul>
 * <li><b>自动降级：</b>当指定服务不可用时，自动选择可用的替代服务</li>
 * <li><b>性能优先：</b>优先选择响应速度快的服务</li>
 * <li><b>成本考虑：</b>在满足需求的前提下选择性价比高的服务</li>
 * <li><b>语言优化：</b>根据文本语言特点选择最适合的模型</li>
 * </ul>
 */
@Component
public class EmbeddingModelFactory implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingModelFactory.class);

    private final LlmProperties llmProperties;
    private final ApplicationContext applicationContext;

    /**
     * 服务提供商映射 (provider -> service)
     */
    private final Map<String, EmbeddingService> providerServices = new ConcurrentHashMap<>();

    /**
     * 模型到提供商的映射 (model -> provider)
     */
    private final Map<String, String> modelToProvider = new ConcurrentHashMap<>();

    /**
     * 提供商优先级配置（数值越小优先级越高）
     */
    private final Map<String, Integer> providerPriority = new ConcurrentHashMap<>();

    /**
     * 初始化状态标志
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * 可选依赖的服务（Spring会自动注入存在的Bean）
     */
    @Autowired(required = false)
    @Qualifier("openAIEmbeddingService")
    private EmbeddingService openAIEmbeddingService;

    @Autowired(required = false)
    @Qualifier("qwenEmbeddingService")
    private EmbeddingService qwenEmbeddingService;

    @Autowired(required = false)
    @Qualifier("anthropicEmbeddingService")
    private EmbeddingService anthropicEmbeddingService;

    public EmbeddingModelFactory(LlmProperties llmProperties,
                                 ApplicationContext applicationContext) {
        this.llmProperties = llmProperties;
        this.applicationContext = applicationContext;
    }

    /**
     * 初始化嵌入服务工厂
     *
     * <p>采用多种策略确保即使某些服务不可用也能正常启动</p>
     */
    @PostConstruct
    public void initialize() {
        log.info("开始初始化嵌入服务工厂，安全加载模式");

        int totalServices = 0;
        int successfulServices = 0;
        Map<String, String> registrationResults = new HashMap<>();

        // 注册OpenAI嵌入服务
        ServiceRegistrationResult openaiResult = registerOpenAiEmbeddingService();
        totalServices++;
        if (openaiResult.success()) {
            successfulServices++;
            providerPriority.put("openai", 2); // 中等优先级，通用性强
        }
        registrationResults.put("OpenAI Embedding", openaiResult.status());

        // 注册通义千问嵌入服务
        ServiceRegistrationResult qwenResult = registerQwenEmbeddingService();
        totalServices++;
        if (qwenResult.success()) {
            successfulServices++;
            providerPriority.put("qwen", 1); // 高优先级（中文优化，成本优势）
        }
        registrationResults.put("Qwen Embedding", qwenResult.status());

        // 注册Anthropic嵌入服务（虽然当前不支持，但保持架构完整性）
        ServiceRegistrationResult anthropicResult = registerAnthropicEmbeddingService();
        totalServices++;
        if (anthropicResult.success()) {
            successfulServices++;
            providerPriority.put("anthropic", 99); // 最低优先级（当前不支持嵌入）
        }
        registrationResults.put("Anthropic Embedding", anthropicResult.status());

        // 验证至少有一个嵌入服务可用
        if (successfulServices == 0) {
            log.error("嵌入服务工厂初始化失败：没有可用的嵌入服务");
            logRegistrationSummary(registrationResults);
            throw new ChatException(ErrorCode.EXT_LLM_001);
        }

        initialized.set(true);

        // 记录初始化结果
        log.info("嵌入服务工厂初始化完成: 成功加载={}/{}, 总模型数={}, 可用提供商={}",
                successfulServices, totalServices, modelToProvider.size(), providerServices.keySet());

        logRegistrationSummary(registrationResults);
        log.debug("可用嵌入模型清单: {}", modelToProvider.keySet());
    }

    @Override
    public float[] generateEmbedding(String text, String modelCode) {
        EmbeddingService service = selectServiceForModel(modelCode);

        try {
            log.debug("使用嵌入服务生成向量: provider={}, model={}, textLength={}",
                    getProviderName(service), modelCode, text.length());
            return service.generateEmbedding(text, modelCode);

        } catch (Exception e) {
            log.warn("嵌入生成失败: model={}, error={}", modelCode, e.getMessage());

            if (llmProperties.getFailoverEnabled()) {
                return handleEmbeddingFailover(text, modelCode, service, e);
            }
            throw e;
        }
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts, String modelCode) {
        EmbeddingService service = selectServiceForModel(modelCode);

        try {
            log.debug("使用嵌入服务批量生成向量: provider={}, model={}, batchSize={}",
                    getProviderName(service), modelCode, texts.size());
            return service.generateEmbeddings(texts, modelCode);

        } catch (Exception e) {
            log.warn("批量嵌入生成失败: model={}, batchSize={}, error={}",
                    modelCode, texts.size(), e.getMessage());

            if (llmProperties.getFailoverEnabled()) {
                return handleBatchEmbeddingFailover(texts, modelCode, service, e);
            }
            throw e;
        }
    }

    @Override
    public EmbeddingResult generateEmbeddingWithDetails(String text, String modelCode) {
        EmbeddingService service = selectServiceForModel(modelCode);
        try {
            log.debug("生成详细嵌入结果: provider={}, model={}",
                    getProviderName(service), modelCode);
            return service.generateEmbeddingWithDetails(text, modelCode);

        } catch (Exception e) {
            log.warn("详细嵌入生成失败: model={}, error={}", modelCode, e.getMessage());

            if (llmProperties.getFailoverEnabled()) {
                return handleDetailedEmbeddingFailover(text, modelCode, service, e);
            }
            throw e;
        }
    }

    @Override
    public List<EmbeddingResult> generateEmbeddingsWithDetails(List<String> texts, String modelCode) {
        EmbeddingService service = selectServiceForModel(modelCode);

        try {
            log.debug("批量生成详细嵌入结果: provider={}, model={}, batchSize={}",
                    getProviderName(service), modelCode, texts.size());
            return service.generateEmbeddingsWithDetails(texts, modelCode);

        } catch (Exception e) {
            log.warn("批量详细嵌入生成失败: model={}, batchSize={}, error={}",
                    modelCode, texts.size(), e.getMessage());

            if (llmProperties.getFailoverEnabled()) {
                return handleBatchDetailedEmbeddingFailover(texts, modelCode, service, e);
            }
            throw e;
        }
    }

    @Override
    public boolean isModelAvailable(String modelCode) {
        try {
            String provider = determineProvider(modelCode);
            EmbeddingService service = providerServices.get(provider);
            return service != null && service.isModelAvailable(modelCode);
        } catch (Exception e) {
            log.debug("嵌入模型可用性检查失败: model={}, error={}", modelCode, e.getMessage());
            return false;
        }
    }

    @Override
    public int getVectorDimension(String modelCode) {
        try {
            String provider = determineProvider(modelCode);
            EmbeddingService service = providerServices.get(provider);
            return service != null ? service.getVectorDimension(modelCode) : getDefaultDimension(modelCode);
        } catch (Exception e) {
            log.debug("获取向量维度失败: model={}, error={}", modelCode, e.getMessage());
            return getDefaultDimension(modelCode);
        }
    }

    @Override
    public ModelInfo getModelInfo(String modelCode) {
        try {
            String provider = determineProvider(modelCode);
            EmbeddingService service = providerServices.get(provider);
            return service != null ? service.getModelInfo(modelCode) : null;
        } catch (Exception e) {
            log.debug("获取模型信息失败: model={}, error={}", modelCode, e.getMessage());
            return null;
        }
    }

    @Override
    public List<String> getSupportedModels() {
        return new ArrayList<>(modelToProvider.keySet());
    }

    @Override
    public boolean isHealthy() {
        if (!initialized.get()) {
            return false;
        }

        boolean healthy = providerServices.values().stream().anyMatch(EmbeddingService::isHealthy);
        log.debug("嵌入服务整体健康状态: {}, 服务详情: {}", healthy, getServiceHealthDetails());
        return healthy;
    }

    @Override
    public boolean warmupModel(String modelCode) {
        try {
            EmbeddingService service = selectServiceForModel(modelCode);
            boolean result = service.warmupModel(modelCode);
            log.debug("模型预热结果: model={}, success={}", modelCode, result);
            return result;
        } catch (Exception e) {
            log.warn("模型预热失败: model={}, error={}", modelCode, e.getMessage());
            return false;
        }
    }

    /**
     * 批量预热指定的嵌入模型
     *
     * <p>对多个模型进行预热，提高后续调用的响应速度。</p>
     *
     * @param modelCodes 要预热的模型列表
     * @return 预热结果映射（modelCode -> success）
     */
    public Map<String, Boolean> batchWarmupModels(List<String> modelCodes) {
        Map<String, Boolean> results = new HashMap<>();

        log.info("开始批量预热嵌入模型: modelCount={}", modelCodes.size());

        for (String modelCode : modelCodes) {
            try {
                boolean result = warmupModel(modelCode);
                results.put(modelCode, result);
                log.debug("模型预热结果: model={}, success={}", modelCode, result);
            } catch (Exception e) {
                results.put(modelCode, false);
                log.warn("模型预热异常: model={}, error={}", modelCode, e.getMessage());
            }
        }

        long successCount = results.values().stream().mapToLong(b -> b ? 1 : 0).sum();
        log.info("批量预热完成: total={}, success={}, failed={}",
                modelCodes.size(), successCount, modelCodes.size() - successCount);

        return results;
    }

    @Override
    public int estimateTokenCount(String text, String modelCode) {
        try {
            String provider = determineProvider(modelCode);
            EmbeddingService service = providerServices.get(provider);
            return service != null ? service.estimateTokenCount(text, modelCode) :
                    estimateTokenCountFallback(text);
        } catch (Exception e) {
            log.debug("Token估算失败: model={}, error={}", modelCode, e.getMessage());
            return estimateTokenCountFallback(text);
        }
    }

    // =================== 工厂特有方法 ===================

    /**
     * 智能选择嵌入服务
     *
     * <p>根据文本特征、模型可用性和性能指标智能选择最适合的嵌入服务。</p>
     *
     * @param text     要处理的文本
     * @param language 文本语言（可选）
     * @return 推荐的嵌入服务和模型
     */
    public EmbeddingRecommendation recommendEmbeddingService(String text, String language) {
        log.debug("开始智能推荐嵌入服务: textLength={}, language={}",
                text != null ? text.length() : 0, language);

        if (language == null && text != null) {
            language = detectLanguage(text);
            log.debug("自动检测语言: {}", language);
        }

        // 根据语言特征选择最适合的服务
        List<String> preferredProviders = getPreferredProviders(language);

        for (String provider : preferredProviders) {
            EmbeddingService service = providerServices.get(provider);
            if (service != null && service.isHealthy()) {
                List<String> models = getProviderModels(provider);
                if (!models.isEmpty()) {
                    String recommendedModel = selectBestModel(models, text);
                    String reason = buildRecommendationReason(provider, language, recommendedModel);

                    log.debug("推荐嵌入服务: provider={}, model={}, reason={}",
                            provider, recommendedModel, reason);

                    return new EmbeddingRecommendation(provider, service, recommendedModel, reason);
                }
            }
        }

        throw new ChatException(ErrorCode.EXT_LLM_012);
    }

    /**
     * 获取指定提供商的嵌入服务
     *
     * @param provider 提供商名称
     * @return 嵌入服务实例，不存在时返回null
     */
    public EmbeddingService getServiceForProvider(String provider) {
        return providerServices.get(provider);
    }

    /**
     * 获取所有嵌入服务的统计信息
     *
     * <p>包含健康状态、支持的模型、优先级等详细信息。</p>
     *
     * @return 包含所有服务统计信息的映射
     */
    public Map<String, Object> getAllServiceStats() {
        Map<String, Object> stats = new HashMap<>();

        for (Map.Entry<String, EmbeddingService> entry : providerServices.entrySet()) {
            String provider = entry.getKey();
            EmbeddingService service = entry.getValue();

            Map<String, Object> serviceStats = new HashMap<>();
            serviceStats.put("healthy", service.isHealthy());
            serviceStats.put("supportedModels", service.getSupportedModels());
            serviceStats.put("enabled", isProviderEnabled(provider));
            serviceStats.put("priority", providerPriority.getOrDefault(provider, 999));
            serviceStats.put("modelCount", getProviderModels(provider).size());

            // 添加特殊状态信息
            if ("anthropic".equals(provider)) {
                serviceStats.put("embeddingSupported", false);
                serviceStats.put("note", "Anthropic目前不支持嵌入功能，保留架构完整性");
            } else {
                serviceStats.put("embeddingSupported", true);
            }

            stats.put(provider, serviceStats);
        }

        stats.put("totalProviders", providerServices.size());
        stats.put("totalModels", modelToProvider.size());
        stats.put("initialized", initialized.get());

        return stats;
    }

    /**
     * 执行带自动降级的嵌入生成
     *
     * <p>优先使用指定的提供商，失败时根据优先级自动降级到其他可用服务。</p>
     *
     * @param text              要嵌入的文本
     * @param preferredProvider 首选提供商
     * @param fallbackEnabled   是否启用自动降级
     * @return 嵌入结果
     * @throws ChatException 当所有服务都不可用时
     */
    public EmbeddingResult generateEmbeddingWithFallback(String text, String preferredProvider,
                                                         boolean fallbackEnabled) {
        log.debug("开始执行嵌入生成，支持降级: preferredProvider={}, fallbackEnabled={}",
                preferredProvider, fallbackEnabled);

        // 首先尝试首选提供商
        try {
            EmbeddingService preferredService = providerServices.get(preferredProvider);
            if (preferredService != null && preferredService.isHealthy()) {
                List<String> models = getProviderModels(preferredProvider);
                if (!models.isEmpty()) {
                    String model = selectBestModel(models, text);
                    log.debug("使用首选嵌入服务: provider={}, model={}", preferredProvider, model);
                    return preferredService.generateEmbeddingWithDetails(text, model);
                }
            }
        } catch (Exception e) {
            log.warn("首选嵌入服务调用失败: provider={}, error={}", preferredProvider, e.getMessage());
        }

        // 如果启用降级，尝试其他服务
        if (fallbackEnabled) {
            List<String> fallbackProviders = getFallbackProviders(preferredProvider);
            log.debug("开始尝试降级服务: fallbackProviders={}", fallbackProviders);

            for (String provider : fallbackProviders) {
                try {
                    EmbeddingService service = providerServices.get(provider);
                    if (service != null && service.isHealthy()) {
                        List<String> models = getProviderModels(provider);
                        if (!models.isEmpty()) {
                            String model = selectBestModel(models, text);
                            log.info("嵌入服务降级成功: {} -> {}, model={}",
                                    preferredProvider, provider, model);
                            return service.generateEmbeddingWithDetails(text, model);
                        }
                    }
                } catch (Exception e) {
                    log.warn("降级嵌入服务调用失败: provider={}, error={}", provider, e.getMessage());
                }
            }
        }

        throw new ChatException(ErrorCode.EXT_LLM_010);
    }

    // =================== 私有方法 ===================

    /**
     * 注册OpenAI嵌入服务
     *
     * @return 注册结果
     */
    private ServiceRegistrationResult registerOpenAiEmbeddingService() {
        if (!llmProperties.getOpenai().getEnabled()) {
            return ServiceRegistrationResult.disabled("OpenAI配置已禁用");
        }

        try {
            EmbeddingService service = null;

            // 策略1: 使用自动注入的服务
            if (openAIEmbeddingService != null) {
                service = openAIEmbeddingService;
                log.debug("使用自动注入的OpenAI嵌入服务");
            } else {
                // 策略2: 手动查找Bean（fallback方式）
                service = findServiceBeanSafely("openAIEmbeddingService", "OpenAI嵌入");
            }

            if (service != null) {
                List<String> models = getOpenAiEmbeddingModels();
                registerProvider("openai", service, models);
                return ServiceRegistrationResult.success("OpenAI嵌入服务注册成功，模型数: " + models.size());
            } else {
                return ServiceRegistrationResult.failed("OpenAI嵌入服务Bean不存在或创建失败");
            }

        } catch (Exception e) {
            log.warn("OpenAI嵌入服务注册失败: {}", e.getMessage(), e);
            return ServiceRegistrationResult.failed("OpenAI嵌入服务注册异常: " + e.getMessage());
        }
    }

    /**
     * 注册通义千问嵌入服务
     *
     * @return 注册结果
     */
    private ServiceRegistrationResult registerQwenEmbeddingService() {
        if (!llmProperties.getQwen().getEnabled()) {
            return ServiceRegistrationResult.disabled("通义千问配置已禁用");
        }

        try {
            EmbeddingService service = null;

            if (qwenEmbeddingService != null) {
                service = qwenEmbeddingService;
                log.debug("使用自动注入的通义千问嵌入服务");
            } else {
                service = findServiceBeanSafely("qwenEmbeddingService", "通义千问嵌入");
            }

            if (service != null) {
                List<String> models = getQwenEmbeddingModels();
                registerProvider("qwen", service, models);
                return ServiceRegistrationResult.success("通义千问嵌入服务注册成功，模型数: " + models.size());
            } else {
                return ServiceRegistrationResult.failed("通义千问嵌入服务Bean不存在或创建失败");
            }

        } catch (Exception e) {
            log.warn("通义千问嵌入服务注册失败: {}", e.getMessage(), e);
            return ServiceRegistrationResult.failed("通义千问嵌入服务注册异常: " + e.getMessage());
        }
    }

    /**
     * 注册Anthropic嵌入服务
     *
     * @return 注册结果
     */
    private ServiceRegistrationResult registerAnthropicEmbeddingService() {
        if (!llmProperties.getAnthropic().getEnabled()) {
            return ServiceRegistrationResult.disabled("Anthropic配置已禁用");
        }

        try {
            EmbeddingService service = null;

            if (anthropicEmbeddingService != null) {
                service = anthropicEmbeddingService;
                log.debug("使用自动注入的Anthropic嵌入服务");
            } else {
                service = findServiceBeanSafely("anthropicEmbeddingService", "Anthropic嵌入");
            }

            if (service != null) {
                List<String> models = getAnthropicEmbeddingModels();
                registerProvider("anthropic", service, models);
                return ServiceRegistrationResult.success("Anthropic嵌入服务注册成功（当前不支持嵌入功能）");
            } else {
                return ServiceRegistrationResult.disabled("Anthropic当前不支持嵌入功能");
            }

        } catch (Exception e) {
            log.debug("Anthropic嵌入服务注册跳过: {}", e.getMessage());
            return ServiceRegistrationResult.disabled("Anthropic当前不支持嵌入功能");
        }
    }

    /**
     * 安全地查找服务Bean
     *
     * @param beanName    Bean名称
     * @param serviceName 服务显示名称（用于日志）
     * @return 找到的服务实例，如果不存在则返回null
     */
    private EmbeddingService findServiceBeanSafely(String beanName, String serviceName) {
        try {
            if (!applicationContext.containsBean(beanName)) {
                log.debug("{}服务Bean不存在: {}", serviceName, beanName);
                return null;
            }

            EmbeddingService service = applicationContext.getBean(beanName, EmbeddingService.class);
            log.debug("成功通过手动查找获取{}服务", serviceName);
            return service;

        } catch (NoSuchBeanDefinitionException e) {
            log.debug("{}服务Bean定义不存在: {}", serviceName, e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("获取{}服务Bean时发生异常: {}", serviceName, e.getMessage());
            return null;
        }
    }

    private void registerProvider(String provider, EmbeddingService service, List<String> models) {
        providerServices.put(provider, service);

        List<String> conflicts = new ArrayList<>();
        for (String model : models) {
            String old = modelToProvider.putIfAbsent(model, provider);
            if (old != null && !old.equals(provider)) {
                conflicts.add(model + "(" + old + "->" + provider + ")");
                modelToProvider.put(model, provider); // 明确覆盖
            }
        }

        log.debug("注册{}嵌入服务: 模型数={}, 冲突处理={}", provider, models.size(),
                conflicts.isEmpty() ? "无" : conflicts.size() + "个");

        if (!conflicts.isEmpty()) {
            log.warn("嵌入模型名冲突并已处理: {}", conflicts);
        }
    }

    private List<String> getOpenAiEmbeddingModels() {
        return llmProperties.getOpenai().getModels().stream()
                .filter(model -> model.startsWith("text-embedding"))
                .toList();
    }

    private List<String> getQwenEmbeddingModels() {
        return llmProperties.getQwen().getModels().stream()
                .filter(model -> model.startsWith("text-embedding"))
                .toList();
    }

    private List<String> getAnthropicEmbeddingModels() {
        // Anthropic目前不支持嵌入，返回空列表
        return Collections.emptyList();
    }

    private String determineProvider(String modelCode) {
        if (modelCode == null) {
            return getDefaultProvider();
        }

        String provider = modelToProvider.get(modelCode);
        if (provider != null) {
            return provider;
        }

        // 根据模型名称推断
        if (modelCode.contains("ada") || modelCode.startsWith("text-embedding-3")) {
            return "openai";
        } else if (modelCode.startsWith("text-embedding-v")) {
            return "qwen";
        } else if (modelCode.contains("claude") || modelCode.contains("anthropic")) {
            return "anthropic";
        }

        return getDefaultProvider();
    }

    private String getDefaultProvider() {
        // 选择优先级最高且可用的提供商
        return providerPriority.entrySet().stream()
                .filter(entry -> {
                    EmbeddingService service = providerServices.get(entry.getKey());
                    return service != null && service.isHealthy();
                })
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("openai"); // 默认回退到openai
    }

    private EmbeddingService selectServiceForModel(String modelCode) {
        String provider = determineProvider(modelCode);
        EmbeddingService service = providerServices.get(provider);

        if (service == null) {
            throw new ChatException(ErrorCode.EXT_LLM_007, provider);
        }

        if (!service.isHealthy()) {
            if (llmProperties.getFailoverEnabled()) {
                service = selectHealthyService(provider);
                if (service == null) {
                    throw new ChatException(ErrorCode.EXT_LLM_006);
                }
            } else {
                throw new ChatException(ErrorCode.EXT_LLM_010, provider);
            }
        }

        return service;
    }

    private EmbeddingService selectHealthyService(String excludeProvider) {
        return providerServices.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(excludeProvider))
                .filter(entry -> entry.getValue().isHealthy())
                .min((e1, e2) -> {
                    int priority1 = providerPriority.getOrDefault(e1.getKey(), 999);
                    int priority2 = providerPriority.getOrDefault(e2.getKey(), 999);
                    return Integer.compare(priority1, priority2);
                })
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    private boolean isProviderEnabled(String provider) {
        return switch (provider) {
            case "openai" -> llmProperties.getOpenai().getEnabled();
            case "qwen" -> llmProperties.getQwen().getEnabled();
            case "anthropic" -> llmProperties.getAnthropic().getEnabled();
            default -> false;
        };
    }

    private List<String> getPreferredProviders(String language) {
        List<String> providers = new ArrayList<>();

        if ("zh".equals(language)) {
            // 中文优先使用通义千问（中文优化，成本优势）
            providers.add("qwen");
            providers.add("openai");
        } else {
            // 英文和其他语言优先使用OpenAI（通用性强，质量稳定）
            providers.add("openai");
            providers.add("qwen");
        }

        // Anthropic嵌入当前不可用，不添加到推荐列表
        return providers;
    }

    private List<String> getProviderModels(String provider) {
        EmbeddingService service = providerServices.get(provider);
        return service != null ? service.getSupportedModels() : Collections.emptyList();
    }

    private String selectBestModel(List<String> models, String text) {
        if (models.isEmpty()) {
            return null;
        }

        // 智能模型选择策略
        for (String model : models) {
            // 优先选择性价比高的小模型
            if (model.contains("3-small") || model.contains("v2")) {
                return model;
            }
        }

        // 如果文本较长，选择大模型
        if (text != null && text.length() > 8000) {
            for (String model : models) {
                if (model.contains("3-large") || model.contains("v3")) {
                    return model;
                }
            }
        }

        return models.getFirst(); // 默认选择第一个
    }

    private String buildRecommendationReason(String provider, String language, String model) {
        StringBuilder reason = new StringBuilder();

        if ("zh".equals(language)) {
            if ("qwen".equals(provider)) {
                reason.append("中文优化，成本优势");
            } else {
                reason.append("中文兼容");
            }
        } else {
            if ("openai".equals(provider)) {
                reason.append("英文优化，质量稳定");
            } else {
                reason.append("多语言支持");
            }
        }

        if (model.contains("small") || model.contains("v2")) {
            reason.append("，性价比优选");
        } else if (model.contains("large") || model.contains("v3")) {
            reason.append("，高质量模型");
        }

        return reason.toString();
    }

    private List<String> getFallbackProviders(String excludeProvider) {
        return providerPriority.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(excludeProvider))
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
    }

    private String getProviderName(EmbeddingService service) {
        for (Map.Entry<String, EmbeddingService> entry : providerServices.entrySet()) {
            if (entry.getValue().equals(service)) {
                return entry.getKey();
            }
        }
        return "unknown";
    }

    private Map<String, Boolean> getServiceHealthDetails() {
        Map<String, Boolean> healthDetails = new HashMap<>();
        for (Map.Entry<String, EmbeddingService> entry : providerServices.entrySet()) {
            healthDetails.put(entry.getKey(), entry.getValue().isHealthy());
        }
        return healthDetails;
    }

    private void logRegistrationSummary(Map<String, String> results) {
        log.info("=== 嵌入服务注册摘要 ===");
        for (Map.Entry<String, String> entry : results.entrySet()) {
            log.info("  {}: {}", entry.getKey(), entry.getValue());
        }
        log.info("=== 可用嵌入服务: {} ===", providerServices.keySet());
    }

    private int getDefaultDimension(String modelCode) {
        // 根据模型返回默认维度
        if (modelCode.contains("3-small")) return 1536;
        if (modelCode.contains("3-large")) return 3072;
        if (modelCode.contains("ada-002")) return 1536;
        if (modelCode.contains("v2")) return 1536;
        if (modelCode.contains("v3")) return 2048;
        return 1536; // 默认维度
    }

    private int estimateTokenCountFallback(String text) {
        // 简单的Token估算（英文约4字符=1token，中文约2字符=1token）
        if (text == null) return 0;

        long chineseCount = text.chars().filter(c -> c >= 0x4E00 && c <= 0x9FFF).count();
        long otherCount = text.length() - chineseCount;

        return (int) (chineseCount / 2 + otherCount / 4);
    }

    // =================== 故障转移处理方法 ===================

    private float[] handleEmbeddingFailover(String text, String modelCode,
                                            EmbeddingService failedService, Exception originalException) {
        log.warn("开始嵌入生成故障转移: failedService={}", getProviderName(failedService));

        try {
            EmbeddingService fallbackService = selectHealthyService(getProviderName(failedService));
            if (fallbackService == null) {
                throw new ChatException(ErrorCode.EXT_LLM_009, originalException);
            }

            String fallbackModel = adjustModelForProvider(modelCode, getProviderName(fallbackService));
            return fallbackService.generateEmbedding(text, fallbackModel);

        } catch (Exception e) {
            log.error("嵌入生成故障转移失败", e);
            throw new ChatException(ErrorCode.EXT_LLM_010, originalException);
        }
    }

    private List<float[]> handleBatchEmbeddingFailover(List<String> texts, String modelCode,
                                                       EmbeddingService failedService, Exception originalException) {
        log.warn("开始批量嵌入生成故障转移: failedService={}, batchSize={}",
                getProviderName(failedService), texts.size());

        try {
            EmbeddingService fallbackService = selectHealthyService(getProviderName(failedService));
            if (fallbackService == null) {
                throw new ChatException(ErrorCode.EXT_LLM_009, originalException);
            }

            String fallbackModel = adjustModelForProvider(modelCode, getProviderName(fallbackService));
            return fallbackService.generateEmbeddings(texts, fallbackModel);

        } catch (Exception e) {
            log.error("批量嵌入生成故障转移失败", e);
            throw new ChatException(ErrorCode.EXT_LLM_010, originalException);
        }
    }

    private EmbeddingResult handleDetailedEmbeddingFailover(String text, String modelCode,
                                                            EmbeddingService failedService, Exception originalException) {
        log.warn("开始详细嵌入生成故障转移: failedService={}", getProviderName(failedService));

        try {
            EmbeddingService fallbackService = selectHealthyService(getProviderName(failedService));
            if (fallbackService == null) {
                throw new ChatException(ErrorCode.EXT_LLM_009, originalException);
            }

            String fallbackModel = adjustModelForProvider(modelCode, getProviderName(fallbackService));
            return fallbackService.generateEmbeddingWithDetails(text, fallbackModel);

        } catch (Exception e) {
            log.error("详细嵌入生成故障转移失败", e);
            throw new ChatException(ErrorCode.EXT_LLM_010, originalException);
        }
    }

    private List<EmbeddingResult> handleBatchDetailedEmbeddingFailover(List<String> texts, String modelCode,
                                                                       EmbeddingService failedService, Exception originalException) {
        log.warn("开始批量详细嵌入生成故障转移: failedService={}, batchSize={}",
                getProviderName(failedService), texts.size());

        try {
            EmbeddingService fallbackService = selectHealthyService(getProviderName(failedService));
            if (fallbackService == null) {
                throw new ChatException(ErrorCode.EXT_LLM_009, originalException);
            }

            String fallbackModel = adjustModelForProvider(modelCode, getProviderName(fallbackService));
            return fallbackService.generateEmbeddingsWithDetails(texts, fallbackModel);

        } catch (Exception e) {
            log.error("批量详细嵌入生成故障转移失败", e);
            throw new ChatException(ErrorCode.EXT_LLM_010, originalException);
        }
    }

    private String adjustModelForProvider(String originalModel, String targetProvider) {
        // 根据目标提供商调整模型
        List<String> availableModels = getProviderModels(targetProvider);
        if (availableModels.contains(originalModel)) {
            return originalModel; // 如果目标提供商支持原模型，直接使用
        }

        // 否则选择最适合的替代模型
        if (!availableModels.isEmpty()) {
            String adjustedModel = selectBestModel(availableModels, null);
            log.info("调整嵌入模型用于故障转移: {} -> {} (provider: {})",
                    originalModel, adjustedModel, targetProvider);
            return adjustedModel;
        }

        return originalModel; // 如果没有可用模型，返回原模型（可能会失败）
    }
}