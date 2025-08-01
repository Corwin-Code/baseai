package com.cloud.baseai.infrastructure.external.llm.service;

import com.cloud.baseai.infrastructure.config.properties.CacheProperties;
import com.cloud.baseai.infrastructure.config.properties.KnowledgeBaseProperties;
import com.cloud.baseai.infrastructure.config.properties.LlmProperties;
import com.cloud.baseai.infrastructure.exception.BusinessException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.exception.KnowledgeBaseException;
import com.cloud.baseai.infrastructure.utils.EmbeddingUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>阿里千问嵌入服务实现</h2>
 *
 * <p>这个实现专门针对阿里云DashScope的千问模型进行优化。千问模型在处理中文文本方面
 * 有着出色的表现，特别适合中文为主的知识库应用。</p>
 *
 * <p><b>与OpenAI的主要差异：</b></p>
 * <p>1. <b>API格式</b>：千问使用不同的请求响应格式</p>
 * <p>2. <b>中文优化</b>：对中文文本的理解和向量化效果更好</p>
 * <p>3. <b>成本结构</b>：计费方式和限制策略不同</p>
 */
@Service
public class QianWenEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(QianWenEmbeddingService.class);

    /**
     * 支持的千问模型配置
     */
    private static final Map<String, ModelInfo> SUPPORTED_MODELS = Map.of(
            "text-embedding-v1", new ModelInfo(1536, 2048),
            "text-embedding-v2", new ModelInfo(1536, 8192),
            "text-embedding-async-v1", new ModelInfo(1536, 2048),
            "text-embedding-async-v2", new ModelInfo(1536, 8192)
    );

    private final KnowledgeBaseProperties kbProps;
    private final LlmProperties llmProps;
    private final CacheProperties cacheProps;

    private final RestTemplate restTemplate;

    private final Cache<String, float[]> vectorCache;

    public QianWenEmbeddingService(KnowledgeBaseProperties kbProps,
                                   LlmProperties llmProps,
                                   CacheProperties cacheProps) {
        this.kbProps = kbProps;
        this.llmProps = llmProps;
        this.cacheProps = cacheProps;

        this.restTemplate = createRestTemplate();
        this.vectorCache = createVectorCache();

        log.info("千问嵌入服务初始化完成: baseUrl={}, models={}, responseCacheEnabled={}",
                llmProps.getQwen().getBaseUrl(),
                llmProps.getQwen().getModels(),
                llmProps.getFeatures().getEnableResponseCache());
    }

    @Override
    public float[] generateEmbedding(String text, String modelCode) {
        String cleanedText = EmbeddingUtils.validateAndCleanText(text);
        String model = getValidatedModel(modelCode);

        validateTokenLimit(cleanedText, model);

        // 检查缓存
        boolean cacheEnabled = llmProps.getFeatures().getEnableResponseCache();

        if (cacheEnabled) {
            String cacheKey = generateCacheKey(cleanedText, model);
            float[] cached = vectorCache.getIfPresent(cacheKey);
            if (cached != null) {
                log.debug("缓存命中: model={}, textLength={}", model, cleanedText.length());
                return cached;
            }
        }

        acquireRateLimit();

        float[] embedding = EmbeddingUtils.executeWithRetry(
                () -> callQianWenAPI(cleanedText, model),
                llmProps.getQwen().getMaxRetries(),
                1000,
                30000,
                2L,
                "千问嵌入生成"
        );

        if (cacheEnabled) {
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

        if (texts.size() > kbProps.getEmbedding().getBatchSize()) {
            return processBatchesSequentially(texts, model);
        }

        // 处理缓存
        List<float[]> results = new ArrayList<>(Collections.nCopies(texts.size(), null));
        List<Integer> uncachedIndices = new ArrayList<>();
        List<String> uncachedTexts = new ArrayList<>();

        boolean cacheEnabled = llmProps.getFeatures().getEnableResponseCache();

        if (cacheEnabled) {
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

        if (!uncachedTexts.isEmpty()) {
            acquireRateLimit();

            List<float[]> batchResults = EmbeddingUtils.executeWithRetry(
                    () -> callQianWenBatchAPI(uncachedTexts, model),
                    llmProps.getQwen().getMaxRetries(),
                    1000,
                    30000,
                    2L,
                    "千问批量嵌入生成"
            );

            for (int i = 0; i < uncachedIndices.size(); i++) {
                int originalIndex = uncachedIndices.get(i);
                float[] embedding = batchResults.get(i);
                results.set(originalIndex, embedding);

                if (cacheEnabled) {
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
        return info != null ? info.dimension : 1536;
    }

    private RestTemplate createRestTemplate() {
        RestTemplate template = new RestTemplate();

        template.getInterceptors().add((request, body, execution) -> {
            HttpHeaders headers = request.getHeaders();
            headers.set("Authorization", "Bearer " + llmProps.getQwen().getApiKey());
            headers.set("Content-Type", "application/json");
            headers.set("X-DashScope-Async", "enable");
            return execution.execute(request, body);
        });

        return template;
    }

    private Cache<String, float[]> createVectorCache() {
        boolean cacheEnabled = llmProps.getFeatures().getEnableResponseCache();
        if (!cacheEnabled) {
            return null;
        }

        return CacheBuilder.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(Duration.ofSeconds(cacheProps.getApplication().getConfigCacheTtlSeconds()))
                .recordStats()
                .build();
    }

    private String getValidatedModel(String modelCode) {
        if (modelCode == null || modelCode.trim().isEmpty()) {
            return llmProps.getQwen().getModels().contains(modelCode)
                    ? modelCode : llmProps.getQwen().getModels().getFirst();
        }

        if (!isModelAvailable(modelCode)) {
            throw new KnowledgeBaseException(ErrorCode.EXT_AI_011, modelCode);
        }

        return modelCode;
    }

    private void validateTokenLimit(String text, String model) {
        int estimatedTokens = EmbeddingUtils.estimateTokenCount(text);
        ModelInfo modelInfo = SUPPORTED_MODELS.get(model);

        if (modelInfo != null && estimatedTokens > modelInfo.maxTokens) {
            throw new KnowledgeBaseException(ErrorCode.EXT_AI_006, estimatedTokens, modelInfo.maxTokens);
        }
    }

    private void acquireRateLimit() {
//        if (!embedProps.getRateLimit().isEnabled()) {
//            return;
//        }
//
//        boolean acquired = rateLimiter.tryAcquire(
//                embedProps.getRateLimit().getMaxWaitTimeMs(),
//                TimeUnit.MILLISECONDS
//        );
//
//        if (!acquired) {
//            if (embedProps.getRateLimit().isLogWarnings()) {
//                log.warn("千问API限流等待超时，请求被拒绝");
//            }
//            throw new KnowledgeBaseException(ErrorCode.EXT_AI_007);
//        }
    }

    private String generateCacheKey(String text, String model) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = model + ":" + text;
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return model + ":" + text.hashCode();
        }
    }

    /**
     * 调用千问API生成单个向量
     *
     * <p>千问的API格式与OpenAI有所不同，特别是在请求结构和响应格式上。
     * 这个方法处理了这些差异，确保能够正确调用千问服务。</p>
     */
    private float[] callQianWenAPI(String text, String model) {
        String url = llmProps.getQwen().getBaseUrl() + "/services/embeddings/text-embedding/text-embedding";

        QianWenRequest request = new QianWenRequest(
                model,
                new QianWenInput(Collections.singletonList(text)),
                new QianWenParameters("document")
        );

        try {
            ResponseEntity<QianWenResponse> response = restTemplate.postForEntity(
                    url, request, QianWenResponse.class
            );

            if (response.getBody() == null ||
                    response.getBody().output == null ||
                    response.getBody().output.embeddings == null ||
                    response.getBody().output.embeddings.isEmpty()) {
                throw new KnowledgeBaseException(ErrorCode.EXT_AI_012);
            }

            List<Double> embedding = response.getBody().output.embeddings.getFirst().embedding;

            double[] temp = embedding.stream()
                    .mapToDouble(Double::doubleValue)
                    .toArray();

            float[] result = new float[temp.length];
            for (int i = 0; i < temp.length; i++) {
                result[i] = (float) temp[i];
            }

            return result;

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.EXT_AI_013)
                    .cause(e)
                    .context("operation", "callQianWenAPI")
                    .context("text", text)
                    .context("model", model)
                    .build();
        }
    }

    private List<float[]> callQianWenBatchAPI(List<String> texts, String model) {
        String url = llmProps.getQwen().getBaseUrl() + "/services/embeddings/text-embedding/text-embedding";

        QianWenRequest request = new QianWenRequest(
                model,
                new QianWenInput(texts),
                new QianWenParameters("document")
        );

        try {
            ResponseEntity<QianWenResponse> response = restTemplate.postForEntity(
                    url, request, QianWenResponse.class
            );

            if (response.getBody() == null ||
                    response.getBody().output == null ||
                    response.getBody().output.embeddings == null) {
                throw new KnowledgeBaseException(ErrorCode.EXT_AI_012);
            }

            return response.getBody().output.embeddings.stream()
                    .sorted(Comparator.comparingInt(data -> data.textIndex)) // 确保顺序
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
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.EXT_AI_014)
                    .cause(e)
                    .context("operation", "callQianWenBatchAPI")
                    .context("texts", texts)
                    .context("model", model)
                    .build();
        }
    }

    private List<float[]> processBatchesSequentially(List<String> texts, String model) {
        List<float[]> allResults = new ArrayList<>();
        int batchSize = kbProps.getEmbedding().getBatchSize();

        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);

            List<float[]> batchResults = generateEmbeddings(batch, model);
            allResults.addAll(batchResults);

            log.debug("千问批次处理完成: {}/{}", end, texts.size());
        }

        return allResults;
    }

    /**
     * 模型信息记录
     */
    private record ModelInfo(int dimension, int maxTokens) {
    }

    /**
     * 千问API请求对象
     */
    @Data
    private static class QianWenRequest {
        private String model;
        private QianWenInput input;
        private QianWenParameters parameters;

        public QianWenRequest(String model, QianWenInput input, QianWenParameters parameters) {
            this.model = model;
            this.input = input;
            this.parameters = parameters;
        }
    }

    @Data
    private static class QianWenInput {
        private List<String> texts;

        public QianWenInput(List<String> texts) {
            this.texts = texts;
        }
    }

    @Data
    private static class QianWenParameters {
        @JsonProperty("text_type")
        private String textType;

        public QianWenParameters(String textType) {
            this.textType = textType;
        }
    }

    /**
     * 千问API响应对象
     */
    @Data
    private static class QianWenResponse {
        private QianWenOutput output;
        private QianWenUsage usage;
        @JsonProperty("request_id")
        private String requestId;

        @Data
        private static class QianWenOutput {
            private List<QianWenEmbeddingData> embeddings;
        }

        @Data
        private static class QianWenEmbeddingData {
            private List<Double> embedding;
            @JsonProperty("text_index")
            private int textIndex;
        }

        @Data
        private static class QianWenUsage {
            @JsonProperty("total_tokens")
            private int totalTokens;
        }
    }
}