package com.cloud.baseai.infrastructure.exception;

import lombok.Getter;

/**
 * <h2>对话业务异常</h2>
 */
@Getter
public class ChatBusinessException extends RuntimeException {

    private final String errorCode;

    public ChatBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ChatBusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}