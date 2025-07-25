package com.cloud.baseai.infrastructure.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * <h2>统一API响应格式</h2>
 *
 * <p>系统的标准响应格式，为所有HTTP接口提供一致的数据结构。</p>
 *
 * <p><b>使用示例：</b></p>
 * <pre>
 * // 成功响应
 * return ApiResult.success(documentDTO, "文档上传成功");
 *
 * // 失败响应
 * return ApiResult.error("VALIDATION_ERROR", "参数验证失败");
 * </pre>
 *
 * @param <T> 响应数据的类型，使用泛型确保类型安全
 */
@Setter
@Getter
public class ApiResult<T> {

    /**
     * 操作是否成功的标识
     */
    private boolean success;

    /**
     * 响应消息，提供操作结果的描述信息
     */
    private String message;

    /**
     * 响应数据，业务层返回的具体数据内容
     */
    private T data;

    /**
     * 错误代码，用于程序化的错误处理
     */
    private String errorCode;

    /**
     * 响应时间戳，便于调试和日志分析
     */
    private long timestamp;

    /**
     * 私有构造函数，强制使用静态工厂方法创建实例
     * 这种设计模式提供了更好的API控制和未来的扩展性
     */
    private ApiResult(boolean success, String message, T data, String errorCode) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 创建成功响应的静态工厂方法
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功的API响应对象
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(true, "操作成功", data, null);
    }

    /**
     * 创建带自定义消息的成功响应
     *
     * @param data    响应数据
     * @param message 成功消息
     * @param <T>     数据类型
     * @return 成功的API响应对象
     */
    public static <T> ApiResult<T> success(T data, String message) {
        return new ApiResult<>(true, message, data, null);
    }

    /**
     * 创建错误响应的静态工厂方法
     *
     * @param errorCode 错误代码，用于程序化处理
     * @param message   错误消息，用于用户展示
     * @param <T>       数据类型
     * @return 错误的API响应对象
     */
    public static <T> ApiResult<T> error(String errorCode, String message) {
        return new ApiResult<>(false, message, null, errorCode);
    }

    /**
     * 创建简单错误响应，错误代码自动生成
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 错误的API响应对象
     */
    public static <T> ApiResult<T> error(String message) {
        return error("OPERATION_FAILED", message);
    }

    /**
     * 创建详细错误响应
     *
     * @param response 错误响应
     * @return 错误的API响应对象
     */
    @SuppressWarnings("unchecked")
    public static <T> ApiResult<T> error(ErrorResponse response) {
        return new <T>ApiResult<T>(
                false,
                response.getMessage(),
                (T) response.getDetails(),
                response.getCode()
        );
    }

    /**
     * 检查响应是否表示失败
     *
     * @return true 如果操作失败
     */
    public boolean isFailure() {
        return !success;
    }

    /**
     * 检查是否有错误代码
     *
     * @return true 如果存在错误代码
     */
    public boolean hasErrorCode() {
        return errorCode != null && !errorCode.trim().isEmpty();
    }
}