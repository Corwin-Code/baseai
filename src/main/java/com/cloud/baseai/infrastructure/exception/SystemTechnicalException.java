package com.cloud.baseai.infrastructure.exception;

import lombok.Getter;

/**
 * <h2>系统模块技术异常</h2>
 */
@Getter
public class SystemTechnicalException extends RuntimeException {
    private final String errorCode;

    public SystemTechnicalException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}