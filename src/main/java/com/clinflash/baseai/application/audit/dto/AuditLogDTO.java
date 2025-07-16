package com.clinflash.baseai.application.audit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * <h2>审计日志数据传输对象</h2>
 *
 * <p>这个DTO就像是审计记录的"名片"，它包含了前端界面需要显示的所有关键信息。
 * 设计这个DTO时，我们特别考虑了用户体验 - 比如提供用户友好的显示名称、
 * 人类可读的操作描述、以及结构化的详细信息。</p>
 *
 * <p><b>设计理念：</b></p>
 * <p>我们遵循"展示即所得"的原则，DTO中的每个字段都是为了更好地服务于
 * 前端展示需求。比如，我们不仅提供userId，还提供userDisplayName，
 * 这样前端就不需要额外查询用户信息了。</p>
 *
 * @param id                审计日志唯一标识
 * @param tenantId          租户ID，用于数据隔离
 * @param userId            操作用户ID，null表示系统操作
 * @param userDisplayName   用户显示名称，方便前端展示
 * @param action            操作类型代码
 * @param actionDescription 操作描述，人类可读的操作说明
 * @param targetType        操作目标类型
 * @param targetId          操作目标ID
 * @param ipAddress         操作来源IP地址
 * @param userAgent         用户代理信息
 * @param detail            操作详细信息，结构化数据
 * @param resultStatus      操作结果状态
 * @param logLevel          日志级别
 * @param createdAt         操作时间
 */
@Schema(description = "审计日志数据传输对象")
public record AuditLogDTO(
        @Schema(description = "审计日志ID", example = "12345")
        Long id,

        @Schema(description = "租户ID", example = "1")
        Long tenantId,

        @Schema(description = "操作用户ID，null表示系统操作", example = "123")
        Long userId,

        @Schema(description = "用户显示名称", example = "张三")
        String userDisplayName,

        @Schema(description = "操作类型代码", example = "USER_LOGIN")
        String action,

        @Schema(description = "操作描述", example = "用户登录")
        String actionDescription,

        @Schema(description = "操作目标类型", example = "DOCUMENT")
        String targetType,

        @Schema(description = "操作目标ID", example = "456")
        Long targetId,

        @Schema(description = "操作IP地址", example = "192.168.1.100")
        String ipAddress,

        @Schema(description = "用户代理信息", example = "Mozilla/5.0...")
        String userAgent,

        @Schema(description = "操作详细信息")
        Map<String, Object> detail,

        @Schema(description = "操作结果", example = "SUCCESS")
        String resultStatus,

        @Schema(description = "日志级别", example = "INFO")
        String logLevel,

        @Schema(description = "操作时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        OffsetDateTime createdAt
) {

    /**
     * 判断是否为系统操作
     *
     * <p>系统操作是由系统自动执行的操作，如定时任务、系统维护等。
     * 这些操作没有具体的用户关联。</p>
     *
     * @return true如果是系统操作，false如果是用户操作
     */
    public boolean isSystemOperation() {
        return userId == null;
    }

    /**
     * 判断是否为失败的操作
     *
     * <p>失败的操作通常需要特别关注，可能表示系统问题或安全威胁。</p>
     *
     * @return true如果操作失败，false如果操作成功
     */
    public boolean isFailedOperation() {
        return "FAILED".equals(resultStatus);
    }

    /**
     * 判断是否为高级别日志
     *
     * <p>高级别日志（WARN、ERROR、FATAL）通常表示需要关注的重要事件。</p>
     *
     * @return true如果是高级别日志，false如果是普通日志
     */
    public boolean isHighLevelLog() {
        return "WARN".equals(logLevel) || "ERROR".equals(logLevel) || "FATAL".equals(logLevel);
    }

    /**
     * 获取操作摘要
     *
     * <p>生成简洁的操作摘要，适合在列表界面显示。</p>
     *
     * @return 操作摘要字符串
     */
    public String getOperationSummary() {
        StringBuilder summary = new StringBuilder();

        if (userDisplayName != null) {
            summary.append(userDisplayName);
        } else if (userId != null) {
            summary.append("用户").append(userId);
        } else {
            summary.append("系统");
        }

        summary.append(" ").append(actionDescription != null ? actionDescription : action);

        if (targetType != null && targetId != null) {
            summary.append(" (").append(targetType).append("#").append(targetId).append(")");
        }

        return summary.toString();
    }
}