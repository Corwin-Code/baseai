package com.cloud.baseai.infrastructure.exception;

/**
 * <h2>对话模块专用异常类</h2>
 *
 * <p>对话模块是AI系统的核心，错误处理需要考虑用户体验、
 * 模型可用性、流式响应等复杂场景。</p>
 */
public class ChatException extends BusinessException {

    public ChatException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ChatException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public ChatException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public ChatException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }

    // 静态工厂方法

    /**
     * 对话线程不存在异常
     */
    public static ChatException threadNotFound(String threadId) {
        return new ChatException(ErrorCode.BIZ_CHAT_001, threadId);
    }

    /**
     * 消息内容为空异常
     */
    public static ChatException emptyContent() {
        return new ChatException(ErrorCode.BIZ_CHAT_016);
    }

    /**
     * 消息内容过长异常
     */
    public static ChatException contentTooLong(int currentLength, int maxLength) {
        return new ChatException(ErrorCode.BIZ_CHAT_017, currentLength, maxLength);
    }

    /**
     * AI模型不可用异常
     */
    public static ChatException modelUnavailable(String modelName) {
        return (ChatException) new ChatException(ErrorCode.BIZ_CHAT_022, modelName)
//                .addContext("unavailableReason", reason)
                .addContext("suggestedModel", "gpt-3.5-turbo");
    }

    /**
     * 消息发送频率过高异常
     */
    public static ChatException rateLimitExceeded(int currentRate, int limitRate) {
        return (ChatException) new ChatException(ErrorCode.BIZ_CHAT_032)
                .addContext("currentRate", currentRate)
                .addContext("limitRate", limitRate)
                .addContext("retryAfterSeconds", 60);
    }
}