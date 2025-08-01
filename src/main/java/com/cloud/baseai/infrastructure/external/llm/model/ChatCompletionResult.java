package com.cloud.baseai.infrastructure.external.llm.model;

/**
 * 聊天完成结果
 *
 * <p>这个记录类封装了LLM生成的完整响应信息，包括生成的文本内容、
 * 可能的工具调用、使用统计等。它是连接LLM服务和业务逻辑的重要桥梁。</p>
 */
public record ChatCompletionResult(
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