package com.cloud.baseai.infrastructure.exception;

/**
 * <h2>短信模块专用异常类</h2>
 *
 * <p>短信子系统覆盖验证码、通知、批量群发、余额查询等多种场景，
 * 错误处理既要保证<strong>业务含义</strong>清晰，又要兼顾
 * <strong>可国际化</strong>与<strong>统一返回格式</strong>。</p>
 *
 * <p>本类继承 {@link BusinessException} 并统一使用 {@link ErrorCode}。
 * 通过静态工厂方法为常见失败场景提供语义化创建方式，
 * 避免在业务代码中硬编码错误码 / 文本。</p>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 *  if (!smsService.isValidPhoneNumber(phone)) {
 *      throw SmsException.invalidPhoneNumber(phone);
 *  }
 * }</pre>
 */
public class SmsException extends BusinessException {

    /**
     * 基础构造函数
     */
    public SmsException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 带参数的构造函数
     */
    public SmsException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    /**
     * 带原因的构造函数
     */
    public SmsException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 完整构造函数
     */
    public SmsException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }

    // 静态工厂方法，提供便捷的异常创建方式

    /* ---------- 静态工厂：安全 / 风控 ---------- */

    /**
     * 黑名单号码
     */
    public static SmsException phoneBlacklisted(String phoneNumber) {
        return (SmsException) new SmsException(ErrorCode.EXT_SMS_008)
                .addContext("phone", phoneNumber);
    }

    /**
     * 发送频率超限
     */
    public static SmsException rateLimitExceeded(int limit) {
        return (SmsException) new SmsException(ErrorCode.EXT_SMS_011, limit)
                .addContext("limit", limit);
    }

    /**
     * 重复发送检测
     */
    public static SmsException duplicateSending(int waitMinutes) {
        return (SmsException) new SmsException(ErrorCode.EXT_SMS_012, waitMinutes)
                .addContext("retryAfterMinutes", waitMinutes);
    }

    /* ---------- 静态工厂：提供商 / 技术失败 ---------- */

    /**
     * 服务商不可用
     */
    public static SmsException providerUnavailable(String provider) {
        return new SmsException(ErrorCode.EXT_SMS_013, provider);
    }

    /**
     * 验证码短信发送失败
     */
    public static SmsException verificationCodeSendFailed(String reason) {
        return (SmsException) new SmsException(ErrorCode.EXT_SMS_001, reason)
                .addContext("reason", reason);
    }

    /**
     * 查询状态失败
     */
    public static SmsException queryStatusFailed(String messageId) {
        return (SmsException) new SmsException(ErrorCode.EXT_SMS_006, messageId)
                .addContext("messageId", messageId);
    }

    /**
     * 查询余额失败
     */
    public static SmsException quotaQueryFailed() {
        return new SmsException(ErrorCode.EXT_SMS_007);
    }

    /* ---------- 静态工厂：模板 / 内容 ---------- */

    /**
     * 模板发送失败
     */
    public static SmsException templateSendFailed(String templateCode, String reason) {
        return (SmsException) new SmsException(ErrorCode.EXT_SMS_004, templateCode)
                .addContext("templateCode", templateCode)
                .addContext("reason", reason);
    }

    /**
     * 通知短信发送失败
     */
    public static SmsException notificationFailed(String type, String reason) {
        return (SmsException) new SmsException(ErrorCode.EXT_SMS_003, reason)
                .addContext("notificationType", type)
                .addContext("reason", reason);
    }

    /**
     * 批量发送失败
     */
    public static SmsException batchSendFailed(String batchId, String reason) {
        return (SmsException) new SmsException(ErrorCode.EXT_SMS_005, reason)
                .addContext("batchId", batchId)
                .addContext("reason", reason);
    }
}