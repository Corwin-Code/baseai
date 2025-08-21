package com.cloud.baseai.infrastructure.external.llm.service;

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h2>通义千问向量嵌入服务</h2>
 *
 * <p>基于Spring AI Alibaba框架的通义千问向量化服务实现。该服务专门处理中文文本的向量化，
 * 在中文语义理解方面具有显著优势，同时提供企业级的缓存、重试和监控功能。</p>
 *
 * <p><b>通义千问嵌入特色：</b></p>
 * <ul>
 * <li><b>中文优化：</b>专门针对中文文本优化的向量表示，语义理解更准确</li>
 * <li><b>多语言支持：</b>同时支持中英文等多种语言的高质量向量化</li>
 * <li><b>成本优势：</b>相比国际模型具有显著的成本优势，适合大规模应用</li>
 * <li><b>本土化服务：</b>国内部署，访问速度更快，数据更安全</li>
 * <li><b>企业级功能：</b>内置缓存、重试、监控等生产环境必需功能</li>
 * </ul>
 */
@Service
@ConditionalOnBean(DashScopeEmbeddingModel.class)
public class QwenEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(QwenEmbeddingService.class);

    /**
     * 支持的通义千问嵌入模型及其配置信息
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
     * 构造函数，初始化通义千问嵌入服务
     *
     * @param llmProperties  LLM配置属性
     * @param kbProperties   知识库配置属性
     * @param embeddingModel DashScope嵌入模型
     */
    public QwenEmbeddingService(LlmProperties llmProperties,
                                KnowledgeBaseProperties kbProperties,
                                @Qualifier("qwenEmbeddingModel") EmbeddingModel embeddingModel) {
        this.llmProperties = llmProperties;
        this.kbProperties = kbProperties;
        this.embeddingModel = embeddingModel;
        this.vectorCache = createVectorCache();

        log.info("通义千问嵌入服务初始化完成: baseUrl={}, 默认模型={}, 缓存启用={}",
                llmProperties.getQwen().getBaseUrl(),
                kbProperties.getEmbedding().getDefaultModel(),
                llmProperties.getFeatures().getEnableResponseCache());
    }

    @Override
    public float[] generateEmbedding(String text, String modelCode) {
        // 输入验证和清理
        String cleanedText = KbUtils.cleanText(text);
        String validatedModel = getValidatedModel(modelCode);

        // 检查token限制
        validateTokenLimit(cleanedText, validatedModel);

        // 尝试从缓存获取
        if (llmProperties.getFeatures().getEnableResponseCache()) {
            String cacheKey = generateCacheKey(cleanedText, validatedModel);
            float[] cached = vectorCache.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("通义千问向量缓存命中: model={}, textLength={}", validatedModel, cleanedText.length());
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

            log.debug("通义千问向量生成完成: model={}, dimension={}, latency={}ms",
                    validatedModel, embedding.length, latency);

            return embedding;

        } catch (Exception e) {
            log.error("通义千问向量生成失败: model={}, textLength={}, error={}",
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

            log.debug("通义千问向量生成详情: model={}, tokens={}, latency={}ms, cost=¥{}",
                    validatedModel, tokenCount, latencyMs, cost);

            return EmbeddingResult.success(embedding, validatedModel, tokenCount, latencyMs, cost, "qwen");

        } catch (Exception e) {
            log.error("通义千问详细向量生成失败: model={}, error={}", validatedModel, e.getMessage());
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
                        avgLatencyMs, cost, "qwen"));
            }

            log.info("通义千问批量向量生成完成: model={}, count={}, totalLatency={}ms",
                    validatedModel, texts.size(), totalLatencyMs);

            return results;

        } catch (Exception e) {
            log.error("通义千问批量详细向量生成失败: model={}, count={}, error={}",
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
        return llmProperties.getQwen().getEnabled() &&
                llmProperties.getQwen().getModels().contains(modelCode);
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
        return llmProperties.getQwen().getModels().stream()
                .filter(SUPPORTED_MODELS::containsKey)
                .toList();
    }

    @Override
    public boolean isHealthy() {
        try {
            // 使用最基础的模型进行健康检查
            String testText = "健康检查测试";
            String testModel = "text-embedding-v2";

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
            log.warn("通义千问嵌入服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean warmupModel(String modelCode) {
        try {
            log.info("开始预热通义千问模型: {}", modelCode);

            String warmupText = "这是一个模型预热文本，用于初始化服务。";
            generateEmbedding(warmupText, modelCode);

            log.info("通义千问模型预热成功: {}", modelCode);
            return true;

        } catch (Exception e) {
            log.warn("通义千问模型预热失败: model={}, error={}", modelCode, e.getMessage());
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
            String defaultModel = getDefaultQwenEmbeddingModel();
            log.debug("使用默认通义千问嵌入模型: {}", defaultModel);
            return defaultModel;
        }

        if (!isModelAvailable(modelCode)) {
            // 尝试使用备用模型
            List<String> fallbackModels = getQwenFallbackModels();
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
     * 获取默认的通义千问嵌入模型
     */
    private String getDefaultQwenEmbeddingModel() {
        // 首先检查配置的默认模型是否为通义千问模型
        String defaultModel = kbProperties.getEmbedding().getDefaultModel();
        if (defaultModel.startsWith("text-embedding-v") && isModelAvailable(defaultModel)) {
            return defaultModel;
        }

        // 否则使用通义千问的默认模型
        return "text-embedding-v2";
    }

    /**
     * 获取通义千问备用模型列表
     */
    private List<String> getQwenFallbackModels() {
        return List.of("text-embedding-v2", "text-embedding-v3");
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
        DashScopeEmbeddingOptions options = DashScopeEmbeddingOptions.builder()
                .withModel(modelCode)
                .withDimensions(getVectorDimension(modelCode))
                .build();

        return new EmbeddingRequest(List.of(text), options);
    }

    /**
     * 创建批量嵌入请求
     */
    private EmbeddingRequest createBatchEmbeddingRequest(List<String> texts, String modelCode) {
        DashScopeEmbeddingOptions options = DashScopeEmbeddingOptions.builder()
                .withModel(modelCode)
                .withDimensions(getVectorDimension(modelCode))
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

            log.debug("通义千问缓存命中率: {}/{}", texts.size() - uncachedTexts.size(), texts.size());
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

                log.debug("通义千问批量向量生成完成: model={}, processed={}, latency={}ms",
                        modelCode, uncachedTexts.size(), latency);

            } catch (Exception e) {
                log.error("通义千问批量向量生成失败: model={}, count={}, error={}",
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

            log.debug("处理通义千问批次: {}-{}/{}", i + 1, end, texts.size());

            List<String> cleanedBatch = batch.stream()
                    .map(KbUtils::cleanText)
                    .toList();

            List<float[]> batchResults = processSingleBatch(cleanedBatch, modelCode);
            allResults.addAll(batchResults);

            // 批次间短暂延迟，避免触发限流
            if (i + batchSize < texts.size()) {
                try {
                    Thread.sleep(200); // 200ms延迟，通义千问限流相对宽松
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ChatException(ErrorCode.EXT_EMB_009, e);
                }
            }
        }

        log.info("通义千问分批处理完成: totalTexts={}, batches={}", texts.size(),
                (texts.size() + batchSize - 1) / batchSize);

        return allResults;
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(String text, String modelCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = "qwen:" + modelCode + ":" + text;
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            // 如果哈希失败，使用简单的字符串组合
            return "qwen:" + modelCode + ":" + text.hashCode();
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
            if (message.contains("401") || message.contains("Unauthorized") || message.contains("Invalid API")) {
                return new ChatException(ErrorCode.EXT_AI_008, e);
            } else if (message.contains("429") || message.contains("Rate limit") || message.contains("Throttling")) {
                return new ChatException(ErrorCode.EXT_AI_007, e);
            } else if (message.contains("timeout") || message.contains("Timeout") || message.contains("连接超时")) {
                return new ChatException(ErrorCode.EXT_AI_003, e);
            } else if (message.contains("quota") || message.contains("insufficient") || message.contains("余额不足")) {
                return new ChatException(ErrorCode.EXT_AI_004, e);
            }
        }

        return new ChatException(ErrorCode.EXT_EMB_011, modelCode, e);
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

        // 通义千问嵌入模型
        models.put("text-embedding-v2", ModelInfo.create(
                "text-embedding-v2", "qwen", 1536, 2048, 0.0007,
                "通义千问第二代文本嵌入模型，中文优化，性价比高"
        ));

        models.put("text-embedding-v3", ModelInfo.create(
                "text-embedding-v3", "qwen", 1536, 8192, 0.0007,
                "通义千问第三代文本嵌入模型，支持更长文本，语义理解更准确"
        ));

        return models;
    }
}