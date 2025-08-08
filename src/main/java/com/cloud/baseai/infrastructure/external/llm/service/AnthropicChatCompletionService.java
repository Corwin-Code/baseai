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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * <h2>Anthropic聊天完成服务</h2>
 *
 * <p>基于Spring AI的Anthropic Claude聊天完成服务实现。Claude是Anthropic开发的
 * 大语言模型系列，在复杂推理、长文本理解和安全性方面表现卓越。</p>
 *
 * <p><b>Anthropic Claude模型特色：</b></p>
 * <ul>
 * <li><b>强推理能力：</b>在逻辑推理、数学计算、代码分析等任务上表现优异</li>
 * <li><b>长文本处理：</b>支持超长文档的理解和分析，上下文长度可达200K tokens</li>
 * <li><b>安全可靠：</b>内置安全机制，减少有害或不当内容的生成</li>
 * <li><b>指令遵循：</b>对复杂指令的理解和执行能力强</li>
 * <li><b>多语言支持：</b>在多种语言上都有良好的表现</li>
 * </ul>
 */
@Service
public class AnthropicChatCompletionService implements ChatCompletionService {

    private static final Logger log = LoggerFactory.getLogger(AnthropicChatCompletionService.class);

    private final LlmProperties properties;
    private final AnthropicChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final Map<String, ModelPricing> modelPricingMap;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数，初始化Claude聊天服务
     *
     * @param properties   LLM配置属性
     * @param chatModel    已配置的Anthropic聊天模型
     * @param objectMapper JSON对象映射器
     */
    public AnthropicChatCompletionService(LlmProperties properties,
                                          AnthropicChatModel chatModel,
                                          ObjectMapper objectMapper) {
        this.properties = properties;
        this.chatModel = chatModel;
        this.streamingChatModel = chatModel; // AnthropicChatModel同时实现了StreamingChatModel
        this.objectMapper = objectMapper;
        this.modelPricingMap = initializeModelPricing();

        log.info("Anthropic聊天服务初始化完成: baseUrl={}, models={}",
                properties.getAnthropic().getBaseUrl(),
                properties.getAnthropic().getModels());
    }

    @Override
    public ChatCompletionResult generateCompletion(Map<String, Object> context) {
        long startTime = System.currentTimeMillis();

        try {
            // 构建提示词和选项
            Prompt prompt = buildPrompt(context);

            log.debug("开始生成Anthropic聊天完成: model={}, messages={}",
                    context.get("model"), prompt.getInstructions().size());

            // 调用模型生成响应
            ChatResponse response = chatModel.call(prompt);

            // 处理响应结果
            return processResponse(response, startTime, context);

        } catch (Exception e) {
            log.error("Anthropic聊天完成生成失败", e);
            throw convertException(e);
        }
    }

    @Override
    public void generateStreamResponse(Map<String, Object> context, Consumer<String> onChunk) {
        try {
            // 构建流式提示词
            Prompt prompt = buildPrompt(context);

            log.debug("开始Anthropic流式生成: model={}", context.get("model"));

            // 异步处理流式响应
            CompletableFuture.runAsync(() -> {
                try {
                    Flux<ChatResponse> responseFlux = streamingChatModel.stream(prompt);

                    responseFlux
                            .mapNotNull(response -> response.getResult().getOutput().getText())
                            .filter(content -> content != null && !content.isEmpty())
                            .doOnNext(chunk -> {
                                log.debug("接收到Anthropic流式数据块: {}",
                                        chunk.substring(0, Math.min(50, chunk.length())));
                                onChunk.accept(chunk);
                            })
                            .doOnError(error -> {
                                log.error("Anthropic流式响应处理错误", error);
                                throw new ChatException(ErrorCode.EXT_ANTHROPIC_004, error);
                            })
                            .doOnComplete(() -> log.debug("Anthropic流式响应完成"))
                            .subscribe();

                } catch (Exception e) {
                    log.error("Anthropic流式响应处理失败", e);
                    throw new ChatException(ErrorCode.EXT_ANTHROPIC_005, e);
                }
            });

        } catch (Exception e) {
            log.error("Anthropic流式生成异常", e);
            throw new ChatException(ErrorCode.EXT_ANTHROPIC_006, e);
        }
    }

