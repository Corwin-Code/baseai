package com.clinflash.baseai.infrastructure.exception;

import lombok.Getter;

/**
 * <h2>用户业务异常</h2>
 *
 * <p>业务异常表示违反了业务规则或约束的情况。这类异常通常是由于
 * 用户操作不当或数据状态不符合预期导致的，应该向用户提供清晰的错误信息。</p>
 */
@Getter
public class UserBusinessException extends RuntimeException {

    private final String errorCode;

    public UserBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public UserBusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return String.format("UserBusinessException{errorCode='%s', message='%s'}",
                errorCode, getMessage());
    }
}