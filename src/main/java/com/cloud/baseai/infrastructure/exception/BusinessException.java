package com.cloud.baseai.infrastructure.exception;

import com.cloud.baseai.infrastructure.i18n.MessageManager;
import lombok.Getter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * <h2>统一业务异常基类</h2>
 *
 * <p>所有业务异常都应该继承这个基类，确保异常信息的标准化。</p>
 *
 * <p><b>特点：</b></p>
 * <ul>
 * <li><b>丰富上下文：</b>支持传递任意的上下文信息</li>
 * <li><b>参数化消息：</b>支持带参数的错误消息</li>
 * <li><b>链式构建：</b>提供builder模式的构建方式</li>
 * <li><b>类型安全：</b>编译期就能发现错误代码问题</li>
 * </ul>
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误代码，标识具体的错误类型
     */
    private final ErrorCode errorCode;

    /**
     * 错误消息参数，用于格式化消息
     */
    private final Object[] messageArgs;

    /**
     * 错误上下文信息，包含导致错误的详细信息
     */
    private final Map<String, Object> context;

    /**
     * 异常发生的时间戳
     */
    private final long timestamp;

    /**
     * 基础构造函数
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.messageArgs = null;
        this.context = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 带参数的构造函数
     */
    public BusinessException(ErrorCode errorCode, Object... messageArgs) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
        this.context = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 带原因的构造函数
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
        this.messageArgs = null;
        this.context = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 完整构造函数
     */
    public BusinessException(ErrorCode errorCode, Throwable cause, Object... messageArgs) {
        super(errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
        this.messageArgs = messageArgs;
        this.context = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 私有构造函数，用于Builder模式
     */
    private BusinessException(Builder builder) {
        super(builder.errorCode.getDefaultMessage(), builder.cause);
        this.errorCode = builder.errorCode;
        this.messageArgs = builder.messageArgs;
        this.context = new HashMap<>(builder.context);
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 获取本地化的错误消息
     */
    public String getLocalizedMessage() {
        try {
            return MessageManager.getMessage(errorCode, messageArgs);
        } catch (Exception ignored) {
            // 回退到默认消息，避免启动阶段 MessageSource 未就绪等 NPE
            return errorCode.getDefaultMessage();
        }
    }

    /**
     * 获取本地化的错误消息（指定语言）
     */
    public String getLocalizedMessage(Locale locale) {
        try {
            return MessageManager.getMessage(errorCode, locale, messageArgs);
        } catch (Exception ignored) {
            return errorCode.getDefaultMessage();
        }
    }

    /**
     * 添加上下文信息
     */
    public BusinessException addContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }

    /**
     * 创建Builder实例
     */
    public static Builder builder(ErrorCode errorCode) {
        return new Builder(errorCode);
    }

    /**
     * Builder模式构建器
     */
    public static class Builder {
        private ErrorCode errorCode;
        private Object[] messageArgs;
        private Map<String, Object> context = new HashMap<>();
        private Throwable cause;

        private Builder(ErrorCode errorCode) {
            this.errorCode = errorCode;
        }

        public Builder args(Object... args) {
            this.messageArgs = args;
            return this;
        }

        public Builder context(String key, Object value) {
            this.context.put(key, value);
            return this;
        }

        public Builder context(Map<String, Object> context) {
            this.context.putAll(context);
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public BusinessException build() {
            return new BusinessException(this);
        }
    }

    @Override
    public String toString() {
        return String.format("BusinessException{errorCode=%s, message='%s', timestamp=%d}",
                errorCode.getCode(), getLocalizedMessage(), timestamp);
    }
}