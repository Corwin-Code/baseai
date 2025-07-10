package com.clinflash.baseai.infrastructure.exception;

import com.clinflash.baseai.infrastructure.web.response.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>流程编排模块全局异常处理器</h2>
 *
 * <p>异常处理是系统稳定性的最后一道防线。这个处理器确保即使在出现
 * 未预期错误的情况下，用户也能收到友好的错误信息，而不是技术性的堆栈跟踪。</p>
 */
@RestControllerAdvice
public class FlowGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(FlowGlobalExceptionHandler.class);

    /**
     * 处理流程业务异常
     */
    @ExceptionHandler(FlowException.class)
    public ResponseEntity<ApiResult<Void>> handleFlowException(FlowException ex) {
        log.warn("流程业务异常: code={}, message={}", ex.getErrorCode(), ex.getMessage());

        HttpStatus status = mapFlowExceptionToHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status)
                .body(ApiResult.error(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * 处理流程验证异常
     */
    @ExceptionHandler(FlowValidationException.class)
    public ResponseEntity<ApiResult<Void>> handleFlowValidationException(FlowValidationException ex) {
        log.warn("流程验证异常: message={}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error("FLOW_VALIDATION_ERROR", ex.getMessage()));
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleGenericException(Exception ex) {
        log.error("流程模块未知异常", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error("FLOW_INTERNAL_ERROR", "流程系统发生未知错误"));
    }

    /**
     * 将流程异常代码映射为HTTP状态码
     */
    private HttpStatus mapFlowExceptionToHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "FLOW_NOT_FOUND", "PROJECT_NOT_FOUND", "RUN_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "FLOW_ALREADY_PUBLISHED", "INVALID_FLOW_STATUS", "EXECUTION_IN_PROGRESS" -> HttpStatus.CONFLICT;
            case "FLOW_VALIDATION_FAILED", "INVALID_NODE_CONFIG", "CIRCULAR_DEPENDENCY" -> HttpStatus.BAD_REQUEST;
            case "EXECUTION_TIMEOUT", "RESOURCE_LIMIT_EXCEEDED" -> HttpStatus.REQUEST_TIMEOUT;
            case "UNAUTHORIZED_FLOW_ACCESS" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}