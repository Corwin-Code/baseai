package com.cloud.baseai.infrastructure.exception;

/**
 * <h2>Misc模块专用异常类</h2>
 */
public class MiscException extends BusinessException {

    /**
     * 基础构造函数
     */
    public MiscException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 带参数的构造函数
     */
    public MiscException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    /**
     * 带原因的构造函数
     */
    public MiscException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 完整构造函数
     */
    public MiscException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }

    // 静态工厂方法，提供便捷的异常创建方式

    /**
     * 模板不存在异常
     */
    public static MiscException templateNotFound(String templateId) {
        return new MiscException(ErrorCode.BIZ_TEMPLATE_001, templateId);
    }

    /**
     * 模板已存在异常
     */
    public static MiscException duplicateTemplateName(String name) {
        return new MiscException(ErrorCode.BIZ_TEMPLATE_002, name);
    }

    /**
     * 模板内容无效异常
     */
    public static MiscException invalidTemplate() {
        return new MiscException(ErrorCode.BIZ_TEMPLATE_003);
    }
}