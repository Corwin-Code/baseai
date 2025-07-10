package com.clinflash.baseai.infrastructure.exception;

import com.clinflash.baseai.infrastructure.web.response.ApiResult;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * <h2>用户全局异常处理器</h2>
 *
 * <p>全局异常处理器确保所有未被捕获的异常都能得到适当的处理，
 * 并返回标准格式的错误响应。</p>
 */
@ControllerAdvice
public class UserGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(UserGlobalExceptionHandler.class);

    /**
     * 处理用户业务异常
     */
    @ExceptionHandler(UserBusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleUserBusinessException(UserBusinessException ex) {
        log.warn("用户业务异常: code={}, message={}", ex.getErrorCode(), ex.getMessage());

        HttpStatus status = mapUserExceptionToHttpStatus(ex.getErrorCode());
        return ResponseEntity.status(status)
                .body(ApiResult.error(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * 处理用户技术异常
     */
    @ExceptionHandler(UserTechnicalException.class)
    public ResponseEntity<ApiResult<Void>> handleUserTechnicalException(UserTechnicalException ex) {
        log.error("用户技术异常: code={}, message={}", ex.getErrorCode(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(ex.getErrorCode(), "系统暂时无法处理您的请求，请稍后再试"));
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleGenericException(Exception ex) {
        log.error("用户模块未知异常", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error("USER_INTERNAL_ERROR", "用户系统发生未知错误"));
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<Map<String, Object>> handleValidationException(Exception ex) {
        log.warn("参数验证异常: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", "VALIDATION_ERROR");
        response.put("message", "请求参数不正确，请检查后重试");
        response.put("timestamp", System.currentTimeMillis());
        response.put("status", HttpStatus.BAD_REQUEST.value());

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 将用户异常代码映射为HTTP状态码
     */
    private HttpStatus mapUserExceptionToHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "USER_NOT_FOUND", "TENANT_NOT_FOUND", "ROLE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "DUPLICATE_USERNAME", "DUPLICATE_EMAIL", "DUPLICATE_TENANT_NAME" -> HttpStatus.CONFLICT;
            case "INVALID_PASSWORD", "WEAK_PASSWORD", "INVALID_EMAIL_FORMAT" -> HttpStatus.BAD_REQUEST;
            case "UNAUTHORIZED_ACCESS", "INSUFFICIENT_PERMISSIONS" -> HttpStatus.FORBIDDEN;
            case "INVITATION_EXPIRED", "INVITATION_ALREADY_USED" -> HttpStatus.GONE;
            case "ACCOUNT_LOCKED", "ACCOUNT_SUSPENDED" -> HttpStatus.LOCKED;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}