package com.cloud.baseai.infrastructure.exception;

import com.cloud.baseai.infrastructure.web.response.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <h2>KnowledgeBase模块全局异常处理器</h2>
 *
 * <p>异常处理是系统稳定性的最后一道防线。这个处理器确保即使在出现
 * 未预期错误的情况下，用户也能收到友好的错误信息，而不是技术性的堆栈跟踪。</p>
 */
@RestControllerAdvice
public class KbGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(KbGlobalExceptionHandler.class);

    /**
     * 处理业务异常
     *
     * <p>业务异常通常是由于用户操作不当或数据冲突引起的，我们会返回详细的错误信息
     * 和建议，帮助用户理解问题并采取正确的行动。</p>
     */
    @ExceptionHandler(KbBusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusinessException(KbBusinessException ex) {
        log.warn("业务异常: code={}, message={}", ex.getErrorCode(), ex.getMessage());

        HttpStatus status = mapBusinessExceptionToHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status)
                .body(ApiResult.error(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * 处理技术异常
     *
     * <p>技术异常通常涉及系统内部错误，我们会隐藏具体的技术细节，
     * 向用户返回友好的错误信息，同时记录详细的错误日志供开发人员分析。</p>
     */
    @ExceptionHandler(KbTechnicalException.class)
    public ResponseEntity<ApiResult<Void>> handleTechnicalException(KbTechnicalException ex) {
        log.error("技术异常: code={}, message={}", ex.getErrorCode(), ex.getMessage(), ex);

        // 对外隐藏技术细节，返回通用错误信息
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(ex.getErrorCode(), "系统暂时无法处理您的请求，请稍后再试"));
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler({
            org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.validation.BindException.class,
            jakarta.validation.ConstraintViolationException.class
    })
    public ResponseEntity<ApiResult<Void>> handleValidationException(Exception ex) {
        log.warn("参数验证失败: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error("VALIDATION_ERROR", "请求参数不正确，请检查后重试"));
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleGenericException(Exception ex) {
        log.error("未知异常", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error("INTERNAL_ERROR", "系统发生未知错误，请联系技术支持"));
    }

    /**
     * 将业务异常代码映射为HTTP状态码
     *
     * <p>不同类型的业务错误对应不同的HTTP状态码，这让API的使用更加标准化和直观。</p>
     */
    private HttpStatus mapBusinessExceptionToHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "DOCUMENT_NOT_FOUND", "CHUNK_NOT_FOUND", "TAG_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "DUPLICATE_DOCUMENT_TITLE", "DUPLICATE_DOCUMENT_CONTENT", "DUPLICATE_TAG_NAME" -> HttpStatus.CONFLICT;
            case "DOCUMENT_TOO_LARGE", "BATCH_SIZE_EXCEEDED", "UNSUPPORTED_FILE_TYPE" -> HttpStatus.PAYLOAD_TOO_LARGE;
            case "INVALID_SIMILARITY_THRESHOLD", "INVALID_PAGE_PARAMETERS", "INVALID_TOP_K" -> HttpStatus.BAD_REQUEST;
            case "UNAUTHORIZED_OPERATION", "TENANT_MISMATCH" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
