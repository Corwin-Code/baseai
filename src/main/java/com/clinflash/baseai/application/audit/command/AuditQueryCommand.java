package com.clinflash.baseai.application.audit.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * <h2>审计查询命令对象</h2>
 *
 * <p>命令对象是CQRS（命令查询职责分离）模式的重要组成部分。这个查询命令
 * 就像一张详细的"查询订单"，它精确地描述了用户想要什么样的数据，
 * 以及希望如何组织和展示这些数据。</p>
 *
 * <p><b>设计哲学：</b></p>
 * <p>我们将查询意图封装在命令对象中，这样做的好处是：查询逻辑变得可测试、
 * 可复用、可组合。同时，验证规则也集中在命令对象中，确保数据的完整性。</p>
 *
 * @param userId     用户ID过滤条件，null表示查询所有用户
 * @param tenantId   租户ID，必填，用于数据隔离
 * @param startTime  查询开始时间，null表示无时间限制
 * @param endTime    查询结束时间，null表示无时间限制
 * @param actions    操作类型过滤列表，null表示所有操作类型
 * @param page       页码，从0开始
 * @param size       每页大小，1-100之间
 * @param sortBy     排序字段，默认为createdAt
 * @param sortDir    排序方向，asc或desc
 * @param targetType 目标对象类型过滤
 * @param targetId   目标对象ID过滤
 * @param riskLevels 风险级别过滤（用于安全事件查询）
 * @param sourceIp   来源IP过滤（用于安全分析）
 */
