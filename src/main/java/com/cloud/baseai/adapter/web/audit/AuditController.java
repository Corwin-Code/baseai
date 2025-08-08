package com.cloud.baseai.adapter.web.audit;

import com.cloud.baseai.application.audit.command.AuditQueryCommand;
import com.cloud.baseai.application.audit.dto.AuditLogDTO;
import com.cloud.baseai.application.audit.dto.AuditStatisticsDTO;
import com.cloud.baseai.application.audit.dto.PageResultDTO;
import com.cloud.baseai.application.audit.service.AuditQueryAppService;
import com.cloud.baseai.infrastructure.exception.ApiResult;
import com.cloud.baseai.infrastructure.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * <h1>审计日志查询控制器</h1>
 *
 * <p>这个控制器就像一个智能的历史档案馆，它能够帮助管理员和审计人员
 * 快速查找和分析系统中发生的各种操作记录。想象一下，当需要调查某个
 * 安全事件时，这个控制器就是你的得力助手，能够精确定位到相关的操作记录。</p>
 *
 * <p><b>核心功能：</b></p>
 * <ul>
 * <li><b>灵活查询：</b>支持按用户、时间、操作类型等多种条件组合查询</li>
 * <li><b>安全控制：</b>确保用户只能查询有权限的审计数据</li>
 * <li><b>性能优化：</b>通过分页和索引确保大数据量下的查询性能</li>
 * <li><b>统计分析：</b>提供各种维度的统计信息，帮助发现系统使用模式</li>
 * </ul>
 *
 * <p><b>安全考虑：</b></p>
 * <p>审计日志包含敏感信息，因此所有接口都需要严格的权限控制。
 * 我们使用基于角色的访问控制（RBAC）来确保只有有权限的用户才能查看相关数据。</p>
 */
@RestController
@RequestMapping("/api/v1/audit")
@Validated
@Tag(name = "审计管理", description = "Audit Management APIs - 提供安全审计和操作记录查询功能")
public class AuditController {

    private static final Logger log = LoggerFactory.getLogger(AuditController.class);

    private final AuditQueryAppService auditQueryService;

