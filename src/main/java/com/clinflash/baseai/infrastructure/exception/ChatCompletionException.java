package com.clinflash.baseai.infrastructure.exception;

import lombok.Getter;

/**
 * 聊天完成异常
 *
 * <p>当LLM调用过程中发生错误时抛出此异常。它封装了错误代码、
 * 详细信息等，帮助业务层进行适当的错误处理和用户提示。</p>
 */
@Getter
public class ChatCompletionException extends RuntimeException {
    private final String errorCode;
    private final int httpStatus;

    public ChatCompletionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = 500;
    }

    public ChatCompletionException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public ChatCompletionException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = 500;
    }
}