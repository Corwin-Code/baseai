package com.cloud.baseai.infrastructure.external.llm;

import com.cloud.baseai.infrastructure.external.llm.factory.ChatModelFactory;
import com.cloud.baseai.infrastructure.external.llm.model.ChatCompletionResult;
import com.cloud.baseai.infrastructure.external.llm.service.ChatCompletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>多模型使用示例</h2>
 *
 * <p>展示如何在实际应用中使用多个大语言模型，包括OpenAI、Claude和通义千问。
 * 演示了模型选择、故障转移、特性对比等功能。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiModelTest {

    private final ChatModelFactory chatModelFactory;

    /**
     * 示例1：基础模型对比
     *
     * <p>使用相同的问题测试不同模型的回答风格和质量</p>
     */
    public void compareModels() {
        log.info("\n=== 多模型对比示例 ===\n");

        String question = "请用简洁的语言解释什么是量子计算，并举一个实际应用的例子。";

        // 测试OpenAI GPT-3.5
        testModel("gpt-3.5-turbo", question, "OpenAI GPT-3.5-Turbo");

        // 测试Claude
        testModel("claude-3-haiku-20240307", question, "Claude 3 Haiku");

        // 测试通义千问
        testModel("qwen-turbo", question, "通义千问 Turbo");
    }

    /**
     * 测试单个模型
     */
    private void testModel(String modelCode, String question, String modelName) {
        try {
            log.info("--- {} 的回答 ---", modelName);

            Map<String, Object> context = new HashMap<>();
            context.put("model", modelCode);
            context.put("currentMessage", question);
            context.put("temperature", 0.7);
            context.put("maxTokens", 500);

            ChatCompletionService service = chatModelFactory.getServiceForModel(modelCode);
            ChatCompletionResult result = service.generateCompletion(context);

            log.info("回答: {}", result.content());
            log.info("统计: tokens={}/{}, latency={}ms, cost=${}",
                    result.tokenIn(), result.tokenOut(), result.latencyMs(), result.cost());
            log.info("");

        } catch (Exception e) {
            log.error("{} 调用失败: {}", modelName, e.getMessage());
        }
    }

    /**
     * 示例2：根据场景选择最合适的模型
     */
    public void scenarioBasedModelSelection() {
        log.info("\n=== 场景化模型选择示例 ===\n");

        // 场景1：需要高质量推理的复杂问题 - 使用Claude或GPT-4
        complexReasoningScenario();

        // 场景2：中文内容生成 - 使用通义千问
        chineseContentScenario();

        // 场景3：代码生成 - 使用GPT-4或Claude
        codeGenerationScenario();

        // 场景4：快速响应的简单查询 - 使用GPT-3.5-Turbo或Qwen-Turbo
        quickResponseScenario();
    }

    /**
     * 复杂推理场景
     */
    private void complexReasoningScenario() {
        log.info("场景1：复杂推理问题");

        String problem = "一个水池有三个水管，A管单独开需要4小时注满，B管单独开需要6小时注满，" +
                "C管是排水管，单独开需要8小时排空满池的水。如果三管同时开，需要多久注满水池？" +
                "请详细说明解题步骤。";

        // 优先使用Claude进行推理
        List<String> preferredModels = Arrays.asList(
                "claude-3-sonnet-20240229",  // Claude最擅长推理
                "gpt-4",                      // GPT-4作为备选
                "qwen-max"                    // 通义千问Max作为第三选择
        );

        executeWithModelPreference(problem, preferredModels, "复杂推理");
    }

    /**
     * 中文内容生成场景
     */
    private void chineseContentScenario() {
        log.info("\n场景2：中文内容生成");

        String request = "请写一首关于春天的七言律诗，要求押韵工整，意境优美。";

        // 优先使用通义千问（中文优化）
        List<String> preferredModels = Arrays.asList(
                "qwen-plus",                  // 通义千问在中文方面表现优秀
                "qwen-max",                   // 更高级的通义千问模型
                "gpt-4"                       // GPT-4作为备选
        );

        executeWithModelPreference(request, preferredModels, "中文诗词创作");
    }

    /**
     * 代码生成场景
     */
    private void codeGenerationScenario() {
        log.info("\n场景3：代码生成");

        String request = "请实现一个Python函数，用于检测一个字符串是否是有效的括号序列。" +
                "支持三种括号：()、[]、{}。要包含详细注释和测试用例。";

        // 代码生成优先使用GPT-4或Claude
        List<String> preferredModels = Arrays.asList(
                "gpt-4",                      // GPT-4擅长代码生成
                "claude-3-opus-20240229",     // Claude Opus同样优秀
                "qwen-max"                    // 通义千问作为备选
        );

        executeWithModelPreference(request, preferredModels, "代码生成");
    }

    /**
     * 快速响应场景
     */
    private void quickResponseScenario() {
        log.info("\n场景4：快速简单查询");

        String query = "北京到上海的距离大约是多少公里？";

        // 简单查询使用快速、经济的模型
        List<String> preferredModels = Arrays.asList(
                "gpt-3.5-turbo",              // 快速且经济
                "qwen-turbo",                 // 通义千问Turbo同样快速
                "claude-3-haiku-20240307"     // Claude Haiku轻量快速
        );

        executeWithModelPreference(query, preferredModels, "快速查询");
    }

    /**
     * 使用模型偏好执行
     */
    private void executeWithModelPreference(String prompt, List<String> preferredModels, String scenario) {
        Map<String, Object> context = new HashMap<>();
        context.put("currentMessage", prompt);
        context.put("temperature", 0.7);
        context.put("maxTokens", 1000);

        try {
            // 使用故障转移执行
            ChatCompletionResult result = chatModelFactory.executeWithFailover(context, preferredModels);

            log.info("{} - 使用模型: {}", scenario, context.get("model"));
            log.info("响应: {}", result.content());
            log.info("");

        } catch (Exception e) {
            log.error("{} 执行失败: {}", scenario, e.getMessage());
        }
    }

    /**
     * 示例3：流式响应对比
     */
    public void streamingComparison() {
        log.info("\n=== 流式响应对比示例 ===\n");

        String prompt = "请讲一个有趣的科技发展小故事，大约200字。";

        // 测试不同模型的流式响应
        testStreamingModel("gpt-3.5-turbo", prompt, "OpenAI");
        testStreamingModel("claude-3-haiku-20240307", prompt, "Claude");
        testStreamingModel("qwen-turbo", prompt, "通义千问");
    }

    /**
     * 测试流式响应
     */
    private void testStreamingModel(String modelCode, String prompt, String modelName) {
        log.info("--- {} 流式响应测试 ---", modelName);

        try {
            Map<String, Object> context = new HashMap<>();
            context.put("model", modelCode);
            context.put("currentMessage", prompt);

            ChatCompletionService service = chatModelFactory.getServiceForModel(modelCode);

            StringBuilder fullResponse = new StringBuilder();
            long startTime = System.currentTimeMillis();

            service.generateStreamResponse(context, chunk -> {
                System.out.print(chunk); // 实时打印
                fullResponse.append(chunk);
            });

            // 等待流式响应完成
            Thread.sleep(5000);

            long endTime = System.currentTimeMillis();
            log.info("\n流式响应完成，总耗时: {}ms", endTime - startTime);
            log.info("");

        } catch (Exception e) {
            log.error("{} 流式响应测试失败: {}", modelName, e.getMessage());
        }
    }

    /**
     * 示例4：特殊功能展示
     */
    public void specialFeatures() {
        log.info("\n=== 特殊功能展示 ===\n");

        // Claude的思考模式
        claudeThinkingMode();

        // 通义千问的搜索增强
        qwenSearchEnhanced();

        // 多模态支持（如果启用）
        multiModalExample();
    }

    /**
     * Claude思考模式示例
     */
    private void claudeThinkingMode() {
        log.info("--- Claude思考模式 ---");

        try {
            Map<String, Object> context = new HashMap<>();
            context.put("model", "claude-3-7-sonnet-latest");
            context.put("currentMessage", "证明：对于任意正整数n，1+2+3+...+n = n(n+1)/2");
            context.put("enableThinking", true); // 启用思考模式
            context.put("maxTokens", 2000);

            ChatCompletionService service = chatModelFactory.getServiceForModel("claude-3-7-sonnet-latest");
            ChatCompletionResult result = service.generateCompletion(context);

            log.info("Claude的思考过程和答案:\n{}", result.content());

        } catch (Exception e) {
            log.error("Claude思考模式示例失败: {}", e.getMessage());
        }
    }

    /**
     * 通义千问搜索增强示例
     */
    private void qwenSearchEnhanced() {
        log.info("\n--- 通义千问搜索增强 ---");

        try {
            Map<String, Object> context = new HashMap<>();
            context.put("model", "qwen-plus");
            context.put("currentMessage", "2024年诺贝尔物理学奖获得者是谁？他们的主要贡献是什么？");
            context.put("enableSearch", true); // 启用搜索增强

            ChatCompletionService service = chatModelFactory.getServiceForModel("qwen-plus");
            ChatCompletionResult result = service.generateCompletion(context);

            log.info("通义千问搜索增强结果:\n{}", result.content());

        } catch (Exception e) {
            log.error("通义千问搜索增强示例失败: {}", e.getMessage());
        }
    }

    /**
     * 多模态示例（图像理解）
     */
    private void multiModalExample() {
        log.info("\n--- 多模态功能示例 ---");
        log.info("Claude和GPT-4支持图像理解，但需要特殊的消息格式");
        log.info("这里仅作为示例说明，实际使用需要按照各平台的API要求构建消息");
    }

    /**
     * 示例5：成本和性能统计
     */
    public void costAndPerformanceAnalysis() {
        log.info("\n=== 成本和性能分析 ===\n");

        String testPrompt = "写一段100字左右的产品介绍。";
        Map<String, ModelStats> statsMap = new HashMap<>();

        // 测试所有可用模型
        List<String> testModels = Arrays.asList(
                "gpt-3.5-turbo",
                "gpt-4",
                "claude-3-haiku-20240307",
                "claude-3-sonnet-20240229",
                "qwen-turbo",
                "qwen-plus"
        );

        for (String model : testModels) {
            try {
                ModelStats stats = testModelPerformance(model, testPrompt);
                statsMap.put(model, stats);
            } catch (Exception e) {
                log.debug("模型 {} 测试失败: {}", model, e.getMessage());
            }
        }

        // 输出统计结果
        log.info("性能和成本统计报告:");
        log.info("模型名称 | 响应时间(ms) | 输入Token | 输出Token | 成本");
        log.info("---------|--------------|-----------|-----------|------");

        statsMap.forEach((model, stats) -> {
            log.info("{} | {} | {} | {} | ${}",
                    String.format("%-20s", model),
                    String.format("%12d", stats.latency),
                    String.format("%9d", stats.inputTokens),
                    String.format("%9d", stats.outputTokens),
                    String.format("%.6f", stats.cost)
            );
        });
    }

    /**
     * 测试模型性能
     */
    private ModelStats testModelPerformance(String model, String prompt) {
        Map<String, Object> context = new HashMap<>();
        context.put("model", model);
        context.put("currentMessage", prompt);
        context.put("temperature", 0.7);
        context.put("maxTokens", 200);

        ChatCompletionService service = chatModelFactory.getServiceForModel(model);
        ChatCompletionResult result = service.generateCompletion(context);

        return new ModelStats(
                result.latencyMs(),
                result.tokenIn(),
                result.tokenOut(),
                result.cost()
        );
    }

    /**
     * 模型统计信息
     */
    private record ModelStats(int latency, int inputTokens, int outputTokens, double cost) {
    }
}