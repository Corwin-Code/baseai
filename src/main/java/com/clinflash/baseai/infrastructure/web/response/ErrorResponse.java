package com.clinflash.baseai.infrastructure.web.response;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * <h2>结构化错误响应对象</h2>
 *
 * <p>专门用于复杂错误场景的响应格式，提供比简单错误消息更丰富的错误信息。</p>
 */
@Setter
@Getter
public class ErrorResponse {

    /**
     * 标准化的错误代码，便于程序化处理
     */
    private String code;

    /**
     * 人性化的错误消息，适合直接展示给用户
     */
    private String message;

    /**
     * 详细的错误上下文信息，支持任意结构的数据
     */
    private Map<String, Object> details;

    /**
     * 错误发生的时间戳
     */
    private long timestamp;

    /**
     * 构造函数，创建基本的错误响应
     *
     * @param code    错误代码
     * @param message 错误消息
     */
    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.details = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 添加错误详情的链式调用方法
     * 这种设计让错误信息的构建更加流畅和直观
     *
     * @param key   详情的键名
     * @param value 详情的值
     * @return 当前对象，支持链式调用
     */
    public ErrorResponse addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }

    /**
     * 批量添加错误详情
     *
     * @param detailsMap 详情映射表
     * @return 当前对象，支持链式调用
     */
    public ErrorResponse addDetails(Map<String, Object> detailsMap) {
        if (detailsMap != null) {
            this.details.putAll(detailsMap);
        }
        return this;
    }

    /**
     * 检查是否有详情信息
     *
     * @return true 如果存在详情信息
     */
    public boolean hasDetails() {
        return details != null && !details.isEmpty();
    }

    /**
     * 获取特定的详情信息
     *
     * @param key 详情键名
     * @return 详情值，如果不存在则返回null
     */
    public Object getDetail(String key) {
        return details != null ? details.get(key) : null;
    }

    /**
     * 创建简单错误响应的静态工厂方法
     *
     * @param code    错误代码
     * @param message 错误消息
     * @return 错误响应对象
     */
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message);
    }

    /**
     * 创建验证错误响应的便捷方法
     *
     * @param message 验证错误消息
     * @return 配置好的验证错误响应
     */
    public static ErrorResponse validationError(String message) {
        return new ErrorResponse("VALIDATION_ERROR", message);
    }

    /**
     * 创建业务规则错误响应的便捷方法
     *
     * @param message 业务错误消息
     * @return 配置好的业务错误响应
     */
    public static ErrorResponse businessError(String message) {
        return new ErrorResponse("BUSINESS_ERROR", message);
    }

    /**
     * 创建系统错误响应的便捷方法
     *
     * @param message 系统错误消息
     * @return 配置好的系统错误响应
     */
    public static ErrorResponse systemError(String message) {
        return new ErrorResponse("SYSTEM_ERROR", message);
    }
}