package com.cloud.baseai.infrastructure.exception;

/**
 * <h2>向量生成异常</h2>
 *
 * <p>当外部AI服务调用失败或向量生成过程中出现问题时抛出此异常。</p>
 */
public class EmbeddingGenerationException extends VectorProcessingException {

    public EmbeddingGenerationException(String message) {
        super(message);
    }

    public EmbeddingGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}