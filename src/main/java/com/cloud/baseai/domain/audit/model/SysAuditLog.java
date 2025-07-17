package com.cloud.baseai.domain.audit.model;

import com.cloud.baseai.infrastructure.exception.AuditServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * <h2>系统审计日志领域对象</h2>
 *
 * <p>这是审计系统的核心领域对象，代表了系统中发生的一次完整的审计事件。
 * 每个审计日志都包含了操作的完整上下文信息，包括谁做了什么、在什么时候、
 * 通过什么方式、对什么对象进行了操作。</p>
 *
 * <p><b>设计原则：</b></p>
 * <ul>
 * <li><b>不可变性：</b>审计日志一旦创建就不应该被修改，确保审计数据的完整性</li>
 * <li><b>完整性：</b>包含足够的信息来重现操作的完整上下文</li>
 * <li><b>标准化：</b>使用统一的格式和结构，便于后续分析和处理</li>
 * <li><b>可追溯性：</b>每个操作都能够被完整地追踪和重现</li>
 * </ul>
 *
 * <p><b>关键字段说明：</b></p>
 * <ul>
 * <li><b>action：</b>操作类型，建议使用标准化的动作码，如 USER_LOGIN、DATA_UPDATE 等</li>
 * <li><b>targetType：</b>操作目标类型，如 USER、DOCUMENT、TENANT 等</li>
 * <li><b>detail：</b>详细信息，以JSON格式存储，包含操作的具体参数和结果</li>
 * <li><b>resultStatus：</b>操作结果状态，如 SUCCESS、FAILED、PARTIAL 等</li>
 * <li><b>logLevel：</b>日志级别，用于区分不同重要程度的审计事件</li>
 * </ul>
 *
 * @param id           审计日志的唯一标识符
 * @param tenantId     操作所属的租户标识符，用于多租户环境下的数据隔离
 * @param userId       执行操作的用户标识符，null表示系统自动操作
 * @param action       操作类型代码，描述具体执行的操作
 * @param targetType   操作目标的类型，如用户、文档、租户等
 * @param targetId     操作目标的标识符，与targetType配合使用
 * @param ipAddress    操作发起的IP地址，用于安全分析和地理位置跟踪
 * @param userAgent    用户代理字符串，包含浏览器和操作系统信息
 * @param detail       操作的详细信息，以JSON格式存储复杂的上下文数据
 * @param resultStatus 操作的执行结果状态
 * @param logLevel     审计日志的级别，用于重要性分类
 * @param createdAt    审计日志的创建时间，通常就是操作发生的时间
 */
