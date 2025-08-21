package com.cloud.baseai.infrastructure.external.llm.service;

import com.cloud.baseai.infrastructure.config.properties.KnowledgeBaseProperties;
import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.cloud.baseai.infrastructure.exception.ChatException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.external.llm.model.EmbeddingResult;
import com.cloud.baseai.infrastructure.external.llm.model.ModelInfo;
import com.cloud.baseai.infrastructure.external.llm.model.ModelStats;
import com.cloud.baseai.infrastructure.utils.KbUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h2>OpenAI向量嵌入服务</h2>
 *
 * <p>基于Spring AI框架的OpenAI嵌入服务实现。该服务专注于将文本转换为高质量的向量表示，
 * 为RAG(检索增强生成)系统提供核心的语义理解能力。</p>
 *
 * <p><b>技术特点：</b></p>
 * <ul>
 * <li><b>Spring AI集成：</b>充分利用Spring AI的抽象能力和生态系统</li>
 * <li><b>多模型支持：</b>支持OpenAI的多种嵌入模型，自动选择最优模型</li>
 * <li><b>智能缓存：</b>基于内容哈希的缓存机制，避免重复计算</li>
 * <li><b>批量优化：</b>支持批量处理，显著提升大量文本的处理效率</li>
 * <li><b>故障恢复：</b>内置重试机制和降级策略，保证服务稳定性</li>
 * </ul>
 *
 * <p><b>应用场景：</b></p>
 * <ul>
 * <li>知识库文档向量化</li>
 * <li>语义搜索和推荐</li>
 * <li>文本相似度计算</li>
 * <li>内容分类和聚类</li>
 * </ul>
 */
