package com.clinflash.baseai.infrastructure.exception;

import com.clinflash.baseai.infrastructure.web.response.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>Mcp模块全局异常处理器</h2>
 *
 * <p>异常处理是系统稳定性的最后一道防线。这个处理器确保即使在出现
 * 未预期错误的情况下，用户也能收到友好的错误信息，而不是技术性的堆栈跟踪。</p>
 */
@RestControllerAdvice
public class McpGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(McpGlobalExceptionHandler.class);

    /**
     * 处理Mcp业务异常
     */
    @ExceptionHandler(McpException.class)
    public ResponseEntity<ApiResult<Void>> handleMcpException(McpException ex) {
        log.warn("MCP业务异常: code={}, message={}", ex.getErrorCode(), ex.getMessage());

        HttpStatus status = mapMcpExceptionToHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status)
                .body(ApiResult.error(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * 将Mcp异常代码映射为HTTP状态码
     */
    private HttpStatus mapMcpExceptionToHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "TOOL_NOT_FOUND", "AUTHORIZATION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "DUPLICATE_TOOL_CODE", "ALREADY_AUTHORIZED" -> HttpStatus.CONFLICT;
            case "INVALID_TOOL_CONFIG", "INVALID_PARAMETERS" -> HttpStatus.BAD_REQUEST;
            case "QUOTA_EXCEEDED", "RATE_LIMIT_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "UNAUTHORIZED_TOOL_ACCESS" -> HttpStatus.FORBIDDEN;
            case "TOOL_EXECUTION_TIMEOUT" -> HttpStatus.REQUEST_TIMEOUT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}