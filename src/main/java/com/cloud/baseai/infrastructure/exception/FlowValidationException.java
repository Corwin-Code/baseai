package com.cloud.baseai.infrastructure.exception;

/**
 * <h2>流程验证异常</h2>
 *
 * <p>流程结构或配置验证失败时抛出的异常。</p>
 */
public class FlowValidationException extends FlowException {

    public FlowValidationException(String message) {
        super("FLOW_VALIDATION_ERROR", message);
    }

    public FlowValidationException(String message, Throwable cause) {
        super("FLOW_VALIDATION_ERROR", message, cause);
    }
}