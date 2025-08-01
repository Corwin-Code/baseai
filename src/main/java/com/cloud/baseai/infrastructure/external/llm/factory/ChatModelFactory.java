package com.cloud.baseai.infrastructure.external.llm.factory;

import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.cloud.baseai.infrastructure.exception.ChatException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.external.llm.model.ChatCompletionResult;
import com.cloud.baseai.infrastructure.external.llm.service.ChatCompletionService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h2>聊天模型工厂</h2>
 *
 * <p>统一管理多个大语言模型服务提供商，提供模型选择、故障转移、
 * 负载均衡等功能。</p>
 *
 * <p><b>支持的功能：</b></p>
 * <ul>
 * <li><b>多模型管理：</b>同时支持OpenAI、Claude、通义千问等多个模型</li>
 * <li><b>动态选择：</b>根据模型代码动态选择对应的服务</li>
 * <li><b>故障转移：</b>当主模型不可用时自动切换到备用模型</li>
 * <li><b>负载均衡：</b>支持轮询、随机、加权等负载均衡策略</li>
 * </ul>
 */
@Component
public class ChatModelFactory {

    private static final Logger log = LoggerFactory.getLogger(ChatModelFactory.class);

    private final LlmProperties llmProperties;
    private final ApplicationContext applicationContext;

    /**
     * 服务提供商映射
     */
    private final Map<String, ChatCompletionService> providerServices = new ConcurrentHashMap<>();

    /**
     * 模型到提供商的映射
     */
    private final Map<String, String> modelToProvider = new ConcurrentHashMap<>();

    /**
     * 负载均衡器
     */
    private LoadBalancer loadBalancer;

    @Autowired
    public ChatModelFactory(LlmProperties llmProperties, ApplicationContext applicationContext) {
        this.llmProperties = llmProperties;
        this.applicationContext = applicationContext;
    }

    /**
     * 初始化工厂
     */
    @PostConstruct
    public void initialize() {
        log.info("初始化聊天模型工厂");

        // 注册OpenAI服务
        if (llmProperties.getOpenai().getEnabled()) {
            try {
                ChatCompletionService openAiService = applicationContext.getBean(
                        "openAIChatCompletionService", ChatCompletionService.class);
                registerProvider("openai", openAiService, llmProperties.getOpenai().getModels());
                log.info("注册OpenAI服务: models={}", llmProperties.getOpenai().getModels());
            } catch (Exception e) {
                log.warn("OpenAI服务注册失败: {}", e.getMessage());
            }
        }

        // 注册Claude服务
        if (llmProperties.getClaude().getEnabled()) {
            try {
                ChatCompletionService claudeService = applicationContext.getBean(
                        "claudeChatCompletionService", ChatCompletionService.class);
                registerProvider("claude", claudeService, llmProperties.getClaude().getModels());
                log.info("注册Claude服务: models={}", llmProperties.getClaude().getModels());
            } catch (Exception e) {
                log.warn("Claude服务注册失败: {}", e.getMessage());
            }
        }

        // 注册通义千问服务
        if (llmProperties.getQwen().getEnabled()) {
            try {
                ChatCompletionService qwenService = applicationContext.getBean(
                        "qwenChatCompletionService", ChatCompletionService.class);
                registerProvider("qwen", qwenService, llmProperties.getQwen().getModels());
                log.info("注册通义千问服务: models={}", llmProperties.getQwen().getModels());
            } catch (Exception e) {
                log.warn("通义千问服务注册失败: {}", e.getMessage());
            }
        }

        // 初始化负载均衡器
        this.loadBalancer = createLoadBalancer(llmProperties.getLoadBalancing());

        log.info("聊天模型工厂初始化完成: providers={}, models={}",
                providerServices.keySet(), modelToProvider.keySet());
    }

    /**
     * 注册服务提供商
     */
    private void registerProvider(String providerName, ChatCompletionService service, java.util.List<String> models) {
        providerServices.put(providerName, service);

        // 注册该提供商支持的所有模型
        for (String model : models) {
            modelToProvider.put(model, providerName);
        }
    }

    /**
     * 根据模型代码获取对应的聊天服务
     *
     * @param modelCode 模型代码
     * @return 聊天完成服务
     */
    public ChatCompletionService getServiceForModel(String modelCode) {
        String provider = modelToProvider.get(modelCode);
        if (provider == null) {
            throw new ChatException(ErrorCode.EXT_AI_005, modelCode);
        }

        ChatCompletionService service = providerServices.get(provider);
        if (service == null) {
            throw new ChatException(ErrorCode.EXT_AI_002, provider);
        }

        return service;
    }

    /**
     * 获取默认的聊天服务
     */
    public ChatCompletionService getDefaultService() {
        String defaultProvider = llmProperties.getDefaultProvider();
        ChatCompletionService service = providerServices.get(defaultProvider);

        if (service == null) {
            // 如果默认提供商不可用，返回第一个可用的服务
            if (!providerServices.isEmpty()) {
                service = providerServices.values().iterator().next();
                log.warn("默认提供商 {} 不可用，使用备选服务", defaultProvider);
            } else {
                throw new ChatException(ErrorCode.EXT_AI_024);
            }
        }

        return service;
    }

