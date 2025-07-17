package com.cloud.baseai.infrastructure.exception;

import lombok.Getter;

/**
 * 短信服务异常类
 *
 * <p>封装所有短信发送相关的异常，提供统一的错误处理机制。</p>
 */
@Getter
public class SmsServiceException extends Exception {
    private final String errorCode;
    private final String errorMessage;

    public SmsServiceException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public SmsServiceException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}