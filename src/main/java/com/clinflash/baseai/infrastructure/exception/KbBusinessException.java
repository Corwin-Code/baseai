package com.clinflash.baseai.infrastructure.exception;

/**
 * <h2>知识库业务异常</h2>
 *
 * <p>业务逻辑相关的异常，如重复数据、验证失败等。</p>
 */
public class KbBusinessException extends KbException {

    public KbBusinessException(String errorCode, String message) {
        super(errorCode, message);
    }

    public KbBusinessException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }
}