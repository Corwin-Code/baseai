package com.cloud.baseai.infrastructure.external.llm.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.cloud.baseai.infrastructure.exception.ChatException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.external.llm.model.ChatCompletionResult;
import com.cloud.baseai.infrastructure.external.llm.model.ModelPricing;
import com.cloud.baseai.infrastructure.external.llm.model.QwenModelPricing;
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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * <h2>通义千问聊天完成服务</h2>
 *
 * <p>基于Spring AI Alibaba框架的通义千问服务实现。通义千问是阿里巴巴达摩院开发的
 * 大语言模型系列，在中文理解和生成方面表现卓越，特别适合中文业务场景。</p>
 *
 * <p><b>通义千问特色功能：</b></p>
 * <ul>
 * <li><b>中文优化：</b>深度优化的中文理解和生成能力，对中文语境有更好的把握</li>
 * <li><b>多语言支持：</b>支持119种语言和方言，全球化应用的理想选择</li>
 * <li><b>长文本处理：</b>支持超长文本的理解和生成，适合文档处理场景</li>
 * <li><b>成本效益：</b>相比国际模型更具成本优势，特别适合大规模部署</li>
 * <li><b>本地化服务：</b>服务器部署在国内，访问速度更快，数据更安全</li>
 * </ul>
 */
@Service
public class QwenChatCompletionService implements ChatCompletionService {

    private static final Logger log = LoggerFactory.getLogger(QwenChatCompletionService.class);

    private final LlmProperties properties;
    private final DashScopeChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final Map<String, ModelPricing> modelPricingMap;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数，初始化通义千问聊天服务
     *
     * <p>通过依赖注入获取必要的组件，并初始化模型定价信息。</p>
     *
     * @param properties   LLM配置属性，包含API密钥、超时设置等
     * @param chatModel    已配置的DashScope聊天模型
     * @param objectMapper JSON对象映射器，用于序列化工具调用信息
     */
    public QwenChatCompletionService(LlmProperties properties,
                                     DashScopeChatModel chatModel,
                                     ObjectMapper objectMapper) {
        this.properties = properties;
        this.chatModel = chatModel;
        this.streamingChatModel = chatModel;
        this.objectMapper = objectMapper;
        this.modelPricingMap = initializeModelPricing();

        log.info("通义千问聊天服务初始化完成: baseUrl={}, models={}",
                properties.getQwen().getBaseUrl(),
                properties.getQwen().getModels());
    }

    @Override
    public ChatCompletionResult generateCompletion(Map<String, Object> context) {
        long startTime = System.currentTimeMillis();

        try {
            // 构建提示词和选项
            Prompt prompt = buildPrompt(context);

            log.debug("开始生成通义千问聊天完成: model={}, messages={}",
                    context.get("model"), prompt.getInstructions().size());

            // 调用模型生成响应
            ChatResponse response = chatModel.call(prompt);

            // 处理响应结果
            return processResponse(response, startTime, context);

        } catch (Exception e) {
            log.error("通义千问聊天完成生成失败", e);
            throw convertException(e);
        }
    }

    @Override
    public void generateStreamResponse(Map<String, Object> context, Consumer<String> onChunk) {
        try {
            // 构建流式提示词
            Prompt prompt = buildPrompt(context);

            log.debug("开始通义千问流式生成: model={}", context.get("model"));

            // 异步处理流式响应
            CompletableFuture.runAsync(() -> {
                try {
                    Flux<ChatResponse> responseFlux = streamingChatModel.stream(prompt);

                    responseFlux
                            .mapNotNull(response -> response.getResult().getOutput().getText())
                            .filter(content -> content != null && !content.isEmpty())
                            .doOnNext(chunk -> {
                                log.debug("接收到流式数据块: {}", chunk.substring(0, Math.min(50, chunk.length())));
                                onChunk.accept(chunk);
                            })
                            .doOnError(error -> {
                                log.error("流式响应处理错误", error);
                                throw new ChatException(ErrorCode.EXT_QWEN_005, error);
                            })
                            .doOnComplete(() -> log.debug("流式响应完成"))
                            .subscribe();

                } catch (Exception e) {
                    log.error("流式响应处理失败", e);
                    throw new ChatException(ErrorCode.EXT_QWEN_006, e);
                }
            });

        } catch (Exception e) {
            log.error("通义千问流式生成异常", e);
            throw new ChatException(ErrorCode.EXT_QWEN_007, e);
        }
    }

