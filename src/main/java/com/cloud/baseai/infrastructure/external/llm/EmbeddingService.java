package com.cloud.baseai.infrastructure.external.llm;

import com.cloud.baseai.infrastructure.exception.EmbeddingGenerationException;

/**
 * <h2>向量嵌入服务接口</h2>
 *
 * <p>这个接口定义了与外部AI模型服务的交互契约。在实际的RAG系统中，
 * 向量生成是一个计算密集型的任务，通常需要调用外部的AI服务（如OpenAI、百度、阿里云等）
 * 来完成文本到向量的转换。</p>
 *
 * <p><b>设计原则：</b></p>
 * <p>我们将这个功能抽象为接口，是为了支持多种AI服务提供商。不同的部署环境可能使用
 * 不同的AI服务，通过接口抽象，我们可以轻松切换底层实现而不影响上层业务逻辑。</p>
 */
public interface EmbeddingService {

    /**
     * 生成文本的向量嵌入
     *
     * <p>这个方法是整个向量化过程的核心。它将人类可读的文本转换为AI可处理的数字向量。
     * 这个过程类似于将文字"翻译"成机器的"语言"，让计算机能够理解文本的语义含义。</p>
     *
     * @param text      要向量化的文本内容
     * @param modelCode 使用的AI模型标识
     * @return 向量数组，通常是1536维的浮点数数组
     * @throws EmbeddingGenerationException 当向量生成失败时抛出
     */
    float[] generateEmbedding(String text, String modelCode);

    /**
     * 批量生成向量嵌入
     *
     * <p>批量处理能够显著提高效率，特别是在处理大量文档时。
     * 许多AI服务提供商都支持批量API，能够减少网络请求次数和延迟。</p>
     *
     * @param texts     文本列表
     * @param modelCode 模型标识
     * @return 向量列表，与输入文本顺序对应
     */
    java.util.List<float[]> generateEmbeddings(java.util.List<String> texts, String modelCode);

    /**
     * 检查模型是否可用
     *
     * @param modelCode 模型标识
     * @return 是否可用
     */
    boolean isModelAvailable(String modelCode);

    /**
     * 获取模型的向量维度
     *
     * @param modelCode 模型标识
     * @return 向量维度
     */
    int getVectorDimension(String modelCode);
}