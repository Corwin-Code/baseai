package com.cloud.baseai.infrastructure.exception;

import lombok.Getter;

/**
 * <h2>审计服务异常类</h2>
 *
 * <p>这个异常类是审计系统中所有异常的统一表示。
 * 我们的异常处理系统需要准确地识别和分类不同类型的问题。</p>
 *
 * <p><b>异常分类体系：</b></p>
 * <p>审计异常分为几个主要类别，每个类别都有其特定的处理方式：</p>
 * <ul>
 * <li><b>业务异常：</b>由于业务规则违反导致的异常，如权限不足、数据冲突等</li>
 * <li><b>技术异常：</b>由于技术问题导致的异常，如数据库连接失败、序列化错误等</li>
 * <li><b>配置异常：</b>由于配置错误导致的异常，如缺少必要的配置项等</li>
 * <li><b>系统异常：</b>由于系统级问题导致的异常，如内存不足、磁盘空间不足等</li>
 * </ul>
 */
@Getter
public class AuditServiceException extends RuntimeException {

    /**
     * 错误代码，用于标识具体的错误类型
     */
    private final String errorCode;

    /**
     * 错误类型，用于分类处理
     */
    private final ErrorType errorType;

    /**
     * 错误严重程度
     */
    private final ErrorSeverity severity;

    /**
     * 错误上下文信息，包含导致错误的详细信息
     */
    private final Object context;

    /**
     * 是否可以重试
     */
    private final boolean retryable;

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

