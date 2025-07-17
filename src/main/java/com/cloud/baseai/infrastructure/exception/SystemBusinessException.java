package com.cloud.baseai.infrastructure.exception;

import lombok.Getter;

/**
 * <h2>系统模块业务异常</h2>
 */
@Getter
public class SystemBusinessException extends RuntimeException {
    private final String errorCode;

    public SystemBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}