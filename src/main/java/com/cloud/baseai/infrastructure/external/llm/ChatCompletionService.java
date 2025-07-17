package com.cloud.baseai.infrastructure.external.llm;

import com.cloud.baseai.infrastructure.exception.ChatCompletionException;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <h2>聊天完成服务接口</h2>
 *
 * <p>这是AI对话系统与大语言模型(LLM)交互的核心接口。就像一个多语言翻译官，
 * 它能够与不同的AI模型提供商(OpenAI、Claude、Gemini等)进行统一的交互。</p>
 *
 * <p><b>设计理念：</b></p>
 * <p>通过抽象接口设计，我们实现了对不同LLM提供商的统一封装。这样当需要切换
 * 或者集成新的AI模型时，业务逻辑层完全不需要修改，只需要实现对应的接口即可。</p>
 *
 * <p><b>核心功能：</b></p>
 * <ul>
 * <li><b>同步生成：</b>传统的请求-响应模式，适合简单对话场景</li>
 * <li><b>流式生成：</b>实时流式响应，提供更好的用户体验</li>
 * <li><b>模型管理：</b>检查模型可用性和健康状态</li>
 * <li><b>统一抽象：</b>屏蔽不同AI提供商的API差异</li>
 * </ul>
 */
public interface ChatCompletionService {

    /**
     * 生成聊天完成响应
     *
     * <p>这是最基础的对话生成方法。它接收包含上下文、配置参数的请求，
     * 调用AI模型生成回复，并返回包含生成内容、使用统计等信息的完整响应。</p>
     *
     * @param context 包含消息历史、模型配置、知识上下文等信息的完整上下文
     * @return 生成的完成响应，包含回复内容、Token使用量、耗时等统计信息
     * @throws ChatCompletionException 当LLM调用失败时抛出
     */
    ChatCompletionResult generateCompletion(Map<String, Object> context);

    /**
     * 流式生成聊天完成响应
     *
     * <p>流式生成是现代AI对话的标准模式。它不等待完整回复生成完成，
     * 而是实时返回生成的文本片段，让用户能够看到AI的"思考过程"。</p>
     *
     * <p>这种方式特别适合长文本生成、代码编写、复杂问题分析等场景，
     * 能够显著提升用户的交互体验和参与感。</p>
     *
     * @param context 对话上下文信息
     * @param onChunk 处理每个文本片段的回调函数
     * @throws ChatCompletionException 当流式生成过程中发生错误时抛出
     */
    void generateStreamResponse(Map<String, Object> context, Consumer<String> onChunk);

    /**
     * 检查指定模型是否可用
     *
     * <p>在实际业务中，不同的AI模型可能因为维护、升级、配额限制等原因暂时不可用。
     * 这个方法提供了一种优雅的方式来检查模型状态，避免在关键时刻出现服务中断。</p>
     *
     * @param modelCode 模型代码标识符(如: gpt-4o, claude-3.5-sonnet)
     * @return 如果模型当前可用则返回true，否则返回false
     */
    boolean isModelAvailable(String modelCode);

    /**
     * 检查LLM服务整体健康状态
     *
     * <p>健康检查是微服务架构中的重要组成部分。这个方法会验证与LLM提供商的
     * 网络连接、API认证状态、服务响应时间等关键指标。</p>
     *
     * @return 如果LLM服务健康则返回true，否则返回false
     */
    boolean isHealthy();

    /**
     * 获取支持的模型列表
     *
     * <p>不同的LLM提供商支持不同的模型变体。这个方法返回当前服务
     * 实例支持的所有可用模型，用于动态配置和模型选择。</p>
     *
     * @return 支持的模型代码列表
     */
    List<String> getSupportedModels();

    /**
     * 聊天完成结果
     *
     * <p>这个记录类封装了LLM生成的完整响应信息，包括生成的文本内容、
     * 可能的工具调用、使用统计等。它是连接LLM服务和业务逻辑的重要桥梁。</p>
     */
    record ChatCompletionResult(
            String content,           // 生成的回复内容
            String toolCall,          // 工具调用信息(JSON格式)
            Integer tokenIn,          // 输入Token数量
            Integer tokenOut,         // 输出Token数量
            Integer latencyMs,        // 响应延迟(毫秒)
            Double cost              // 本次调用的费用(美元)
    ) {
        /**
         * 创建成功的完成结果
         */
        public static ChatCompletionResult success(String content, Integer tokenIn,
                                                   Integer tokenOut, Integer latencyMs, Double cost) {
            return new ChatCompletionResult(content, null, tokenIn, tokenOut, latencyMs, cost);
        }

        /**
         * 创建包含工具调用的完成结果
         */
        public static ChatCompletionResult withToolCall(String content, String toolCall,
                                                        Integer tokenIn, Integer tokenOut,
                                                        Integer latencyMs, Double cost) {
            return new ChatCompletionResult(content, toolCall, tokenIn, tokenOut, latencyMs, cost);
        }
    }
}