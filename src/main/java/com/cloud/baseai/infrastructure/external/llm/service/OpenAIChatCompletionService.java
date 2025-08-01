package com.cloud.baseai.infrastructure.external.llm.service;

import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.cloud.baseai.infrastructure.exception.ChatException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.external.llm.model.ChatCompletionResult;
import com.cloud.baseai.infrastructure.external.llm.model.ModelPricing;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * <h2>OpenAI聊天完成服务</h2>
 *
 * <p>基于Spring AI的OpenAI服务实现。这个版本利用了Spring AI提供的
 * 强大抽象能力，简化了与OpenAI API的集成，同时保持了原有的业务功能。</p>
 */
@Service
public class OpenAIChatCompletionService implements ChatCompletionService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIChatCompletionService.class);

    private final LlmProperties properties;
    private final OpenAiChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final Map<String, ModelPricing> modelPricingMap;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数，初始化OpenAI聊天服务
     *
     * @param properties   LLM配置属性
     * @param objectMapper JSON对象映射器
     */
    public OpenAIChatCompletionService(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.modelPricingMap = initializeModelPricing();

        // 初始化OpenAI API和聊天模型
        OpenAiApi openAiApi = createOpenAiApi();
        this.chatModel = createChatModel(openAiApi);
        this.streamingChatModel = this.chatModel; // OpenAiChatModel同时实现了StreamingChatModel

        log.info("OpenAI聊天服务初始化完成: baseUrl={}, models={}",
                properties.getOpenai().getBaseUrl(),
                properties.getOpenai().getModels());
    }

    @Override
    public ChatCompletionResult generateCompletion(Map<String, Object> context) {
        long startTime = System.currentTimeMillis();

        try {
            // 构建提示词和选项
            Prompt prompt = buildPrompt(context);

            log.debug("开始生成OpenAI聊天完成: model={}, messages={}",
                    context.get("model"), prompt.getInstructions().size());

            // 调用模型生成响应
            ChatResponse response = chatModel.call(prompt);

            // 处理响应结果
            return processResponse(response, startTime, context);

        } catch (Exception e) {
            log.error("OpenAI聊天完成生成失败", e);
            throw convertException(e);
        }
    }

    @Override
    public void generateStreamResponse(Map<String, Object> context, Consumer<String> onChunk) {
        try {
            // 构建流式提示词
            Prompt prompt = buildPrompt(context);

            log.debug("开始OpenAI流式生成: model={}", context.get("model"));

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
                                throw new ChatException(ErrorCode.EXT_AI_021, error);
                            })
                            .doOnComplete(() -> log.debug("流式响应完成"))
                            .subscribe();

                } catch (Exception e) {
                    log.error("流式响应处理失败", e);
                    throw new ChatException(ErrorCode.EXT_AI_022, e);
                }
            });

        } catch (Exception e) {
            log.error("OpenAI流式生成异常", e);
            throw new ChatException(ErrorCode.EXT_AI_022, e);
        }
    }

    @Override
    public boolean isModelAvailable(String modelCode) {
        if (!properties.getOpenai().getModels().contains(modelCode)) {
            return false;
        }

        try {
            // 创建测试提示词
            Prompt testPrompt = new Prompt(
                    new UserMessage("test"),
                    OpenAiChatOptions.builder()
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
            // 使用最基础的模型进行健康检查
            return isModelAvailable("gpt-3.5-turbo");
        } catch (Exception e) {
            log.warn("OpenAI服务健康检查失败", e);
            return false;
        }
    }

    @Override
    public List<String> getSupportedModels() {
        return new ArrayList<>(properties.getOpenai().getModels());
    }

    // =================== 私有辅助方法 ===================

    /**
     * 创建OpenAI API实例
     */
    private OpenAiApi createOpenAiApi() {
        LlmProperties.OpenAiProperties openAiConfig = properties.getOpenai();

        // 构建自定义的RestClient
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(openAiConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiConfig.getApiKey())
                .defaultHeader(HttpHeaders.USER_AGENT, "BaseAI-SpringAI/2.0");

        // 如果配置了组织ID
        if (openAiConfig.getOrganization() != null && !openAiConfig.getOrganization().isEmpty()) {
            restClientBuilder.defaultHeader("OpenAI-Organization", openAiConfig.getOrganization());
        }

        // 创建OpenAI API
        return new OpenAiApi.Builder()
                .baseUrl(openAiConfig.getBaseUrl())
                .apiKey(openAiConfig.getApiKey())
                .restClientBuilder(restClientBuilder)
                .build();
    }

    /**
     * 创建聊天模型
     */
    private OpenAiChatModel createChatModel(OpenAiApi openAiApi) {
        // 创建重试模板
        RetryTemplate retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;

        // 设置默认选项
        OpenAiChatOptions defaultOptions = OpenAiChatOptions.builder()
                .model(properties.getDefaultParameters().getModel())
                .temperature(properties.getDefaultParameters().getTemperature())
                .maxTokens(properties.getDefaultParameters().getMaxTokens())
                .topP(properties.getDefaultParameters().getTopP())
                .frequencyPenalty(properties.getDefaultParameters().getFrequencyPenalty())
                .presencePenalty(properties.getDefaultParameters().getPresencePenalty())
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
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

                switch (role.toLowerCase()) {
                    case "system":
                        messages.add(new SystemMessage(content));
                        break;
                    case "user":
                        messages.add(new UserMessage(content));
                        break;
                    case "assistant":
                        messages.add(new AssistantMessage(content));
                        break;
                    default:
                        log.warn("未知的消息角色: {}", role);
                }
            }
        }

        // 添加当前用户消息
        if (context.containsKey("currentMessage")) {
            messages.add(new UserMessage((String) context.get("currentMessage")));
        }

        // 构建聊天选项
        OpenAiChatOptions options = buildChatOptions(context);

        return new Prompt(messages, options);
    }

    /**
     * 构建聊天选项
     */
    @SuppressWarnings("unchecked")
    private OpenAiChatOptions buildChatOptions(Map<String, Object> context) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder();

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

        // 处理工具调用 - Spring AI 1.0.1中通过函数名称调用
        if (context.containsKey("availableTools")) {
            List<String> toolNames = (List<String>) context.get("availableTools");
            if (!toolNames.isEmpty()) {
                // 设置可用的函数列表
                builder.toolNames(new HashSet<>(toolNames));
                log.debug("启用工具调用: {}", toolNames);
            }
        }

        // 如果明确指定了要调用的工具
        if (context.containsKey("toolChoice")) {
            String toolChoice = (String) context.get("toolChoice");
            builder.toolChoice(toolChoice);
            log.debug("指定工具调用: {}", toolChoice);
        }

        return builder.build();
    }

    /**
     * 处理响应结果
     */
    private ChatCompletionResult processResponse(ChatResponse response, long startTime, Map<String, Object> context) {
        if (response == null) {
            throw new ChatException(ErrorCode.EXT_AI_008);
        } else {
            response.getResult();
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

        log.info("OpenAI响应完成: model={}, tokens={}/{}, latency={}ms, cost=${}",
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

        return new ChatException(ErrorCode.EXT_AI_020, e);
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

        double inputCost = (promptTokens / 1000.0) * pricing.getInputPricePerMTokens();
        double outputCost = (completionTokens / 1000.0) * pricing.getOutputPricePerMTokens();

        return inputCost + outputCost;
    }

    /**
     * 初始化模型定价信息
     */
    private Map<String, ModelPricing> initializeModelPricing() {
        Map<String, ModelPricing> pricing = new ConcurrentHashMap<>();

        // GPT-4系列定价 (每1000个token的价格，单位：美元)
        pricing.put("gpt-4", new ModelPricing(0.03, 0.06));
        pricing.put("gpt-4-32k", new ModelPricing(0.06, 0.12));
        pricing.put("gpt-4-turbo", new ModelPricing(0.01, 0.03));
        pricing.put("gpt-4o", new ModelPricing(0.005, 0.015));
        pricing.put("gpt-4o-mini", new ModelPricing(0.00015, 0.0006));

        // GPT-3.5系列定价
        pricing.put("gpt-3.5-turbo", new ModelPricing(0.0005, 0.0015));
        pricing.put("gpt-3.5-turbo-16k", new ModelPricing(0.003, 0.004));

        return pricing;
    }
}