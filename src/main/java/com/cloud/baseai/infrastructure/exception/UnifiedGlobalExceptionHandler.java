package com.cloud.baseai.infrastructure.exception;

import com.cloud.baseai.infrastructure.config.ErrorHandlingProperties;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <h1>统一全局异常处理器</h1>
 *
 * <p>这是整个系统错误处理的核心控制器，就像是一个经验丰富的
 * 急诊科医生，能够快速诊断各种"病症"，并给出合适的"治疗方案"。</p>
 *
 * <p><b>设计理念：</b></p>
 * <p>这个处理器采用"分层处理，统一响应"的策略。不同类型的异常
 * 会被分发到不同的处理方法，但最终都会返回统一格式的响应。
 * 这确保了API的一致性，同时也方便了客户端的错误处理。</p>
 *
 * <p><b>处理层次：</b></p>
 * <ol>
 * <li><b>业务异常：</b>优先级最高，提供详细的业务错误信息</li>
 * <li><b>验证异常：</b>处理参数验证失败的情况</li>
 * <li><b>系统异常：</b>处理技术性问题，保护敏感信息</li>
 * <li><b>未知异常：</b>最后的安全网，确保系统不会崩溃</li>
 * </ol>
 */
@RequiredArgsConstructor
@RestControllerAdvice
public class UnifiedGlobalExceptionHandler {

    private final ErrorHandlingProperties errorProps;

    private static final Logger log = LoggerFactory.getLogger(UnifiedGlobalExceptionHandler.class);

    /**
     * 处理统一业务异常
     *
     * <p>这是最重要的异常处理方法，处理所有继承自BusinessException的异常。
     * 我们会根据错误代码的前缀来判断HTTP状态码，并返回详细的错误信息。</p>
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Object>> handleBusinessException(
            BusinessException ex, WebRequest request) {

        // 生成追踪ID用于问题排查
        String traceId = generateTraceId();
        MDC.put("traceId", traceId);

        // 记录业务异常日志
        log.warn("业务异常 [{}]: errorCode={}, message={}",
                traceId, ex.getErrorCode().getCode(), ex.getLocalizedMessage());

        // 构建详细的错误响应
        ErrorResponse errorResponse = ErrorResponse.from(ex)
                .addDetail("path", extractPath(request));

        if (errorProps.isIncludeTraceId()) {
            errorResponse.addDetail("traceId", traceId);
        }

        if (errorProps.isIncludeException()) {
            errorResponse.addDetail("exception", ex.getClass().getSimpleName());
        }
        if (errorProps.isIncludeStackTrace()) {
            errorResponse.addDetail("stackTrace", ex.getStackTrace());
        }

        // 如果是安全相关的错误，需要特殊处理
        if (errorProps.isSecurityEventLogging() && isSecurityRelatedError(ex.getErrorCode())) {
            logSecurityEvent(ex, request, traceId);
        }

        // 根据错误代码确定HTTP状态码
        HttpStatus httpStatus = determineHttpStatus(ex.getErrorCode());
        ApiResult<Object> result = ApiResult.error(errorResponse);

        return ResponseEntity.status(httpStatus).body(result);
    }

    /**
     * 处理参数验证异常
     *
     * <p>当用户提供的参数不符合验证规则时，这个方法会被调用。
     * 我们会收集所有的验证错误，并以用户友好的方式展示。</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Object>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        String traceId = generateTraceId();
        MDC.put("traceId", traceId);

        log.warn("参数验证失败 [{}]: {}", traceId, ex.getMessage());

        // 收集所有字段的验证错误
        Map<String, Object> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("请求参数验证失败")
                .detail("fieldErrors", fieldErrors)
                .detail("path", extractPath(request))
                .detail("traceId", errorProps.isIncludeTraceId() ? traceId : null)
                .build();

        ApiResult<Object> result = ApiResult.error(errorResponse);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 处理约束验证异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Object>> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {

        String traceId = generateTraceId();
        log.warn("约束验证失败 [{}]: {}", traceId, ex.getMessage());

        // 提取验证错误信息
        Map<String, String> violations = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("CONSTRAINT_VIOLATION")
                .message("数据约束验证失败")
                .detail("violations", violations)
                .detail("traceId", errorProps.isIncludeTraceId() ? traceId : null)
                .build();

        ApiResult<Object> result = ApiResult.error(errorResponse);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResult<Object>> handleBindException(
            BindException ex, WebRequest request) {

        String traceId = generateTraceId();
        log.warn("参数绑定失败 [{}]: {}", traceId, ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("BINDING_ERROR")
                .message("参数绑定失败")
                .detail("fieldErrors", fieldErrors)
                .detail("traceId", errorProps.isIncludeTraceId() ? traceId : null)
                .build();

        ApiResult<Object> result = ApiResult.error(errorResponse);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResult<Object>> handleMaxUploadSizeException(
            MaxUploadSizeExceededException ex, WebRequest request) {

        String traceId = generateTraceId();
        log.warn("文件上传大小超限 [{}]: {}", traceId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("FILE_SIZE_EXCEEDED")
                .message("上传文件大小超出限制")
                .detail("maxSize", ex.getMaxUploadSize())
                .detail("traceId", errorProps.isIncludeTraceId() ? traceId : null)
                .build();

        ApiResult<Object> result = ApiResult.error(errorResponse);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(result);
    }

    /**
     * 处理数据访问异常
     *
     * <p>数据库相关的异常通常是技术性问题，我们需要保护敏感信息，
     * 向用户提供简洁的错误信息，同时在日志中记录详细的技术细节。</p>
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResult<Object>> handleDataAccessException(
            DataAccessException ex, WebRequest request) {

        String traceId = generateTraceId();
        log.error("数据访问异常 [{}]: {}", traceId, ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("DATABASE_ERROR")
                .message("数据处理暂时不可用，请稍后重试")
                .detail("retryable", true)
                .detail("traceId", errorProps.isIncludeTraceId() ? traceId : null)
                .build();

        ApiResult<Object> result = ApiResult.error(errorResponse);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        String traceId = generateTraceId();
        log.warn("非法参数异常 [{}]: {}", traceId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("ILLEGAL_ARGUMENT")
                .message("请求参数不正确: " + ex.getMessage())
                .detail("traceId", errorProps.isIncludeTraceId() ? traceId : null)
                .build();

        ApiResult<Object> result = ApiResult.error(errorResponse);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 处理运行时异常（兜底处理）
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResult<Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        String traceId = generateTraceId();
        log.error("未预期的运行时异常 [{}]: {}", traceId, ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("RUNTIME_ERROR")
                .message("系统运行时发生错误，请稍后重试")
                .detail("traceId", errorProps.isIncludeTraceId() ? traceId : null)
                .detail("recoverable", isRecoverableException(ex))
                .build();

        ApiResult<Object> result = ApiResult.error(errorResponse);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    /**
     * 处理通用异常（最终兜底）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Object>> handleGeneralException(
            Exception ex, WebRequest request) {

        String traceId = generateTraceId();
        log.error("系统异常 [{}]: {}", traceId, ex.getMessage(), ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("SYSTEM_ERROR")
                .message("系统暂时不可用，请稍后重试")
                .detail("traceId", errorProps.isIncludeTraceId() ? traceId : null)
                .build();

        ApiResult<Object> result = ApiResult.error(errorResponse);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    // =================== 私有辅助方法 ===================

    /**
     * 根据错误代码确定HTTP状态码
     */
    private HttpStatus determineHttpStatus(ErrorCode errorCode) {
        String code = errorCode.getCode();

        // 根据错误代码前缀和具体错误类型判断
        if (code.startsWith("SYS_AUTH_") || code.startsWith("BIZ_USER_006") || code.startsWith("BIZ_USER_007")) {
            return HttpStatus.UNAUTHORIZED;
        }

        if (code.startsWith("SYS_PERM_") || code.startsWith("BIZ_MCP_006")) {
            return HttpStatus.FORBIDDEN;
        }

        if (code.contains("_NOT_FOUND") || code.endsWith("_001")) {
            return HttpStatus.NOT_FOUND;
        }

        if (code.contains("_EXISTS") || code.endsWith("_002")) {
            return HttpStatus.CONFLICT;
        }

        if (code.startsWith("SYS_NET_") || code.startsWith("EXT_")) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }

