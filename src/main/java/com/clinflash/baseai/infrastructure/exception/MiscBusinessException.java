package com.clinflash.baseai.infrastructure.exception;

import lombok.Getter;

/**
 * <h2>杂项模块业务异常</h2>
 */
@Getter
public class MiscBusinessException extends RuntimeException {
    private final String errorCode;

    public MiscBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}