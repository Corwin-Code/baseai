package com.clinflash.baseai.infrastructure.exception;

import lombok.Getter;

/**
 * <h2>杂项模块技术异常</h2>
 */
@Getter
public class MiscTechnicalException extends RuntimeException {
    private final String errorCode;

    public MiscTechnicalException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}