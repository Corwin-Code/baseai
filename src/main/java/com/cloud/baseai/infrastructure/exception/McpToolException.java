package com.cloud.baseai.infrastructure.exception;

/**
 * <h2>MCP工具模块专用异常类</h2>
 */
public class McpToolException extends BusinessException {

    public McpToolException(ErrorCode errorCode) {
        super(errorCode);
    }

    public McpToolException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public McpToolException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public McpToolException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, cause, args);
    }

    // 静态工厂方法

    public static McpToolException toolNotFound(String toolCode) {
        return new McpToolException(ErrorCode.BIZ_MCP_001, toolCode);
    }

    public static McpToolException toolDisabled(String toolCode, String reason) {
        return (McpToolException) new McpToolException(ErrorCode.BIZ_MCP_003, toolCode)
                .addContext("disabledReason", reason);
    }

    public static McpToolException quotaExceeded(String tenantId, int usedQuota, int totalQuota) {
        return (McpToolException) new McpToolException(ErrorCode.BIZ_MCP_008)
                .addContext("tenantId", tenantId)
                .addContext("usedQuota", usedQuota)
                .addContext("totalQuota", totalQuota);
    }
}