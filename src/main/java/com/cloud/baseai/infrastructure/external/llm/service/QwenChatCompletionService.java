package com.cloud.baseai.infrastructure.external.llm.service;

import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.cloud.baseai.infrastructure.exception.ChatException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.external.llm.model.ChatCompletionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * <h2>通义千问聊天完成服务</h2>
 *
 * <p>基于阿里云DashScope API的通义千问服务实现。通义千问是阿里巴巴达摩院开发的
 * 大语言模型系列，在中文理解和生成方面表现卓越。</p>
 *
 * <p><b>通义千问特色功能：</b></p>
 * <ul>
 * <li><b>中文优化：</b>深度优化的中文理解和生成能力</li>
 * <li><b>多语言支持：</b>支持119种语言和方言</li>
 * <li><b>长文本处理：</b>支持超长文本的理解和生成</li>
 * <li><b>成本效益：</b>相比国际模型更具成本优势</li>
 * </ul>
 *
 * <p><b>注意事项：</b></p>
 * <p>由于Spring AI原生不支持通义千问，这个实现直接使用DashScope API。</p>
 */
@Service
public class QwenChatCompletionService implements ChatCompletionService {

    private static final Logger log = LoggerFactory.getLogger(QwenChatCompletionService.class);

    private static final String DASHSCOPE_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
    private static final String SSE_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    private final LlmProperties properties;
    private final RestTemplate restTemplate;
    private final Map<String, ModelInfo> modelInfoMap;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数，初始化通义千问聊天服务
     *
     * @param properties   LLM配置属性
     * @param restTemplate REST客户端
     * @param objectMapper JSON对象映射器
     */
    public QwenChatCompletionService(LlmProperties properties,
                                     RestTemplate restTemplate,
                                     ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.modelInfoMap = initializeModelInfo();

        log.info("通义千问聊天服务初始化完成: baseUrl={}, models={}",
                properties.getQwen().getBaseUrl(),
                properties.getQwen().getModels());
    }

    @Override
    public ChatCompletionResult generateCompletion(Map<String, Object> context) {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("开始生成通义千问聊天完成: model={}", context.get("model"));

            // 构建请求
            Map<String, Object> request = buildRequest(context);

            // 发送HTTP请求
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    DASHSCOPE_API_URL, entity, Map.class);

