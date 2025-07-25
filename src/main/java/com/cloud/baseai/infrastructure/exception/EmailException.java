package com.cloud.baseai.infrastructure.exception;

/**
 * <h2>邮件模块专用异常类</h2>
 *
 * <p>统一封装邮件子系统产生的所有业务/技术错误，配合 {@link ErrorCode}
 * 形成<strong>可国际化</strong>、<strong>可追踪</strong>的错误响应。</p>
 *
 * <p>调用示例：</p>
 * <pre>{@code
 * if (!emailService.isValidEmail(addr)) {
 *     throw EmailException.invalidEmail(addr);
 * }
 * }</pre>
 */
public class EmailException extends BusinessException {

    /* ===== 构造函数 ===== */

    public EmailException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EmailException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public EmailException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public EmailException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }

    /* ======= 发送失败场景 ======= */

    public static EmailException activationFailed(String reason) {
        return (EmailException) new EmailException(ErrorCode.EXT_EMAIL_015, reason)
                .addContext("reason", reason);
    }

    public static EmailException invitationFailed(String reason) {
        return (EmailException) new EmailException(ErrorCode.EXT_EMAIL_014, reason)
                .addContext("reason", reason);
    }

    public static EmailException passwordResetFailed(String reason) {
        return (EmailException) new EmailException(ErrorCode.EXT_EMAIL_019, reason)
                .addContext("reason", reason);
    }

    public static EmailException notificationFailed(String reason) {
        return (EmailException) new EmailException(ErrorCode.EXT_EMAIL_017, reason)
                .addContext("reason", reason);
    }

    /* ======= 模板 / 批量 ======= */

    /**
     * 模板引擎不可用
     */
    public static EmailException templateEngineUnavailable() {
        return new EmailException(ErrorCode.EXT_EMAIL_013);
    }

    /**
     * 模板邮件发送失败
     */
    public static EmailException templateSendFailed(String templateName, String reason) {
        return (EmailException) new EmailException(ErrorCode.EXT_EMAIL_016, reason)
                .addContext("templateName", templateName)
                .addContext("reason", reason);
    }

    /**
     * 批量发送失败
     */
    public static EmailException batchSendFailed(String reason) {
        return (EmailException) new EmailException(ErrorCode.EXT_EMAIL_018, reason)
                .addContext("reason", reason);
    }

    /* ======= 服务商 / 技术 ======= */

    /**
     * 邮件服务商不可用
     */
    public static EmailException providerUnavailable(String provider) {
        return (EmailException) new EmailException(ErrorCode.EXT_EMAIL_020, provider)
                .addContext("provider", provider);
    }
}