package com.cloud.baseai.infrastructure.exception;

/**
 * <h2>知识库技术异常</h2>
 *
 * <p>技术相关的异常，如向量计算错误、外部服务调用失败等。</p>
 */
public class KbTechnicalException extends KbException {

    public KbTechnicalException(String errorCode, String message) {
        super(errorCode, message);
    }

    public KbTechnicalException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}