public record SysAuditLog(
        Long id,
        Long tenantId,
        Long userId,
        String action,
        String targetType,
        Long targetId,
        String ipAddress,
        String userAgent,
        String detail,
        String resultStatus,
        String logLevel,
        OffsetDateTime createdAt
) {

    /**
     * 审计日志的结果状态常量
     */
    public static final class ResultStatus {
        public static final String SUCCESS = "SUCCESS";
        public static final String FAILED = "FAILED";
        public static final String PARTIAL = "PARTIAL";
        public static final String PENDING = "PENDING";

        private ResultStatus() {
            // 工具类，防止实例化
        }
    }

    /**
     * 审计日志的级别常量
     */
    public static final class LogLevel {
        public static final String DEBUG = "DEBUG";
        public static final String INFO = "INFO";
        public static final String WARN = "WARN";
        public static final String ERROR = "ERROR";
        public static final String FATAL = "FATAL";

        private LogLevel() {
            // 工具类，防止实例化
        }
    }

    /**
     * 常见的操作类型常量
     */
    public static final class ActionType {
        // 用户相关操作
        public static final String USER_LOGIN = "USER_LOGIN";
        public static final String USER_LOGOUT = "USER_LOGOUT";
        public static final String USER_REGISTER = "USER_REGISTER";
        public static final String USER_UPDATE = "USER_UPDATE";
        public static final String USER_DELETE = "USER_DELETE";

        // 数据操作
        public static final String DATA_CREATE = "DATA_CREATE";
        public static final String DATA_UPDATE = "DATA_UPDATE";
        public static final String DATA_DELETE = "DATA_DELETE";
        public static final String DATA_QUERY = "DATA_QUERY";

        // 系统操作
        public static final String SYSTEM_CONFIG = "SYSTEM_CONFIG";
        public static final String SYSTEM_BACKUP = "SYSTEM_BACKUP";
        public static final String SYSTEM_RESTORE = "SYSTEM_RESTORE";

        // 安全相关
        public static final String SECURITY_VIOLATION = "SECURITY_VIOLATION";
        public static final String PERMISSION_DENIED = "PERMISSION_DENIED";
        public static final String AUTH_FAILURE = "AUTH_FAILURE";

        private ActionType() {
            // 工具类，防止实例化
        }
    }

    /**
     * 创建一个新的审计日志实例
     *
     * <p>这是创建审计日志的推荐方式，它会自动设置创建时间并进行基本的参数验证。
     * 这个方法确保了审计日志的一致性和完整性。</p>
     *
     * @param tenantId     租户ID，不能为null
     * @param userId       用户ID，可以为null（表示系统操作）
     * @param action       操作类型，不能为空
     * @param targetType   目标类型，可以为null
     * @param targetId     目标ID，可以为null
     * @param ipAddress    IP地址，可以为null
     * @param userAgent    用户代理，可以为null
     * @param detail       详细信息，可以为null
     * @param resultStatus 结果状态，默认为SUCCESS
     * @param logLevel     日志级别，默认为INFO
     * @return 新创建的审计日志实例
     * @throws IllegalArgumentException 当必填参数为空时抛出
     */
    public static SysAuditLog create(
            Long tenantId,
            Long userId,
            String action,
            String targetType,
            Long targetId,
            String ipAddress,
            String userAgent,
            String detail,
            String resultStatus,
            String logLevel) {

        // 参数验证
        Objects.requireNonNull(tenantId, "租户ID不能为null");
        Objects.requireNonNull(action, "操作类型不能为null");

        if (action.trim().isEmpty()) {
            throw new IllegalArgumentException("操作类型不能为空");
        }

        // 设置默认值
        String actualResultStatus = resultStatus != null ? resultStatus : ResultStatus.SUCCESS;
        String actualLogLevel = logLevel != null ? logLevel : LogLevel.INFO;

        return new SysAuditLog(
                null, // ID由数据库生成
                tenantId,
                userId,
                action.trim(),
                targetType,
                targetId,
                ipAddress,
                userAgent,
                detail,
                actualResultStatus,
                actualLogLevel,
                OffsetDateTime.now()
        );
    }

    /**
     * 创建一个简化的审计日志实例
     *
     * <p>这是一个便捷方法，适用于大多数常见的审计场景。它使用默认的结果状态和日志级别。</p>
     *
     * @param tenantId   租户ID
     * @param userId     用户ID
     * @param action     操作类型
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @param detail     详细信息
     * @return 新创建的审计日志实例
     */
    public static SysAuditLog create(
            Long tenantId,
            Long userId,
            String action,
            String targetType,
            Long targetId,
            String detail) {

        return create(tenantId, userId, action, targetType, targetId,
                null, null, detail, ResultStatus.SUCCESS, LogLevel.INFO);
    }

    /**
     * 创建一个失败的审计日志实例
     *
     * <p>这个方法专门用于记录操作失败的情况，会自动设置结果状态为FAILED，
     * 日志级别为ERROR。</p>
     *
     * @param tenantId    租户ID
     * @param userId      用户ID
     * @param action      操作类型
     * @param targetType  目标类型
     * @param targetId    目标ID
     * @param errorDetail 错误详情
     * @param ipAddress   IP地址
     * @param userAgent   用户代理
     * @return 新创建的失败审计日志实例
     */
    public static SysAuditLog createFailure(
            Long tenantId,
            Long userId,
            String action,
            String targetType,
            Long targetId,
            String errorDetail,
            String ipAddress,
            String userAgent) {

        return create(tenantId, userId, action, targetType, targetId,
                ipAddress, userAgent, errorDetail, ResultStatus.FAILED, LogLevel.ERROR);
    }

    /**
     * 创建一个系统操作的审计日志
     *
     * <p>这个方法专门用于记录系统自动执行的操作，userId会被设置为null，
     * 表示这是系统操作而非用户操作。</p>
     *
     * @param tenantId 租户ID
     * @param action   操作类型
     * @param detail   详细信息
     * @return 新创建的系统操作审计日志实例
     */
    public static SysAuditLog createSystemOperation(
            Long tenantId,
            String action,
            String detail) {

        return create(tenantId, null, action, "SYSTEM", null,
                "127.0.0.1", "System", detail, ResultStatus.SUCCESS, LogLevel.INFO);
    }

    /**
     * 检查这个审计日志是否表示一个成功的操作
     *
     * @return 如果操作成功则返回true，否则返回false
     */
    public boolean isSuccess() {
        return ResultStatus.SUCCESS.equals(resultStatus);
    }

    /**
     * 检查这个审计日志是否表示一个失败的操作
     *
     * @return 如果操作失败则返回true，否则返回false
     */
    public boolean isFailure() {
        return ResultStatus.FAILED.equals(resultStatus);
    }

    /**
     * 检查这个审计日志是否是系统操作
     *
     * @return 如果是系统操作则返回true，否则返回false
     */
    public boolean isSystemOperation() {
        return userId == null;
    }

    /**
     * 检查这个审计日志是否是高级别的（警告或错误）
     *
     * @return 如果是高级别日志则返回true，否则返回false
     */
    public boolean isHighLevel() {
        return LogLevel.WARN.equals(logLevel) ||
                LogLevel.ERROR.equals(logLevel) ||
                LogLevel.FATAL.equals(logLevel);
    }

    /**
     * 解析详细信息为Map对象
     *
     * <p>这个方法尝试将JSON格式的详细信息解析为Map对象，便于程序处理。
     * 如果解析失败，会抛出AuditServiceException。</p>
     *
     * @param objectMapper JSON映射器
     * @return 解析后的Map对象，如果detail为null则返回空Map
     * @throws AuditServiceException 当JSON解析失败时抛出
     */
    public Map<String, Object> parseDetailAsMap(ObjectMapper objectMapper) throws AuditServiceException {
        if (detail == null || detail.trim().isEmpty()) {
            return Map.of();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(detail, Map.class);
            return result;
        } catch (JsonProcessingException e) {
            throw new AuditServiceException("DETAIL_PARSE_ERROR",
                    "解析审计日志详细信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取格式化的显示字符串
     *
     * <p>这个方法返回一个人类可读的审计日志描述，适合在日志显示界面中使用。</p>
     *
     * @return 格式化的显示字符串
     */
    public String getDisplayString() {
        StringBuilder sb = new StringBuilder();

        sb.append("[").append(logLevel).append("] ");

        if (userId != null) {
            sb.append("用户 ").append(userId).append(" ");
        } else {
            sb.append("系统 ");
        }

        sb.append("执行了 ").append(action).append(" 操作");

        if (targetType != null) {
            sb.append("，目标类型：").append(targetType);
            if (targetId != null) {
                sb.append("，目标ID：").append(targetId);
            }
        }

        sb.append("，结果：").append(resultStatus);

        if (ipAddress != null) {
            sb.append("，IP：").append(ipAddress);
        }

        return sb.toString();
    }

    /**
     * 验证审计日志的有效性
     *
     * <p>这个方法检查审计日志的各个字段是否符合业务规则。
     * 它会验证必填字段、字段长度、格式等。</p>
     *
     * @throws IllegalArgumentException 当验证失败时抛出
     */
    public void validate() {
        if (tenantId == null) {
            throw new IllegalArgumentException("租户ID不能为null");
        }

        if (action == null || action.trim().isEmpty()) {
            throw new IllegalArgumentException("操作类型不能为空");
        }

        if (action.length() > 64) {
            throw new IllegalArgumentException("操作类型长度不能超过64个字符");
        }

        if (targetType != null && targetType.length() > 64) {
            throw new IllegalArgumentException("目标类型长度不能超过64个字符");
        }

        if (resultStatus == null || resultStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("结果状态不能为空");
        }

        if (logLevel == null || logLevel.trim().isEmpty()) {
            throw new IllegalArgumentException("日志级别不能为空");
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("创建时间不能为null");
        }

        // 验证IP地址格式（简单验证）
        if (ipAddress != null && !ipAddress.trim().isEmpty()) {
            if (!isValidIpAddress(ipAddress)) {
                throw new IllegalArgumentException("IP地址格式不正确: " + ipAddress);
            }
        }
    }

    /**
     * 简单的IP地址格式验证
     *
     * @param ip 待验证的IP地址
     * @return 如果格式正确则返回true，否则返回false
     */
    private boolean isValidIpAddress(String ip) {
        // 这里可以实现更复杂的IP地址验证逻辑
        // 为了简化，我们只做基本的格式检查
        return ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$") ||
                ip.matches("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$") ||
                "127.0.0.1".equals(ip) ||
                "::1".equals(ip);
    }

    /**
     * 创建一个带有更新ID的新实例
     *
     * <p>由于这是一个record类型，所有字段都是不可变的。
     * 这个方法用于在保存到数据库后更新ID字段。</p>
     *
     * @param newId 新的ID值
     * @return 带有新ID的审计日志实例
     */
    public SysAuditLog withId(Long newId) {
        return new SysAuditLog(
                newId,
                tenantId,
                userId,
                action,
                targetType,
                targetId,
                ipAddress,
                userAgent,
                detail,
                resultStatus,
                logLevel,
                createdAt
        );
    }

    /**
     * 创建一个带有更新详细信息的新实例
     *
     * <p>这个方法用于在需要更新详细信息时创建新的实例。
     * 通常用于添加额外的上下文信息。</p>
     *
     * @param newDetail 新的详细信息
     * @return 带有新详细信息的审计日志实例
     */
    public SysAuditLog withDetail(String newDetail) {
        return new SysAuditLog(
                id,
                tenantId,
                userId,
                action,
                targetType,
                targetId,
                ipAddress,
                userAgent,
                newDetail,
                resultStatus,
                logLevel,
                createdAt
        );
    }

    /**
     * 重写toString方法，提供详细的字符串表示
     *
     * @return 详细的字符串表示
     */
    @Override
    public String toString() {
        return String.format(
                "SysAuditLog{id=%d, tenantId=%d, userId=%s, action='%s', targetType='%s', " +
                        "targetId=%s, resultStatus='%s', logLevel='%s', createdAt=%s}",
                id, tenantId, userId, action, targetType, targetId, resultStatus, logLevel, createdAt
        );
    }
}