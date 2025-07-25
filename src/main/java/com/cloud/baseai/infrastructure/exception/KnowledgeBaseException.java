package com.cloud.baseai.infrastructure.exception;

/**
 * <h2>知识库模块专用异常类</h2>
 *
 * <p>知识库模块处理文档上传、向量化、搜索等功能，
 * 需要处理文件相关和AI服务相关的各种异常。</p>
 */
public class KnowledgeBaseException extends BusinessException {

    public KnowledgeBaseException(ErrorCode errorCode) {
        super(errorCode);
    }

    public KnowledgeBaseException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public KnowledgeBaseException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public KnowledgeBaseException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }

    // 静态工厂方法

    /**
     * 文档不存在异常
     */
    public static KnowledgeBaseException documentNotFound(String documentId) {
        return new KnowledgeBaseException(ErrorCode.BIZ_KB_001, documentId);
    }

    /**
     * 文档标题重复异常
     */
    public static KnowledgeBaseException duplicateDocumentTitle(String title) {
        return new KnowledgeBaseException(ErrorCode.BIZ_KB_003, title);
    }

    /**
     * 文件大小超限异常
     */
    public static KnowledgeBaseException fileSizeExceeded(long fileSize, long maxSize) {
        return (KnowledgeBaseException) new KnowledgeBaseException(ErrorCode.BIZ_KB_005, fileSize, maxSize)
                .addContext("fileSizeMB", fileSize / (1024 * 1024))
                .addContext("maxSizeMB", maxSize / (1024 * 1024));
    }

    /**
     * 不支持的文件类型异常
     */
    public static KnowledgeBaseException unsupportedFileType(String fileType) {
        return (KnowledgeBaseException) new KnowledgeBaseException(ErrorCode.BIZ_KB_004, fileType)
                .addContext("supportedTypes", new String[]{"PDF", "DOCX", "TXT", "MD"});
    }
}