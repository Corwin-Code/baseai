package com.cloud.baseai.infrastructure.exception;

/**
 * <h2>流程构建异常</h2>
 *
 * <p>流程构建过程中发生的异常，如结构验证失败、配置错误等。</p>
 */
public class FlowBuildException extends FlowException {

    public FlowBuildException(String message) {
        super("FLOW_BUILD_ERROR", message);
    }

    public FlowBuildException(String message, Throwable cause) {
        super("FLOW_BUILD_ERROR", message, cause);
    }
}