            // 处理响应
            return processResponse(response.getBody(), startTime, context);

        } catch (Exception e) {
            log.error("通义千问聊天完成生成失败", e);
            throw convertException(e);
        }
    }

    @Override
    public void generateStreamResponse(Map<String, Object> context, Consumer<String> onChunk) {
        try {
            log.debug("开始通义千问流式生成: model={}", context.get("model"));

            // 构建流式请求
            Map<String, Object> request = buildRequest(context);
            request.put("stream", true); // 启用流式输出

            // 异步处理流式响应
            CompletableFuture.runAsync(() -> {
                try {
                    processStreamResponse(request, onChunk);
                } catch (Exception e) {
                    log.error("流式响应处理失败", e);
                    throw new ChatException(ErrorCode.EXT_AI_022, e);
                }
            });

        } catch (Exception e) {
            log.error("通义千问流式生成异常", e);
            throw new ChatException(ErrorCode.EXT_AI_022, e);
        }
    }

    @Override
    public boolean isModelAvailable(String modelCode) {
        if (!properties.getQwen().getModels().contains(modelCode)) {
            return false;
        }

        try {
            // 使用最简单的请求测试模型可用性
            Map<String, Object> testContext = Map.of(
                    "model", modelCode,
                    "currentMessage", "你好",
                    "maxTokens", 1
            );

            generateCompletion(testContext);
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
            return isModelAvailable("qwen-turbo");
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
     * 构建请求对象
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildRequest(Map<String, Object> context) {
        Map<String, Object> request = new HashMap<>();

        // 设置模型
        String model = (String) context.getOrDefault("model", "qwen-turbo");
        request.put("model", model);

        // 构建输入
        Map<String, Object> input = new HashMap<>();
        List<Map<String, Object>> messages = buildMessages(context);
        input.put("messages", messages);
        request.put("input", input);

        // 设置参数
        Map<String, Object> parameters = new HashMap<>();

        // 基础参数
        parameters.put("result_format", "message");

        // 从上下文获取参数
        if (context.containsKey("temperature")) {
            parameters.put("temperature", context.get("temperature"));
        } else {
            parameters.put("temperature", properties.getDefaultParameters().getTemperature());
        }

        if (context.containsKey("maxTokens")) {
            parameters.put("max_tokens", context.get("maxTokens"));
        } else {
            parameters.put("max_tokens", properties.getDefaultParameters().getMaxTokens());
        }

        if (context.containsKey("topP")) {
            parameters.put("top_p", context.get("topP"));
        } else {
            parameters.put("top_p", properties.getDefaultParameters().getTopP());
        }

        if (context.containsKey("topK")) {
            parameters.put("top_k", context.get("topK"));
        }

        // 是否启用搜索增强
        if (context.containsKey("enableSearch")) {
            parameters.put("enable_search", context.get("enableSearch"));
        }

        request.put("parameters", parameters);

        return request;
    }

    /**
     * 构建消息列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildMessages(Map<String, Object> context) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // 添加知识上下文
        if (context.containsKey("knowledgeContext")) {
            List<String> knowledgeContext = (List<String>) context.get("knowledgeContext");
            if (!knowledgeContext.isEmpty()) {
                String contextContent = "请根据以下相关知识内容回答用户问题。\n\n" +
                        "相关知识内容:\n" + String.join("\n\n---\n\n", knowledgeContext);
                messages.add(createMessage("system", contextContent));
            }
        }

        // 添加历史消息
        if (context.containsKey("messages")) {
            List<Map<String, Object>> historyMessages = (List<Map<String, Object>>) context.get("messages");
            messages.addAll(historyMessages);
        }

        // 添加当前消息
        if (context.containsKey("currentMessage")) {
            messages.add(createMessage("user", (String) context.get("currentMessage")));
        }

        return messages;
    }

    /**
     * 创建消息对象
     */
    private Map<String, Object> createMessage(String role, String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    /**
     * 创建HTTP请求头
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + properties.getQwen().getApiKey());
        headers.set("X-DashScope-SSE", "disable"); // 非流式请求
        return headers;
    }

    /**
     * 处理响应
     */
    @SuppressWarnings("unchecked")
    private ChatCompletionResult processResponse(Map<String, Object> responseBody, long startTime, Map<String, Object> context) {
        if (responseBody == null) {
            throw new ChatException(ErrorCode.EXT_AI_008);
        }

        // 检查错误
        if (responseBody.containsKey("code") && !"0".equals(responseBody.get("code").toString())) {
            String errorMsg = (String) responseBody.get("message");
            log.error("DashScope API错误: code={}, message={}", responseBody.get("code"), errorMsg);
            throw new ChatException(ErrorCode.EXT_AI_009, errorMsg);
        }

        // 提取输出
        Map<String, Object> output = (Map<String, Object>) responseBody.get("output");
        if (output == null || !output.containsKey("choices")) {
            throw new ChatException(ErrorCode.EXT_AI_008);
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) output.get("choices");
        if (choices.isEmpty()) {
            throw new ChatException(ErrorCode.EXT_AI_008);
        }

        Map<String, Object> choice = choices.getFirst();
        Map<String, Object> message = (Map<String, Object>) choice.get("message");
        String content = (String) message.get("content");

        // 提取使用统计
        Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
        Integer promptTokens = null;
        Integer completionTokens = null;

        if (usage != null) {
            promptTokens = ((Number) usage.get("input_tokens")).intValue();
            completionTokens = ((Number) usage.get("output_tokens")).intValue();
        }

        // 计算耗时和费用
        int latencyMs = (int) (System.currentTimeMillis() - startTime);
        String model = (String) context.get("model");
        double cost = calculateCost(model, promptTokens, completionTokens);

        log.info("通义千问响应完成: model={}, tokens={}/{}, latency={}ms, cost=¥{}",
                model, promptTokens, completionTokens, latencyMs, cost);

        // 检查是否有工具调用（通义千问的工具调用格式可能不同）
        String toolCallJson = null;
        if (message.containsKey("tool_calls")) {
            try {
                toolCallJson = objectMapper.writeValueAsString(message.get("tool_calls"));
            } catch (Exception e) {
                log.error("序列化工具调用失败", e);
            }
        }

        if (toolCallJson != null) {
            return ChatCompletionResult.withToolCall(content, toolCallJson, promptTokens, completionTokens, latencyMs, cost);
        } else {
            return ChatCompletionResult.success(content, promptTokens, completionTokens, latencyMs, cost);
        }
    }

    /**
     * 处理流式响应
     */
    private void processStreamResponse(Map<String, Object> request, Consumer<String> onChunk) {
        // 流式API需要特殊处理，这里提供简化实现
        // 实际应用中应该使用WebClient或其他支持SSE的客户端

        HttpHeaders headers = createHeaders();
        headers.set("X-DashScope-SSE", "enable"); // 启用流式
        headers.setAccept(Collections.singletonList(MediaType.TEXT_EVENT_STREAM));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        // 模拟流式处理
        try {
            // 发送请求并逐行处理响应
            // 实际实现需要使用支持SSE的HTTP客户端
            ResponseEntity<String> response = restTemplate.postForEntity(
                    SSE_API_URL, entity, String.class);

            // 简化处理：将整个响应作为一个块返回
            if (response.getBody() != null) {
                onChunk.accept(response.getBody());
            }

        } catch (Exception e) {
            log.error("流式请求失败", e);
            throw new ChatException(ErrorCode.EXT_AI_021, e);
        }
    }

    /**
     * 转换异常
     */
    private ChatException convertException(Exception e) {
        if (e instanceof ChatException) {
            return (ChatException) e;
        }

        if (e.getMessage() != null) {
            if (e.getMessage().contains("401") || e.getMessage().contains("Unauthorized")) {
                return new ChatException(ErrorCode.EXT_AI_003, e);
            } else if (e.getMessage().contains("429") || e.getMessage().contains("Rate limit")) {
                return new ChatException(ErrorCode.EXT_AI_004, e);
            } else if (e.getMessage().contains("timeout") || e.getMessage().contains("Timeout")) {
                return new ChatException(ErrorCode.EXT_AI_005, e);
            }
        }

        return new ChatException(ErrorCode.EXT_AI_020, e);
    }

    /**
     * 计算调用费用（人民币）
     */
    private double calculateCost(String model, Integer promptTokens, Integer completionTokens) {
        if (model == null || promptTokens == null || completionTokens == null) {
            return 0.0;
        }

        ModelInfo modelInfo = modelInfoMap.get(model);
        if (modelInfo == null) {
            log.warn("未找到模型信息: {}", model);
            return 0.0;
        }

        // 通义千问按千token计费
        double inputCost = (promptTokens / 1000.0) * modelInfo.inputPrice;
        double outputCost = (completionTokens / 1000.0) * modelInfo.outputPrice;

        return inputCost + outputCost;
    }

    /**
     * 初始化模型信息
     */
    private Map<String, ModelInfo> initializeModelInfo() {
        Map<String, ModelInfo> models = new ConcurrentHashMap<>();

        // 通义千问系列定价（每千token的价格，单位：人民币）
        models.put("qwen-turbo", new ModelInfo("qwen-turbo", 0.002, 0.006, 8000));
        models.put("qwen-plus", new ModelInfo("qwen-plus", 0.004, 0.012, 32000));
        models.put("qwen-max", new ModelInfo("qwen-max", 0.04, 0.12, 8000));
        models.put("qwen-max-longcontext", new ModelInfo("qwen-max-longcontext", 0.04, 0.12, 30000));

        // 向量模型
        models.put("text-embedding-v1", new ModelInfo("text-embedding-v1", 0.0007, 0.0, 2048));
        models.put("text-embedding-v2", new ModelInfo("text-embedding-v2", 0.0007, 0.0, 2048));

        return models;
    }

    /**
     * 模型信息
     */
    private static class ModelInfo {
        final String name;
        final double inputPrice;   // 每千个输入token的价格(RMB)
        final double outputPrice;  // 每千个输出token的价格(RMB)
        final int maxTokens;      // 最大支持的token数

        ModelInfo(String name, double inputPrice, double outputPrice, int maxTokens) {
            this.name = name;
            this.inputPrice = inputPrice;
            this.outputPrice = outputPrice;
            this.maxTokens = maxTokens;
        }
    }
}