    /**
     * 构造函数 - 创建一个基本的审计服务异常
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     */
    public AuditServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorType = ErrorType.BUSINESS_ERROR;
        this.severity = ErrorSeverity.MEDIUM;
        this.context = null;
        this.retryable = false;
    }

    /**
     * 构造函数 - 创建一个带有原因的审计服务异常
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param cause     原始异常
     */
    public AuditServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorType = ErrorType.TECHNICAL_ERROR;
        this.severity = ErrorSeverity.MEDIUM;
        this.context = null;
        this.retryable = isRetryableException(cause);
    }

    /**
     * 构造函数 - 创建一个完整的审计服务异常
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param cause     原始异常
     * @param errorType 错误类型
     * @param severity  错误严重程度
     * @param context   错误上下文
     * @param retryable 是否可以重试
     */
    public AuditServiceException(String errorCode, String message, Throwable cause,
                                 ErrorType errorType, ErrorSeverity severity,
                                 Object context, boolean retryable) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorType = errorType;
        this.severity = severity;
        this.context = context;
        this.retryable = retryable;
    }

    /**
     * 创建一个业务异常的静态工厂方法
     *
     * <p>这个方法用于创建业务逻辑相关的异常，如权限不足、数据验证失败等。
     * 这类异常通常不需要重试，需要用户修正输入或获取相应权限。</p>
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     * @return 业务异常实例
     */
    public static AuditServiceException businessError(String errorCode, String message) {
        return new AuditServiceException(errorCode, message, null,
                ErrorType.BUSINESS_ERROR, ErrorSeverity.MEDIUM, null, false);
    }

    /**
     * 创建一个技术异常的静态工厂方法
     *
     * <p>这个方法用于创建技术相关的异常，如数据库连接失败、网络超时等。
     * 这类异常通常可以重试，或者需要技术人员介入处理。</p>
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param cause     原始异常
     * @return 技术异常实例
     */
    public static AuditServiceException technicalError(String errorCode, String message, Throwable cause) {
        return new AuditServiceException(errorCode, message, cause,
                ErrorType.TECHNICAL_ERROR, ErrorSeverity.HIGH, null, true);
    }

    /**
     * 创建一个配置异常的静态工厂方法
     *
     * <p>这个方法用于创建配置相关的异常，如缺少必要的配置项、配置值格式错误等。
     * 这类异常需要管理员修正配置。</p>
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     * @return 配置异常实例
     */
    public static AuditServiceException configurationError(String errorCode, String message) {
        return new AuditServiceException(errorCode, message, null,
                ErrorType.CONFIGURATION_ERROR, ErrorSeverity.HIGH, null, false);
    }

    /**
     * 创建一个安全异常的静态工厂方法
     *
     * <p>这个方法用于创建安全相关的异常，如认证失败、权限不足等。
     * 这类异常需要特别关注，可能涉及安全威胁。</p>
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param context   安全上下文信息
     * @return 安全异常实例
     */
    public static AuditServiceException securityError(String errorCode, String message, Object context) {
        return new AuditServiceException(errorCode, message, null,
                ErrorType.SECURITY_ERROR, ErrorSeverity.CRITICAL, context, false);
    }

    /**
     * 创建一个系统异常的静态工厂方法
     *
     * <p>这个方法用于创建系统级异常，如内存不足、磁盘空间不足等。
     * 这类异常通常需要运维人员介入处理。</p>
     *
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param cause     原始异常
     * @return 系统异常实例
     */
    public static AuditServiceException systemError(String errorCode, String message, Throwable cause) {
        return new AuditServiceException(errorCode, message, cause,
                ErrorType.SYSTEM_ERROR, ErrorSeverity.CRITICAL, null, false);
    }

    /**
     * 判断异常是否可以重试
     *
     * <p>这个方法根据异常的类型来判断是否可以重试。通常，网络超时、
     * 数据库连接失败等临时性问题是可以重试的。</p>
     *
     * @param cause 原始异常
     * @return 如果可以重试则返回true，否则返回false
     */
    private boolean isRetryableException(Throwable cause) {
        if (cause == null) {
            return false;
        }

        String className = cause.getClass().getSimpleName();
        String message = cause.getMessage();

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
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("timeout") ||
                    lowerMessage.contains("connection") ||
                    lowerMessage.contains("temporary")) {
                return true;
            }
        }

        return false;
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
        return String.format("[%s] %s: %s", errorCode, errorType, getMessage());
    }

    /**
     * 获取错误的详细信息
     *
     * <p>这个方法返回一个包含所有错误信息的详细描述，包括错误代码、
     * 类型、严重程度、上下文等信息。</p>
     *
     * @return 错误的详细信息
     */
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("错误代码: ").append(errorCode).append("\n");
        sb.append("错误类型: ").append(errorType).append("\n");
        sb.append("严重程度: ").append(severity).append("\n");
        sb.append("错误消息: ").append(getMessage()).append("\n");
        sb.append("可重试: ").append(retryable ? "是" : "否").append("\n");

        if (context != null) {
            sb.append("上下文信息: ").append(context).append("\n");
        }

        if (getCause() != null) {
            sb.append("原始异常: ").append(getCause().getClass().getSimpleName())
                    .append(" - ").append(getCause().getMessage()).append("\n");
        }

        return sb.toString();
    }

    /**
     * 检查是否需要立即处理
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

    // Getter methods

    /**
     * 常用的错误代码常量
     */
    public static final class ErrorCodes {
        // 审计记录相关
        public static final String AUDIT_RECORD_FAILED = "AUDIT_RECORD_FAILED";
        public static final String AUDIT_QUERY_FAILED = "AUDIT_QUERY_FAILED";
        public static final String AUDIT_VALIDATION_FAILED = "AUDIT_VALIDATION_FAILED";

        // 权限相关
        public static final String PERMISSION_DENIED = "PERMISSION_DENIED";
        public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
        public static final String AUTHORIZATION_FAILED = "AUTHORIZATION_FAILED";

        // 数据相关
        public static final String DATA_NOT_FOUND = "DATA_NOT_FOUND";
        public static final String DATA_INTEGRITY_VIOLATION = "DATA_INTEGRITY_VIOLATION";
        public static final String DATA_CORRUPTION = "DATA_CORRUPTION";

        // 系统相关
        public static final String SYSTEM_UNAVAILABLE = "SYSTEM_UNAVAILABLE";
        public static final String RESOURCE_EXHAUSTED = "RESOURCE_EXHAUSTED";
        public static final String CONFIGURATION_MISSING = "CONFIGURATION_MISSING";

        // 网络相关
        public static final String NETWORK_TIMEOUT = "NETWORK_TIMEOUT";
        public static final String CONNECTION_FAILED = "CONNECTION_FAILED";
        public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";

        private ErrorCodes() {
            // 防止实例化
        }
    }
}