@Schema(description = "审计查询命令")
public record AuditQueryCommand(
        @Schema(description = "用户ID，null表示查询所有用户", example = "123")
        Long userId,

        @Schema(description = "租户ID，必填", example = "1")
        @NotNull(message = "租户ID不能为空")
        Long tenantId,

        @Schema(description = "开始时间", example = "2024-01-01T00:00:00Z")
        OffsetDateTime startTime,

        @Schema(description = "结束时间", example = "2024-01-31T23:59:59Z")
        OffsetDateTime endTime,

        @Schema(description = "操作类型列表", example = "[\"USER_LOGIN\", \"DATA_UPDATE\"]")
        List<String> actions,

        @Schema(description = "页码（从0开始）", example = "0")
        @Min(value = 0, message = "页码不能小于0")
        int page,

        @Schema(description = "每页大小", example = "20")
        @Min(value = 1, message = "每页大小不能小于1")
        @Max(value = 100, message = "每页大小不能超过100")
        int size,

        @Schema(description = "排序字段", example = "createdAt")
        String sortBy,

        @Schema(description = "排序方向", example = "desc")
        @Pattern(regexp = "asc|desc", message = "排序方向只能是asc或desc")
        String sortDir,

        @Schema(description = "目标对象类型", example = "DOCUMENT")
        String targetType,

        @Schema(description = "目标对象ID", example = "456")
        Long targetId,

        @Schema(description = "风险级别列表", example = "[\"HIGH\", \"CRITICAL\"]")
        List<String> riskLevels,

        @Schema(description = "来源IP地址", example = "192.168.1.100")
        String sourceIp
) {

    /**
     * 创建基本查询命令的便利构造函数
     *
     * <p>这个构造函数提供了最常用的查询参数，简化了客户端的调用。
     * 它体现了"约定优于配置"的设计理念。</p>
     */
    public AuditQueryCommand(Long userId, Long tenantId, OffsetDateTime startTime,
                             OffsetDateTime endTime, List<String> actions,
                             int page, int size, String sortBy, String sortDir) {
        this(userId, tenantId, startTime, endTime, actions, page, size,
                sortBy, sortDir, null, null, null, null);
    }

    /**
     * 创建安全事件查询命令的静态工厂方法
     *
     * <p>这个工厂方法专门用于创建安全事件查询，它预设了一些安全查询的默认参数。</p>
     */
    public static AuditQueryCommand forSecurityEvents(Long tenantId, OffsetDateTime startTime,
                                                      OffsetDateTime endTime, List<String> riskLevels,
                                                      String sourceIp, int page, int size) {
        return new AuditQueryCommand(
                null, tenantId, startTime, endTime,
                getSecurityActions(), // 预定义的安全相关操作列表
                page, size, "createdAt", "desc",
                null, null, riskLevels, sourceIp
        );
    }

    /**
     * 创建对象历史查询命令的静态工厂方法
     *
     * <p>专门用于查询特定对象的操作历史，这在问题追踪和审计调查中很有用。</p>
     */
    public static AuditQueryCommand forObjectHistory(String targetType, Long targetId,
                                                     Long tenantId, int page, int size) {
        return new AuditQueryCommand(
                null, tenantId, null, null, null,
                page, size, "createdAt", "desc",
                targetType, targetId, null, null
        );
    }

    /**
     * 验证查询命令的有效性
     *
     * <p>这个方法执行业务层面的验证，确保查询参数在业务逻辑上是合理的。</p>
     */
    public void validate() {
        // 验证时间范围
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("开始时间不能晚于结束时间");
        }

        // 验证排序字段
        if (sortBy != null && !isValidSortField(sortBy)) {
            throw new IllegalArgumentException("无效的排序字段: " + sortBy);
        }

        // 验证操作类型
        if (actions != null) {
            for (String action : actions) {
                if (action == null || action.trim().isEmpty()) {
                    throw new IllegalArgumentException("操作类型不能为空");
                }
            }
        }
    }

    /**
     * 判断是否为复杂查询
     *
     * <p>复杂查询可能需要特殊的性能优化策略。</p>
     */
    public boolean isComplexQuery() {
        int conditionCount = 0;

        if (userId != null) conditionCount++;
        if (startTime != null || endTime != null) conditionCount++;
        if (actions != null && !actions.isEmpty()) conditionCount++;
        if (targetType != null || targetId != null) conditionCount++;
        if (sourceIp != null) conditionCount++;

        return conditionCount >= 3; // 三个以上条件认为是复杂查询
    }

    /**
     * 获取查询摘要
     *
     * <p>生成便于日志记录和调试的查询摘要信息。</p>
     */
    public String getQuerySummary() {
        StringBuilder summary = new StringBuilder("审计查询: ");

        if (userId != null) {
            summary.append("用户=").append(userId).append(" ");
        }

        if (startTime != null || endTime != null) {
            summary.append("时间范围=[").append(startTime).append(",").append(endTime).append("] ");
        }

        if (actions != null && !actions.isEmpty()) {
            summary.append("操作=").append(String.join(",", actions)).append(" ");
        }

        summary.append("分页=[").append(page).append(",").append(size).append("]");

        return summary.toString();
    }

    // 私有辅助方法
    private static List<String> getSecurityActions() {
        return List.of(
                "USER_LOGIN_FAILED", "PERMISSION_DENIED", "AUTH_FAILURE",
                "SECURITY_VIOLATION", "SUSPICIOUS_ACTIVITY"
        );
    }

    private boolean isValidSortField(String field) {
        List<String> validFields = List.of(
                "id", "createdAt", "action", "userId", "targetType", "resultStatus", "logLevel"
        );
        return validFields.contains(field);
    }

    /**
     * <h3>报告导出命令子类</h3>
     *
     * <p>这个内部记录类专门用于审计报告的导出请求。它包含了生成报告所需的
     * 所有参数，如报告类型、时间范围、导出格式等。</p>
     */
    @Schema(description = "审计报告导出命令")
    public record ReportExportCommand(
            @Schema(description = "租户ID", example = "1")
            @NotNull Long tenantId,

            @Schema(description = "报告类型", example = "SECURITY_MONTHLY")
            @NotBlank String reportType,

            @Schema(description = "开始时间")
            @NotNull OffsetDateTime startTime,

            @Schema(description = "结束时间")
            @NotNull OffsetDateTime endTime,

            @Schema(description = "导出格式", example = "PDF")
            @Pattern(regexp = "PDF|EXCEL|CSV", message = "格式只能是PDF、EXCEL或CSV")
            String format,

            @Schema(description = "是否包含详细信息", example = "true")
            boolean includeDetails,

            @Schema(description = "附加过滤条件")
            Map<String, Object> filters
    ) {
        /**
         * 验证导出命令的有效性
         */
        public void validate() {
            if (startTime.isAfter(endTime)) {
                throw new IllegalArgumentException("开始时间不能晚于结束时间");
            }

            // 检查时间范围是否合理（不超过1年）
            if (startTime.isBefore(endTime.minusYears(1))) {
                throw new IllegalArgumentException("报告时间范围不能超过1年");
            }
        }

        /**
         * 获取导出文件名
         */
        public String getFileName() {
            return String.format("%s_%s_%s.%s",
                    reportType,
                    startTime.toLocalDate(),
                    endTime.toLocalDate(),
                    format.toLowerCase()
            );
        }
    }
}