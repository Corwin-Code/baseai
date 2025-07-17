package com.cloud.baseai.infrastructure.exception;

/**
 * <h2>文档解析异常</h2>
 */
public class DocumentParsingException extends KbTechnicalException {

    public DocumentParsingException(String message) {
        super("DOCUMENT_PARSING_ERROR", message);
    }

    public DocumentParsingException(String message, Throwable cause) {
        super("DOCUMENT_PARSING_ERROR", message, cause);
    }
}