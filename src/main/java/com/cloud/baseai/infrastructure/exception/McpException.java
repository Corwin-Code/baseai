package com.cloud.baseai.infrastructure.exception;

import lombok.Getter;

/**
 * <h2>MCP业务异常</h2>
 */
@Getter
public class McpException extends RuntimeException {

    private final String errorCode;

    public McpException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public McpException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

}