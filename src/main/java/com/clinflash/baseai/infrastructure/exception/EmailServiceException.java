package com.clinflash.baseai.infrastructure.exception;

import lombok.Getter;

/**
 * 邮件发送异常
 *
 * <p>所有邮件发送相关的异常都会包装为此异常类型，便于上层代码统一处理。</p>
 */
@Getter
public class EmailServiceException extends Exception {
    private final String errorCode;
    private final String errorMessage;

    public EmailServiceException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public EmailServiceException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}