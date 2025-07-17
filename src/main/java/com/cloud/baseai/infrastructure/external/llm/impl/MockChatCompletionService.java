package com.cloud.baseai.infrastructure.external.llm.impl;

import com.cloud.baseai.infrastructure.exception.ChatCompletionException;
import com.cloud.baseai.infrastructure.external.llm.ChatCompletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * <h2>Mock聊天完成服务</h2>
 *
 * <p>这个Mock实现是开发和测试阶段的好伙伴。它模拟真实AI服务的行为，
 * 让开发者可以在没有真实API Key或网络连接的情况下进行开发和调试。</p>
 *
 * <p><b>Mock服务的价值：</b></p>
 * <ul>
 * <li><b>成本控制：</b>开发阶段不消耗真实的API调用额度</li>
 * <li><b>稳定性：</b>不依赖外部网络，测试更稳定可靠</li>
 * <li><b>可控性：</b>可以模拟各种场景，包括错误情况</li>
 * <li><b>速度：</b>响应时间可控，加快开发反馈循环</li>
 * </ul>
 */
public class MockChatCompletionService implements ChatCompletionService {

    private static final Logger log = LoggerFactory.getLogger(MockChatCompletionService.class);

    private final Random random = new Random();

    // 预定义的模拟响应
    private static final List<String> MOCK_RESPONSES = List.of(
            "这是一个模拟的AI回复。在实际开发中，这里会返回真实的AI生成内容。",
            "您好！我是一个模拟的AI助手。我可以帮助您测试对话功能的各种场景。",
            "感谢您的问题。这个Mock服务会根据您的输入生成相应的测试回复，帮助您验证系统功能。",
            "在开发模式下，我会提供一致性的回复来帮助您测试应用的各种功能模块。",
            "这是一个较长的模拟回复，用来测试系统如何处理不同长度的AI响应内容。它包含了足够的文字来验证UI展示效果。"
    );

    @Override
    public ChatCompletionResult generateCompletion(Map<String, Object> context) {
        log.debug("Mock聊天完成服务收到请求: {}", context);

        // 模拟处理延迟
        simulateProcessingDelay();

        // 模拟偶发错误（如果配置启用）
        simulateRandomErrors();

        // 生成模拟响应
        String content = generateMockContent(context);
        int inputTokens = estimateTokens(extractInputContent(context));
        int outputTokens = estimateTokens(content);
        int latency = random.nextInt(500) + 200; // 200-700ms
        double cost = calculateMockCost(inputTokens, outputTokens);

        return ChatCompletionResult.success(content, inputTokens, outputTokens, latency, cost);
    }

    @Override
    public void generateStreamResponse(Map<String, Object> context, Consumer<String> onChunk) {
        log.debug("Mock流式聊天完成服务收到请求: {}", context);

        String fullResponse = generateMockContent(context);

        // 模拟流式发送
        Thread.ofVirtual().start(() -> {
            try {
                for (int i = 0; i < fullResponse.length(); i += 5) {
                    int endIndex = Math.min(i + 5, fullResponse.length());
                    String chunk = fullResponse.substring(i, endIndex);
                    onChunk.accept(chunk);

                    // 模拟网络延迟
                    Thread.sleep(random.nextInt(100) + 50);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Mock流式响应被中断");
            }
        });
    }

    @Override
    public boolean isModelAvailable(String modelCode) {
        // Mock服务支持所有以"mock-"开头的模型
        return modelCode != null && modelCode.startsWith("mock-");
    }

    @Override
    public boolean isHealthy() {
        return true; // Mock服务总是健康的
    }

    @Override
    public List<String> getSupportedModels() {
        return List.of(
                "mock-gpt-4o",
                "mock-gpt-4",
                "mock-gpt-3.5-turbo",
                "mock-claude-3.5-sonnet",
                "mock-claude-3-opus"
        );
    }

    // =================== 私有辅助方法 ===================

    /**
     * 生成模拟内容
     */
    private String generateMockContent(Map<String, Object> context) {
        String inputContent = extractInputContent(context);

        // 根据输入内容生成相应的模拟回复
        if (inputContent.toLowerCase().contains("错误") || inputContent.toLowerCase().contains("error")) {
            return "我理解您遇到了错误。这是一个模拟的错误处理回复，用来测试系统的错误处理流程。";
        }

        if (inputContent.toLowerCase().contains("代码") || inputContent.toLowerCase().contains("code")) {
            return "```java\n// 这是一个模拟的代码示例\npublic class MockExample {\n    public void example() {\n        System.out.println(\"Mock response\");\n    }\n}\n```";
        }

        // 返回随机的模拟响应
        return MOCK_RESPONSES.get(random.nextInt(MOCK_RESPONSES.size()));
    }

    /**
     * 从上下文中提取输入内容
     */
    @SuppressWarnings("unchecked")
    private String extractInputContent(Map<String, Object> context) {
        if (context.containsKey("currentMessage")) {
            return (String) context.get("currentMessage");
        }

        if (context.containsKey("messages")) {
            List<Map<String, Object>> messages = (List<Map<String, Object>>) context.get("messages");
            if (!messages.isEmpty()) {
                Map<String, Object> lastMessage = messages.get(messages.size() - 1);
                return (String) lastMessage.get("content");
            }
        }

        return "默认输入";
    }

    /**
     * 估算Token数量
     */
    private int estimateTokens(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }

        // 简化的Token估算：英文约4字符1个Token，中文约1.5字符1个Token
        int englishChars = (int) content.chars().filter(c -> c < 128).count();
        int chineseChars = content.length() - englishChars;

        return (englishChars / 4) + (chineseChars * 2 / 3);
    }

    /**
     * 计算模拟费用
     */
    private double calculateMockCost(int inputTokens, int outputTokens) {
        // 使用类似GPT-3.5的费用标准进行模拟
        double inputCost = (inputTokens / 1000.0) * 0.0005;
        double outputCost = (outputTokens / 1000.0) * 0.0015;
        return inputCost + outputCost;
    }

    /**
     * 模拟处理延迟
     */
    private void simulateProcessingDelay() {
        try {
            // 模拟200-800ms的处理时间
            Thread.sleep(random.nextInt(600) + 200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 模拟随机错误
     */
    private void simulateRandomErrors() {
        // 5%的概率模拟错误
        if (random.nextDouble() < 0.05) {
            throw new ChatCompletionException("MOCK_ERROR", "模拟的随机错误，用于测试错误处理");
        }
    }
}