    @Override
    public boolean isModelAvailable(String modelCode) {
        if (!properties.getAnthropic().getModels().contains(modelCode)) {
            log.debug("模型不在支持列表中: {}", modelCode);
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
            log.debug("模型可用性检查通过: {}", modelCode);
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
            boolean healthy = isModelAvailable("claude-3-haiku-20240307");
            log.debug("Anthropic服务健康检查结果: {}", healthy);
            return healthy;

        } catch (Exception e) {
            log.warn("Anthropic服务健康检查失败", e);
            return false;
        }
    }

    @Override
    public List<String> getSupportedModels() {
        return new ArrayList<>(properties.getAnthropic().getModels());
    }

    // =================== 私有辅助方法 ===================

    /**
     * 构建提示词
     *
     * <p>将业务上下文转换为Spring AI标准的Prompt对象，
     * 包括系统消息、历史对话和当前用户输入。</p>
     *
     * @param context 业务上下文
     * @return 构建好的提示词
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

        // 添加自定义系统提示词
        if (context.containsKey("systemPrompt")) {
            messages.add(new SystemMessage((String) context.get("systemPrompt")));
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
     *
     * <p>根据角色类型创建相应的消息对象，支持系统、用户和助手三种角色。</p>
     *
     * @param role    消息角色 (system/user/assistant)
     * @param content 消息内容
     * @return 消息对象
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
     *
     * <p>根据上下文信息构建Anthropic特定的聊天选项，包括模型选择、
     * 参数设置和特殊功能启用。</p>
     *
     * @param context 上下文信息
     * @return 聊天选项
     */
    @SuppressWarnings("unchecked")
    private AnthropicChatOptions buildChatOptions(Map<String, Object> context) {
        AnthropicChatOptions.Builder builder = new AnthropicChatOptions.Builder();

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
        } else {
            // 使用默认的Anthropic模型
            builder.model("claude-3-sonnet-20240229");
        }

        // 设置生成参数
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

        // 处理工具调用 - Spring AI的Anthropic集成可能支持函数调用
        if (context.containsKey("availableTools")) {
            List<String> toolNames = (List<String>) context.get("availableTools");
            if (!toolNames.isEmpty()) {
                // 设置可用的函数列表（如果Anthropic支持）
                log.debug("Anthropic工具调用配置: {}", toolNames);
                // 注意：具体的工具调用API需要根据Spring AI的实际接口调整
            }
        }

