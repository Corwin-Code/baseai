package com.cloud.baseai.infrastructure.exception;

import com.cloud.baseai.infrastructure.i18n.MessageManager;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * <h2>统一错误响应格式</h2>
 *
 * <p>定义了API返回的标准错误响应格式。这个格式就像是一份标准的
 * 医疗诊断报告，包含了诊断结果、详细说明、建议措施等完整信息。</p>
 *
 * <p><b>响应结构：</b></p>
 * <ul>
 * <li><b>错误代码：</b>标准化的错误标识</li>
 * <li><b>错误消息：</b>用户友好的错误描述</li>
 * <li><b>详细信息：</b>可选的详细错误信息</li>
 * <li><b>时间戳：</b>错误发生的时间</li>
 * <li><b>追踪ID：</b>用于问题排查的唯一标识</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * 错误代码
     */
    private final String code;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 详细信息，可以包含字段验证错误、上下文信息等
     */
    private final Map<String, Object> details;

    /**
     * 错误发生时间戳
     */
    private final long timestamp;

    /**
     * 请求追踪ID，用于问题排查
     */
    private final String traceId;

    /**
     * 构造函数，创建基本的错误响应
     *
     * @param builder 构造器
     */
    public ErrorResponse(Builder builder) {
        this.code = builder.code;
        this.message = builder.message;
        this.details = builder.details.isEmpty() ? null : new HashMap<>(builder.details);
        this.timestamp = builder.timestamp;
        this.traceId = builder.traceId;
    }

    /**
     * 创建基本错误响应
     */
    public static ErrorResponse of(ErrorCode errorCode) {
        return builder()
                .code(errorCode.getCode())
                .message(MessageManager.getMessage(errorCode))
                .build();
    }

    /**
     * 创建带参数的错误响应
     */
    public static ErrorResponse of(ErrorCode errorCode, Object... args) {
        return builder()
                .code(errorCode.getCode())
                .message(MessageManager.getMessage(errorCode, args))
                .build();
    }

    /**
     * 创建简单错误响应的静态工厂方法
     *
     * @param code    错误代码
     * @param message 错误消息
     * @return 错误响应对象
     */
    public static ErrorResponse of(String code, String message) {
        return builder()
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 从业务异常创建错误响应
     */
    public static ErrorResponse from(BusinessException ex) {
        Builder builder = builder()
                .code(ex.getErrorCode().getCode())
                .message(ex.getLocalizedMessage())
                .timestamp(ex.getTimestamp());

        // 添加上下文信息
        if (ex.getContext() != null && !ex.getContext().isEmpty()) {
            builder.details(ex.getContext());
        }

        return builder.build();
    }

    /**
     * 创建验证错误响应的便捷方法
     *
     * @param message 验证错误消息
     * @return 配置好的验证错误响应
     */
    public static ErrorResponse validationError(String message) {
        return builder()
                .code("VALIDATION_ERROR")
                .message(message)
                .build();
    }

    /**
     * 创建业务规则错误响应的便捷方法
     *
     * @param message 业务错误消息
     * @return 配置好的业务错误响应
     */
    public static ErrorResponse businessError(String message) {
        return builder()
                .code("BUSINESS_ERROR")
                .message(message)
                .build();
    }

    /**
     * 创建系统错误响应的便捷方法
     *
     * @param message 系统错误消息
     * @return 配置好的系统错误响应
     */
    public static ErrorResponse systemError(String message) {
        return builder()
                .code("SYSTEM_ERROR")
                .message(message)
                .build();
    }

    /**
     * 创建Builder实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder模式构建器
     */
    public static class Builder {
        private String code;
        private String message;
        private Map<String, Object> details = new HashMap<>();
        private long timestamp = System.currentTimeMillis();
        private String traceId;

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder detail(String key, Object value) {
            this.details.put(key, value);
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details.putAll(details);
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(this);
        }
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
        if (this.details == null) {
            // 如果details为null，创建新的Map
            Map<String, Object> newDetails = new HashMap<>();
            newDetails.put(key, value);
            return builder()
                    .code(this.code)
                    .message(this.message)
                    .details(newDetails)
                    .timestamp(this.timestamp)
                    .traceId(this.traceId)
                    .build();
        } else {
            this.details.put(key, value);
            return this;
        }
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
}