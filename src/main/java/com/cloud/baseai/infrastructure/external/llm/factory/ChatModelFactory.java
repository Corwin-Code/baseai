package com.cloud.baseai.infrastructure.external.llm.factory;

import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.cloud.baseai.infrastructure.exception.ChatException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.external.llm.model.ChatCompletionResult;
import com.cloud.baseai.infrastructure.external.llm.model.ModelRecommendation;
import com.cloud.baseai.infrastructure.external.llm.service.ChatCompletionService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * <h2>聊天模型工厂</h2>
 *
 * <p>统一管理多个大语言模型服务提供商的工厂类，同时实现ChatCompletionService接口，
 * 提供统一的LLM服务入口。集成了模型选择、故障转移、负载均衡等企业级功能。</p>
 *
 * <p><b>支持的提供商：</b></p>
 * <ul>
 * <li><b>OpenAI：</b>GPT-4系列、GPT-3.5系列，适合通用场景</li>
 * <li><b>通义千问：</b>qwen系列，中文优化，成本优势</li>
 * <li><b>Claude：</b>claude-3系列，强推理能力，长文本处理</li>
 * </ul>
 *
 * <p><b>核心职责：</b></p>
 * <ul>
 * <li><b>服务管理：</b>注册和管理OpenAI、Claude、通义千问等多个LLM服务</li>
 * <li><b>智能路由：</b>根据模型类型自动选择最适合的服务提供商</li>
 * <li><b>故障转移：</b>当主要服务不可用时自动切换到备用服务</li>
 * <li><b>负载均衡：</b>支持轮询、随机、权重等多种负载均衡策略</li>
 * <li><b>统一接口：</b>对外提供统一的ChatCompletionService接口</li>
 * </ul>
 */
@Component
public class ChatModelFactory implements ChatCompletionService {

    private static final Logger log = LoggerFactory.getLogger(ChatModelFactory.class);

    private final LlmProperties llmProperties;
    private final ApplicationContext applicationContext;

    /**
     * 服务提供商映射 (provider -> service)
     */
    private final Map<String, ChatCompletionService> providerServices = new ConcurrentHashMap<>();

    /**
     * 模型到提供商的映射 (model -> provider)
     */
    private final Map<String, String> modelToProvider = new ConcurrentHashMap<>();

    /**
     * 提供商权重配置（用于负载均衡）
     */
    private final Map<String, Integer> providerWeights = new ConcurrentHashMap<>();

    /**
     * 负载均衡器
     */
    private LoadBalancer loadBalancer;

    /**
     * 轮询计数器
     */
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    /**
     * 随机数生成器
     */
    private final Random random = new Random();

    @Autowired
    public ChatModelFactory(LlmProperties llmProperties, ApplicationContext applicationContext) {
        this.llmProperties = llmProperties;
        this.applicationContext = applicationContext;
    }

    /**
     * 初始化工厂，注册所有可用的LLM服务
     */
    @PostConstruct
    public void initialize() {
        log.info("初始化聊天模型工厂");

        int registeredServices = 0;

        // 注册OpenAI服务
        if (llmProperties.getOpenai().getEnabled()) {
            registeredServices += registerOpenAiService();
            providerWeights.put("openai", 30); // 稳定性好，生态丰富
        }

        // 注册Anthropic服务
        if (llmProperties.getAnthropic().getEnabled()) {
            registeredServices += registerAnthropicService();
            providerWeights.put("anthropic", 25); // 推理能力强，适合复杂任务
        }

        // 注册通义千问服务
        if (llmProperties.getQwen().getEnabled()) {
            registeredServices += registerQwenService();
            providerWeights.put("qwen", 45); // 中文优化，成本优势
        }

        // 初始化负载均衡器
        this.loadBalancer = createLoadBalancer(llmProperties.getLoadBalancing());

        // 验证至少有一个服务可用
        if (registeredServices == 0) {
            throw new ChatException(ErrorCode.EXT_LLM_001);
        }

        log.info("聊天模型工厂初始化完成: 注册服务数={}, 提供商={}, 总模型数={}, 负载均衡策略={}, 故障转移={}",
                registeredServices, providerServices.keySet(),
                modelToProvider.size(),
                llmProperties.getLoadBalancing(),
                llmProperties.getFailoverEnabled());

        log.debug("可用模型清单: {}", modelToProvider.keySet());
    }

