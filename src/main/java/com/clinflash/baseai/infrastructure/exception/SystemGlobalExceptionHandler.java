package com.clinflash.baseai.infrastructure.exception;

import com.clinflash.baseai.infrastructure.web.response.ApiResult;
import com.clinflash.baseai.infrastructure.web.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * <h1>System模块全局异常处理器</h1>
 */
@RestControllerAdvice
public class SystemGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SystemGlobalExceptionHandler.class);

    /**
     * 处理System模块的业务异常
     *
     * <p>系统管理的业务异常通常涉及配置错误、任务状态不当、权限限制等。
     * 这些异常需要特别小心处理，既要给管理员足够的信息来解决问题，
     * 又不能暴露过多的系统内部细节。</p>
     *
     * @param ex 系统业务异常
     * @return 安全的错误响应
     */
    @ExceptionHandler(SystemBusinessException.class)
    public ResponseEntity<ApiResult<Object>> handleSystemBusinessException(SystemBusinessException ex) {
        log.warn("系统业务异常: code={}, message={}", ex.getErrorCode(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理System模块的技术异常
     *
     * <p>系统技术异常往往是更严重的问题，比如数据库连接失败、文件系统异常、
     * 外部服务不可用等。这类异常需要立即关注，因为可能影响整个系统的稳定性。</p>
     *
     * @param ex 系统技术异常
     * @return 安全的错误响应
     */
    @ExceptionHandler(SystemTechnicalException.class)
    public ResponseEntity<ApiResult<Object>> handleSystemTechnicalException(SystemTechnicalException ex) {
        // 系统技术异常需要详细记录，便于运维团队快速响应
        log.error("严重的系统技术异常: code={}, message={}", ex.getErrorCode(), ex.getMessage(), ex);

        // 对外提供的错误信息要谨慎，避免暴露系统内部信息
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ex.getErrorCode())
                .message("系统服务暂时不可用，技术团队正在处理")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理访问权限异常
     *
     * <p>系统管理功能通常需要高级权限，当用户试图访问超出其权限范围的功能时，
     * 我们需要明确但不过分详细地拒绝访问。这就像银行金库的门卫，
     * 会礼貌但坚决地拒绝无关人员进入。</p>
     *
     * @param ex 访问拒绝异常
     * @return 权限不足的错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        // 权限异常需要记录，但级别不用太高，因为这是正常的安全机制
        log.info("访问权限不足: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("ACCESS_DENIED")
                .message("您没有权限执行此操作，请联系系统管理员")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理任务超时异常
     *
     * <p>系统任务可能因为各种原因超时，比如数据量过大、外部服务响应慢等。
     * 超时异常需要特殊处理，因为任务可能处于不确定状态。</p>
     *
     * @param ex 超时异常
     * @return 超时错误响应
     */
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ApiResult<Object>> handleTimeoutException(TimeoutException ex) {
        log.warn("系统任务执行超时: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("TASK_TIMEOUT")
                .message("操作执行时间过长已超时，请检查系统负载或稍后重试")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理参数验证异常
     *
     * <p>系统管理接口的参数验证失败可能意味着客户端传递了错误的配置值或任务参数。
     * 我们需要提供清晰的验证错误信息，帮助管理员纠正输入。</p>
     *
     * @param ex 参数验证异常
     * @return 详细的验证错误信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("系统管理参数验证失败: {}", ex.getMessage());

        // 收集所有字段的验证错误
        Map<String, Object> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("SYSTEM_VALIDATION_FAILED")
                .message("系统配置参数验证失败")
                .details(fieldErrors)
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理约束验证异常
     *
     * @param ex 约束验证异常
     * @return 格式化的错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("系统管理约束验证失败: {}", ex.getMessage());

        String errorMessage = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("SYSTEM_CONSTRAINT_VIOLATION")
                .message("系统参数约束验证失败: " + errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理非法状态异常
     *
     * <p>当系统或任务处于不允许执行某操作的状态时，会抛出IllegalStateException。
     * 比如试图重试一个已经成功的任务，或在系统维护期间执行某些操作。</p>
     *
     * @param ex 非法状态异常
     * @return 状态错误响应
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResult<Object>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("系统状态异常: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("ILLEGAL_STATE")
                .message("当前系统状态不允许执行此操作: " + ex.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理并发修改异常
     *
     * <p>在多用户环境中，可能出现多个管理员同时修改系统配置的情况。
     * 并发修改异常提醒用户需要重新加载数据后再进行修改。</p>
     *
     * @param ex 并发修改异常
     * @return 并发冲突错误响应
     */
    @ExceptionHandler(java.util.ConcurrentModificationException.class)
    public ResponseEntity<ApiResult<Object>> handleConcurrentModificationException(
            java.util.ConcurrentModificationException ex) {
        log.warn("并发修改冲突: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("CONCURRENT_MODIFICATION")
                .message("配置已被其他管理员修改，请刷新页面后重试")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理资源不足异常
     *
     * <p>当系统资源（如内存、磁盘空间、数据库连接等）不足时，
     * 某些系统管理操作可能无法执行。这类异常需要立即关注。</p>
     *
     * @param ex 资源不足异常
     * @return 资源不足错误响应
     */
    @ExceptionHandler(OutOfMemoryError.class)
    public ResponseEntity<ApiResult<Object>> handleOutOfMemoryError(OutOfMemoryError ex) {
        // 内存不足是严重问题，需要立即记录
        log.error("系统内存不足", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("INSUFFICIENT_RESOURCES")
                .message("系统资源不足，无法处理当前请求")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理运行时异常
     *
     * <p>作为系统管理模块的兜底异常处理器，这里要特别注意安全性，
     * 避免泄露敏感的系统信息。</p>
     *
     * @param ex 运行时异常
     * @return 安全的通用错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResult<Object>> handleRuntimeException(RuntimeException ex) {
        // 系统管理模块的运行时异常可能比较严重，需要详细记录
        log.error("系统管理模块发生未处理的运行时异常", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("SYSTEM_RUNTIME_ERROR")
                .message("系统管理服务异常，请联系技术支持")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理所有其他未捕获的异常
     *
     * <p>系统管理模块的最后防线，确保任何异常都能得到安全的处理。</p>
     *
     * @param ex 任何异常
     * @return 安全的通用错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Object>> handleGenericException(Exception ex) {
        // 所有未预期的异常都需要详细记录，因为可能影响系统稳定性
        log.error("系统管理模块发生未预期的严重异常", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("SYSTEM_CRITICAL_ERROR")
                .message("系统发生严重错误，请立即联系系统管理员")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(errorResponse));
    }
}