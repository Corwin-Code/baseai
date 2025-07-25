package com.cloud.baseai.infrastructure.exception;

/**
 * <h2>用户模块专用异常类</h2>
 *
 * <p>用户模块涉及认证、授权、用户资料管理等核心功能，
 * 错误处理需要特别注意安全性和用户体验。</p>
 */
public class UserException extends BusinessException {

    /**
     * 基础构造函数
     */
    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 带参数的构造函数
     */
    public UserException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    /**
     * 带原因的构造函数
     */
    public UserException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 完整构造函数
     */
    public UserException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }

    // 静态工厂方法，提供便捷的异常创建方式

    /**
     * 用户不存在异常
     */
    public static UserException userNotFound(String userId) {
        return new UserException(ErrorCode.BIZ_USER_001, userId);
    }

    /**
     * 用户名已存在异常
     */
    public static UserException duplicateUsername(String username) {
        return new UserException(ErrorCode.BIZ_USER_002, username);
    }

    /**
     * 密码错误异常
     */
    public static UserException invalidPassword() {
        return new UserException(ErrorCode.BIZ_USER_006);
    }

    /**
     * 账户锁定异常
     */
    public static UserException accountLocked(String lockReason) {
        return (UserException) new UserException(ErrorCode.BIZ_USER_007)
                .addContext("lockReason", lockReason)
                .addContext("lockTime", System.currentTimeMillis());
    }

    /**
     * 租户不存在异常
     */
    public static UserException tenantNotFound(String tenantId) {
        return new UserException(ErrorCode.BIZ_TENANT_001, tenantId);
    }
}