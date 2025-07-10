package com.clinflash.baseai.infrastructure.exception;

import lombok.Getter;

/**
 * <h2>流程业务异常</h2>
 *
 * <p>流程编排模块的通用业务异常类。用于表示业务规则违反、
 * 状态不一致等业务层面的错误。</p>
 */
@Getter
public class FlowException extends RuntimeException {

    private final String errorCode;

    public FlowException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public FlowException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

}