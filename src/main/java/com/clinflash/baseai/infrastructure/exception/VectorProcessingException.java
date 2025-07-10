package com.clinflash.baseai.infrastructure.exception;

/**
 * <h2>向量处理异常</h2>
 */
public class VectorProcessingException extends KbTechnicalException {

    public VectorProcessingException(String message) {
        super("VECTOR_PROCESSING_ERROR", message);
    }

    public VectorProcessingException(String message, Throwable cause) {
        super("VECTOR_PROCESSING_ERROR", message, cause);
    }
}