    // =================== ChatCompletionService 接口实现 ===================

    @Override
    public ChatCompletionResult generateCompletion(Map<String, Object> context) {
        String model = (String) context.get("model");
        String provider = determineProvider(model);

        ChatCompletionService service = selectService(provider, context);

        try {
            log.debug("使用服务生成完成: provider={}, model={}", provider, model);
            return service.generateCompletion(context);

        } catch (Exception e) {
            log.warn("服务调用失败: provider={}, model={}, error={}", provider, model, e.getMessage());

            if (llmProperties.getFailoverEnabled()) {
                return handleFailover(context, provider, e);
            }
            throw e;
        }
    }

    @Override
    public void generateStreamResponse(Map<String, Object> context, Consumer<String> onChunk) {
        String model = (String) context.get("model");
        String provider = determineProvider(model);

        ChatCompletionService service = selectService(provider, context);

        try {
            log.debug("使用服务生成流式响应: provider={}, model={}", provider, model);
            service.generateStreamResponse(context, onChunk);

        } catch (Exception e) {
            log.warn("流式响应失败: provider={}, model={}, error={}", provider, model, e.getMessage());

            if (llmProperties.getFailoverEnabled()) {
                // 流式响应的故障转移比较复杂，这里记录错误并抛出异常
                log.error("流式响应故障转移暂不支持，请使用非流式接口");
                throw new ChatException(ErrorCode.EXT_LLM_003, e);
            }
            throw e;
        }
    }

