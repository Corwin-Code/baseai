package com.cloud.baseai.infrastructure.external.llm.factory;

import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.cloud.baseai.infrastructure.exception.ChatException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.external.llm.model.EmbeddingRecommendation;
import com.cloud.baseai.infrastructure.external.llm.model.EmbeddingResult;
import com.cloud.baseai.infrastructure.external.llm.model.ModelInfo;
import com.cloud.baseai.infrastructure.external.llm.service.EmbeddingService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
     * 服务提供商映射
     */
    private final Map<String, EmbeddingService> providerServices = new ConcurrentHashMap<>();

    /**
     * 模型到提供商的映射
     */
    private final Map<String, String> modelToProvider = new ConcurrentHashMap<>();

    /**
     * 提供商优先级配置（数值越小优先级越高）
     */
    private final Map<String, Integer> providerPriority = new ConcurrentHashMap<>();


    public EmbeddingModelFactory(LlmProperties llmProperties,
                                 ApplicationContext applicationContext) {
        this.llmProperties = llmProperties;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void initialize() {
        log.info("初始化嵌入服务工厂");

        int registeredServices = 0;

        // 注册OpenAI嵌入服务
        if (llmProperties.getOpenai().getEnabled()) {
            registeredServices += registerOpenAiEmbeddingService();
            providerPriority.put("openai", 2); // 中等优先级
        }

        // 注册通义千问嵌入服务
        if (llmProperties.getQwen().getEnabled()) {
            registeredServices += registerQwenEmbeddingService();
            providerPriority.put("qwen", 1); // 高优先级（中文优化）
        }

        // 注册Anthropic嵌入服务（虽然当前不支持，但保持架构完整性）
        if (llmProperties.getAnthropic().getEnabled()) {
            registeredServices += registerAnthropicEmbeddingService();
            providerPriority.put("anthropic", 99); // 最低优先级（当前不支持）
        }

        if (registeredServices == 0) {
            throw new ChatException(ErrorCode.EXT_LLM_001);
        }

        log.info("嵌入服务工厂初始化完成: 注册服务数={}, 提供商={}, 可用模型={}",
                registeredServices, providerServices.keySet(), modelToProvider.keySet());
    }

    @Override
    public float[] generateEmbedding(String text, String modelCode) {
        EmbeddingService service = selectServiceForModel(modelCode);
        return service.generateEmbedding(text, modelCode);
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts, String modelCode) {
        EmbeddingService service = selectServiceForModel(modelCode);
        return service.generateEmbeddings(texts, modelCode);
    }

    @Override
    public EmbeddingResult generateEmbeddingWithDetails(String text, String modelCode) {
        EmbeddingService service = selectServiceForModel(modelCode);
        return service.generateEmbeddingWithDetails(text, modelCode);
    }

    @Override
    public List<EmbeddingResult> generateEmbeddingsWithDetails(List<String> texts, String modelCode) {
        EmbeddingService service = selectServiceForModel(modelCode);
        return service.generateEmbeddingsWithDetails(texts, modelCode);
    }

    @Override
    public boolean isModelAvailable(String modelCode) {
        String provider = determineProvider(modelCode);
        EmbeddingService service = providerServices.get(provider);
        return service != null && service.isModelAvailable(modelCode);
    }

    @Override
    public int getVectorDimension(String modelCode) {
        String provider = determineProvider(modelCode);
        EmbeddingService service = providerServices.get(provider);
        return service != null ? service.getVectorDimension(modelCode) : 1536;
    }

    @Override
    public ModelInfo getModelInfo(String modelCode) {
        String provider = determineProvider(modelCode);
        EmbeddingService service = providerServices.get(provider);
        return service != null ? service.getModelInfo(modelCode) : null;
    }

    @Override
    public List<String> getSupportedModels() {
        return new ArrayList<>(modelToProvider.keySet());
    }

    @Override
    public boolean isHealthy() {
        return providerServices.values().stream().anyMatch(EmbeddingService::isHealthy);
    }

    @Override
    public boolean warmupModel(String modelCode) {
        try {
            EmbeddingService service = selectServiceForModel(modelCode);
            return service.warmupModel(modelCode);
        } catch (Exception e) {
            log.warn("模型预热失败: {}", modelCode, e);
            return false;
        }
    }

    @Override
    public int estimateTokenCount(String text, String modelCode) {
        String provider = determineProvider(modelCode);
        EmbeddingService service = providerServices.get(provider);
        return service != null ? service.estimateTokenCount(text, modelCode) : 0;
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
        if (language == null && text != null) {
            language = detectLanguage(text);
        }

        // 根据语言特征选择最适合的服务
        List<String> preferredProviders = getPreferredProviders(language);

        for (String provider : preferredProviders) {
            EmbeddingService service = providerServices.get(provider);
            if (service != null && service.isHealthy()) {
                List<String> models = getProviderModels(provider);
                if (!models.isEmpty()) {
                    String recommendedModel = selectBestModel(models, text);
                    return new EmbeddingRecommendation(provider, service, recommendedModel,
                            "基于" + language + "语言特征推荐");
                }
            }
        }

        throw new ChatException(ErrorCode.EXT_LLM_006, "没有可用的嵌入服务");
    }

    /**
     * 获取指定提供商的服务
     */
    public EmbeddingService getServiceForProvider(String provider) {
        return providerServices.get(provider);
    }

    /**
     * 获取所有服务统计信息
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

            // 添加特殊状态信息
            if ("anthropic".equals(provider)) {
                serviceStats.put("embeddingSupported", false);
                serviceStats.put("note", "Anthropic目前不支持嵌入功能");
            }

            stats.put(provider, serviceStats);
        }

        return stats;
    }

    /**
     * 执行带自动降级的嵌入生成
     *
     * <p>尝试使用首选服务，失败时自动降级到其他可用服务。</p>
     *
     * @param text              要嵌入的文本
     * @param preferredProvider 首选提供商
     * @param fallbackEnabled   是否启用自动降级
     * @return 嵌入结果
     */
    public EmbeddingResult generateEmbeddingWithFallback(String text, String preferredProvider, boolean fallbackEnabled) {
        // 首先尝试首选提供商
        try {
            EmbeddingService preferredService = providerServices.get(preferredProvider);
            if (preferredService != null && preferredService.isHealthy()) {
                List<String> models = getProviderModels(preferredProvider);
                if (!models.isEmpty()) {
                    String model = models.getFirst(); // 使用第一个可用模型
                    return preferredService.generateEmbeddingWithDetails(text, model);
                }
            }
        } catch (Exception e) {
            log.warn("首选嵌入服务调用失败: provider={}, error={}", preferredProvider, e.getMessage());
        }

        // 如果启用降级，尝试其他服务
        if (fallbackEnabled) {
            List<String> fallbackProviders = getFallbackProviders(preferredProvider);
            for (String provider : fallbackProviders) {
                try {
                    EmbeddingService service = providerServices.get(provider);
                    if (service != null && service.isHealthy()) {
                        List<String> models = getProviderModels(provider);
                        if (!models.isEmpty()) {
                            String model = models.getFirst();
                            log.info("使用降级嵌入服务: {} -> {}", preferredProvider, provider);
                            return service.generateEmbeddingWithDetails(text, model);
                        }
                    }
                } catch (Exception e) {
                    log.warn("降级嵌入服务调用失败: provider={}, error={}", provider, e.getMessage());
                }
            }
        }

        throw new ChatException(ErrorCode.EXT_LLM_010, "所有嵌入服务都不可用");
    }

    // =================== 私有方法 ===================

    private int registerOpenAiEmbeddingService() {
        try {
            EmbeddingService openAiService = applicationContext.getBean(
                    "openAIEmbeddingService", EmbeddingService.class);
            registerProvider("openai", openAiService, getOpenAiEmbeddingModels());
            log.info("成功注册OpenAI嵌入服务");
            return 1;
        } catch (Exception e) {
            log.warn("OpenAI嵌入服务注册失败: {}", e.getMessage());
            return 0;
        }
    }

    private int registerQwenEmbeddingService() {
        try {
            EmbeddingService qwenService = applicationContext.getBean(
                    "qwenEmbeddingService", EmbeddingService.class);
            registerProvider("qwen", qwenService, getQwenEmbeddingModels());
            log.info("成功注册通义千问嵌入服务");
            return 1;
        } catch (Exception e) {
            log.warn("通义千问嵌入服务注册失败: {}", e.getMessage());
            return 0;
        }
    }

    private int registerAnthropicEmbeddingService() {
        try {
            EmbeddingService anthropicService = applicationContext.getBean(
                    "anthropicEmbeddingService", EmbeddingService.class);
            registerProvider("anthropic", anthropicService, getAnthropicEmbeddingModels());
            log.info("成功注册Anthropic嵌入服务 (当前不支持嵌入功能)");
            return 1;
        } catch (Exception e) {
            log.warn("Anthropic嵌入服务注册失败: {}", e.getMessage());
            return 0;
        }
    }

    private void registerProvider(String provider, EmbeddingService service, List<String> models) {
        providerServices.put(provider, service);

        for (String model : models) {
            modelToProvider.put(model, provider);
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
            // 中文优先使用通义千问
            providers.add("qwen");
            providers.add("openai");
        } else {
            // 英文优先使用OpenAI
            providers.add("openai");
            providers.add("qwen");
        }

        // Claude嵌入当前不可用，不添加到推荐列表
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

        // 简单选择策略：优先选择性价比高的模型
        for (String model : models) {
            if (model.contains("3-small") || model.contains("v2")) {
                return model; // 优先选择小模型
            }
        }

        return models.getFirst(); // 默认选择第一个
    }

    private List<String> getFallbackProviders(String excludeProvider) {
        return providerPriority.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(excludeProvider))
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
    }
}