        return builder.build();
    }

    /**
     * 处理响应结果
     *
     * <p>将Spring AI的ChatResponse转换为业务层的ChatCompletionResult，
     * 包括内容提取、使用统计计算和费用计算。</p>
     *
     * @param response  Spring AI响应对象
     * @param startTime 请求开始时间
     * @param context   原始上下文
     * @return 业务层响应结果
     */
    private ChatCompletionResult processResponse(ChatResponse response, long startTime, Map<String, Object> context) {
        if (response == null || response.getResult() == null) {
            throw new ChatException(ErrorCode.EXT_ANTHROPIC_002);
        }

        Generation generation = response.getResult();
        String content = generation.getOutput().getText();

        if (content == null || content.trim().isEmpty()) {
            log.warn("Anthropic返回空内容");
            content = "";
        }

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

            // 计算费用（美元）
            String model = (String) context.get("model");
            cost = calculateCost(model, promptTokens, completionTokens);
        }

        // 检查是否有工具调用
        String toolCallJson = null;
        if (!generation.getOutput().getToolCalls().isEmpty()) {
            try {
                toolCallJson = objectMapper.writeValueAsString(generation.getOutput().getToolCalls());
                log.debug("检测到Anthropic工具调用: {}", toolCallJson);
            } catch (Exception e) {
                log.error("序列化工具调用失败", e);
            }
        }

        log.info("Anthropic响应完成: model={}, tokens={}/{}, latency={}ms, cost=${}",
                context.get("model"), promptTokens, completionTokens, latencyMs, cost);

        if (toolCallJson != null) {
            return ChatCompletionResult.withToolCall(content, toolCallJson, promptTokens, completionTokens, latencyMs, cost);
        } else {
            return ChatCompletionResult.success(content, promptTokens, completionTokens, latencyMs, cost);
        }
    }

    /**
     * 转换异常
     *
     * <p>将各种类型的异常转换为统一的ChatException，便于上层业务处理。</p>
     *
     * @param e 原始异常
     * @return 转换后的ChatException
     */
    private ChatException convertException(Exception e) {
        if (e instanceof ChatException) {
            return (ChatException) e;
        }

        // 根据不同的异常类型返回相应的错误码
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("401") || message.contains("Unauthorized") || message.contains("Invalid API")) {
                return new ChatException(ErrorCode.EXT_AI_008, e);
            } else if (message.contains("429") || message.contains("Rate limit") || message.contains("Throttling")) {
                return new ChatException(ErrorCode.EXT_AI_007, e);
            } else if (message.contains("timeout") || message.contains("Timeout") || message.contains("连接超时")) {
                return new ChatException(ErrorCode.EXT_AI_003, e);
            } else if (message.contains("quota") || message.contains("insufficient") || message.contains("余额不足")) {
                return new ChatException(ErrorCode.EXT_AI_004, e);
            } else if (message.contains("model") || message.contains("模型")) {
                return new ChatException(ErrorCode.EXT_AI_001, e);
            }
        }

        return new ChatException(ErrorCode.EXT_ANTHROPIC_003, e);
    }

    /**
     * 计算调用费用
     *
     * <p>根据模型定价和token使用量计算本次调用的费用（美元）。
     * Anthropic按千token计费，价格相对合理。</p>
     *
     * @param model            使用的模型名称
     * @param promptTokens     输入token数量
     * @param completionTokens 输出token数量
     * @return 费用金额（美元）
     */
    private double calculateCost(String model, Integer promptTokens, Integer completionTokens) {
        if (model == null || promptTokens == null || completionTokens == null) {
            return 0.0;
        }

        ModelPricing pricing = modelPricingMap.get(model);
        if (pricing == null) {
            log.warn("未找到Anthropic模型定价信息: {}", model);
            return 0.0;
        }

        // Anthropic按千token计费
        double inputCost = (promptTokens / 1000.0) * pricing.getInputPricePerMTokens();
        double outputCost = (completionTokens / 1000.0) * pricing.getOutputPricePerMTokens();

        double totalCost = inputCost + outputCost;
        log.debug("费用计算: model={}, 输入={}token/${}, 输出={}token/${}, 总计=${}",
                model, promptTokens, inputCost, completionTokens, outputCost, totalCost);

        return totalCost;
    }

    /**
     * 初始化模型定价信息
     *
     * <p>根据Anthropic官方定价初始化各个模型的计费标准。
     * 定价以美元计算，按千个token收费。</p>
     *
     * @return 模型定价映射表
     */
    private Map<String, ModelPricing> initializeModelPricing() {
        Map<String, ModelPricing> pricing = new ConcurrentHashMap<>();

        // Anthropic Claude系列定价 (每千token的价格，单位：美元)
        // 数据来源：Anthropic官方定价 (2024年数据，实际使用时需要更新)

        // Claude 3 Haiku - 最快速度，最低成本
        pricing.put("claude-3-haiku-20240307", new ModelPricing(0.00025, 0.00125));

        // Claude 3 Sonnet - 平衡性能与速度
        pricing.put("claude-3-sonnet-20240229", new ModelPricing(0.003, 0.015));

        // Claude 3 Opus - 最高性能
        pricing.put("claude-3-opus-20240229", new ModelPricing(0.015, 0.075));

        // Claude 3.5 Sonnet - 最新版本
        pricing.put("claude-3-5-sonnet-20241022", new ModelPricing(0.003, 0.015));
        pricing.put("claude-3-5-sonnet-20240620", new ModelPricing(0.003, 0.015));

        log.info("初始化Anthropic模型定价信息: {} 个模型", pricing.size());
        return pricing;
    }
}