    public AuditController(AuditQueryAppService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    /**
     * 查询用户操作历史
     *
     * <p>这个接口是审计系统的核心功能，就像银行的交易记录查询一样，
     * 能够准确地显示用户在系统中的所有操作轨迹。无论是合规检查、
     * 安全调查还是问题排查，这个接口都能提供详细的历史信息。</p>
     *
     * <p><b>查询能力：</b></p>
     * <p>支持多维度的查询条件组合，可以精确定位到特定的操作记录。
     * 比如查看某个用户在特定时间段内的所有登录记录，或者查看
     * 某个时间段内所有的数据修改操作。</p>
     */
    @GetMapping("/logs")
    @Operation(
            summary = "查询审计日志",
            description = "根据多种条件查询系统审计日志，支持按用户、时间范围、操作类型等条件过滤。适用于安全分析、合规检查和问题排查。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功",
                    content = @Content(schema = @Schema(implementation = PageResultDTO.class))),
            @ApiResponse(responseCode = "400", description = "查询参数无效",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "权限不足",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<ApiResult<PageResultDTO<AuditLogDTO>>> queryAuditLogs(
            @Parameter(description = "用户ID，不填则查询所有用户", example = "123")
            @RequestParam(required = false) Long userId,

            @Parameter(description = "租户ID，必填参数用于数据隔离", example = "1")
            @RequestParam Long tenantId,

            @Parameter(description = "开始时间，ISO格式", example = "2024-01-01T00:00:00Z")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,

            @Parameter(description = "结束时间，ISO格式", example = "2024-01-31T23:59:59Z")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,

            @Parameter(description = "操作类型列表，支持多选", example = "USER_LOGIN,DATA_UPDATE")
            @RequestParam(required = false) List<String> actions,

            @Parameter(description = "页码，从0开始", example = "0")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "页码不能小于0") Integer page,

            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "每页大小不能小于1")
            @Max(value = 100, message = "每页大小不能超过100") Integer size,

            @Parameter(description = "排序字段", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "排序方向", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.debug("收到审计日志查询请求: userId={}, tenantId={}, page={}, size={}",
                userId, tenantId, page, size);

        try {
            // 构建查询命令
            AuditQueryCommand command = new AuditQueryCommand(
                    userId, tenantId, startTime, endTime, actions,
                    page, size, sortBy, sortDir
            );

            // 执行查询
            PageResultDTO<AuditLogDTO> result = auditQueryService.queryAuditLogs(command);

            String message = String.format("查询完成，共找到 %d 条记录", result.totalElements());

            return ResponseEntity.ok(ApiResult.success(result, message));

        } catch (Exception e) {
            log.error("审计日志查询失败", e);
            throw e; // 让全局异常处理器处理
        }
    }

    /**
     * 查询安全事件
     *
     * <p>安全事件是审计系统中的重点关注对象。这个接口专门用于查询
     * 可能涉及安全风险的操作记录，如连续的登录失败、权限提升、
     * 异常时间的操作等。</p>
     */
    @GetMapping("/security-events")
    @Operation(
            summary = "查询安全事件",
            description = "专门查询安全相关的审计记录，如登录失败、权限异常等。用于安全监控和威胁分析。"
    )
    @PreAuthorize("hasRole('SECURITY_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResult<PageResultDTO<AuditLogDTO>>> querySecurityEvents(
            @RequestParam Long tenantId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endTime,
            @RequestParam(required = false) List<String> riskLevels,
            @RequestParam(required = false) String sourceIp,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {

        log.info("查询安全事件: tenantId={}, sourceIp={}, timeRange=[{}, {}]",
                tenantId, sourceIp, startTime, endTime);

        // 构建安全事件查询命令
        AuditQueryCommand command = AuditQueryCommand.forSecurityEvents(
                tenantId, startTime, endTime, riskLevels, sourceIp, page, size
        );

        PageResultDTO<AuditLogDTO> result = auditQueryService.querySecurityEvents(command);

        String message = String.format("安全事件查询完成，发现 %d 条可疑记录", result.totalElements());

        return ResponseEntity.ok(ApiResult.success(result, message));
    }

    /**
     * 获取审计统计信息
     *
     * <p>统计信息能够帮助我们从宏观角度了解系统的使用情况。
     * 就像企业的运营报表一样，这些数据能够揭示系统的使用模式、
     * 热点功能、异常趋势等重要信息。</p>
     */
    @GetMapping("/statistics")
    @Operation(
            summary = "获取审计统计信息",
            description = "获取审计数据的统计分析，包括操作频率、用户活跃度、趋势分析等。支持不同时间维度的统计。"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<ApiResult<AuditStatisticsDTO>> getAuditStatistics(
            @Parameter(description = "租户ID", required = true, example = "1")
            @RequestParam Long tenantId,

            @Parameter(description = "统计时间范围", example = "LAST_7_DAYS")
            @RequestParam(defaultValue = "LAST_7_DAYS") String timeRange,

            @Parameter(description = "统计维度", example = "BY_ACTION")
            @RequestParam(defaultValue = "BY_ACTION") String dimension) {

        log.debug("获取审计统计信息: tenantId={}, timeRange={}, dimension={}",
                tenantId, timeRange, dimension);

        AuditStatisticsDTO statistics = auditQueryService.getAuditStatistics(
                tenantId, timeRange, dimension
        );

        return ResponseEntity.ok(ApiResult.success(statistics, "统计信息获取成功"));
    }

    /**
     * 查询指定对象的操作历史
     *
     * <p>有时我们需要查看对特定对象（如某个文档、某个用户账户）
     * 的所有操作记录。这个接口就像为每个重要对象建立的专属档案，
     * 记录了它的完整生命周期。</p>
     */
    @GetMapping("/objects/{targetType}/{targetId}/history")
    @Operation(
            summary = "查询对象操作历史",
            description = "查询针对特定对象的所有操作记录，如某个文档的所有变更历史。适用于对象生命周期追踪。"
    )
    @PreAuthorize("hasPermission(#targetId, #targetType, 'READ')")
    public ResponseEntity<ApiResult<PageResultDTO<AuditLogDTO>>> queryObjectHistory(
            @Parameter(description = "目标对象类型", example = "DOCUMENT")
            @PathVariable String targetType,

            @Parameter(description = "目标对象ID", example = "123")
            @PathVariable Long targetId,

            @Parameter(description = "租户ID", example = "1")
            @RequestParam Long tenantId,

            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {

        log.debug("查询对象操作历史: targetType={}, targetId={}, tenantId={}",
                targetType, targetId, tenantId);

        AuditQueryCommand command = AuditQueryCommand.forObjectHistory(
                targetType, targetId, tenantId, page, size
        );

        PageResultDTO<AuditLogDTO> result = auditQueryService.queryObjectHistory(command);

        String message = String.format("对象历史查询完成，找到 %d 条操作记录", result.totalElements());

        return ResponseEntity.ok(ApiResult.success(result, message));
    }

    /**
     * 导出审计报告
     *
     * <p>合规部门经常需要生成各种审计报告用于监管提交或内部审查。
     * 这个接口能够根据指定条件生成标准化的审计报告。</p>
     */
    @PostMapping("/reports/export")
    @Operation(
            summary = "导出审计报告",
            description = "根据指定条件生成并导出审计报告，支持多种格式。适用于合规检查和定期审计。"
    )
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResult<String>> exportAuditReport(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "报告导出参数",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "月度安全审计报告",
                                    value = """
                                            {
                                              "tenantId": 1,
                                              "reportType": "SECURITY_MONTHLY",
                                              "startTime": "2024-01-01T00:00:00Z",
                                              "endTime": "2024-01-31T23:59:59Z",
                                              "format": "PDF",
                                              "includeDetails": true
                                            }
                                            """
                            )
                    )
            ) AuditQueryCommand.ReportExportCommand command) {

        log.info("收到审计报告导出请求: reportType={}, tenantId={}, format={}",
                command.reportType(), command.tenantId(), command.format());

        String reportId = auditQueryService.exportAuditReport(command);

        String message = String.format("审计报告生成已启动，报告ID: %s", reportId);

        return ResponseEntity.ok(ApiResult.success(reportId, message));
    }

    /**
     * 获取当前用户的审计摘要
     *
     * <p>这个接口为当前登录用户提供他们自己的操作摘要，
     * 帮助用户了解自己的系统使用情况。这既是透明度的体现，
     * 也是安全意识培养的一部分。</p>
     */
    @GetMapping("/my-summary")
    @Operation(
            summary = "获取当前用户审计摘要",
            description = "获取当前登录用户的操作摘要，包括近期活动统计和安全提醒。"
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResult<AuditStatisticsDTO>> getMyAuditSummary(
            @Parameter(description = "统计天数", example = "30")
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) Integer days) {

        log.debug("获取当前用户审计摘要: days={}", days);

        // 从当前认证上下文获取用户信息
        Long currentUserId = getCurrentUserId();
        Long currentTenantId = getCurrentTenantId();

        AuditStatisticsDTO summary = auditQueryService.getUserAuditSummary(
                currentUserId, currentTenantId, days
        );

        return ResponseEntity.ok(ApiResult.success(summary, "个人审计摘要获取成功"));
    }

    // =================== 私有辅助方法 ===================

    /**
     * 从当前认证上下文获取用户ID
     *
     * <p>这个方法从Spring Security的认证上下文中提取当前登录用户的ID。
     * 在实际项目中，这里的实现会依赖于具体的认证机制。</p>
     */
    private Long getCurrentUserId() {
        // 实际实现中，这里应该从 SecurityContextHolder 获取当前用户信息
        // 为了演示，暂时返回一个固定值
        return 1L; // 这应该从认证上下文获取
    }

    /**
     * 从当前认证上下文获取租户ID
     */
    private Long getCurrentTenantId() {
        // 实际实现中，这里应该从当前用户的上下文获取租户信息
        return 1L; // 这应该从用户上下文获取
    }
}