@Service
@ConditionalOnBean(OpenAiEmbeddingModel.class)
public class OpenAIEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIEmbeddingService.class);

    /**
     * 支持的模型及其配置信息
     */
    private static final Map<String, ModelInfo> SUPPORTED_MODELS = initializeSupportedModels();

    private final LlmProperties llmProperties;
    private final KnowledgeBaseProperties kbProperties;
    private final EmbeddingModel embeddingModel;

    /**
     * 向量缓存，基于文本内容哈希
     */
    private final Cache<String, float[]> vectorCache;

    /**
     * 模型性能统计
     */
    private final Map<String, ModelStats> modelStatsMap = new ConcurrentHashMap<>();


    /**
     * 构造函数，初始化OpenAI嵌入服务
     *
     * @param llmProperties  LLM配置属性
     * @param kbProperties   知识库配置属性
     * @param embeddingModel 嵌入模型
     */
    public OpenAIEmbeddingService(LlmProperties llmProperties,
                                  KnowledgeBaseProperties kbProperties,
                                  @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        this.llmProperties = llmProperties;
        this.kbProperties = kbProperties;
        this.embeddingModel = embeddingModel;
        this.vectorCache = createVectorCache();

        log.info("OpenAI嵌入服务初始化完成: baseUrl={}, 默认模型={}, 缓存启用={}",
                llmProperties.getOpenai().getBaseUrl(),
                kbProperties.getEmbedding().getDefaultModel(),
                llmProperties.getFeatures().getEnableResponseCache());
    }

    @Override
    public float[] generateEmbedding(String text, String modelCode) {
        // 输入验证
        String cleanedText = KbUtils.cleanText(text);
        String validatedModel = getValidatedModel(modelCode);

        // 检查token限制
        validateTokenLimit(cleanedText, validatedModel);

        // 尝试从缓存获取
        if (llmProperties.getFeatures().getEnableResponseCache()) {
            String cacheKey = generateCacheKey(cleanedText, validatedModel);
            float[] cached = vectorCache.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("向量缓存命中: model={}, textLength={}", validatedModel, cleanedText.length());
                return cached;
            }
        }

        long startTime = System.currentTimeMillis();

        try {
            // 创建嵌入请求
            EmbeddingRequest request = createEmbeddingRequest(cleanedText, validatedModel);

            // 调用Spring AI嵌入模型
            EmbeddingResponse response = embeddingModel.call(request);

            // 提取向量
            float[] embedding = extractEmbedding(response);

            // 计算统计信息
            long latency = System.currentTimeMillis() - startTime;
            updateModelStats(validatedModel, 1, latency);

            // 缓存结果
            if (llmProperties.getFeatures().getEnableResponseCache()) {
                String cacheKey = generateCacheKey(cleanedText, validatedModel);
                vectorCache.put(cacheKey, embedding);
            }

            log.debug("向量生成完成: model={}, dimension={}, latency={}ms",
                    validatedModel, embedding.length, latency);

            return embedding;

        } catch (Exception e) {
            log.error("向量生成失败: model={}, textLength={}, error={}",
                    validatedModel, cleanedText.length(), e.getMessage());
            throw convertException(e, validatedModel);
        }
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts, String modelCode) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        String validatedModel = getValidatedModel(modelCode);

        // 验证批量大小
        int batchSize = kbProperties.getEmbedding().getBatchSize();
        if (texts.size() > batchSize) {
            log.info("文本数量超过批量限制，将分批处理: count={}, batchSize={}", texts.size(), batchSize);
            return processBatchesSequentially(texts, validatedModel);
        }

        // 清理文本并检查缓存
        List<String> cleanedTexts = texts.stream()
                .map(KbUtils::cleanText)
                .toList();

        return processSingleBatch(cleanedTexts, validatedModel);
    }

    @Override
    public EmbeddingResult generateEmbeddingWithDetails(String text, String modelCode) {
        long startTime = System.currentTimeMillis();
        String cleanedText = KbUtils.cleanText(text);
        String validatedModel = getValidatedModel(modelCode);

        try {
            // 生成向量
            float[] embedding = generateEmbedding(cleanedText, validatedModel);

            // 计算详细信息
            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            int tokenCount = estimateTokenCount(cleanedText, validatedModel);
            double cost = calculateCost(validatedModel, tokenCount);

            log.debug("向量生成详情: model={}, tokens={}, latency={}ms, cost=${}",
                    validatedModel, tokenCount, latencyMs, cost);

            return EmbeddingResult.success(embedding, validatedModel, tokenCount, latencyMs, cost, "openai");

        } catch (Exception e) {
            log.error("详细向量生成失败: model={}, error={}", validatedModel, e.getMessage());
            throw convertException(e, validatedModel);
        }
    }

    @Override
    public List<EmbeddingResult> generateEmbeddingsWithDetails(List<String> texts, String modelCode) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        long startTime = System.currentTimeMillis();
        String validatedModel = getValidatedModel(modelCode);

        try {
            // 生成向量
            List<float[]> embeddings = generateEmbeddings(texts, validatedModel);

            // 计算统计信息
            int totalLatencyMs = (int) (System.currentTimeMillis() - startTime);
            int avgLatencyMs = totalLatencyMs / Math.max(1, texts.size());

            // 构建结果列表
            List<EmbeddingResult> results = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                String text = texts.get(i);
                float[] embedding = embeddings.get(i);

                int tokenCount = estimateTokenCount(text, validatedModel);
                double cost = calculateCost(validatedModel, tokenCount);

                results.add(EmbeddingResult.success(embedding, validatedModel, tokenCount,
                        avgLatencyMs, cost, "openai"));
            }

            log.info("批量向量生成完成: model={}, count={}, totalLatency={}ms",
                    validatedModel, texts.size(), totalLatencyMs);

            return results;

        } catch (Exception e) {
            log.error("批量详细向量生成失败: model={}, count={}, error={}",
                    validatedModel, texts.size(), e.getMessage());
            throw convertException(e, validatedModel);
        }
    }

    @Override
    public boolean isModelAvailable(String modelCode) {
        if (modelCode == null || modelCode.trim().isEmpty()) {
            return false;
        }

        // 检查是否在支持列表中
        if (!SUPPORTED_MODELS.containsKey(modelCode)) {
            return false;
        }

        // 检查是否在配置的模型列表中
        return llmProperties.getOpenai().getModels().contains(modelCode);
    }

    @Override
    public int getVectorDimension(String modelCode) {
        ModelInfo modelInfo = SUPPORTED_MODELS.get(modelCode);
        return modelInfo != null ? modelInfo.dimension() : 1536; // 默认维度
    }

    @Override
    public ModelInfo getModelInfo(String modelCode) {
        ModelInfo baseInfo = SUPPORTED_MODELS.get(modelCode);
        if (baseInfo == null) {
            return null;
        }

        // 检查实际可用性
        boolean available = isModelAvailable(modelCode);

        return new ModelInfo(
                baseInfo.name(),
                baseInfo.provider(),
                baseInfo.dimension(),
                baseInfo.maxTokens(),
                baseInfo.costPerToken(),
                baseInfo.description(),
                available
        );
    }

    @Override
    public List<String> getSupportedModels() {
        return llmProperties.getOpenai().getModels().stream()
                .filter(SUPPORTED_MODELS::containsKey)
                .toList();
    }

    @Override
    public boolean isHealthy() {
        try {
            // 使用最基础的模型进行健康检查
            String testText = "健康检查测试";
            String testModel = "text-embedding-3-small";

            if (!isModelAvailable(testModel)) {
                testModel = getSupportedModels().stream()
                        .filter(this::isModelAvailable)
                        .findFirst()
                        .orElse(null);
            }

            if (testModel == null) {
                return false;
            }

            generateEmbedding(testText, testModel);
            return true;

        } catch (Exception e) {
            log.warn("OpenAI嵌入服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean warmupModel(String modelCode) {
        try {
            log.info("开始预热模型: {}", modelCode);

            String warmupText = "This is a warmup text for model initialization.";
            generateEmbedding(warmupText, modelCode);

            log.info("模型预热成功: {}", modelCode);
            return true;

        } catch (Exception e) {
            log.warn("模型预热失败: model={}, error={}", modelCode, e.getMessage());
            return false;
        }
    }

    @Override
    public int estimateTokenCount(String text, String modelCode) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        String language = KbUtils.detectLanguage(text);

        return KbUtils.estimateTokenCount(text, language);
    }

    // =================== 私有方法 ===================

    /**
     * 创建向量缓存
     */
    private Cache<String, float[]> createVectorCache() {
        if (!llmProperties.getFeatures().getEnableResponseCache()) {
            return null;
        }

        int cacheTtl = llmProperties.getFeatures().getResponseCacheTtl();

        return CacheBuilder.newBuilder()
                .maximumSize(10000) // 最大缓存1万个向量
                .expireAfterWrite(Duration.ofSeconds(cacheTtl))
                .recordStats() // 启用统计信息
                .build();
    }

    /**
     * 获取并验证模型
     */
    private String getValidatedModel(String modelCode) {
        if (modelCode == null || modelCode.trim().isEmpty()) {
            String defaultModel = kbProperties.getEmbedding().getDefaultModel();
            log.debug("使用默认嵌入模型: {}", defaultModel);
            return defaultModel;
        }

        if (!isModelAvailable(modelCode)) {
            // 尝试使用备用模型
            List<String> fallbackModels = kbProperties.getEmbedding().getFallbackModels();
            for (String fallback : fallbackModels) {
                if (isModelAvailable(fallback)) {
                    log.warn("指定模型不可用，使用备用模型: {} -> {}", modelCode, fallback);
                    return fallback;
                }
            }

            throw new ChatException(ErrorCode.EXT_AI_005, modelCode);
        }

        return modelCode;
    }

    /**
     * 验证token限制
     */
    private void validateTokenLimit(String text, String modelCode) {
        int estimatedTokens = estimateTokenCount(text, modelCode);
        ModelInfo modelInfo = SUPPORTED_MODELS.get(modelCode);

        if (modelInfo != null && estimatedTokens > modelInfo.maxTokens()) {
            throw new ChatException(ErrorCode.EXT_AI_006, estimatedTokens, modelInfo.maxTokens());
        }
    }

    /**
     * 创建嵌入请求
     */
    private EmbeddingRequest createEmbeddingRequest(String text, String modelCode) {
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(modelCode)
                .build();

        return new EmbeddingRequest(List.of(text), options);
    }

    /**
     * 创建批量嵌入请求
     */
    private EmbeddingRequest createBatchEmbeddingRequest(List<String> texts, String modelCode) {
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(modelCode)
                .build();

        return new EmbeddingRequest(texts, options);
    }

    /**
     * 从响应中提取向量
     */
    private float[] extractEmbedding(EmbeddingResponse response) {
        if (response == null
                || response.getResults().isEmpty()
                || response.getResults().getFirst() == null) {
            throw new ChatException(ErrorCode.EXT_EMB_007);
        }

        float[] embedding = response.getResults().getFirst().getOutput();

        // 标准化向量（如果配置启用）
        if (kbProperties.getEmbedding().getNormalize()) {
            embedding = KbUtils.normalizeVector(embedding);
        }

        return embedding;
    }

    /**
     * 从批量响应中提取向量列表
     */
    private List<float[]> extractBatchEmbeddings(EmbeddingResponse response) {
        if (response == null || response.getResults().isEmpty()) {
            throw new ChatException(ErrorCode.EXT_EMB_008);
        }

        return response.getResults().stream()
                .map(result -> {
                    float[] embedding = result.getOutput();

                    // 标准化向量（如果配置启用）
                    if (kbProperties.getEmbedding().getNormalize()) {
                        embedding = KbUtils.normalizeVector(embedding);
                    }

                    return embedding;
                })
                .toList();
    }

    /**
     * 处理单个批次
     */
    private List<float[]> processSingleBatch(List<String> texts, String modelCode) {
        // 处理缓存命中和未命中的文本
        List<float[]> results = new ArrayList<>(Collections.nCopies(texts.size(), null));
        List<Integer> uncachedIndices = new ArrayList<>();
        List<String> uncachedTexts = new ArrayList<>();

        boolean cacheEnabled = llmProperties.getFeatures().getEnableResponseCache();

        if (cacheEnabled && vectorCache != null) {
            for (int i = 0; i < texts.size(); i++) {
                String text = texts.get(i);
                String cacheKey = generateCacheKey(text, modelCode);
                float[] cached = vectorCache.getIfPresent(cacheKey);

                if (cached != null) {
                    results.set(i, cached);
                } else {
                    uncachedIndices.add(i);
                    uncachedTexts.add(text);
                }
            }

            log.debug("缓存命中率: {}/{}", texts.size() - uncachedTexts.size(), texts.size());
        } else {
            uncachedIndices.addAll(java.util.stream.IntStream.range(0, texts.size()).boxed().toList());
            uncachedTexts.addAll(texts);
        }

        // 批量处理未缓存的文本
        if (!uncachedTexts.isEmpty()) {
            long startTime = System.currentTimeMillis();

            try {
                EmbeddingRequest batchRequest = createBatchEmbeddingRequest(uncachedTexts, modelCode);
                EmbeddingResponse response = embeddingModel.call(batchRequest);
                List<float[]> batchResults = extractBatchEmbeddings(response);

                // 将结果填回原位置并缓存
                for (int i = 0; i < uncachedIndices.size(); i++) {
                    int originalIndex = uncachedIndices.get(i);
                    float[] embedding = batchResults.get(i);
                    results.set(originalIndex, embedding);

                    if (cacheEnabled && vectorCache != null) {
                        String cacheKey = generateCacheKey(uncachedTexts.get(i), modelCode);
                        vectorCache.put(cacheKey, embedding);
                    }
                }

                // 更新统计信息
                long latency = System.currentTimeMillis() - startTime;
                updateModelStats(modelCode, uncachedTexts.size(), latency);

                log.debug("批量向量生成完成: model={}, processed={}, latency={}ms",
                        modelCode, uncachedTexts.size(), latency);

            } catch (Exception e) {
                log.error("批量向量生成失败: model={}, count={}, error={}",
                        modelCode, uncachedTexts.size(), e.getMessage());
                throw convertException(e, modelCode);
            }
        }

        return results;
    }

    /**
     * 分批处理大量文本
     */
    private List<float[]> processBatchesSequentially(List<String> texts, String modelCode) {
        List<float[]> allResults = new ArrayList<>();
        int batchSize = kbProperties.getEmbedding().getBatchSize();

        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);

            log.debug("处理批次: {}-{}/{}", i + 1, end, texts.size());

            List<String> cleanedBatch = batch.stream()
                    .map(KbUtils::cleanText)
                    .toList();

            List<float[]> batchResults = processSingleBatch(cleanedBatch, modelCode);
            allResults.addAll(batchResults);

            // 批次间短暂延迟，避免触发限流
            if (i + batchSize < texts.size()) {
                try {
                    Thread.sleep(100); // 100ms延迟
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ChatException(ErrorCode.EXT_EMB_009, e);
                }
            }
        }

        log.info("分批处理完成: totalTexts={}, batches={}", texts.size(),
                (texts.size() + batchSize - 1) / batchSize);

        return allResults;
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(String text, String modelCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = modelCode + ":" + text;
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            // 如果哈希失败，使用简单的字符串组合
            return modelCode + ":" + text.hashCode();
        }
    }

    /**
     * 计算费用
     */
    private double calculateCost(String modelCode, int tokenCount) {
        ModelInfo modelInfo = SUPPORTED_MODELS.get(modelCode);
        if (modelInfo == null) {
            return 0.0;
        }

        return modelInfo.estimateCost(tokenCount);
    }

    /**
     * 转换异常
     */
    private ChatException convertException(Exception e, String modelCode) {
        if (e instanceof ChatException) {
            return (ChatException) e;
        }

        String message = e.getMessage();
        if (message != null) {
            if (message.contains("401") || message.contains("Unauthorized")) {
                return new ChatException(ErrorCode.EXT_AI_008, e);
            } else if (message.contains("429") || message.contains("Rate limit")) {
                return new ChatException(ErrorCode.EXT_AI_007, e);
            } else if (message.contains("timeout") || message.contains("Timeout")) {
                return new ChatException(ErrorCode.EXT_AI_003, e);
            } else if (message.contains("quota") || message.contains("insufficient")) {
                return new ChatException(ErrorCode.EXT_AI_004, e);
            }
        }

        return new ChatException(ErrorCode.EXT_EMB_010, modelCode, e);
    }

    /**
     * 更新模型统计信息
     */
    private void updateModelStats(String modelCode, int requestCount, long latencyMs) {
        modelStatsMap.compute(modelCode, (key, stats) -> {
            if (stats == null) {
                stats = new ModelStats();
            }
            stats.setRequestCount(stats.getRequestCount() + requestCount);
            stats.setTotalLatencyMs(stats.getTotalLatencyMs() + latencyMs);
            stats.setLastUsedTime(System.currentTimeMillis());
            return stats;
        });
    }

    /**
     * 获取模型统计信息
     */
    public Map<String, Map<String, Object>> getModelStatistics() {
        Map<String, Map<String, Object>> statistics = new HashMap<>();

        for (Map.Entry<String, ModelStats> entry : modelStatsMap.entrySet()) {
            String model = entry.getKey();
            ModelStats stats = entry.getValue();

            Map<String, Object> modelStats = new HashMap<>();
            modelStats.put("requestCount", stats.getRequestCount());
            modelStats.put("averageLatencyMs", stats.getRequestCount() > 0 ?
                    stats.getTotalLatencyMs() / stats.getRequestCount() : 0);
            modelStats.put("totalLatencyMs", stats.getTotalLatencyMs());
            modelStats.put("lastUsedTime", stats.getLastUsedTime());

            statistics.put(model, modelStats);
        }

        return statistics;
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStatistics() {
        if (vectorCache == null) {
            return Map.of("enabled", false);
        }

        return Map.of(
                "enabled", true,
                "size", vectorCache.size(),
                "hitRate", vectorCache.stats().hitRate(),
                "missRate", vectorCache.stats().missRate(),
                "requestCount", vectorCache.stats().requestCount(),
                "hitCount", vectorCache.stats().hitCount(),
                "missCount", vectorCache.stats().missCount()
        );
    }

    /**
     * 初始化支持的模型信息
     */
    private static Map<String, ModelInfo> initializeSupportedModels() {
        Map<String, ModelInfo> models = new HashMap<>();

        // OpenAI最新嵌入模型
        models.put("text-embedding-3-small", ModelInfo.create(
                "text-embedding-3-small", "openai", 1536, 8192, 0.00002,
                "OpenAI第三代小型嵌入模型，性价比极高，适合大规模应用"
        ));

        models.put("text-embedding-3-large", ModelInfo.create(
                "text-embedding-3-large", "openai", 3072, 8192, 0.00013,
                "OpenAI第三代大型嵌入模型，更高的向量维度，更强的语义表达能力"
        ));

        // 经典ada-002模型
        models.put("text-embedding-ada-002", ModelInfo.create(
                "text-embedding-ada-002", "openai", 1536, 8192, 0.0001,
                "OpenAI经典嵌入模型，稳定可靠，广泛验证"
        ));

        return models;
    }
}