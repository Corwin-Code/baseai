package com.clinflash.baseai.infrastructure.flow.executor;

import com.clinflash.baseai.application.kb.command.VectorSearchCommand;
import com.clinflash.baseai.application.kb.dto.SearchResultDTO;
import com.clinflash.baseai.application.kb.service.KnowledgeBaseAppService;
import com.clinflash.baseai.domain.flow.model.NodeTypes;
import com.clinflash.baseai.infrastructure.config.KnowledgeBaseConfig;
import com.clinflash.baseai.infrastructure.external.llm.ChatCompletionService;
import com.clinflash.baseai.infrastructure.external.llm.EmbeddingService;
import com.clinflash.baseai.infrastructure.flow.model.FlowExecutionContext;
import com.clinflash.baseai.infrastructure.flow.model.NodeExecutionInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>AI节点执行器</h2>
 *
 * <p>这个执行器负责处理所有与人工智能相关的节点。
 * 它能够理解自然语言、检索知识、生成向量、进行分类等各种智能任务。</p>
 *
 * <p><b>支持的AI能力：</b></p>
 * <ul>
 * <li><b>LLM节点：</b>调用大语言模型进行文本生成和推理</li>
 * <li><b>检索节点：</b>从知识库中检索相关信息</li>
 * <li><b>向量节点：</b>将文本转换为向量表示</li>
 * <li><b>对话节点：</b>维护多轮对话上下文</li>
 * <li><b>分类节点：</b>对文本进行智能分类</li>
 * </ul>
 */
