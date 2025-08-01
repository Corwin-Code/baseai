package com.cloud.baseai.infrastructure.external.llm.service;

import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.cloud.baseai.infrastructure.exception.ChatException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.external.llm.model.ChatCompletionResult;
import com.cloud.baseai.infrastructure.external.llm.model.ModelPricing;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * <h2>Claude聊天完成服务</h2>
 *
 * <p>基于Spring AI 框架的Claude (Anthropic)服务实现。Claude是由Anthropic开发的
 * 先进AI模型，以其卓越的推理能力、安全性和遵循指令的能力而著称。</p>
 *
 * <p><b>Claude特色功能：</b></p>
 * <ul>
 * <li><b>超长上下文：</b>支持高达200K token的上下文窗口</li>
 * <li><b>思考模式：</b>展示推理过程，提供透明的决策路径</li>
 * <li><b>多模态支持：</b>处理文本、图像、PDF等多种格式</li>
 * <li><b>安全对齐：</b>内置安全机制，减少有害输出</li>
 * </ul>
 */
@Service
public class ClaudeChatCompletionService implements ChatCompletionService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeChatCompletionService.class);

    private final LlmProperties properties;
    private final AnthropicChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final Map<String, ModelPricing> modelPricingMap;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数，初始化Claude聊天服务
     *
     * @param properties   LLM配置属性
     * @param objectMapper JSON对象映射器
     */
    public ClaudeChatCompletionService(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.modelPricingMap = initializeModelPricing();

        // 初始化Anthropic API和聊天模型
        AnthropicApi anthropicApi = createAnthropicApi();
        this.chatModel = createChatModel(anthropicApi);
        this.streamingChatModel = this.chatModel; // AnthropicChatModel同时实现了StreamingChatModel

        log.info("Claude聊天服务初始化完成: baseUrl={}, models={}",
                properties.getClaude().getBaseUrl(),
                properties.getClaude().getModels());
    }

    @Override
    public ChatCompletionResult generateCompletion(Map<String, Object> context) {
        long startTime = System.currentTimeMillis();

        try {
            // 构建提示词和选项
            Prompt prompt = buildPrompt(context);

            log.debug("开始生成Claude聊天完成: model={}, messages={}",
                    context.get("model"), prompt.getInstructions().size());

            // 调用模型生成响应
            ChatResponse response = chatModel.call(prompt);

            // 处理响应结果
            return processResponse(response, startTime, context);

        } catch (Exception e) {
            log.error("Claude聊天完成生成失败", e);
            throw convertException(e);
        }
    }

    @Override
    public void generateStreamResponse(Map<String, Object> context, Consumer<String> onChunk) {
        try {
            // 构建流式提示词
            Prompt prompt = buildPrompt(context);

            log.debug("开始Claude流式生成: model={}", context.get("model"));

            // 异步处理流式响应
            CompletableFuture.runAsync(() -> {
                try {
                    Flux<ChatResponse> responseFlux = streamingChatModel.stream(prompt);

                    responseFlux
                            .mapNotNull(response -> response.getResult().getOutput().getText())
                            .filter(content -> content != null && !content.isEmpty())
                            .doOnNext(onChunk)
                            .doOnError(error -> {
                                log.error("流式响应处理错误", error);
                                throw new ChatException(ErrorCode.EXT_AI_018, error);
                            })
                            .doOnComplete(() -> log.debug("流式响应完成"))
                            .subscribe();

                } catch (Exception e) {
                    log.error("流式响应处理失败", e);
                    throw new ChatException(ErrorCode.EXT_AI_018, e);
                }
            });

        } catch (Exception e) {
            log.error("Claude流式生成异常", e);
            throw new ChatException(ErrorCode.EXT_AI_018, e);
        }
    }

    @Override
    public boolean isModelAvailable(String modelCode) {
        if (!properties.getClaude().getModels().contains(modelCode)) {
            return false;
        }

        try {
            // 创建测试提示词
            Prompt testPrompt = new Prompt(
                    new UserMessage("test"),
                    AnthropicChatOptions.builder()
                            .model(modelCode)
                            .maxTokens(1)
                            .build()
            );

            // 尝试调用模型
            chatModel.call(testPrompt);
            return true;

        } catch (Exception e) {
            log.warn("模型可用性检查失败: model={}, error={}", modelCode, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            // 使用最基础的Claude模型进行健康检查
            return isModelAvailable("claude-3-haiku-20240307");

        } catch (Exception e) {
            log.warn("Claude服务健康检查失败", e);
            return false;
        }
    }

    @Override
    public List<String> getSupportedModels() {
        return new ArrayList<>(properties.getClaude().getModels());
    }

    // =================== 私有辅助方法 ===================

    /**
     * 创建Anthropic API实例
     */
    private AnthropicApi createAnthropicApi() {
        LlmProperties.ClaudeProperties claudeConfig = properties.getClaude();

        // 构建自定义的RestClient
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(claudeConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + claudeConfig.getApiKey())
                .defaultHeader(HttpHeaders.USER_AGENT, "BaseAI-SpringAI/2.0");

        // 创建Anthropic API
        return AnthropicApi.builder()
                .baseUrl(claudeConfig.getBaseUrl())
                .apiKey(claudeConfig.getApiKey())
                .restClientBuilder(restClientBuilder)
                .build();
    }

    /**
     * 创建聊天模型
     */
    private AnthropicChatModel createChatModel(AnthropicApi anthropicApi) {
        // 创建重试模板
        RetryTemplate retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;

        // 设置默认选项
        AnthropicChatOptions defaultOptions = AnthropicChatOptions.builder()
                .model(properties.getDefaultParameters().getModel())
                .temperature(properties.getDefaultParameters().getTemperature())
                .maxTokens(properties.getDefaultParameters().getMaxTokens())
                .topP(properties.getDefaultParameters().getTopP())
                .build();

        return AnthropicChatModel.builder()
                .anthropicApi(anthropicApi)
                .defaultOptions(defaultOptions)
                .retryTemplate(retryTemplate)
                .build();
    }

    /**
     * 构建提示词
     */
    @SuppressWarnings("unchecked")
    private Prompt buildPrompt(Map<String, Object> context) {
        List<Message> messages = new ArrayList<>();

        // 添加知识上下文作为系统消息
        if (context.containsKey("knowledgeContext")) {
            List<String> knowledgeContext = (List<String>) context.get("knowledgeContext");
            if (!knowledgeContext.isEmpty()) {
                String contextContent = "请根据以下相关知识内容回答用户问题。如果知识内容与问题相关，" +
                        "请优先使用这些内容。如果无关，可以基于你的通用知识回答。\n\n" +
                        "相关知识内容:\n" + String.join("\n\n---\n\n", knowledgeContext);
                messages.add(new SystemMessage(contextContent));
            }
        }

        // 添加历史消息
        if (context.containsKey("messages")) {
            List<Map<String, Object>> historyMessages = (List<Map<String, Object>>) context.get("messages");
            for (Map<String, Object> msg : historyMessages) {
                String role = (String) msg.get("role");
                String content = (String) msg.get("content");

                messages.add(createMessage(role, content));
            }
        }

        // 添加当前用户消息
        if (context.containsKey("currentMessage")) {
            messages.add(new UserMessage((String) context.get("currentMessage")));
        }

        // 构建聊天选项
        AnthropicChatOptions options = buildChatOptions(context);

        return new Prompt(messages, options);
    }

    /**
     * 创建消息对象
     */
    private Message createMessage(String role, String content) {
        return switch (role.toLowerCase()) {
            case "system" -> new SystemMessage(content);
            case "user" -> new UserMessage(content);
            case "assistant" -> new AssistantMessage(content);
            default -> {
                log.warn("未知的消息角色: {}, 默认作为用户消息", role);
                yield new UserMessage(content);
            }
        };
    }

    /**
     * 构建聊天选项
     */
    @SuppressWarnings("unchecked")
    private AnthropicChatOptions buildChatOptions(Map<String, Object> context) {
        AnthropicChatOptions.Builder builder = AnthropicChatOptions.builder();

        // 设置模型
        String model = (String) context.get("model");
        if (model != null) {
            builder.model(model);

            // 应用模型特定参数
            LlmProperties.ModelParameters modelParams = properties.getModelSpecificParameters().get(model);
            if (modelParams != null) {
                if (modelParams.getTemperature() != null) {
                    builder.temperature(modelParams.getTemperature());
                }
                if (modelParams.getMaxTokens() != null) {
                    builder.maxTokens(modelParams.getMaxTokens());
                }
            }
        }

        // 设置其他参数
        if (context.containsKey("temperature")) {
            builder.temperature(((Number) context.get("temperature")).doubleValue());
        }
        if (context.containsKey("maxTokens")) {
            builder.maxTokens((Integer) context.get("maxTokens"));
        }
        if (context.containsKey("topP")) {
            builder.topP(((Number) context.get("topP")).doubleValue());
        }
        if (context.containsKey("topK")) {
            builder.topK((Integer) context.get("topK"));
        }

        // Claude特有功能：思考模式
        if (context.containsKey("enableThinking") && (Boolean) context.get("enableThinking")) {
            // Claude 3.7支持显式的思考模式
            if (model != null && (model.contains("3-7") || model.contains("claude-4"))) {
                builder.thinking(AnthropicApi.ThinkingType.ENABLED, 2048); // 思考token限制
                log.debug("启用Claude思考模式");
            }
        }

        // 处理工具调用
        if (context.containsKey("availableTools")) {
            List<String> toolNames = (List<String>) context.get("availableTools");
            if (!toolNames.isEmpty()) {
                // Claude使用不同的工具调用方式
                log.debug("Claude工具调用配置: {}", toolNames);
                // 注意：Claude的工具调用API可能与OpenAI不同
            }
        }

        return builder.build();
    }

    /**
     * 处理响应结果
     */
    private ChatCompletionResult processResponse(ChatResponse response, long startTime, Map<String, Object> context) {
        if (response == null) {
            throw new ChatException(ErrorCode.EXT_AI_015);
        }

        Generation generation = response.getResult();
        String content = generation.getOutput().getText();

        // 计算耗时
        int latencyMs = (int) (System.currentTimeMillis() - startTime);

        // 获取使用统计和计算费用
        Integer promptTokens = null;
        Integer completionTokens = null;
        Double cost = 0.0;

        ChatResponseMetadata metadata = response.getMetadata();
        Usage usage = metadata.getUsage();
        if (usage != null) {
            promptTokens = Math.toIntExact(usage.getPromptTokens());
            completionTokens = Math.toIntExact(usage.getCompletionTokens());

            // 计算费用
            String model = (String) context.get("model");
            cost = calculateCost(model, promptTokens, completionTokens);
        }

        // 检查是否有工具调用
        String toolCallJson = null;
        if (!generation.getOutput().getToolCalls().isEmpty()) {
            try {
                toolCallJson = objectMapper.writeValueAsString(generation.getOutput().getToolCalls());
            } catch (Exception e) {
                log.error("序列化工具调用失败", e);
            }
        }

        log.info("Claude响应完成: model={}, tokens={}/{}, latency={}ms, cost=${}",
                context.get("model"), promptTokens, completionTokens, latencyMs, cost);

        if (toolCallJson != null) {
            return ChatCompletionResult.withToolCall(content, toolCallJson, promptTokens, completionTokens, latencyMs, cost);
        } else {
            return ChatCompletionResult.success(content, promptTokens, completionTokens, latencyMs, cost);
        }
    }

    /**
     * 转换异常
     */
    private ChatException convertException(Exception e) {
        if (e instanceof ChatException) {
            return (ChatException) e;
        }

        // 根据不同的异常类型返回相应的错误码
        if (e.getMessage() != null) {
            if (e.getMessage().contains("401") || e.getMessage().contains("Unauthorized")) {
                return new ChatException(ErrorCode.EXT_AI_001, e);
            } else if (e.getMessage().contains("429") || e.getMessage().contains("Rate limit")) {
                return new ChatException(ErrorCode.EXT_AI_007, e);
            } else if (e.getMessage().contains("timeout") || e.getMessage().contains("Timeout")) {
                return new ChatException(ErrorCode.EXT_AI_003, e);
            }
        }

        return new ChatException(ErrorCode.EXT_AI_017, e);
    }

    /**
     * 计算调用费用
     */
    private double calculateCost(String model, Integer promptTokens, Integer completionTokens) {
        if (model == null || promptTokens == null || completionTokens == null) {
            return 0.0;
        }

        ModelPricing pricing = modelPricingMap.get(model);
        if (pricing == null) {
            log.warn("未找到模型定价信息: {}", model);
            return 0.0;
        }

        double inputCost = (promptTokens / 1000000.0) * pricing.getInputPricePerMTokens();
        double outputCost = (completionTokens / 1000000.0) * pricing.getOutputPricePerMTokens();

        return inputCost + outputCost;
    }

    /**
     * 初始化模型定价信息
     *
     * <p>Claude的定价以每百万token计算</p>
     */
    private Map<String, ModelPricing> initializeModelPricing() {
        Map<String, ModelPricing> pricing = new ConcurrentHashMap<>();

        // Claude 3系列定价 (每百万token的价格，单位：美元)
        pricing.put("claude-3-opus-20240229", new ModelPricing(15.0, 75.0));
        pricing.put("claude-3-7-sonnet-latest", new ModelPricing(3.0, 15.0));
        pricing.put("claude-3-sonnet-20240229", new ModelPricing(3.0, 15.0));
        pricing.put("claude-3-haiku-20240307", new ModelPricing(0.25, 1.25));

        // Claude 4系列定价（假设值，需要根据实际定价更新）
        pricing.put("claude-opus-4", new ModelPricing(20.0, 100.0));
        pricing.put("claude-4-sonnet", new ModelPricing(5.0, 25.0));

        return pricing;
    }
}