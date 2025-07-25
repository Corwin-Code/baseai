package com.cloud.baseai.infrastructure.exception;

/**
 * <h2>系统模块专用异常类</h2>
 */
public class SystemException extends BusinessException {

    /**
     * 基础构造函数
     */
    public SystemException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 带参数的构造函数
     */
    public SystemException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    /**
     * 带原因的构造函数
     */
    public SystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 完整构造函数
     */
    public SystemException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }

    // 静态工厂方法，提供便捷的异常创建方式

    /**
     * 设置不存在异常
     */
    public static SystemException settingNotFound() {
        return new SystemException(ErrorCode.SYS_SETTING_001);
    }
}