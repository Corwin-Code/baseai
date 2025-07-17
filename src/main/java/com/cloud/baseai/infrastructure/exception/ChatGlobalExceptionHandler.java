package com.cloud.baseai.infrastructure.exception;

import com.cloud.baseai.infrastructure.web.response.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * <h2>Chat模块全局异常处理器</h2>
 *
 * <p>异常处理是系统稳定性的最后一道防线。这个处理器确保即使在出现
 * 未预期错误的情况下，用户也能收到友好的错误信息，而不是技术性的堆栈跟踪。</p>
 */
@RestControllerAdvice
public class ChatGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatGlobalExceptionHandler.class);

    /**
     * 处理Chat完成服务异常
     */
    @ExceptionHandler(ChatCompletionException.class)
    public ResponseEntity<Map<String, Object>> handleChatCompletionException(ChatCompletionException ex) {

        Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error_code", ex.getErrorCode(),
                "message", "AI服务暂时不可用，请稍后重试",
                "details", ex.getMessage(),
                "timestamp", OffsetDateTime.now(),
                "type", "CHAT_COMPLETION_ERROR"
        );

        HttpStatus status = mapChatExceptionToHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * 处理对话业务异常
     */
    @ExceptionHandler(ChatBusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleChatBusinessException(ChatBusinessException ex) {
        log.warn("对话业务异常: code={}, message={}", ex.getErrorCode(), ex.getMessage());

        HttpStatus status = mapChatExceptionToHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status)
                .body(ApiResult.error("CHAT_BUSINESS_ERROR", ex.getMessage()));
    }

    /**
     * 处理对话技术异常
     */
    @ExceptionHandler(ChatTechnicalException.class)
    public ResponseEntity<ApiResult<Void>> handleChatTechnicalException(ChatTechnicalException ex) {
        log.error("对话技术异常: code={}, message={}", ex.getErrorCode(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error("CHAT_TECHNICAL_ERROR", "系统暂时无法处理您的请求，请稍后再试"));
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleGenericException(Exception ex) {
        log.error("对话模块未知异常", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error("CHAT_INTERNAL_ERROR", "对话系统发生未知错误"));
    }

    /**
     * 将对话异常代码映射为HTTP状态码
     */
    private HttpStatus mapChatExceptionToHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "THREAD_NOT_FOUND", "MESSAGE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "INVALID_MESSAGE_TYPE", "CONTENT_TOO_LONG" -> HttpStatus.BAD_REQUEST;
            case "RATE_LIMIT_EXCEEDED", "QUOTA_EXCEEDED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "UNAUTHORIZED_ACCESS" -> HttpStatus.FORBIDDEN;
            case "MODEL_UNAVAILABLE", "SERVICE_OVERLOAD" -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}