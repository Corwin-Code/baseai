package com.cloud.baseai.infrastructure.exception;

import com.cloud.baseai.infrastructure.web.response.ApiResult;
import com.cloud.baseai.infrastructure.web.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
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

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <h2>审计模块全局异常处理器</h2>
 *
 * <p>这个异常处理器就像是一个经验丰富的客服主管，无论遇到什么样的问题，
 * 都能够冷静地分析情况，给出合适的解决方案，并以专业友好的方式
 * 向用户说明问题的原因和解决方法。</p>
 *
 * <p><b>处理策略：</b></p>
 * <ul>
 * <li><b>分类处理：</b>根据异常类型采用不同的处理策略</li>
 * <li><b>信息脱敏：</b>避免向用户暴露敏感的技术细节</li>
 * <li><b>日志记录：</b>详细记录异常信息用于排查问题</li>
 * <li><b>统一格式：</b>所有错误响应都使用统一的格式</li>
 * </ul>
 *
 * <p><b>设计原则：</b></p>
 * <p>我们遵循"对用户友好，对开发者详细"的原则。用户看到的是简洁明了的错误信息，
 * 而开发者在日志中能够看到完整的技术细节，这样既保证了用户体验，
 * 又便于问题排查和系统维护。</p>
 */
@RestControllerAdvice(basePackages = "com.clinflash.baseai.adapter.web.audit")
public class AuditGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuditGlobalExceptionHandler.class);

    /**
     * 处理审计服务业务异常
     *
     * <p>这是最重要的异常处理方法，专门处理审计业务相关的异常。
     * 我们会根据异常的类型和严重程度来决定响应的HTTP状态码和错误信息。</p>
     *
     * @param ex      审计服务异常
     * @param request Web请求对象
     * @return 标准化的错误响应
     */
    @ExceptionHandler(AuditServiceException.class)
    public ResponseEntity<ApiResult<Object>> handleAuditServiceException(
            AuditServiceException ex, WebRequest request) {

        // 记录详细的异常信息，用于问题排查
        log.error("审计服务异常: errorCode={}, errorType={}, severity={}, message={}",
                ex.getErrorCode(), ex.getErrorType(), ex.getSeverity(), ex.getMessage(), ex);

        // 根据异常类型确定HTTP状态码
        HttpStatus httpStatus = determineHttpStatus(ex);

        // 构建错误响应
        ErrorResponse errorResponse = ErrorResponse.of(ex.getErrorCode(), ex.getMessage())
                .addDetail("errorType", ex.getErrorType().name())
                .addDetail("severity", ex.getSeverity().name())
                .addDetail("retryable", ex.isRetryable())
                .addDetail("timestamp", OffsetDateTime.now().toString());

        // 如果有上下文信息，也加入到响应中
        if (ex.getContext() != null) {
            errorResponse.addDetail("context", ex.getContext());
        }

        // 对于安全相关的异常，添加特殊标记
        if (ex.isSecurityRelated()) {
            errorResponse.addDetail("securityAlert", true);
            // 安全异常可能需要通知安全团队
            notifySecurityTeam(ex, request);
        }

        ApiResult<Object> result = ApiResult.error(errorResponse);

        return new ResponseEntity<>(result, httpStatus);
    }

    /**
     * 处理参数验证异常
     *
     * <p>当用户提供的参数不符合验证规则时，这个方法会被调用。
     * 我们会收集所有的验证错误，并以用户友好的方式展示给用户。</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        log.warn("参数验证失败: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse errorResponse = ErrorResponse.validationError("请求参数验证失败")
                .addDetail("fieldErrors", fieldErrors)
                .addDetail("rejectedValue", ex.getBindingResult().getTarget());

        ApiResult<Object> result = ApiResult.error(errorResponse);

        return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理绑定异常（通常是参数类型转换错误）
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResult<Object>> handleBindException(BindException ex) {

        log.warn("参数绑定失败: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse errorResponse = ErrorResponse.validationError("参数格式错误")
                .addDetail("fieldErrors", fieldErrors);

        ApiResult<Object> result = ApiResult.error(errorResponse);

        return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理约束违反异常（Bean Validation）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Object>> handleConstraintViolationException(
            ConstraintViolationException ex) {

        log.warn("约束验证失败: {}", ex.getMessage());

        Map<String, String> violations = new HashMap<>();
        Set<ConstraintViolation<?>> constraintViolations = ex.getConstraintViolations();

        for (ConstraintViolation<?> violation : constraintViolations) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            violations.put(propertyPath, message);
        }

        ErrorResponse errorResponse = ErrorResponse.validationError("数据约束验证失败")
                .addDetail("violations", violations);

        ApiResult<Object> result = ApiResult.error(errorResponse);

        return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理数据访问异常
     *
     * <p>数据库相关的异常通常是技术性问题，我们需要向用户提供简洁的信息，
     * 同时在日志中记录详细的技术细节。</p>
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResult<Object>> handleDataAccessException(
            DataAccessException ex, WebRequest request) {

        log.error("数据访问异常: requestURI={}", request.getDescription(false), ex);

        // 向用户提供简化的错误信息，避免暴露数据库细节
        ErrorResponse errorResponse = ErrorResponse.systemError("数据处理暂时不可用，请稍后重试")
                .addDetail("errorType", "DATABASE_ERROR")
                .addDetail("retryable", true);

        ApiResult<Object> result = ApiResult.error(errorResponse);

        return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        log.warn("参数异常: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.validationError(ex.getMessage())
                .addDetail("errorType", "INVALID_ARGUMENT");

        ApiResult<Object> result = ApiResult.error(errorResponse);

        return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理运行时异常（兜底处理）
     *
     * <p>这是最后的安全网，捕获所有其他未被特定处理的运行时异常。
     * 我们需要确保系统的稳定性，不让任何异常导致系统崩溃。</p>
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResult<Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        log.error("未预期的运行时异常: requestURI={}", request.getDescription(false), ex);

        ErrorResponse errorResponse = ErrorResponse.systemError("系统内部错误，请稍后重试")
                .addDetail("errorType", "UNEXPECTED_ERROR")
                .addDetail("requestInfo", extractRequestInfo(request));

        ApiResult<Object> result = ApiResult.error(errorResponse);

        return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 处理通用异常（最终兜底）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Object>> handleGeneralException(
            Exception ex, WebRequest request) {

        log.error("系统异常: requestURI={}", request.getDescription(false), ex);

        ErrorResponse errorResponse = ErrorResponse.systemError("系统暂时不可用")
                .addDetail("errorType", "SYSTEM_ERROR")
                .addDetail("requestInfo", extractRequestInfo(request));

        ApiResult<Object> result = ApiResult.error(errorResponse);

        return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // =================== 私有辅助方法 ===================

    /**
     * 根据审计服务异常确定HTTP状态码
     *
     * <p>不同类型的业务异常对应不同的HTTP状态码，这样客户端可以
     * 根据状态码来决定如何处理错误。</p>
     */
    private HttpStatus determineHttpStatus(AuditServiceException ex) {
        return switch (ex.getErrorType()) {
            case BUSINESS_ERROR -> HttpStatus.BAD_REQUEST;           // 400
            case SECURITY_ERROR -> HttpStatus.FORBIDDEN;            // 403
            case CONFIGURATION_ERROR -> HttpStatus.SERVICE_UNAVAILABLE; // 503
            case TECHNICAL_ERROR, SYSTEM_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR; // 500
        };
    }

    /**
     * 通知安全团队处理安全相关异常
     *
     * <p>当发生安全相关异常时，我们需要及时通知安全团队。
     * 这里可以集成邮件、短信、钉钉等通知方式。</p>
     */
    private void notifySecurityTeam(AuditServiceException ex, WebRequest request) {
        try {
            // 这里可以实现具体的通知逻辑
            // 比如发送邮件、调用告警接口等
            log.warn("安全异常需要关注: errorCode={}, message={}, request={}",
                    ex.getErrorCode(), ex.getMessage(), request.getDescription(false));

            // 实际项目中可以这样实现：
            // notificationService.sendSecurityAlert(ex, request);

        } catch (Exception e) {
            log.error("发送安全告警失败", e);
            // 通知失败不应该影响主要的异常处理流程
        }
    }

    /**
     * 提取请求信息用于错误分析
     *
     * <p>收集一些基本的请求信息，帮助开发者分析问题。
     * 注意不要收集敏感信息。</p>
     */
    private Map<String, Object> extractRequestInfo(WebRequest request) {
        Map<String, Object> requestInfo = new HashMap<>();

        try {
            requestInfo.put("description", request.getDescription(false));
            requestInfo.put("sessionId", request.getSessionId());

            // 提取一些安全的请求头信息
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                requestInfo.put("userAgent", userAgent.length() > 200 ?
                        userAgent.substring(0, 200) + "..." : userAgent);
            }

            requestInfo.put("timestamp", OffsetDateTime.now().toString());

        } catch (Exception e) {
            log.debug("提取请求信息失败", e);
            requestInfo.put("error", "无法提取请求信息");
        }

        return requestInfo;
    }

    /**
     * 判断异常是否需要立即关注
     *
     * <p>某些异常可能表示系统的严重问题，需要立即引起注意。</p>
     */
    private boolean requiresImmediateAttention(Exception ex) {
        // 安全相关异常
        if (ex instanceof AuditServiceException auditEx) {
            return auditEx.requiresImmediateAttention();
        }

        // 数据库连接问题
        if (ex instanceof DataAccessException) {
            String message = ex.getMessage();
            if (message != null) {
                return message.contains("Connection") ||
                        message.contains("timeout") ||
                        message.contains("deadlock");
            }
        }

        return false;
    }
}