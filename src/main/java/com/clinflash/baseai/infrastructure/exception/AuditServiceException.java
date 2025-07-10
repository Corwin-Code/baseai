package com.clinflash.baseai.infrastructure.exception;

import lombok.Getter;

/**
 * 审计服务异常
 */
@Getter
public class AuditServiceException extends Exception {
    private final String errorCode;

    public AuditServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AuditServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}