    @Override
    public boolean isModelAvailable(String modelCode) {
        try {
            String provider = determineProvider(modelCode);
            ChatCompletionService service = providerServices.get(provider);
            return service != null && service.isModelAvailable(modelCode);
        } catch (Exception e) {
            log.debug("模型可用性检查失败: model={}, error={}", modelCode, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isHealthy() {
        // 至少有一个服务健康就认为整体健康
        boolean healthy = providerServices.values().stream().anyMatch(ChatCompletionService::isHealthy);
        log.debug("整体健康状态: {}, 服务详情: {}", healthy, getServiceHealthDetails());
        return healthy;
    }

    @Override
    public List<String> getSupportedModels() {
        return new ArrayList<>(modelToProvider.keySet());
    }

    // =================== 工厂特有的公共方法 ===================

    /**
     * 根据任务类型智能推荐模型
     *
     * <p>根据任务特点、文本语言、复杂度等因素推荐最适合的模型。</p>
     *
     * @param taskType   任务类型：chat, code, analysis, creative, translation等
     * @param language   文本语言：zh, en等
     * @param complexity 复杂度：simple, medium, complex
     * @return 推荐的模型和提供商
     */
    public ModelRecommendation recommendModel(String taskType, String language, String complexity) {
        log.debug("推荐模型: taskType={}, language={}, complexity={}", taskType, language, complexity);

        // 根据任务类型和特征选择最适合的提供商和模型
        if ("zh".equals(language)) {
            // 中文任务优先使用通义千问
            if (providerServices.containsKey("qwen") && providerServices.get("qwen").isHealthy()) {
                String model = selectBestQwenModel(taskType, complexity);
                return new ModelRecommendation("qwen", model, "中文任务优化");
            }
        }

        if ("code".equals(taskType)) {
            // 代码相关任务
            if (providerServices.containsKey("anthropic") && providerServices.get("anthropic").isHealthy()) {
                return new ModelRecommendation("anthropic", "claude-3-sonnet-20240229", "代码分析能力强");
            }
            if (providerServices.containsKey("openai") && providerServices.get("openai").isHealthy()) {
                return new ModelRecommendation("openai", "gpt-4", "代码生成质量高");
            }
        }

        if ("analysis".equals(taskType) || "complex".equals(complexity)) {
            // 复杂分析任务
            if (providerServices.containsKey("anthropic") && providerServices.get("anthropic").isHealthy()) {
                return new ModelRecommendation("anthropic", "claude-3-opus-20240229", "推理能力最强");
            }
            if (providerServices.containsKey("openai") && providerServices.get("openai").isHealthy()) {
                return new ModelRecommendation("openai", "gpt-4", "复杂任务处理能力强");
            }
        }

        // 默认推荐
        return getDefaultRecommendation();
    }

    /**
     * 根据模型代码获取对应的聊天服务
     *
     * @param modelCode 模型代码
     * @return 聊天完成服务
     * @throws ChatException 当模型不被支持时
     */
    public ChatCompletionService getServiceForModel(String modelCode) {
        String provider = modelToProvider.get(modelCode);
        if (provider == null) {
            throw new ChatException(ErrorCode.EXT_AI_005, modelCode);
        }

        ChatCompletionService service = providerServices.get(provider);
        if (service == null) {
            throw new ChatException(ErrorCode.EXT_LLM_004, provider);
        }

        return service;
    }

    /**
     * 获取默认的聊天服务
     *
     * @return 默认的聊天完成服务
     * @throws ChatException 当没有可用服务时
     */
    public ChatCompletionService getDefaultService() {
        String defaultProvider = llmProperties.getDefaultProvider();
        ChatCompletionService service = providerServices.get(defaultProvider);

        if (service == null || !service.isHealthy()) {
            // 如果默认提供商不可用，选择一个健康的服务
            service = selectHealthyService(defaultProvider);
            if (service != null) {
                log.warn("默认提供商 {} 不可用，使用备选服务", defaultProvider);
            }
        }

        if (service == null) {
            throw new ChatException(ErrorCode.EXT_LLM_001);
        }

        return service;
    }

    /**
     * 获取所有可用的服务
     *
     * @return 服务提供商映射表的副本
     */
    public Map<String, ChatCompletionService> getAllServices() {
        return new ConcurrentHashMap<>(providerServices);
    }

    /**
     * 执行带故障转移的聊天完成
     *
     * <p>按照优先级列表尝试不同的模型，直到成功为止。
     * 这对于关键业务场景特别有用。</p>
     *
     * @param context         聊天上下文
     * @param preferredModels 按优先级排序的模型列表
     * @return 聊天完成结果
     * @throws ChatException 当所有模型都失败时
     */
    public ChatCompletionResult executeWithFailover(
            Map<String, Object> context,
            List<String> preferredModels) {

        if (!llmProperties.getFailoverEnabled()) {
            // 如果未启用故障转移，直接使用首选模型
            String model = preferredModels.getFirst();
            ChatCompletionService service = getServiceForModel(model);
            context.put("model", model);
            return service.generateCompletion(context);
        }

        // 尝试每个模型，直到成功
        Exception lastException = null;
        for (String model : preferredModels) {
            try {
                if (!isModelAvailable(model)) {
                    log.debug("跳过不可用的模型: {}", model);
                    continue;
                }

                ChatCompletionService service = getServiceForModel(model);
                context.put("model", model);
                ChatCompletionResult result = service.generateCompletion(context);

                log.info("故障转移成功: 使用模型 {}", model);
                return result;

            } catch (Exception e) {
                log.warn("模型 {} 调用失败，尝试下一个: {}", model, e.getMessage());
                lastException = e;
            }
        }

        // 所有模型都失败
        throw new ChatException(ErrorCode.EXT_LLM_005, lastException, preferredModels);
    }

    /**
     * 使用负载均衡选择服务
     *
     * @return 负载均衡选择的服务
     */
    public ChatCompletionService selectServiceWithLoadBalancing() {
        Set<String> healthyProviders = getHealthyProviders();
        if (healthyProviders.isEmpty()) {
            throw new ChatException(ErrorCode.EXT_LLM_006);
        }

        String provider = loadBalancer.selectProvider(healthyProviders);
        return providerServices.get(provider);
    }

    /**
     * 获取服务健康状态详情
     *
     * @return 包含所有服务健康状态的映射
     */
    public Map<String, Object> getServiceStats() {
        Map<String, Object> stats = new HashMap<>();

        for (Map.Entry<String, ChatCompletionService> entry : providerServices.entrySet()) {
            String serviceName = entry.getKey();
            ChatCompletionService service = entry.getValue();

            Map<String, Object> serviceStats = new HashMap<>();
            serviceStats.put("healthy", service.isHealthy());
            serviceStats.put("supportedModels", service.getSupportedModels());
            serviceStats.put("enabled", isProviderEnabled(serviceName));
            serviceStats.put("weight", providerWeights.getOrDefault(serviceName, 0));

            stats.put(serviceName, serviceStats);
        }

        return stats;
    }

    // =================== 私有方法 ===================

    /**
     * 注册OpenAI服务
     */
    private int registerOpenAiService() {
        try {
            ChatCompletionService openAiService = applicationContext.getBean(
                    "openAIChatCompletionService", ChatCompletionService.class);
            registerProvider("openai", openAiService, llmProperties.getOpenai().getModels());
//            log.info("成功注册OpenAI服务: models={}", llmProperties.getOpenai().getModels());
            return 1;
        } catch (Exception e) {
            log.warn("OpenAI服务注册失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 注册Anthropic服务
     */
    private int registerAnthropicService() {
        try {
            ChatCompletionService anthropicService = applicationContext.getBean(
                    "anthropicChatCompletionService", ChatCompletionService.class);
            registerProvider("anthropic", anthropicService, llmProperties.getAnthropic().getModels());
//            log.info("成功注册Anthropic服务: models={}", llmProperties.getAnthropic().getModels());
            return 1;
        } catch (Exception e) {
            log.warn("Anthropic服务注册失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 注册通义千问服务
     */
    private int registerQwenService() {
        try {
            ChatCompletionService qwenService = applicationContext.getBean(
                    "qwenChatCompletionService", ChatCompletionService.class);
            registerProvider("qwen", qwenService, llmProperties.getQwen().getModels());
//            log.info("成功注册通义千问服务: models={}", llmProperties.getQwen().getModels());
            return 1;
        } catch (Exception e) {
            log.warn("通义千问服务注册失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 注册服务提供商
     */
    private void registerProvider(String providerName, ChatCompletionService service, List<String> models) {
        providerServices.put(providerName, service);

        // 注册该提供商支持的所有模型
        List<String> conflicts = new ArrayList<>();
        for (String model : models) {
            String old = modelToProvider.putIfAbsent(model, providerName);
            if (old != null && !old.equals(providerName)) {
                conflicts.add(model + "(" + old + "->" + providerName + ")");
                modelToProvider.put(model, providerName); // 明确覆盖
            }
        }

        // 生产更友好：INFO 打简洁摘要，DEBUG/TRACE 打细节
        log.info("成功注册{}服务: modelCount={}, provider={}",
                providerName, models.size(), providerName);
        log.debug("注册模型映射详情({}): {}", providerName, models);

        if (!conflicts.isEmpty()) {
            log.warn("模型名冲突并已处理: {}", conflicts);
        }
    }

    /**
     * 确定模型对应的服务提供商
     */
    private String determineProvider(String model) {
        if (model == null) {
            return llmProperties.getDefaultProvider();
        }

        // 先从映射表查找
        String provider = modelToProvider.get(model);
        if (provider != null) {
            return provider;
        }

        // 如果映射表中没有，根据模型名称推断
        if (model.startsWith("gpt") || model.contains("openai")) {
            return "openai";
        } else if (model.startsWith("claude")) {
            return "anthropic";
        } else if (model.startsWith("qwen") || model.contains("dashscope")) {
            return "qwen";
        }

        log.debug("未知模型 {}，使用默认提供商: {}", model, llmProperties.getDefaultProvider());
        return llmProperties.getDefaultProvider();
    }

    /**
     * 选择具体的服务实例
     */
    private ChatCompletionService selectService(String provider, Map<String, Object> context) {
        ChatCompletionService service = providerServices.get(provider);
        if (service == null) {
            throw new ChatException(ErrorCode.EXT_LLM_007, provider);
        }

        // 检查服务健康状态
        if (!service.isHealthy()) {
            if (llmProperties.getFailoverEnabled()) {
                log.warn("服务不健康，尝试故障转移: provider={}", provider);
                return selectHealthyService(provider);
            } else {
                throw new ChatException(ErrorCode.EXT_LLM_008, provider);
            }
        }

        return service;
    }

    /**
     * 选择健康的服务
     */
    private ChatCompletionService selectHealthyService(String excludeProvider) {
        List<ChatCompletionService> healthyServices = providerServices.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(excludeProvider))
                .filter(entry -> entry.getValue().isHealthy())
                .map(Map.Entry::getValue)
                .toList();

        if (healthyServices.isEmpty()) {
            log.error("没有健康的备用服务可用");
            return null;
        }

        // 使用负载均衡策略选择服务
        String strategy = llmProperties.getLoadBalancing();
        ChatCompletionService selected = switch (strategy) {
            case "random" -> healthyServices.get(random.nextInt(healthyServices.size()));
            case "round_robin" -> healthyServices.get(roundRobinCounter.getAndIncrement() % healthyServices.size());
            default -> healthyServices.getFirst(); // 默认选择第一个
        };

        log.info("故障转移成功: 选择服务 {}", getProviderName(selected));
        return selected;
    }

    /**
     * 处理故障转移
     */
    private ChatCompletionResult handleFailover(Map<String, Object> context, String failedProvider, Exception originalException) {
        log.warn("开始故障转移: failedProvider={}", failedProvider);

        try {
            ChatCompletionService fallbackService = selectHealthyService(failedProvider);
            if (fallbackService == null) {
                throw new ChatException(ErrorCode.EXT_LLM_009, originalException);
            }

            // 可能需要调整模型参数以适配新的服务
            adjustContextForProvider(context, getProviderName(fallbackService));

            return fallbackService.generateCompletion(context);

        } catch (Exception e) {
            log.error("故障转移失败", e);
            throw new ChatException(ErrorCode.EXT_LLM_010, originalException);
        }
    }

    /**
     * 根据服务提供商调整上下文
     */
    private void adjustContextForProvider(Map<String, Object> context, String provider) {
        String currentModel = (String) context.get("model");

        // 如果当前模型不被新提供商支持，选择一个默认模型
        if (!isModelSupportedByProvider(currentModel, provider)) {
            String fallbackModel = getDefaultModelForProvider(provider);
            context.put("model", fallbackModel);
            log.info("调整模型用于故障转移: {} -> {} (provider: {})", currentModel, fallbackModel, provider);
        }
    }

    /**
     * 检查模型是否被提供商支持
     */
    private boolean isModelSupportedByProvider(String model, String provider) {
        return modelToProvider.get(model) != null && modelToProvider.get(model).equals(provider);
    }

    /**
     * 获取提供商的默认模型
     */
    private String getDefaultModelForProvider(String provider) {
        return switch (provider) {
            case "openai" -> "gpt-3.5-turbo";
            case "anthropic" -> "claude-3-sonnet-20240229";
            case "qwen" -> "qwen-plus";
            default -> null;
        };
    }

    /**
     * 获取服务的提供商名称
     */
    private String getProviderName(ChatCompletionService service) {
        for (Map.Entry<String, ChatCompletionService> entry : providerServices.entrySet()) {
            if (entry.getValue().equals(service)) {
                return entry.getKey();
            }
        }
        return "unknown";
    }

    /**
     * 获取健康的提供商列表
     */
    private Set<String> getHealthyProviders() {
        return providerServices.entrySet().stream()
                .filter(entry -> entry.getValue().isHealthy())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 获取服务健康状态详情
     */
    private Map<String, Boolean> getServiceHealthDetails() {
        Map<String, Boolean> healthDetails = new HashMap<>();
        for (Map.Entry<String, ChatCompletionService> entry : providerServices.entrySet()) {
            healthDetails.put(entry.getKey(), entry.getValue().isHealthy());
        }
        return healthDetails;
    }

    /**
     * 检查提供商是否启用
     */
    private boolean isProviderEnabled(String provider) {
        return switch (provider) {
            case "openai" -> llmProperties.getOpenai().getEnabled();
            case "anthropic" -> llmProperties.getAnthropic().getEnabled();
            case "qwen" -> llmProperties.getQwen().getEnabled();
            default -> false;
        };
    }

    /**
     * 选择最佳的通义千问模型
     */
    private String selectBestQwenModel(String taskType, String complexity) {
        return switch (taskType) {
            case "code" -> "qwen-coder";
            case "math" -> "qwen-math";
            default -> switch (complexity) {
                case "simple" -> "qwen-turbo";
                case "complex" -> "qwen-max";
                default -> "qwen-plus";
            };
        };
    }

    /**
     * 获取默认推荐
     */
    private ModelRecommendation getDefaultRecommendation() {
        // 按优先级选择可用的服务
        if (providerServices.containsKey("qwen") && providerServices.get("qwen").isHealthy()) {
            return new ModelRecommendation("qwen", "qwen-plus", "默认推荐，性价比高");
        }
        if (providerServices.containsKey("openai") && providerServices.get("openai").isHealthy()) {
            return new ModelRecommendation("openai", "gpt-3.5-turbo", "默认推荐，稳定可靠");
        }
        if (providerServices.containsKey("anthropic") && providerServices.get("anthropic").isHealthy()) {
            return new ModelRecommendation("anthropic", "claude-3-sonnet-20240229", "默认推荐，推理能力强");
        }

        throw new ChatException(ErrorCode.EXT_LLM_006, "没有可用的聊天服务");
    }

    /**
     * 创建负载均衡器
     */
    private LoadBalancer createLoadBalancer(String strategy) {
        return switch (strategy.toLowerCase()) {
            case "round_robin" -> new RoundRobinLoadBalancer();
            case "random" -> new RandomLoadBalancer();
            case "weighted" -> new WeightedLoadBalancer(providerWeights);
            default -> {
                log.warn("未知的负载均衡策略: {}，使用轮询", strategy);
                yield new RoundRobinLoadBalancer();
            }
        };
    }

    // =================== 负载均衡器实现 ===================

    /**
     * 负载均衡器接口
     */
    private interface LoadBalancer {
        String selectProvider(Set<String> providers);
    }

    /**
     * 轮询负载均衡器
     */
    private class RoundRobinLoadBalancer implements LoadBalancer {
        @Override
        public String selectProvider(Set<String> providers) {
            if (providers.isEmpty()) {
                return null;
            }
            List<String> list = new ArrayList<>(providers);
            String selected = list.get(roundRobinCounter.getAndIncrement() % list.size());
            log.debug("轮询选择提供商: {}", selected);
            return selected;
        }
    }

    /**
     * 随机负载均衡器
     */
    private class RandomLoadBalancer implements LoadBalancer {
        @Override
        public String selectProvider(Set<String> providers) {
            if (providers.isEmpty()) {
                return null;
            }
            List<String> list = new ArrayList<>(providers);
            String selected = list.get(random.nextInt(list.size()));
            log.debug("随机选择提供商: {}", selected);
            return selected;
        }
    }

    /**
     * 加权负载均衡器
     */
    private class WeightedLoadBalancer implements LoadBalancer {
        private final Map<String, Integer> weights;

        WeightedLoadBalancer(Map<String, Integer> weights) {
            this.weights = weights;
        }

        @Override
        public String selectProvider(Set<String> providers) {
            if (providers.isEmpty()) {
                return null;
            }

            // 过滤出有权重的提供商
            Map<String, Integer> availableWeights = new HashMap<>();
            for (String provider : providers) {
                availableWeights.put(provider, weights.getOrDefault(provider, 1));
            }

            // 计算总权重
            int totalWeight = availableWeights.values().stream().mapToInt(Integer::intValue).sum();
            if (totalWeight == 0) {
                return providers.iterator().next(); // 如果所有权重都是0，返回第一个
            }

            // 随机选择
            int randomWeight = random.nextInt(totalWeight);
            int currentWeight = 0;

            for (Map.Entry<String, Integer> entry : availableWeights.entrySet()) {
                currentWeight += entry.getValue();
                if (randomWeight < currentWeight) {
                    String selected = entry.getKey();
                    log.debug("加权选择提供商: {} (权重: {})", selected, entry.getValue());
                    return selected;
                }
            }

            // 不应该到达这里，返回第一个作为兜底
            return providers.iterator().next();
        }
    }
}