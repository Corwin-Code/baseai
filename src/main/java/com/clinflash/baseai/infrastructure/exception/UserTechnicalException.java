package com.clinflash.baseai.infrastructure.exception;

import lombok.Getter;

/**
 * <h2>用户技术异常</h2>
 *
 * <p>技术异常表示系统内部错误，如数据库连接失败、网络超时等。
 * 这类异常通常不应该直接暴露给用户，而是应该记录日志并返回通用的错误信息。</p>
 */
@Getter
public class UserTechnicalException extends RuntimeException {

    private final String errorCode;

    public UserTechnicalException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public UserTechnicalException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return String.format("UserTechnicalException{errorCode='%s', message='%s'}",
                errorCode, getMessage());
    }
}