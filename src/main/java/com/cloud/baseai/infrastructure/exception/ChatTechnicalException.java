package com.cloud.baseai.infrastructure.exception;

import lombok.Getter;

/**
 * <h2>对话技术异常</h2>
 */
@Getter
public class ChatTechnicalException extends RuntimeException {

    private final String errorCode;

    public ChatTechnicalException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ChatTechnicalException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}