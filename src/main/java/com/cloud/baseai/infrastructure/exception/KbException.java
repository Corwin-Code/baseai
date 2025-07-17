package com.cloud.baseai.infrastructure.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * <h2>知识库异常基类</h2>
 *
 * <p>知识库模块的所有自定义异常的基类，提供统一的异常处理机制。</p>
 */
@Setter
@Getter
public abstract class KbException extends RuntimeException {

    private final String errorCode;
    private final Object[] args;

    protected KbException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = null;
    }

    protected KbException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = null;
    }

    protected KbException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

}