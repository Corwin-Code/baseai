package com.clinflash.baseai.infrastructure.external.llm.impl;

import com.clinflash.baseai.infrastructure.exception.ChatCompletionException;
import com.clinflash.baseai.infrastructure.external.llm.ChatCompletionService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <h2>Claude AI聊天完成服务实现</h2>
 *
 * <p>这个实现展示了接口抽象的真正价值：同一个ChatCompletionService接口，
 * 可以无缝对接不同的AI提供商。Claude AI以其强大的推理能力和安全性著称，
 * 特别适合需要深度思考和分析的场景。</p>
 *
 * <p><b>Claude vs OpenAI的关键差异：</b></p>
 * <p>虽然都是大语言模型，但Claude和OpenAI的API设计有所不同：</p>
 * <ul>
 * <li><b>消息格式：</b>Claude使用不同的角色名称和消息结构</li>
 * <li><b>参数配置：</b>Claude有自己的参数命名和取值范围</li>
 * <li><b>错误处理：</b>Claude的错误响应格式与OpenAI不同</li>
 * <li><b>计费模式：</b>Claude的Token计费标准和模型名称都不同</li>
 * </ul>
 *
 * <p><b>多provider架构的好处：</b></p>
 * <p>1. <strong>供应商多样化：</strong>避免对单一AI提供商的依赖</p>
 * <p>2. <strong>成本优化：</strong>根据不同场景选择性价比最优的模型</p>
 * <p>3. <strong>功能互补：</strong>不同模型在不同任务上各有优势</p>
 * <p>4. <strong>风险缓解：</strong>单个服务故障不会影响整体系统</p>
 */