@Component
public class AINodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(AINodeExecutor.class);

    private final ObjectMapper objectMapper;
    private final ChatCompletionService chatService;
    private final EmbeddingService embeddingService;
    private final KnowledgeBaseConfig config;

    @Autowired(required = false)
    private KnowledgeBaseAppService kbService;

    public AINodeExecutor(ObjectMapper objectMapper,
                          ChatCompletionService chatService,
                          EmbeddingService embeddingService, KnowledgeBaseConfig config) {
        this.objectMapper = objectMapper;
        this.chatService = chatService;
        this.embeddingService = embeddingService;
        this.config = config;
    }

    @Override
    public List<String> getSupportedNodeTypes() {
        return List.of(NodeTypes.LLM, NodeTypes.RETRIEVER, NodeTypes.EMBEDDER,
                NodeTypes.CHAT, NodeTypes.CLASSIFIER);
    }

    @Override
    public Map<String, Object> execute(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                       FlowExecutionContext context) {
        log.debug("执行AI节点: type={}, key={}", nodeInfo.nodeTypeCode(), nodeInfo.nodeKey());

        return switch (nodeInfo.nodeTypeCode()) {
            case NodeTypes.LLM -> executeLLMNode(nodeInfo, input, context);
            case NodeTypes.RETRIEVER -> executeRetrieverNode(nodeInfo, input, context);
            case NodeTypes.EMBEDDER -> executeEmbedderNode(nodeInfo, input, context);
            case NodeTypes.CHAT -> executeChatNode(nodeInfo, input, context);
            case NodeTypes.CLASSIFIER -> executeClassifierNode(nodeInfo, input, context);
            default -> throw new IllegalArgumentException("不支持的AI节点类型: " + nodeInfo.nodeTypeCode());
        };
    }

    /**
     * 执行大语言模型节点
     *
     * <p>这是AI能力的核心节点，能够调用各种大语言模型来处理复杂的文本任务。
     * 它支持prompt模板、参数调节、多模型选择等高级功能。</p>
     */
    private Map<String, Object> executeLLMNode(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                               FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());
            String model = (String) config.get("model");
            String promptTemplate = (String) config.get("prompt");
            Float temperature = ((Number) config.getOrDefault("temperature", 0.7)).floatValue();
            Integer maxTokens = (Integer) config.getOrDefault("maxTokens", 1000);

            if (model == null || promptTemplate == null) {
                throw new IllegalArgumentException("LLM节点需要配置model和prompt参数");
            }

            // 构建实际的prompt，支持变量替换
            String actualPrompt = buildPrompt(promptTemplate, input, context);

            // 准备LLM调用上下文
            Map<String, Object> llmContext = prepareLLMContext(model, actualPrompt, temperature, maxTokens, input);

            // 调用LLM服务
            ChatCompletionService.ChatCompletionResult result = chatService.generateCompletion(llmContext);

            // 构建输出
            Map<String, Object> output = new HashMap<>(input);
            output.put("response", result.content());
            output.put("model", model);
            output.put("tokenIn", result.tokenIn());
            output.put("tokenOut", result.tokenOut());
            output.put("latencyMs", result.latencyMs());
            output.put("cost", result.cost());
            output.put("timestamp", System.currentTimeMillis());

            // 如果有工具调用，也包含在输出中
            if (result.toolCall() != null) {
                output.put("toolCall", result.toolCall());
            }

            log.debug("LLM节点执行完成: model={}, responseLength={}, cost=${}",
                    model, result.content().length(), result.cost());
            return output;

        } catch (Exception e) {
            log.error("LLM节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("LLM节点执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行知识检索节点
     *
     * <p>这个节点能够从企业知识库中检索相关信息，为AI生成提供准确的上下文。
     * 它支持向量检索、关键词搜索、混合检索等多种检索策略。</p>
     */
    private Map<String, Object> executeRetrieverNode(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                                     FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());
            String query = (String) input.get("query");
            String modelCode = (String) config.getOrDefault("modelCode", "text-embedding-3-small");
            Integer topK = (Integer) config.getOrDefault("topK", 10);
            Float threshold = ((Number) config.getOrDefault("threshold", 0.7)).floatValue();

            if (query == null || query.trim().isEmpty()) {
                throw new IllegalArgumentException("检索查询不能为空");
            }

            Map<String, Object> output = new HashMap<>(input);

            if (kbService != null) {
                // 调用知识库检索服务
                VectorSearchCommand cmd = new VectorSearchCommand(context.getTenantId(), query, modelCode, topK, threshold, null);
                List<SearchResultDTO> results = kbService.vectorSearch(cmd);
                output.put("results", results);
                output.put("resultCount", results.size());

                log.warn("知识库服务未配置，返回空结果");
            } else {
                output.put("results", List.of());
                output.put("resultCount", 0);
                log.warn("知识库服务不可用");
            }

            output.put("query", query);
            output.put("modelCode", modelCode);
            output.put("timestamp", System.currentTimeMillis());

            log.debug("检索节点执行完成: query={}, topK={}", query, topK);
            return output;

        } catch (Exception e) {
            log.error("检索节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("检索节点执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行向量生成节点
     *
     * <p>将文本转换为数值向量，这是现代AI系统的基础能力。
     * 生成的向量可以用于相似性计算、聚类分析、检索排序等任务。</p>
     */
    private Map<String, Object> executeEmbedderNode(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                                    FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());
            String text = (String) input.get("text");
            String modelCode = (String) config.getOrDefault("modelCode", "text-embedding-3-small");

            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("输入文本不能为空");
            }

            // 生成向量
            float[] embedding = embeddingService.generateEmbedding(text, modelCode);

            Map<String, Object> output = new HashMap<>(input);
            output.put("text", text);
            output.put("embedding", embedding);
            output.put("dimension", embedding.length);
            output.put("modelCode", modelCode);
            output.put("timestamp", System.currentTimeMillis());

            log.debug("向量生成节点执行完成: textLength={}, dimension={}", text.length(), embedding.length);
            return output;

        } catch (Exception e) {
            log.error("向量生成节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("向量生成节点执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行对话节点
     *
     * <p>维护多轮对话的上下文状态，让AI能够理解对话历史并生成连贯的回复。
     * 这对于构建智能客服、助手等应用至关重要。</p>
     */
    private Map<String, Object> executeChatNode(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                                FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());
            String message = (String) input.get("message");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> history = (List<Map<String, String>>) input.get("history");
            String model = (String) config.getOrDefault("model", "gpt-3.5-turbo");
            Float temperature = ((Number) config.getOrDefault("temperature", 0.7)).floatValue();

            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("对话消息不能为空");
            }

            // 构建完整的消息列表
            List<Map<String, Object>> messages = buildChatMessages(history, message);

            // 准备LLM调用上下文
            Map<String, Object> llmContext = Map.of(
                    "model", model,
                    "messages", messages,
                    "temperature", temperature,
                    "maxTokens", config.getOrDefault("maxTokens", 1000)
            );

            // 调用LLM服务
            ChatCompletionService.ChatCompletionResult result = chatService.generateCompletion(llmContext);

            Map<String, Object> output = new HashMap<>(input);
            output.put("message", message);
            output.put("response", result.content());
            output.put("model", model);
            output.put("tokenIn", result.tokenIn());
            output.put("tokenOut", result.tokenOut());
            output.put("timestamp", System.currentTimeMillis());

            log.debug("对话节点执行完成: messageLength={}, responseLength={}",
                    message.length(), result.content().length());
            return output;

        } catch (Exception e) {
            log.error("对话节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("对话节点执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行分类器节点
     *
     * <p>对输入文本进行智能分类，可以用于意图识别、情感分析、
     * 内容审核等各种文本分类任务。</p>
     */
    private Map<String, Object> executeClassifierNode(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                                      FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());
            String text = (String) input.get("text");
            @SuppressWarnings("unchecked")
            List<String> categories = (List<String>) config.get("categories");
            String model = (String) config.getOrDefault("model", "gpt-3.5-turbo");

            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("待分类文本不能为空");
            }

            if (categories == null || categories.isEmpty()) {
                throw new IllegalArgumentException("分类类别不能为空");
            }

            // 构建分类prompt
            String prompt = buildClassificationPrompt(text, categories);

            // 准备LLM调用上下文
            Map<String, Object> llmContext = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "temperature", 0.3f,
                    "maxTokens", 100
            );

            // 调用LLM服务
            ChatCompletionService.ChatCompletionResult result = chatService.generateCompletion(llmContext);

            Map<String, Object> output = new HashMap<>(input);
            output.put("text", text);
            output.put("categories", categories);
            output.put("prediction", result.content().trim());
            output.put("confidence", 0.8); // 简化的置信度，实际可能需要更复杂的计算
            output.put("model", model);
            output.put("timestamp", System.currentTimeMillis());

            log.debug("分类节点执行完成: textLength={}, prediction={}", text.length(), result.content().trim());
            return output;

        } catch (Exception e) {
            log.error("分类节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("分类节点执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void validateConfig(String configJson) {
        try {
            Map<String, Object> config = parseConfig(configJson);

            // 基础验证：确保有model参数
            if (!config.containsKey("model")) {
                throw new IllegalArgumentException("AI节点必须配置model参数");
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("AI节点配置验证失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            return chatService.isHealthy() && embeddingService.isModelAvailable(
                    config.getVector().getDefaultModel());
        } catch (Exception e) {
            log.warn("AI节点执行器健康检查失败", e);
            return false;
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 解析配置JSON
     */
    private Map<String, Object> parseConfig(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("配置JSON解析失败，使用默认配置: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 构建prompt，支持变量替换和上下文注入
     */
    private String buildPrompt(String template, Map<String, Object> input, FlowExecutionContext context) {
        String prompt = template;

        // 替换输入变量
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (prompt.contains(placeholder)) {
                prompt = prompt.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }

        // 替换上下文变量
        for (Map.Entry<String, Object> entry : context.getAllGlobalVariables().entrySet()) {
            String placeholder = "{ctx." + entry.getKey() + "}";
            if (prompt.contains(placeholder)) {
                prompt = prompt.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }

        return prompt;
    }

    /**
     * 准备LLM调用上下文
     */
    private Map<String, Object> prepareLLMContext(String model, String prompt, Float temperature,
                                                  Integer maxTokens, Map<String, Object> input) {
        Map<String, Object> context = new HashMap<>();
        context.put("model", model);
        context.put("temperature", temperature);
        context.put("maxTokens", maxTokens);

        // 构建消息列表
        List<Map<String, Object>> messages = List.of(
                Map.of("role", "user", "content", prompt)
        );
        context.put("messages", messages);

        // 如果输入中有知识上下文，添加到LLM上下文中
        if (input.containsKey("knowledgeContext")) {
            context.put("knowledgeContext", input.get("knowledgeContext"));
        }

        return context;
    }

    /**
     * 构建对话消息列表
     */
    private List<Map<String, Object>> buildChatMessages(List<Map<String, String>> history, String currentMessage) {
        List<Map<String, Object>> messages = new java.util.ArrayList<>();

        // 添加历史消息
        if (history != null) {
            for (Map<String, String> turn : history) {
                if (turn.containsKey("user")) {
                    messages.add(Map.of("role", "user", "content", turn.get("user")));
                }
                if (turn.containsKey("assistant")) {
                    messages.add(Map.of("role", "assistant", "content", turn.get("assistant")));
                }
            }
        }

        // 添加当前消息
        messages.add(Map.of("role", "user", "content", currentMessage));

        return messages;
    }

    /**
     * 构建分类prompt
     */
    private String buildClassificationPrompt(String text, List<String> categories) {
        return "请将以下文本分类到这些类别中的一个：\n" +
                "类别：" + String.join(", ", categories) + "\n\n" +
                "文本：" + text + "\n\n" +
                "请只返回类别名称，不要包含其他解释。";
    }
}