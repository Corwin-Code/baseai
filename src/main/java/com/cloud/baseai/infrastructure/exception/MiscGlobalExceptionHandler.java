package com.cloud.baseai.infrastructure.exception;

import com.cloud.baseai.infrastructure.web.response.ApiResult;
import com.cloud.baseai.infrastructure.web.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <h1>Misc模块全局异常处理器</h1>
 */
@RestControllerAdvice
public class MiscGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MiscGlobalExceptionHandler.class);

    /**
     * 处理Misc模块的业务异常
     *
     * <p>业务异常通常是用户操作不当或者业务规则限制导致的，比如文件太大、
     * 模板名称重复等。这类异常不是程序错误，而是正常的业务流程的一部分，
     * 所以我们要给用户友好的提示，而不是吓人的错误堆栈。</p>
     *
     * @param ex 业务异常
     * @return 格式化的错误响应
     */
    @ExceptionHandler(MiscBusinessException.class)
    public ResponseEntity<ApiResult<Object>> handleBusinessException(MiscBusinessException ex) {
        log.warn("Misc业务异常: code={}, message={}", ex.getErrorCode(), ex.getMessage());

        // 业务异常通常返回400 Bad Request，表示客户端请求有问题
        ErrorResponse errorResponse = ErrorResponse.of(ex.getErrorCode(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理Misc模块的技术异常
     *
     * <p>技术异常通常是系统内部错误，比如数据库连接失败、文件系统错误等。
     * 这类异常用户无法解决，需要技术人员介入。我们要记录详细的错误信息
     * 供开发者排查，但给用户的提示要简洁友好。</p>
     *
     * @param ex 技术异常
     * @return 格式化的错误响应
     */
    @ExceptionHandler(MiscTechnicalException.class)
    public ResponseEntity<ApiResult<Object>> handleTechnicalException(MiscTechnicalException ex) {
        log.error("Misc技术异常: code={}, message={}", ex.getErrorCode(), ex.getMessage(), ex);

        // 技术异常通常返回500 Internal Server Error
        ErrorResponse errorResponse = ErrorResponse.of(ex.getErrorCode(), "系统处理请求时发生错误，请稍后重试");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理参数验证异常
     *
     * <p>当用户提交的数据不符合验证规则时（比如邮箱格式错误、密码太短等），
     * Spring会抛出MethodArgumentNotValidException。我们需要将这些技术性的
     * 验证消息转换为用户能理解的友好提示。</p>
     *
     * @param ex 参数验证异常
     * @return 包含详细验证错误信息的响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("参数验证失败: {}", ex.getMessage());

        // 收集所有字段的验证错误
        Map<String, Object> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("VALIDATION_FAILED")
                .message("请求参数验证失败")
                .details(fieldErrors)
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理约束验证异常
     *
     * <p>这是另一种类型的验证异常，通常发生在方法级别的参数验证上。
     * 比如@PathVariable或@RequestParam的验证失败。</p>
     *
     * @param ex 约束验证异常
     * @return 格式化的错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Object>> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("约束验证失败: {}", ex.getMessage());

        // 提取验证错误消息
        String errorMessage = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("CONSTRAINT_VIOLATION")
                .message("参数约束验证失败: " + errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理文件上传大小超限异常
     *
     * <p>当用户上传的文件超过系统设定的大小限制时，Spring会抛出这个异常。
     * 我们需要给用户明确的提示，告诉他们文件太大了，应该怎么办。</p>
     *
     * @param ex 文件大小超限异常
     * @return 友好的错误提示
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResult<Object>> handleMaxUploadSizeException(MaxUploadSizeExceededException ex) {
        log.warn("文件上传大小超限: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("FILE_SIZE_EXCEEDED")
                .message("上传文件大小超出限制，请选择较小的文件")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理非法参数异常
     *
     * <p>当方法接收到不合法的参数时（比如null值、格式错误等），
     * 会抛出IllegalArgumentException。这通常表示客户端传递了错误的数据。</p>
     *
     * @param ex 非法参数异常
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("非法参数异常: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("ILLEGAL_ARGUMENT")
                .message("请求参数不正确: " + ex.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理运行时异常
     *
     * <p>RuntimeException是所有运行时异常的父类。当我们没有专门的处理器
     * 来处理某个具体异常时，这个方法就是"兜底"的处理器。</p>
     *
     * @param ex 运行时异常
     * @return 通用错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResult<Object>> handleRuntimeException(RuntimeException ex) {
        log.error("未处理的运行时异常", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("RUNTIME_ERROR")
                .message("系统运行时发生错误，请联系技术支持")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理所有其他未捕获的异常
     *
     * <p>这是最后的"安全网"，捕获所有其他类型的异常。
     * 就像医院的急诊科，不管什么病都会先接收，然后再分流到专科。</p>
     *
     * @param ex 任何异常
     * @return 通用错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Object>> handleGenericException(Exception ex) {
        log.error("系统发生未预期的异常", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("SYSTEM_ERROR")
                .message("系统发生未知错误，请联系管理员")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(errorResponse));
    }

    /**
     * 处理空指针异常
     *
     * <p>空指针异常是Java编程中最常见的异常之一。虽然在良好的编程实践中
     * 应该避免NPE，但如果发生了，我们也要优雅地处理。</p>
     *
     * @param ex 空指针异常
     * @return 错误响应
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResult<Object>> handleNullPointerException(NullPointerException ex) {
        log.error("空指针异常", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("NULL_POINTER_ERROR")
                .message("系统内部数据异常，请稍后重试")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(errorResponse));
    }
}