        if (code.startsWith("PARAM_") || code.contains("_INVALID")) {
            return HttpStatus.BAD_REQUEST;
        }

        if (code.contains("_EXCEEDED") || code.contains("_LIMIT")) {
            return HttpStatus.TOO_MANY_REQUESTS;
        }

        // 默认返回400
        return HttpStatus.BAD_REQUEST;
    }

    /**
     * 判断是否是安全相关的错误
     */
    private boolean isSecurityRelatedError(ErrorCode errorCode) {
        String code = errorCode.getCode();
        return code.startsWith("SYS_AUTH_") ||
                code.startsWith("SYS_PERM_") ||
                code.equals("BIZ_USER_006") ||
                code.equals("BIZ_USER_007");
    }

    /**
     * 记录安全事件
     */
    private void logSecurityEvent(BusinessException ex, WebRequest request, String traceId) {
        Map<String, Object> securityContext = new HashMap<>();
        securityContext.put("traceId", traceId);
        securityContext.put("errorCode", ex.getErrorCode().getCode());
        securityContext.put("path", extractPath(request));
        securityContext.put("userAgent", request.getHeader("User-Agent"));
        securityContext.put("timestamp", System.currentTimeMillis());

        log.warn("安全事件 [{}]: {}", traceId, securityContext);

        // 这里可以集成到安全监控系统
        // securityMonitoringService.reportSecurityEvent(securityContext);
    }

    /**
     * 生成追踪ID
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 提取请求路径
     */
    private String extractPath(WebRequest request) {
        String description = request.getDescription(false);
        return description.substring(description.indexOf("=") + 1);
    }

    /**
     * 判断异常是否可恢复
     */
    private boolean isRecoverableException(RuntimeException ex) {
        String message = ex.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("timeout") ||
                    lowerMessage.contains("connection") ||
                    lowerMessage.contains("temporary") ||
                    lowerMessage.contains("retry");
        }
        return false;
    }
}