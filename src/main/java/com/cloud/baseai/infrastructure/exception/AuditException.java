package com.cloud.baseai.infrastructure.exception;

import lombok.Getter;

import java.io.Serial;

/**
 * <h2>审计模块统一异常类</h2>
 *
 * <p>该异常适用于审计命令与查询两大场景，统一携带错误码与上下文信息，
 * 并提供错误类型、严重程度及是否可重试等信号，便于全局异常处理器决定返回策略。</p>
 *
 * <p><b>使用规范：</b></p>
 * <ul>
 *   <li>必须使用 {@link ErrorCode} 定义错误码，禁止硬编码字符串消息。</li>
 *   <li>通过静态工厂方法快速创建语义化异常（如 {@code createFailed(reason)}）。</li>
 *   <li>如需附加诊断信息，使用 {@code addContext(key, value)} 链式添加。</li>
 *   <li>异常最终由全局异常处理器统一转换为标准 API 响应。</li>
 * </ul>
 */
@Getter
public class AuditException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 错误类型，用于分类处理
     */
    private final ErrorType errorType;

    /**
     * 错误严重程度
     */
    private final ErrorSeverity severity;

    /**
     * 是否可以重试
     */
    private final boolean retryable;

    // ========== 枚举定义 ==========

    /**
     * 错误类型枚举
     */
    public enum ErrorType {
        /**
         * 业务逻辑错误
         * <p>这类错误通常是由于业务规则违反导致的，如权限不足、数据验证失败等。
         * 这类错误通常不需要重试，需要用户修正输入或获取相应权限。</p>
         */
        BUSINESS_ERROR,

        /**
         * 技术错误
         * <p>这类错误通常是由于技术问题导致的，如数据库连接失败、网络超时等。
         * 这类错误通常可以重试，或者需要技术人员介入处理。</p>
         */
        TECHNICAL_ERROR,

        /**
         * 配置错误
         * <p>这类错误通常是由于系统配置错误导致的，如缺少必要的配置项、
         * 配置值格式错误等。这类错误需要管理员修正配置。</p>
         */
        CONFIGURATION_ERROR,

        /**
         * 系统错误
         * <p>这类错误通常是由于系统级问题导致的，如内存不足、磁盘空间不足等。
         * 这类错误通常需要运维人员介入处理。</p>
         */
        SYSTEM_ERROR,

        /**
         * 安全错误
         * <p>这类错误通常是由于安全相关问题导致的，如认证失败、权限不足等。
         * 这类错误需要特别关注，可能涉及安全威胁。</p>
         */
        SECURITY_ERROR
    }

    /**
     * 错误严重程度枚举
     */
    public enum ErrorSeverity {
        /**
         * 低级别错误
         * <p>这类错误不影响核心功能，用户可以继续使用其他功能。</p>
         */
        LOW,

        /**
         * 中级别错误
         * <p>这类错误影响部分功能，但不影响系统整体运行。</p>
         */
        MEDIUM,

        /**
         * 高级别错误
         * <p>这类错误严重影响系统功能，需要立即处理。</p>
         */
        HIGH,

        /**
         * 严重错误
         * <p>这类错误导致系统不可用，需要紧急处理。</p>
         */
        CRITICAL
    }

    // ========== 构造函数 ==========

    /**
     * 构造函数 - 创建一个基本的审计服务异常
     *
     * @param errorCode 错误代码
     */
    public AuditException(ErrorCode errorCode) {
        this(errorCode, ErrorType.BUSINESS_ERROR, ErrorSeverity.MEDIUM, false, null);
    }

    /**
     * 构造函数 - 创建一个带有原因的审计服务异常
     *
     * @param errorCode 错误代码
     * @param cause     原始异常
     */
    public AuditException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, ErrorType.TECHNICAL_ERROR, ErrorSeverity.MEDIUM, isRetryable(cause), cause);
    }

    /**
     * 构造函数 - 创建一个完整的审计服务异常
     *
     * @param errorCode 错误代码
     * @param cause     原始异常
     * @param errorType 错误类型
     * @param severity  错误严重程度
     * @param retryable 是否可以重试
     */
    public AuditException(ErrorCode errorCode,
                          ErrorType errorType,
                          ErrorSeverity severity,
                          boolean retryable,
                          Throwable cause,
                          Object... args) {
        super(errorCode, cause, args);
        this.errorType = errorType;
        this.severity = severity;
        this.retryable = retryable;
    }

    /**
     * 创建一个业务异常的静态工厂方法
     *
     * <p>这个方法用于创建业务逻辑相关的异常，如权限不足、数据验证失败等。
     * 这类异常通常不需要重试，需要用户修正输入或获取相应权限。</p>
     *
     * @param errorCode 错误代码
     * @param cause     原始异常
     * @return 业务异常实例
     */
    public static AuditException businessError(ErrorCode errorCode, Throwable cause) {
        return new AuditException(errorCode, ErrorType.BUSINESS_ERROR, ErrorSeverity.MEDIUM, false, cause);
    }

    /**
     * 创建一个技术异常的静态工厂方法
     *
     * <p>这个方法用于创建技术相关的异常，如数据库连接失败、网络超时等。
     * 这类异常通常可以重试，或者需要技术人员介入处理。</p>
     *
     * @param errorCode 错误代码
     * @param cause     原始异常
     * @return 技术异常实例
     */
    public static AuditException technicalError(ErrorCode errorCode, Throwable cause, Object... args) {
        return new AuditException(errorCode, ErrorType.TECHNICAL_ERROR, ErrorSeverity.HIGH, true, cause, args);
    }

    /**
     * 创建一个配置异常的静态工厂方法
     *
     * <p>这个方法用于创建配置相关的异常，如缺少必要的配置项、配置值格式错误等。
     * 这类异常需要管理员修正配置。</p>
     *
     * @param errorCode 错误代码
     * @return 配置异常实例
     */
    public static AuditException configurationError(ErrorCode errorCode) {
        return new AuditException(errorCode, ErrorType.CONFIGURATION_ERROR, ErrorSeverity.HIGH, false, null);
    }

    /**
     * 创建一个安全异常的静态工厂方法
     *
     * <p>这个方法用于创建安全相关的异常，如认证失败、权限不足等。
     * 这类异常需要特别关注，可能涉及安全威胁。</p>
     *
     * @param errorCode 错误代码
     * @param context   安全上下文信息
     * @return 安全异常实例
     */
    public static AuditException securityError(ErrorCode errorCode, Object context) {
        return new AuditException(errorCode, ErrorType.SECURITY_ERROR, ErrorSeverity.CRITICAL, false, null, context);
    }

    /**
     * 创建一个系统异常的静态工厂方法
     *
     * <p>这个方法用于创建系统级异常，如内存不足、磁盘空间不足等。
     * 这类异常通常需要运维人员介入处理。</p>
     *
     * @param errorCode 错误代码
     * @param cause     原始异常
     * @return 系统异常实例
     */
    public static AuditException systemError(ErrorCode errorCode, Throwable cause) {
        return new AuditException(errorCode, ErrorType.SYSTEM_ERROR, ErrorSeverity.CRITICAL, false, cause);
    }

    // ========== 静态工厂 - 命令侧 ==========

    public static AuditException createFailed(String reason) {
        return new AuditException(ErrorCode.BIZ_AUDIT_001, ErrorType.TECHNICAL_ERROR,
                ErrorSeverity.MEDIUM, false, null, reason);
    }

    public static AuditException batchCreateFailed(int total, String reason) {
        return (AuditException) new AuditException(ErrorCode.BIZ_AUDIT_002, ErrorType.TECHNICAL_ERROR,
                ErrorSeverity.MEDIUM, false, null, reason)
                .addContext("totalCount", total);
    }

    public static AuditException securityAuditCreateFailed(String eventType, String riskLevel) {
        return (AuditException) new AuditException(ErrorCode.BIZ_AUDIT_003, ErrorType.SECURITY_ERROR,
                ErrorSeverity.HIGH, false, null)
                .addContext("securityEventType", eventType)
                .addContext("riskLevel", riskLevel);
    }

    // ========== 静态工厂 - 查询/统计侧 ==========

    public static AuditException queryFailed(String reason) {
        return new AuditException(ErrorCode.BIZ_AUDIT_004, ErrorType.TECHNICAL_ERROR,
                ErrorSeverity.MEDIUM, false, null, reason);
    }

    public static AuditException securityEventsQueryFailed() {
        return new AuditException(ErrorCode.BIZ_AUDIT_005, ErrorType.SECURITY_ERROR,
                ErrorSeverity.MEDIUM, false, null);
    }

    public static AuditException objectHistoryQueryFailed(String targetType, Long targetId) {
        return (AuditException) new AuditException(ErrorCode.BIZ_AUDIT_006, ErrorType.BUSINESS_ERROR,
                ErrorSeverity.MEDIUM, false, null)
                .addContext("targetType", targetType)
                .addContext("targetId", targetId);
    }

    public static AuditException statisticsFailed(Long tenantId) {
        return (AuditException) new AuditException(ErrorCode.BIZ_AUDIT_007, ErrorType.TECHNICAL_ERROR,
                ErrorSeverity.MEDIUM, true, null)
                .addContext("tenantId", tenantId);
    }

    public static AuditException userAuditSummaryFailed(Long userId, Long tenantId) {
        return (AuditException) new AuditException(ErrorCode.BIZ_AUDIT_008, ErrorType.BUSINESS_ERROR,
                ErrorSeverity.MEDIUM, false, null)
                .addContext("userId", userId)
                .addContext("tenantId", tenantId);
    }

    // ========== 静态工厂 - 报告/导出 ==========

    public static AuditException reportExportFailed(String reportType) {
        return (AuditException) new AuditException(ErrorCode.BIZ_AUDIT_009, ErrorType.TECHNICAL_ERROR,
                ErrorSeverity.MEDIUM, true, null)
                .addContext("reportType", reportType);
    }

    public static AuditException reportGenerationFailed(String reportType, String reason) {
        return (AuditException) new AuditException(ErrorCode.BIZ_AUDIT_010, ErrorType.TECHNICAL_ERROR,
                ErrorSeverity.HIGH, true, null, reason)
                .addContext("reportType", reportType);
    }

    public static AuditException quickSummaryFailed(Long tenantId) {
        return (AuditException) new AuditException(ErrorCode.BIZ_AUDIT_011, ErrorType.TECHNICAL_ERROR,
                ErrorSeverity.MEDIUM, true, null)
                .addContext("tenantId", tenantId);
    }

    public static AuditException securityAnalysisFailed(Long tenantId) {
        return (AuditException) new AuditException(ErrorCode.BIZ_AUDIT_012, ErrorType.SECURITY_ERROR,
                ErrorSeverity.HIGH, false, null)
                .addContext("tenantId", tenantId);
    }

    public static AuditException complianceReportFailed(Long tenantId, String regulation) {
        return (AuditException) new AuditException(ErrorCode.BIZ_AUDIT_013, ErrorType.BUSINESS_ERROR,
                ErrorSeverity.MEDIUM, false, null)
                .addContext("tenantId", tenantId)
                .addContext("regulation", regulation);
    }

    public static AuditException generatorNotFound(String reportType) {
        return (AuditException) new AuditException(ErrorCode.BIZ_AUDIT_014, ErrorType.CONFIGURATION_ERROR,
                ErrorSeverity.HIGH, false, null)
                .addContext("reportType", reportType);
    }

    public static AuditException dataCollectionFailed() {
        return new AuditException(ErrorCode.BIZ_AUDIT_015, ErrorType.TECHNICAL_ERROR,
                ErrorSeverity.MEDIUM, true, null);
    }

    public static AuditException saveReportFailed(String reportId) {
        return (AuditException) new AuditException(ErrorCode.BIZ_AUDIT_016, ErrorType.TECHNICAL_ERROR,
                ErrorSeverity.HIGH, true, null)
                .addContext("reportId", reportId);
    }

    // ========== 静态工厂 - 参数校验 ==========

    public static AuditException unsupportedReportType(String type) {
        return (AuditException) new AuditException(ErrorCode.PARAM_022, ErrorType.BUSINESS_ERROR,
                ErrorSeverity.MEDIUM, false, null, type)
                .addContext("reportType", type);
    }

    public static AuditException invalidTimeRange() {
        return new AuditException(ErrorCode.PARAM_021, ErrorType.BUSINESS_ERROR,
                ErrorSeverity.MEDIUM, false, null);
    }

    public static AuditException pageSizeOutOfRange(int size) {
        return (AuditException) new AuditException(ErrorCode.PARAM_020, ErrorType.BUSINESS_ERROR,
                ErrorSeverity.MEDIUM, false, null)
                .addContext("size", size);
    }

    // ========== 工具方法 ==========

    /**
     * 判断异常是否可以重试
     *
     * <p>这个方法根据异常的类型来判断是否可以重试。通常，网络超时、
     * 数据库连接失败等临时性问题是可以重试的。</p>
     *
     * @param cause 原始异常
     * @return 如果可以重试则返回true，否则返回false
     */
    private static boolean isRetryable(Throwable cause) {
        if (cause == null) {
            return false;
        }
        String className = cause.getClass().getSimpleName();
        String message = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";

        // 数据库连接相关的异常通常可以重试
        if (className.contains("SQLException") ||
                className.contains("DataAccessException") ||
                className.contains("TransactionException")) {
            return true;
        }

        // 网络相关的异常通常可以重试
        if (className.contains("SocketTimeoutException") ||
                className.contains("ConnectException") ||
                className.contains("UnknownHostException")) {
            return true;
        }

        // 某些特定的错误消息表示可以重试
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("timeout") ||
                lowerMessage.contains("connection") ||
                lowerMessage.contains("temporary");
    }

    /**
     * 检查是否需要立即处理（高或严重级别）
     *
     * <p>这个方法判断异常是否需要立即处理，通常高级别和严重级别的异常
     * 需要立即处理。</p>
     *
     * @return 如果需要立即处理则返回true，否则返回false
     */
    public boolean requiresImmediateAttention() {
        return severity == ErrorSeverity.HIGH || severity == ErrorSeverity.CRITICAL;
    }

    /**
     * 检查是否是安全相关的异常
     *
     * <p>安全相关的异常需要特别关注，可能涉及安全威胁。</p>
     *
     * @return 如果是安全异常则返回true，否则返回false
     */
    public boolean isSecurityRelated() {
        return errorType == ErrorType.SECURITY_ERROR;
    }

    /**
     * 获取格式化的错误消息
     *
     * <p>这个方法返回一个包含错误代码、类型和消息的格式化字符串，
     * 便于日志记录和错误显示。</p>
     *
     * @return 格式化的错误消息
     */
    public String getFormattedMessage() {
        return String.format("[%s] %s/%s: %s", getErrorCode().getCode(), errorType, severity, getMessage());
    }

    /**
     * 获取错误的详细信息（可在调试/告警场景使用）
     *
     * <p>这个方法返回一个包含所有错误信息的详细描述，包括错误代码、
     * 类型、严重程度、上下文等信息。</p>
     *
     * @return 错误的详细信息
     */
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("errorCode: ").append(getErrorCode().getCode()).append('\n');
        sb.append("type: ").append(errorType).append('\n');
        sb.append("severity: ").append(severity).append('\n');
        sb.append("message: ").append(getMessage()).append('\n');
        sb.append("retryable: ").append(retryable).append('\n');

        if (getContext() != null && !getContext().isEmpty()) {
            sb.append("context: ").append(getContext()).append('\n');
        }

        if (getCause() != null) {
            sb.append("cause: ").append(getCause().getClass().getSimpleName())
                    .append(" - ").append(getCause().getMessage()).append('\n');
        }
        return sb.toString();
    }
}