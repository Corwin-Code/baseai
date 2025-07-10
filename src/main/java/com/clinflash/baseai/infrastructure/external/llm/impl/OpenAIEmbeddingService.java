package com.clinflash.baseai.infrastructure.external.llm.impl;

import com.clinflash.baseai.infrastructure.config.EmbeddingServiceConfig;
import com.clinflash.baseai.infrastructure.exception.VectorProcessingException;
import com.clinflash.baseai.infrastructure.external.llm.EmbeddingService;
import com.clinflash.baseai.infrastructure.utils.EmbeddingUtils;
import com.clinflash.baseai.infrastructure.utils.KbConstants;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <h2>OpenAI嵌入服务实现</h2>
 *
 * <p>这个实现专注于与OpenAI API的可靠交互。相比于之前的复杂设计，
 * 这个版本更加务实，专注于解决真实世界中的问题。</p>
 *
 * <p><b>设计哲学：</b></p>
 * <p>1. <b>简单优于复杂</b>：只实现必要的功能，避免过度工程化</p>
 * <p>2. <b>可靠优于完美</b>：确保在网络不稳定时依然能工作</p>
 * <p>3. <b>实用优于理论</b>：基于真实使用场景设计，而不是理想情况</p>
 */
public class OpenAIEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OpenAIEmbeddingService.class);

    /**
     * 支持的模型及其维度配置
     */
    private static final Map<String, ModelInfo> SUPPORTED_MODELS = Map.of(
            KbConstants.VectorModels.TEXT_EMBEDDING_ADA_002, new ModelInfo(KbConstants.VectorDimensions.TEXT_EMBEDDING_ADA_002, 8192),
            KbConstants.VectorModels.TEXT_EMBEDDING_3_SMALL, new ModelInfo(KbConstants.VectorDimensions.TEXT_EMBEDDING_3_SMALL, 8192),
            KbConstants.VectorModels.TEXT_EMBEDDING_3_LARGE, new ModelInfo(KbConstants.VectorDimensions.TEXT_EMBEDDING_3_LARGE, 8192)
    );

    private final EmbeddingServiceConfig config;
    private final RestTemplate restTemplate;
    private final RateLimiter rateLimiter;
    private final Cache<String, float[]> vectorCache;
    private final ObjectMapper objectMapper;

    public OpenAIEmbeddingService(EmbeddingServiceConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();

        // 初始化HTTP客户端
        this.restTemplate = createRestTemplate();

        // 初始化限流器
        this.rateLimiter = EmbeddingUtils.createRateLimiter(
                config.getOpenai().getRequestsPerSecond()
        );

        // 初始化缓存
        this.vectorCache = createVectorCache();

        log.info("OpenAI嵌入服务初始化完成: baseUrl={}, defaultModel={}, cacheEnabled={}",
                config.getOpenai().getBaseUrl(),
                config.getOpenai().getDefaultModel(),
                config.isCacheEnabled());
    }

    @Override
    public float[] generateEmbedding(String text, String modelCode) {
        // 输入验证和清理
        String cleanedText = EmbeddingUtils.validateAndCleanText(text);
        String model = getValidatedModel(modelCode);

        // 检查Token限制
        validateTokenLimit(cleanedText, model);

        // 尝试从缓存获取
        if (config.isCacheEnabled()) {
            String cacheKey = generateCacheKey(cleanedText, model);
            float[] cached = vectorCache.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("缓存命中: model={}, textLength={}", model, cleanedText.length());
                return cached;
            }
        }

        // 限流控制
        acquireRateLimit();

        // 调用API生成向量
        float[] embedding = EmbeddingUtils.executeWithRetry(
                () -> callOpenAIAPI(cleanedText, model),
                config.getMaxRetries(),
                config.getRetry().getInitialDelayMs(),
                config.getRetry().getMultiplier(),
                config.getRetry().getMaxDelayMs(),
                "OpenAI嵌入生成"
        );

        // 缓存结果
        if (config.isCacheEnabled()) {
            String cacheKey = generateCacheKey(cleanedText, model);
            vectorCache.put(cacheKey, embedding);
        }

        return embedding;
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts, String modelCode) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        String model = getValidatedModel(modelCode);

        // 验证批量大小
        if (texts.size() > config.getBatchSize()) {
            log.warn("批量大小超出限制，将分批处理: requestSize={}, batchSize={}",
                    texts.size(), config.getBatchSize());
            return processBatchesSequentially(texts, model);
        }

        // 处理缓存命中和未命中的文本
        List<float[]> results = new ArrayList<>(Collections.nCopies(texts.size(), null));
        List<Integer> uncachedIndices = new ArrayList<>();
        List<String> uncachedTexts = new ArrayList<>();

        if (config.isCacheEnabled()) {
            for (int i = 0; i < texts.size(); i++) {
                String cleanedText = EmbeddingUtils.validateAndCleanText(texts.get(i));
                String cacheKey = generateCacheKey(cleanedText, model);
                float[] cached = vectorCache.getIfPresent(cacheKey);

                if (cached != null) {
                    results.set(i, cached);
                } else {
                    uncachedIndices.add(i);
                    uncachedTexts.add(cleanedText);
                }
            }
        } else {
            for (int i = 0; i < texts.size(); i++) {
                uncachedIndices.add(i);
                uncachedTexts.add(EmbeddingUtils.validateAndCleanText(texts.get(i)));
            }
        }

        // 批量处理未缓存的文本
        if (!uncachedTexts.isEmpty()) {
            acquireRateLimit();

            List<float[]> batchResults = EmbeddingUtils.executeWithRetry(
                    () -> callOpenAIBatchAPI(uncachedTexts, model),
                    config.getMaxRetries(),
                    config.getRetry().getInitialDelayMs(),
                    config.getRetry().getMultiplier(),
                    config.getRetry().getMaxDelayMs(),
                    "OpenAI批量嵌入生成"
            );

            // 将结果填回原位置并缓存
            for (int i = 0; i < uncachedIndices.size(); i++) {
                int originalIndex = uncachedIndices.get(i);
                float[] embedding = batchResults.get(i);
                results.set(originalIndex, embedding);

                if (config.isCacheEnabled()) {
                    String cacheKey = generateCacheKey(uncachedTexts.get(i), model);
                    vectorCache.put(cacheKey, embedding);
                }
            }
        }

        return results;
    }

    @Override
    public boolean isModelAvailable(String modelCode) {
        if (modelCode == null || modelCode.trim().isEmpty()) {
            return false;
        }
        return SUPPORTED_MODELS.containsKey(modelCode);
    }

    @Override
    public int getVectorDimension(String modelCode) {
        ModelInfo info = SUPPORTED_MODELS.get(modelCode);
        return info != null ? info.dimension : 1536; // 默认维度
    }

    /**
     * 创建配置好的RestTemplate
     */
    private RestTemplate createRestTemplate() {
        RestTemplate template = new RestTemplate();

        // 设置请求拦截器添加认证头
        template.getInterceptors().add((request, body, execution) -> {
            HttpHeaders headers = request.getHeaders();
            headers.set("Authorization", "Bearer " + config.getApiKey());
            headers.set("Content-Type", "application/json");
            headers.set("User-Agent", "ClinFlash-BaseAI/1.0");
            return execution.execute(request, body);
        });

        return template;
    }

    /**
     * 创建向量缓存
     */
    private Cache<String, float[]> createVectorCache() {
        if (!config.isCacheEnabled()) {
            return null;
        }

        return CacheBuilder.newBuilder()
                .maximumSize(10000) // 最大缓存1万个向量
                .expireAfterWrite(Duration.ofSeconds(config.getCacheTtlSeconds()))
                .recordStats()
                .build();
    }

    /**
     * 获取有效的模型名称
     */
    private String getValidatedModel(String modelCode) {
        if (modelCode == null || modelCode.trim().isEmpty()) {
            return config.getOpenai().getDefaultModel();
        }

        if (!isModelAvailable(modelCode)) {
            throw new VectorProcessingException("不支持的OpenAI模型: " + modelCode);
        }

        return modelCode;
    }

    /**
     * 验证Token限制
     */
    private void validateTokenLimit(String text, String model) {
        int estimatedTokens = EmbeddingUtils.estimateTokenCount(text);
        ModelInfo modelInfo = SUPPORTED_MODELS.get(model);

        if (modelInfo != null && estimatedTokens > modelInfo.maxTokens) {
            throw new VectorProcessingException(
                    String.format("文本过长，估算Token数: %d，模型限制: %d",
                            estimatedTokens, modelInfo.maxTokens));
        }
    }

    /**
     * 获取限流许可
     */
    private void acquireRateLimit() {
        if (!config.getRateLimit().isEnabled()) {
            return;
        }

        boolean acquired = rateLimiter.tryAcquire(
                config.getRateLimit().getMaxWaitTimeMs(),
                TimeUnit.MILLISECONDS
        );

        if (!acquired) {
            if (config.getRateLimit().isLogWarnings()) {
                log.warn("OpenAI API限流等待超时，请求被拒绝");
            }
            throw new VectorProcessingException("API请求频率过高，请稍后重试");
        }
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(String text, String model) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = model + ":" + text;
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            // 如果哈希失败，使用简单的字符串组合
            return model + ":" + text.hashCode();
        }
    }

    /**
     * 调用OpenAI API生成单个向量
     */
    private float[] callOpenAIAPI(String text, String model) {
        String url = config.getOpenai().getBaseUrl() + "/embeddings";

        OpenAIRequest request = new OpenAIRequest(text, model);

        try {
            ResponseEntity<OpenAIResponse> response = restTemplate.postForEntity(
                    url, request, OpenAIResponse.class
            );

            if (response.getBody() == null ||
                    response.getBody().data == null ||
                    response.getBody().data.isEmpty()) {
                throw new VectorProcessingException("OpenAI返回空响应");
            }

            List<Double> embedding = response.getBody().data.getFirst().embedding;
            double[] temp = embedding.stream()
                    .mapToDouble(Double::doubleValue)
                    .toArray();

            float[] result = new float[temp.length];
            for (int i = 0; i < temp.length; i++) {
                result[i] = (float) temp[i];
            }

            return result;

        } catch (Exception e) {
            if (e instanceof VectorProcessingException) {
                throw e;
            }
            throw new VectorProcessingException("OpenAI API调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用OpenAI API生成批量向量
     */
    private List<float[]> callOpenAIBatchAPI(List<String> texts, String model) {
        String url = config.getOpenai().getBaseUrl() + "/embeddings";

        OpenAIRequest request = new OpenAIRequest(texts, model);

        try {
            ResponseEntity<OpenAIResponse> response = restTemplate.postForEntity(
                    url, request, OpenAIResponse.class
            );

            if (response.getBody() == null || response.getBody().data == null) {
                throw new VectorProcessingException("OpenAI返回空响应");
            }

            return response.getBody().data.stream()
                    .sorted(Comparator.comparingInt(data -> data.index)) // 确保顺序正确
                    .map(data -> {
                        // List<Double> → float[]
                        float[] arr = new float[data.embedding.size()];
                        int i = 0;
                        for (Double v : data.embedding) {
                            arr[i++] = v.floatValue();
                        }
                        return arr;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            if (e instanceof VectorProcessingException) {
                throw e;
            }
            throw new VectorProcessingException("OpenAI批量API调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 分批处理大量文本
     */
    private List<float[]> processBatchesSequentially(List<String> texts, String model) {
        List<float[]> allResults = new ArrayList<>();
        int batchSize = config.getBatchSize();

        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);

            List<float[]> batchResults = generateEmbeddings(batch, model);
            allResults.addAll(batchResults);

            log.debug("处理批次完成: {}/{}", end, texts.size());
        }

        return allResults;
    }

    /**
     * 模型信息记录
     */
    private record ModelInfo(int dimension, int maxTokens) {
    }

    /**
     * OpenAI API请求对象
     */
    @Data
    private static class OpenAIRequest {
        private Object input; // String或List<String>
        private String model;
        @JsonProperty("encoding_format")
        private String encodingFormat = "float";

        public OpenAIRequest(String text, String model) {
            this.input = text;
            this.model = model;
        }

        public OpenAIRequest(List<String> texts, String model) {
            this.input = texts;
            this.model = model;
        }
    }

    /**
     * OpenAI API响应对象
     */
    @Data
    private static class OpenAIResponse {
        private List<EmbeddingData> data;
        private String model;
        private Usage usage;

        @Data
        private static class EmbeddingData {
            private List<Double> embedding;
            private int index;
            private String object;
        }

        @Data
        private static class Usage {
            @JsonProperty("prompt_tokens")
            private int promptTokens;
            @JsonProperty("total_tokens")
            private int totalTokens;
        }
    }
}