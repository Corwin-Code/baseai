package com.clinflash.baseai.infrastructure.external.llm.impl;

import com.clinflash.baseai.infrastructure.exception.VectorProcessingException;
import com.clinflash.baseai.infrastructure.external.llm.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * <h2>向量嵌入服务的模拟实现</h2>
 *
 * <p>这是一个用于开发和测试的模拟实现。在实际生产环境中，这里应该是对接真实的AI服务，
 * 如OpenAI的text-embedding-3-small模型、百度的ERNIE等。</p>
 *
 * <p><b>真实实现的考虑因素：</b></p>
 * <ul>
 * <li><b>API限流：</b>大多数AI服务都有请求频率限制</li>
 * <li><b>错误重试：</b>网络请求可能失败，需要实现重试机制</li>
 * <li><b>缓存策略：</b>相同文本的向量可以缓存，避免重复计算</li>
 * <li><b>成本控制：</b>向量生成通常按token收费，需要监控成本</li>
 * </ul>
 */
@Service
@ConditionalOnProperty(name = "kb.embedding.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(MockEmbeddingService.class);

    private final Random random = new Random(42); // 固定种子确保结果可重现
    private static final int DEFAULT_DIMENSION = 1536;

    @Override
    public float[] generateEmbedding(String text, String modelCode) {
        log.debug("生成向量嵌入: text={}, model={}",
                text.length() > 50 ? text.substring(0, 50) + "..." : text, modelCode);

        try {
            // 模拟网络延迟
            Thread.sleep(100 + random.nextInt(200));

            // 基于文本内容生成"伪随机"向量，确保相同文本产生相同向量
            return generateDeterministicVector(text, DEFAULT_DIMENSION);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VectorProcessingException("向量生成被中断", e);
        } catch (Exception e) {
            throw new VectorProcessingException("向量生成失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts, String modelCode) {
        log.debug("批量生成向量嵌入: count={}, model={}", texts.size(), modelCode);

        return texts.stream()
                .map(text -> generateEmbedding(text, modelCode))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isModelAvailable(String modelCode) {
        // 模拟实现：支持常见的模型
        return modelCode != null && (
                modelCode.startsWith("text-embedding") ||
                        modelCode.startsWith("ernie") ||
                        modelCode.startsWith("bge")
        );
    }

    @Override
    public int getVectorDimension(String modelCode) {
        // 根据模型返回不同的维度
        return switch (modelCode) {
            case "text-embedding-3-large" -> 3072;
            case "text-embedding-3-small", "text-embedding-ada-002" -> 1536;
            case "bge-large-zh" -> 1024;
            default -> DEFAULT_DIMENSION;
        };
    }

    /**
     * 生成基于文本内容的确定性向量
     *
     * <p>这个方法确保相同的文本总是产生相同的向量，这对于测试和开发很重要。
     * 在真实的AI模型中，这种一致性是自然保证的。</p>
     */
    private float[] generateDeterministicVector(String text, int dimension) {
        // 使用文本哈希码作为随机种子
        Random textRandom = new Random(text.hashCode());
        float[] vector = new float[dimension];

        // 生成正态分布的随机向量
        for (int i = 0; i < dimension; i++) {
            vector[i] = (float) textRandom.nextGaussian();
        }

        // 归一化向量
        return normalizeVector(vector);
    }

    /**
     * 向量归一化
     */
    private float[] normalizeVector(float[] vector) {
        double norm = 0.0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }

        return vector;
    }
}