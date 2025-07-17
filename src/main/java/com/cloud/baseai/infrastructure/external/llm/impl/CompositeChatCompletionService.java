package com.cloud.baseai.infrastructure.external.llm.impl;

import com.cloud.baseai.infrastructure.config.LLMServiceProperties;
import com.cloud.baseai.infrastructure.exception.ChatCompletionException;
import com.cloud.baseai.infrastructure.external.llm.ChatCompletionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>复合聊天完成服务</h2>
 *
 * <p>这是一个智能路由器，实现了多个AI提供商之间的负载均衡和故障转移。
 * 它就像一个经验丰富的项目经理，知道什么时候该找哪个专家来解决问题。</p>
 */
public class CompositeChatCompletionService implements ChatCompletionService {

    private final LLMServiceProperties properties;
    private final Map<String, ChatCompletionService> services;
    private final ChatCompletionService defaultService;

    public CompositeChatCompletionService(
            LLMServiceProperties properties,
            ChatCompletionService openaiService,
            ChatCompletionService claudeService,
            ChatCompletionService mockService) {

        this.properties = properties;
        this.services = new HashMap<>();
        this.services.put("openai", openaiService);
        if (claudeService != null) {
            this.services.put("claude", claudeService);
        }
        if (mockService != null) {
            this.services.put("mock", mockService);
        }

        // 根据配置确定默认服务
        String defaultProvider = properties.getDefaultProvider();
        this.defaultService = services.getOrDefault(defaultProvider, openaiService);
    }

    @Override
    public ChatCompletionResult generateCompletion(Map<String, Object> context) {
        String modelCode = (String) context.get("model");
        ChatCompletionService service = selectServiceForModel(modelCode);

        try {
            return service.generateCompletion(context);
        } catch (ChatCompletionException e) {
            // 实现故障转移逻辑
            if (properties.isFailoverEnabled() && service != defaultService) {
                return defaultService.generateCompletion(context);
            }
            throw e;
        }
    }

    @Override
    public void generateStreamResponse(Map<String, Object> context, java.util.function.Consumer<String> onChunk) {
        String modelCode = (String) context.get("model");
        ChatCompletionService service = selectServiceForModel(modelCode);
        service.generateStreamResponse(context, onChunk);
    }

    @Override
    public boolean isModelAvailable(String modelCode) {
        ChatCompletionService service = selectServiceForModel(modelCode);
        return service.isModelAvailable(modelCode);
    }

    @Override
    public boolean isHealthy() {
        return services.values().stream().anyMatch(ChatCompletionService::isHealthy);
    }

    @Override
    public List<String> getSupportedModels() {
        return services.values().stream()
                .flatMap(service -> service.getSupportedModels().stream())
                .distinct()
                .toList();
    }

    /**
     * 根据模型选择合适的服务提供商
     */
    private ChatCompletionService selectServiceForModel(String modelCode) {
        if (modelCode == null) {
            return defaultService;
        }

        // 根据模型前缀选择服务
        if (modelCode.startsWith("gpt-")) {
            return services.get("openai");
        } else if (modelCode.startsWith("claude-")) {
            return services.get("claude");
        } else if (modelCode.startsWith("mock-")) {
            return services.get("mock");
        }

        return defaultService;
    }
}