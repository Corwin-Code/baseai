package com.clinflash.baseai.infrastructure.flow.executor;

import com.clinflash.baseai.application.kb.service.KnowledgeBaseAppService;
import com.clinflash.baseai.domain.flow.model.NodeTypes;
import com.clinflash.baseai.infrastructure.external.llm.ChatCompletionService;
import com.clinflash.baseai.infrastructure.external.llm.EmbeddingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>AI节点执行器</h2>
 *
 * <p>处理所有AI相关的节点执行，包括大语言模型调用、知识检索、向量生成等。
 * 这些节点是流程中智能化处理的核心组件。</p>
 */
@Component
public class AINodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(AINodeExecutor.class);

    private final ObjectMapper objectMapper;
    private final ChatCompletionService chatService;
    private final EmbeddingService embeddingService;
    private final KnowledgeBaseAppService kbService;

    public AINodeExecutor(ObjectMapper objectMapper,
                          ChatCompletionService chatService,
                          EmbeddingService embeddingService,
                          KnowledgeBaseAppService kbService) {
        this.objectMapper = objectMapper;
        this.chatService = chatService;
        this.embeddingService = embeddingService;
        this.kbService = kbService;
    }

    @Override
    public List<String> getSupportedNodeTypes() {
        return List.of(NodeTypes.LLM, NodeTypes.RETRIEVER, NodeTypes.EMBEDDER,
                NodeTypes.CHAT, NodeTypes.CLASSIFIER);
    }

    @Override
    public Map<String, Object> execute(Object nodeInfo, Map<String, Object> input, Object context) {
        String nodeTypeCode = extractNodeTypeCode(nodeInfo);
        String configJson = extractConfigJson(nodeInfo);

        log.debug("执行AI节点: type={}", nodeTypeCode);

        return switch (nodeTypeCode) {
            case NodeTypes.LLM -> executeLLMNode(input, configJson);
            case NodeTypes.RETRIEVER -> executeRetrieverNode(input, configJson);
            case NodeTypes.EMBEDDER -> executeEmbedderNode(input, configJson);
            case NodeTypes.CHAT -> executeChatNode(input, configJson);
            case NodeTypes.CLASSIFIER -> executeClassifierNode(input, configJson);
            default -> throw new IllegalArgumentException("不支持的AI节点类型: " + nodeTypeCode);
        };
    }

    /**
     * 执行LLM节点
     */
    private Map<String, Object> executeLLMNode(Map<String, Object> input, String configJson) {
        try {
            Map<String, Object> config = parseConfig(configJson);
            String model = (String) config.get("model");
            String promptTemplate = (String) config.get("prompt");
            Double temperature = (Double) config.getOrDefault("temperature", 0.7);
            Integer maxTokens = (Integer) config.getOrDefault("maxTokens", 1000);

            // 构建实际的prompt
            String actualPrompt = buildPrompt(promptTemplate, input);

            // 调用LLM服务
            String response = chatService.chat(model, actualPrompt, temperature.floatValue(), maxTokens);

            Map<String, Object> output = new HashMap<>();
            output.put("response", response);
            output.put("model", model);
            output.put("tokenUsed", response.length()); // 简化的token计算
            output.put("timestamp", System.currentTimeMillis());

            log.debug("LLM节点执行完成: model={}, responseLength={}", model, response.length());
            return output;

        } catch (Exception e) {
            log.error("LLM节点执行失败", e);
            throw new RuntimeException("LLM节点执行失败: " + e.getMessage());
        }
    }

    /**
     * 执行知识检索节点
     */
    private Map<String, Object> executeRetrieverNode(Map<String, Object> input, String configJson) {
        try {
            Map<String, Object> config = parseConfig(configJson);
            String query = (String) input.get("query");
            Long tenantId = ((Number) config.get("tenantId")).longValue();
            String modelCode = (String) config.getOrDefault("modelCode", "text-embedding-3-small");
            Integer topK = (Integer) config.getOrDefault("topK", 10);
            Float threshold = ((Number) config.getOrDefault("threshold", 0.7)).floatValue();

            if (query == null || query.trim().isEmpty()) {
                throw new IllegalArgumentException("检索查询不能为空");
            }

            // 这里需要调用知识库搜索服务
            // 简化实现，实际应该调用 kbService.vectorSearch
            Map<String, Object> output = new HashMap<>();
            output.put("query", query);
            output.put("results", List.of()); // 占位结果
            output.put("resultCount", 0);
            output.put("timestamp", System.currentTimeMillis());

            log.debug("检索节点执行完成: query={}, topK={}", query, topK);
            return output;

        } catch (Exception e) {
            log.error("检索节点执行失败", e);
            throw new RuntimeException("检索节点执行失败: " + e.getMessage());
        }
    }

    /**
     * 执行向量生成节点
     */
    private Map<String, Object> executeEmbedderNode(Map<String, Object> input, String configJson) {
        try {
            Map<String, Object> config = parseConfig(configJson);
            String text = (String) input.get("text");
            String modelCode = (String) config.getOrDefault("modelCode", "text-embedding-3-small");

            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("输入文本不能为空");
            }

            // 生成向量
            float[] embedding = embeddingService.generateEmbedding(text, modelCode);

            Map<String, Object> output = new HashMap<>();
            output.put("text", text);
            output.put("embedding", embedding);
            output.put("dimension", embedding.length);
            output.put("modelCode", modelCode);
            output.put("timestamp", System.currentTimeMillis());

            log.debug("向量生成节点执行完成: textLength={}, dimension={}", text.length(), embedding.length);
            return output;

        } catch (Exception e) {
            log.error("向量生成节点执行失败", e);
            throw new RuntimeException("向量生成节点执行失败: " + e.getMessage());
        }
    }

    /**
     * 执行对话节点
     */
    private Map<String, Object> executeChatNode(Map<String, Object> input, String configJson) {
        try {
            Map<String, Object> config = parseConfig(configJson);
            String message = (String) input.get("message");
            @SuppressWarnings("unchecked")
            List<Map<String, String>> history = (List<Map<String, String>>) input.get("history");
            String model = (String) config.getOrDefault("model", "gpt-3.5-turbo");

            // 构建对话上下文
            StringBuilder contextBuilder = new StringBuilder();
            if (history != null) {
                for (Map<String, String> turn : history) {
                    contextBuilder.append("用户: ").append(turn.get("user")).append("\n");
                    contextBuilder.append("助手: ").append(turn.get("assistant")).append("\n");
                }
            }
            contextBuilder.append("用户: ").append(message);

            String response = chatService.chat(model, contextBuilder.toString(), 0.7f, 1000);

            Map<String, Object> output = new HashMap<>();
            output.put("message", message);
            output.put("response", response);
            output.put("model", model);
            output.put("timestamp", System.currentTimeMillis());

            log.debug("对话节点执行完成: messageLength={}, responseLength={}",
                    message.length(), response.length());
            return output;

        } catch (Exception e) {
            log.error("对话节点执行失败", e);
            throw new RuntimeException("对话节点执行失败: " + e.getMessage());
        }
    }

    /**
     * 执行分类器节点
     */
    private Map<String, Object> executeClassifierNode(Map<String, Object> input, String configJson) {
        try {
            Map<String, Object> config = parseConfig(configJson);
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
            String response = chatService.chat(model, prompt, 0.3f, 100);

            Map<String, Object> output = new HashMap<>();
            output.put("text", text);
            output.put("categories", categories);
            output.put("prediction", response.trim());
            output.put("confidence", 0.8); // 简化的置信度
            output.put("timestamp", System.currentTimeMillis());

            log.debug("分类节点执行完成: textLength={}, prediction={}", text.length(), response.trim());
            return output;

        } catch (Exception e) {
            log.error("分类节点执行失败", e);
            throw new RuntimeException("分类节点执行失败: " + e.getMessage());
        }
    }

    // =================== 辅助方法 ===================

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
     * 构建prompt
     */
    private String buildPrompt(String template, Map<String, Object> input) {
        String prompt = template;

        // 简单的模板变量替换
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (prompt.contains(placeholder)) {
                prompt = prompt.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }

        return prompt;
    }

    /**
     * 构建分类prompt
     */
    private String buildClassificationPrompt(String text, List<String> categories) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请将以下文本分类到这些类别中的一个：\n");
        prompt.append("类别：").append(String.join(", ", categories)).append("\n\n");
        prompt.append("文本：").append(text).append("\n\n");
        prompt.append("请只返回类别名称，不要包含其他解释。");

        return prompt.toString();
    }

    // 占位方法，需要根据实际的nodeInfo对象实现
    private String extractNodeTypeCode(Object nodeInfo) {
        return "LLM"; // 占位实现
    }

    private String extractConfigJson(Object nodeInfo) {
        return "{}"; // 占位实现
    }
}