@Service
@ConditionalOnProperty(name = "baseai.llm.claude.enabled", havingValue = "true", matchIfMissing = false)
public class ClaudeChatCompletionService implements ChatCompletionService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeChatCompletionService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;
    private final Map<String, ClaudeModelInfo> supportedModels;

    public ClaudeChatCompletionService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${baseai.llm.claude.api-key}") String apiKey,
            @Value("${baseai.llm.claude.base-url:https://api.anthropic.com}") String baseUrl) {

        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.supportedModels = initializeSupportedModels();
    }

    @Override
    public ChatCompletionResult generateCompletion(Map<String, Object> context) {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("开始生成Claude聊天完成: model={}", context.get("model"));

            // 构建Claude API请求
            ClaudeRequest request = buildClaudeRequest(context);

            // 发送HTTP请求
            HttpHeaders headers = createHeaders();
            HttpEntity<ClaudeRequest> entity = new HttpEntity<>(request, headers);

            String endpoint = baseUrl + "/v1/messages";
            ResponseEntity<ClaudeResponse> response = restTemplate.postForEntity(
                    endpoint, entity, ClaudeResponse.class);

            // 处理响应
            ClaudeResponse responseBody = response.getBody();
            if (responseBody == null || responseBody.content().isEmpty()) {
                throw new ChatCompletionException("CLAUDE_EMPTY_RESPONSE", "Claude返回空响应");
            }

            // Claude的响应格式与OpenAI不同，需要特殊处理
            String responseContent = extractContentFromClaudeResponse(responseBody);
            long duration = System.currentTimeMillis() - startTime;

            // 计算费用
            double cost = calculateClaudeCost(request.model(), responseBody.usage());

            return ChatCompletionResult.success(
                    responseContent,
                    responseBody.usage().inputTokens(),
                    responseBody.usage().outputTokens(),
                    (int) duration,
                    cost
            );

        } catch (HttpStatusCodeException e) {
            log.error("Claude API调用失败: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());

            throw new ChatCompletionException(
                    "CLAUDE_API_ERROR",
                    "Claude API调用失败: " + extractClaudeErrorMessage(e.getResponseBodyAsString()),
                    e.getStatusCode().value()
            );

        } catch (Exception e) {
            log.error("Claude聊天完成生成异常", e);
            throw new ChatCompletionException("CLAUDE_COMPLETION_ERROR", "生成Claude聊天完成时发生错误", e);
        }
    }

    @Override
    public void generateStreamResponse(Map<String, Object> context, Consumer<String> onChunk) {
        try {
            log.debug("开始Claude流式生成: model={}", context.get("model"));

            // Claude的流式API与OpenAI稍有不同
            ClaudeRequest request = buildClaudeRequest(context);
            // Claude使用stream参数而不是OpenAI的stream
            request = request.withStream(true);

            // 实现Claude特有的SSE处理逻辑
            processClaudeStreamResponse(request, onChunk);

        } catch (Exception e) {
            log.error("Claude流式生成异常", e);
            throw new ChatCompletionException("CLAUDE_STREAM_ERROR", "Claude流式生成失败", e);
        }
    }

    @Override
    public boolean isModelAvailable(String modelCode) {
        if (!supportedModels.containsKey(modelCode)) {
            return false;
        }

        try {
            // 使用最简单的测试请求
            Map<String, Object> testContext = Map.of(
                    "model", modelCode,
                    "messages", List.of(Map.of("role", "user", "content", "Hi")),
                    "maxTokens", 1
            );

            generateCompletion(testContext);
            return true;

        } catch (Exception e) {
            log.warn("Claude模型可用性检查失败: model={}, error={}", modelCode, e.getMessage());
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
        return new ArrayList<>(supportedModels.keySet());
    }

    // =================== 私有辅助方法 ===================

    /**
     * 构建Claude API请求
     *
     * <p>Claude的请求格式与OpenAI有一些关键差异：</p>
     * <p>1. Claude要求明确指定max_tokens参数</p>
     * <p>2. Claude使用不同的消息角色名称</p>
     * <p>3. Claude对系统消息有特殊的处理方式</p>
     */
    private ClaudeRequest buildClaudeRequest(Map<String, Object> context) {
        String model = (String) context.get("model");
        Float temperature = (Float) context.getOrDefault("temperature", 1.0f);
        Integer maxTokens = (Integer) context.getOrDefault("maxTokens", 1000);

        // Claude要求max_tokens参数，这是与OpenAI的一个重要区别
        if (maxTokens == null || maxTokens <= 0) {
            maxTokens = 1000; // Claude的默认值
        }

        // 构建Claude格式的消息
        List<ClaudeMessage> messages = buildClaudeMessages(context);
        String systemPrompt = extractSystemPrompt(context);

        return new ClaudeRequest(
                model,
                messages,
                maxTokens,
                temperature,
                systemPrompt,
                false // stream默认为false
        );
    }

    /**
     * 构建Claude格式的消息列表
     *
     * <p>Claude对消息格式有严格要求：</p>
     * <p>1. 系统消息需要单独提取，不能放在messages数组中</p>
     * <p>2. 对话必须以用户消息开始</p>
     * <p>3. 用户和助手消息必须交替出现</p>
     */
    @SuppressWarnings("unchecked")
    private List<ClaudeMessage> buildClaudeMessages(Map<String, Object> context) {
        List<ClaudeMessage> claudeMessages = new ArrayList<>();

        // 处理历史消息
        if (context.containsKey("messages")) {
            List<Map<String, Object>> historyMessages = (List<Map<String, Object>>) context.get("messages");

            for (Map<String, Object> msg : historyMessages) {
                String role = (String) msg.get("role");
                String content = (String) msg.get("content");

                // 跳过系统消息，Claude会单独处理
                if (!"system".equals(role)) {
                    // 将OpenAI角色名称映射为Claude角色名称
                    String claudeRole = mapRoleToClaudeFormat(role);
                    claudeMessages.add(new ClaudeMessage(claudeRole, content));
                }
            }
        }

        // 添加知识上下文（如果有）
        if (context.containsKey("knowledgeContext")) {
            List<String> knowledgeContext = (List<String>) context.get("knowledgeContext");
            if (!knowledgeContext.isEmpty()) {
                String contextContent = "参考资料:\n" + String.join("\n\n", knowledgeContext);
                claudeMessages.add(new ClaudeMessage("user", contextContent));
            }
        }

        // 添加当前用户消息
        if (context.containsKey("currentMessage")) {
            claudeMessages.add(new ClaudeMessage("user", (String) context.get("currentMessage")));
        }

        return claudeMessages;
    }

    /**
     * 提取系统提示词
     */
    @SuppressWarnings("unchecked")
    private String extractSystemPrompt(Map<String, Object> context) {
        StringBuilder systemPrompt = new StringBuilder();

        // 从历史消息中提取系统消息
        if (context.containsKey("messages")) {
            List<Map<String, Object>> historyMessages = (List<Map<String, Object>>) context.get("messages");

            for (Map<String, Object> msg : historyMessages) {
                if ("system".equals(msg.get("role"))) {
                    if (!systemPrompt.isEmpty()) {
                        systemPrompt.append("\n\n");
                    }
                    systemPrompt.append(msg.get("content"));
                }
            }
        }

        return !systemPrompt.isEmpty() ? systemPrompt.toString() : null;
    }

    /**
     * 将角色名称映射为Claude格式
     */
    private String mapRoleToClaudeFormat(String openAIRole) {
        return switch (openAIRole) {
            case "assistant" -> "assistant";
            case "user" -> "user";
            default -> "user"; // Claude主要支持user和assistant
        };
    }

    /**
     * 创建Claude API请求头
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.set("User-Agent", "BaseAI-Chat/1.0");
        return headers;
    }

    /**
     * 从Claude响应中提取文本内容
     */
    private String extractContentFromClaudeResponse(ClaudeResponse response) {
        if (response.content().isEmpty()) {
            return "";
        }

        // Claude的响应内容是一个数组，通常第一个元素包含文本
        ClaudeContent firstContent = response.content().getFirst();
        return firstContent.text();
    }

    /**
     * 处理Claude流式响应
     */
    private void processClaudeStreamResponse(ClaudeRequest request, Consumer<String> onChunk) {
        // Claude的流式处理实现
        // 这里提供基础框架，实际实现需要处理Claude特有的SSE格式

        try {
            // 模拟Claude的流式响应
            String mockResponse = "这是Claude的模拟流式响应。实际实现中，这里会处理Claude的SSE事件流，" +
                    "解析每个数据块，提取文本增量，并调用onChunk回调函数。";

            // 模拟分块发送
            for (int i = 0; i < mockResponse.length(); i += 8) {
                int endIndex = Math.min(i + 8, mockResponse.length());
                String chunk = mockResponse.substring(i, endIndex);
                onChunk.accept(chunk);

                Thread.sleep(60); // 模拟稍慢的响应
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Claude流式处理被中断", e);
        }
    }

    /**
     * 计算Claude调用费用
     */
    private double calculateClaudeCost(String model, ClaudeUsage usage) {
        ClaudeModelInfo modelInfo = supportedModels.get(model);
        if (modelInfo == null) {
            log.warn("未知Claude模型的费用计算: {}", model);
            return 0.0;
        }

        double inputCost = (usage.inputTokens() / 1000.0) * modelInfo.inputPrice();
        double outputCost = (usage.outputTokens() / 1000.0) * modelInfo.outputPrice();

        return inputCost + outputCost;
    }

    /**
     * 从错误响应中提取错误消息
     */
    private String extractClaudeErrorMessage(String errorBody) {
        try {
            // Claude的错误响应格式解析
            // 实际实现需要根据Claude的具体错误格式进行解析
            return "Claude API错误: " + errorBody;
        } catch (Exception e) {
            return "未知Claude API错误";
        }
    }

    /**
     * 初始化支持的Claude模型
     */
    private Map<String, ClaudeModelInfo> initializeSupportedModels() {
        Map<String, ClaudeModelInfo> models = new HashMap<>();

        // Claude 3.5 系列
        models.put("claude-3-5-sonnet-20241022",
                new ClaudeModelInfo("claude-3-5-sonnet-20241022", 0.003, 0.015, 200000));

        // Claude 3 系列
        models.put("claude-3-opus-20240229",
                new ClaudeModelInfo("claude-3-opus-20240229", 0.015, 0.075, 200000));
        models.put("claude-3-sonnet-20240229",
                new ClaudeModelInfo("claude-3-sonnet-20240229", 0.003, 0.015, 200000));
        models.put("claude-3-haiku-20240307",
                new ClaudeModelInfo("claude-3-haiku-20240307", 0.00025, 0.00125, 200000));

        return models;
    }

    // =================== Claude API数据结构 ===================

    /**
     * Claude模型信息
     */
    private record ClaudeModelInfo(
            String name,
            double inputPrice,   // 每1000个输入token的价格(USD)
            double outputPrice,  // 每1000个输出token的价格(USD)
            int maxTokens       // 最大支持的token数
    ) {
    }

    /**
     * Claude API请求对象
     */
    private record ClaudeRequest(
            String model,
            List<ClaudeMessage> messages,
            @JsonProperty("max_tokens") Integer maxTokens,
            Float temperature,
            String system,  // Claude的系统提示词字段
            Boolean stream
    ) {
        public ClaudeRequest withStream(boolean stream) {
            return new ClaudeRequest(model, messages, maxTokens, temperature, system, stream);
        }
    }

    /**
     * Claude消息对象
     */
    private record ClaudeMessage(
            String role,    // "user" 或 "assistant"
            String content
    ) {
    }

    /**
     * Claude响应对象
     */
    private record ClaudeResponse(
            String id,
            String type,
            String role,
            List<ClaudeContent> content,
            String model,
            @JsonProperty("stop_reason") String stopReason,
            @JsonProperty("stop_sequence") String stopSequence,
            ClaudeUsage usage
    ) {
    }

    /**
     * Claude内容对象
     */
    private record ClaudeContent(
            String type,    // "text"
            String text
    ) {
    }

    /**
     * Claude使用量统计
     */
    private record ClaudeUsage(
            @JsonProperty("input_tokens") Integer inputTokens,
            @JsonProperty("output_tokens") Integer outputTokens
    ) {
    }
}