    /**
     * 获取所有可用的服务
     */
    public Map<String, ChatCompletionService> getAllServices() {
        return new ConcurrentHashMap<>(providerServices);
    }

    /**
     * 检查模型是否可用
     */
    public boolean isModelAvailable(String modelCode) {
        try {
            ChatCompletionService service = getServiceForModel(modelCode);
            return service.isModelAvailable(modelCode);
        } catch (Exception e) {
            log.debug("模型不可用: {}", modelCode);
            return false;
        }
    }

    /**
     * 获取所有支持的模型列表
     */
    public List<String> getAllSupportedModels() {
        return new ArrayList<>(modelToProvider.keySet());
    }

    /**
     * 执行带故障转移的聊天完成
     */
    public ChatCompletionResult executeWithFailover(
            Map<String, Object> context,
            java.util.List<String> preferredModels) {

        if (!llmProperties.getFailoverEnabled()) {
            // 如果未启用故障转移，直接使用首选模型
            String model = preferredModels.getFirst();
            ChatCompletionService service = getServiceForModel(model);
            return service.generateCompletion(context);
        }

        // 尝试每个模型，直到成功
        Exception lastException = null;
        for (String model : preferredModels) {
            try {
                ChatCompletionService service = getServiceForModel(model);
                context.put("model", model);
                return service.generateCompletion(context);
            } catch (Exception e) {
                log.warn("模型 {} 调用失败，尝试下一个", model, e);
                lastException = e;
            }
        }

        // 所有模型都失败
        throw new ChatException(ErrorCode.EXT_AI_025, preferredModels, lastException);
    }

    /**
     * 使用负载均衡选择服务
     */
    public ChatCompletionService selectServiceWithLoadBalancing() {
        String provider = loadBalancer.selectProvider(providerServices.keySet());
        return providerServices.get(provider);
    }

    /**
     * 创建负载均衡器
     */
    private LoadBalancer createLoadBalancer(String strategy) {
        return switch (strategy.toLowerCase()) {
            case "round_robin" -> new RoundRobinLoadBalancer();
            case "random" -> new RandomLoadBalancer();
            case "weighted" -> new WeightedLoadBalancer(getProviderWeights());
            default -> {
                log.warn("未知的负载均衡策略: {}，使用轮询", strategy);
                yield new RoundRobinLoadBalancer();
            }
        };
    }

    /**
     * 获取提供商权重配置
     */
    private Map<String, Integer> getProviderWeights() {
        // 这里可以从配置中读取权重，暂时使用默认值
        Map<String, Integer> weights = new ConcurrentHashMap<>();
        weights.put("openai", 50);
        weights.put("claude", 30);
        weights.put("qwen", 20);
        return weights;
    }

    /**
     * 负载均衡器接口
     */
    private interface LoadBalancer {
        String selectProvider(java.util.Set<String> providers);
    }

    /**
     * 轮询负载均衡器
     */
    private static class RoundRobinLoadBalancer implements LoadBalancer {
        private int counter = 0;

        @Override
        public synchronized String selectProvider(java.util.Set<String> providers) {
            if (providers.isEmpty()) {
                return null;
            }
            java.util.List<String> list = new java.util.ArrayList<>(providers);
            String selected = list.get(counter % list.size());
            counter++;
            return selected;
        }
    }

    /**
     * 随机负载均衡器
     */
    private static class RandomLoadBalancer implements LoadBalancer {
        private final java.util.Random random = new java.util.Random();

        @Override
        public String selectProvider(java.util.Set<String> providers) {
            if (providers.isEmpty()) {
                return null;
            }
            java.util.List<String> list = new java.util.ArrayList<>(providers);
            return list.get(random.nextInt(list.size()));
        }
    }

    /**
     * 加权负载均衡器
     */
    private static class WeightedLoadBalancer implements LoadBalancer {
        private final Map<String, Integer> weights;
        private final java.util.Random random = new java.util.Random();

        WeightedLoadBalancer(Map<String, Integer> weights) {
            this.weights = weights;
        }

        @Override
        public String selectProvider(java.util.Set<String> providers) {
            if (providers.isEmpty()) {
                return null;
            }

            // 计算总权重
            int totalWeight = providers.stream()
                    .mapToInt(p -> weights.getOrDefault(p, 1))
                    .sum();

            // 随机选择
            int randomWeight = random.nextInt(totalWeight);
            int currentWeight = 0;

            for (String provider : providers) {
                currentWeight += weights.getOrDefault(provider, 1);
                if (randomWeight < currentWeight) {
                    return provider;
                }
            }

            // 不应该到达这里
            return providers.iterator().next();
        }
    }
}