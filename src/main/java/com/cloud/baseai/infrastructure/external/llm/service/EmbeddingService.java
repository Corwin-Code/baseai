package com.cloud.baseai.infrastructure.external.llm.service;

import com.cloud.baseai.infrastructure.exception.ChatException;
import com.cloud.baseai.infrastructure.external.llm.model.EmbeddingResult;
import com.cloud.baseai.infrastructure.external.llm.model.ModelInfo;

import java.util.List;

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
 *
 * <p><b>核心功能：</b></p>
 * <ul>
 * <li><b>单文本向量化：</b>将单个文本转换为向量表示</li>
 * <li><b>批量向量化：</b>高效处理大量文本的向量化需求</li>
 * <li><b>模型管理：</b>支持多种嵌入模型的动态切换</li>
 * <li><b>性能优化：</b>内置缓存、重试、限流等机制</li>
 * </ul>
 */
public interface EmbeddingService {

    /**
     * 生成文本的向量嵌入
     *
     * <p>这个方法是整个向量化过程的核心。它将人类可读的文本转换为AI可处理的数字向量。
     * 这个过程类似于将文字"翻译"成机器的"语言"，让计算机能够理解文本的语义含义。</p>
     *
     * @param text      要向量化的文本内容，不能为空
     * @param modelCode 使用的AI模型标识，如"text-embedding-3-small"
     * @return 向量数组，通常是1536维的浮点数数组
     * @throws ChatException 当向量化失败时抛出
     */
    float[] generateEmbedding(String text, String modelCode);

    /**
     * 批量生成向量嵌入
     *
     * <p>批量处理能够显著提高效率，特别是在处理大量文档时。
     * 许多AI服务提供商都支持批量API，能够减少网络请求次数和延迟。</p>
     *
     * @param texts     文本列表，不能为空
     * @param modelCode 模型标识
     * @return 向量列表，与输入文本顺序对应
     * @throws ChatException 当批量向量化失败时抛出
     */
    List<float[]> generateEmbeddings(java.util.List<String> texts, String modelCode);

    /**
     * 生成带详细信息的向量嵌入结果
     *
     * <p>相比于基础的向量生成方法，这个方法返回更丰富的信息，
     * 包括消耗的token数量、处理时间、费用等，便于监控和成本控制。</p>
     *
     * @param text      要向量化的文本内容
     * @param modelCode 使用的AI模型标识
     * @return 包含向量和元数据的完整结果
     */
    EmbeddingResult generateEmbeddingWithDetails(String text, String modelCode);

    /**
     * 批量生成带详细信息的向量嵌入结果
     *
     * @param texts     文本列表
     * @param modelCode 模型标识
     * @return 包含向量和元数据的完整结果列表
     */
    List<EmbeddingResult> generateEmbeddingsWithDetails(List<String> texts, String modelCode);

    /**
     * 检查模型是否可用
     *
     * <p>在生产环境中，不同的模型可能因为维护、配额限制等原因不可用。
     * 这个方法提供了检查模型状态的能力。</p>
     *
     * @param modelCode 模型标识
     * @return 是否可用
     */
    boolean isModelAvailable(String modelCode);

    /**
     * 获取模型的向量维度
     *
     * <p>不同的嵌入模型可能产生不同维度的向量，这个信息对于
     * 向量数据库的设计和查询优化非常重要。</p>
     *
     * @param modelCode 模型标识
     * @return 向量维度，如1536
     */
    int getVectorDimension(String modelCode);

    /**
     * 获取模型详细信息
     *
     * <p>提供模型的完整信息，包括维度、最大token数、费用等，
     * 用于智能模型选择和成本估算。</p>
     *
     * @param modelCode 模型标识
     * @return 模型信息，如果模型不存在则返回null
     */
    ModelInfo getModelInfo(String modelCode);

    /**
     * 获取支持的模型列表
     *
     * @return 支持的模型代码列表
     */
    List<String> getSupportedModels();

    /**
     * 检查服务健康状态
     *
     * @return 如果服务健康则返回true
     */
    boolean isHealthy();

    /**
     * 预热模型
     *
     * <p>对于某些AI服务，第一次调用可能比较慢。预热功能可以
     * 在系统启动时提前调用模型，减少用户的等待时间。</p>
     *
     * @param modelCode 要预热的模型
     * @return 是否预热成功
     */
    boolean warmupModel(String modelCode);

    /**
     * 估算文本的token消耗
     *
     * <p>在调用API之前估算token消耗，用于成本控制和批量大小优化。</p>
     *
     * @param text      要估算的文本
     * @param modelCode 模型标识
     * @return 估算的token数量
     */
    int estimateTokenCount(String text, String modelCode);
}