    @Override
    public boolean isModelAvailable(String modelCode) {
        if (!properties.getQwen().getModels().contains(modelCode)) {
            log.debug("模型不在支持列表中: {}", modelCode);
            return false;
        }

        try {
            // 创建测试提示词
            Prompt testPrompt = new Prompt(
                    new UserMessage("测试"),
                    DashScopeChatOptions.builder()
                            .withModel(modelCode)
                            .withMaxToken(1)
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
            boolean healthy = isModelAvailable("qwen-turbo");
            log.debug("通义千问服务健康检查结果: {}", healthy);
            return healthy;

        } catch (Exception e) {
            log.warn("通义千问服务健康检查失败", e);
            return false;
        }
    }

    @Override
    public List<String> getSupportedModels() {
        return new ArrayList<>(properties.getQwen().getModels());
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

        // 添加系统提示词（如果有）
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
        DashScopeChatOptions options = buildChatOptions(context);

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
     * <p>根据上下文信息构建DashScope特定的聊天选项，包括模型选择、
     * 参数设置和特殊功能启用。</p>
     *
     * @param context 上下文信息
     * @return 聊天选项
     */
    @SuppressWarnings("unchecked")
    private DashScopeChatOptions buildChatOptions(Map<String, Object> context) {
        DashScopeChatOptions.DashscopeChatOptionsBuilder builder = DashScopeChatOptions.builder();

        // 设置模型
        String model = (String) context.get("model");
        if (model != null) {
            builder.withModel(model);

            // 应用模型特定参数
            LlmProperties.ModelParameters modelParams = properties.getModelSpecificParameters().get(model);
            if (modelParams != null) {
                if (modelParams.getTemperature() != null) {
                    builder.withTemperature(modelParams.getTemperature());
                }
                if (modelParams.getMaxTokens() != null) {
                    builder.withMaxToken(modelParams.getMaxTokens());
                }
            }
        } else {
            // 使用默认的Qwen模型
            builder.withModel("qwen-plus");
        }

        // 设置生成参数
        if (context.containsKey("temperature")) {
            builder.withTemperature(((Number) context.get("temperature")).doubleValue());
        }
        if (context.containsKey("maxTokens")) {
            builder.withMaxToken((Integer) context.get("maxTokens"));
        }
        if (context.containsKey("topP")) {
            builder.withTopP(((Number) context.get("topP")).doubleValue());
        }
        if (context.containsKey("topK")) {
            builder.withTopK((Integer) context.get("topK"));
        }

        // 通义千问特有功能：搜索增强
        if (context.containsKey("enableSearch") && (Boolean) context.get("enableSearch")) {
            builder.withEnableSearch(true);
            log.debug("启用通义千问搜索增强功能");
        }

        // 通义千问特有功能：增量输出
        if (context.containsKey("incrementalOutput")) {
            builder.withIncrementalOutput((Boolean) context.get("incrementalOutput"));
        }

        // 处理工具调用
        if (context.containsKey("availableTools")) {
            List<String> toolNames = (List<String>) context.get("availableTools");
            if (!toolNames.isEmpty()) {
                // 通义千问的工具调用配置
                log.debug("通义千问工具调用配置: {}", toolNames);
                // 注意：具体的工具调用API需要根据DashScope的实际接口调整
                // builder.withTools(toolNames);
            }
        }

        // 如果明确指定了要调用的工具
        if (context.containsKey("toolChoice")) {
            String toolChoice = (String) context.get("toolChoice");
            // builder.withToolChoice(toolChoice);
            log.debug("指定工具调用: {}", toolChoice);
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
            throw new ChatException(ErrorCode.EXT_QWEN_002);
        }

        Generation generation = response.getResult();
        String content = generation.getOutput().getText();

        if (content == null || content.trim().isEmpty()) {
            log.warn("通义千问返回空内容");
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

            // 计算费用（人民币）
            String model = (String) context.get("model");
            cost = calculateCost(model, promptTokens, completionTokens);
        }

        // 检查是否有工具调用
        String toolCallJson = null;
        if (!generation.getOutput().getToolCalls().isEmpty()) {
            try {
                toolCallJson = objectMapper.writeValueAsString(generation.getOutput().getToolCalls());
                log.debug("检测到工具调用: {}", toolCallJson);
            } catch (Exception e) {
                log.error("序列化工具调用失败", e);
            }
        }

        log.info("通义千问响应完成: model={}, tokens={}/{}, latency={}ms, cost=¥{}",
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
                return new ChatException(ErrorCode.EXT_QWEN_008, e);
            } else if (message.contains("429") || message.contains("Rate limit") || message.contains("Throttling")) {
                return new ChatException(ErrorCode.EXT_QWEN_009, e);
            } else if (message.contains("timeout") || message.contains("Timeout") || message.contains("连接超时")) {
                return new ChatException(ErrorCode.EXT_QWEN_010, e);
            } else if (message.contains("quota") || message.contains("配额") || message.contains("余额不足")) {
                return new ChatException(ErrorCode.EXT_QWEN_011, e);
            } else if (message.contains("model") || message.contains("模型")) {
                return new ChatException(ErrorCode.EXT_QWEN_012, e);
            }
        }

        return new ChatException(ErrorCode.EXT_QWEN_003, e);
    }

    /**
     * 计算调用费用
     *
     * <p>根据模型定价和token使用量计算本次调用的费用（人民币）。
     * 通义千问按千token计费，相比国际模型更具成本优势。</p>
     *
     * @param model            使用的模型名称
     * @param promptTokens     输入token数量
     * @param completionTokens 输出token数量
     * @return 费用金额（人民币）
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

        // 通义千问按千token计费
        double inputCost = (promptTokens / 1000.0) * pricing.getInputPricePerMTokens();
        double outputCost = (completionTokens / 1000.0) * pricing.getOutputPricePerMTokens();

        double totalCost = inputCost + outputCost;
        log.debug("费用计算: model={}, 输入={}token/¥{}, 输出={}token/¥{}, 总计=¥{}",
                model, promptTokens, inputCost, completionTokens, outputCost, totalCost);

        return totalCost;
    }

    /**
     * 初始化模型定价信息
     *
     * <p>根据阿里云官方定价初始化各个模型的计费标准。
     * 定价以人民币计算，按千个token收费。</p>
     *
     * @return 模型定价映射表
     */
    private Map<String, ModelPricing> initializeModelPricing() {
        Map<String, ModelPricing> pricing = new ConcurrentHashMap<>();

        // 通义千问系列定价 (每千token的价格，单位：人民币)
        // 数据来源：阿里云官方定价 (2024年数据，实际使用时需要更新)

        // 基础模型
        pricing.put("qwen-turbo", new ModelPricing(0.002, 0.006));           // 高性价比模型
        pricing.put("qwen-plus", new ModelPricing(0.004, 0.012));            // 平衡性能与成本
        pricing.put("qwen-max", new ModelPricing(0.04, 0.12));               // 最高性能模型
        pricing.put("qwen-max-longcontext", new ModelPricing(0.04, 0.12));   // 长文本优化

        // 专业模型
        pricing.put("qwen-coder", new ModelPricing(0.002, 0.006));           // 代码生成专用
        pricing.put("qwen-math", new ModelPricing(0.004, 0.012));            // 数学推理专用

        // 多模态模型
        pricing.put("qwen-vl-plus", new ModelPricing(0.008, 0.024));         // 视觉理解模型
        pricing.put("qwen-vl-max", new ModelPricing(0.02, 0.06));            // 高级视觉模型

        // 向量化模型 (只有输入费用，无输出费用)
        pricing.put("text-embedding-v1", new ModelPricing(0.0007, 0.0));
        pricing.put("text-embedding-v2", new ModelPricing(0.0007, 0.0));
        pricing.put("text-embedding-v3", new ModelPricing(0.0007, 0.0));

        log.info("初始化通义千问模型定价信息: {} 个模型", pricing.size());
        return pricing;
    }

    /**
     * 获取模型信息
     *
     * <p>提供模型的详细信息，用于监控和管理。</p>
     *
     * @param modelCode 模型代码
     * @return 模型信息，如果模型不存在则返回null
     */
    public QwenModelPricing getModelInfo(String modelCode) {
        ModelPricing pricing = modelPricingMap.get(modelCode);
        if (pricing == null) {
            return null;
        }

        return new QwenModelPricing(
                modelCode,
                pricing.getInputPricePerMTokens(),
                pricing.getOutputPricePerMTokens(),
                getModelMaxTokens(modelCode),
                getModelDescription(modelCode)
        );
    }

    /**
     * 获取模型最大token数
     */
    private int getModelMaxTokens(String modelCode) {
        return switch (modelCode) {
            case "qwen-turbo" -> 8000;
            case "qwen-plus" -> 32000;
            case "qwen-max" -> 8000;
            case "qwen-max-longcontext" -> 1000000; // 100万token超长上下文
            case "qwen-coder" -> 8000;
            case "qwen-math" -> 8000;
            case "qwen-vl-plus", "qwen-vl-max" -> 8000;
            default -> 8000;
        };
    }

    /**
     * 获取模型描述
     */
    private String getModelDescription(String modelCode) {
        return switch (modelCode) {
            case "qwen-turbo" -> "通义千问超大规模语言模型，适合日常对话和简单任务";
            case "qwen-plus" -> "通义千问增强版模型，平衡性能与成本的最佳选择";
            case "qwen-max" -> "通义千问旗舰模型，提供最强的理解和生成能力";
            case "qwen-max-longcontext" -> "通义千问长文本模型，支持百万级token上下文";
            case "qwen-coder" -> "通义千问代码模型，专门优化代码生成和理解";
            case "qwen-math" -> "通义千问数学模型，专门优化数学推理和计算";
            case "qwen-vl-plus" -> "通义千问视觉模型，支持图像理解和描述";
            case "qwen-vl-max" -> "通义千问高级视觉模型，提供最强的多模态能力";
            default -> "通义千问语言模型";
        };
    }
}