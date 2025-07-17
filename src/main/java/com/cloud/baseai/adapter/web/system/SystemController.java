package com.cloud.baseai.adapter.web.system;

import com.cloud.baseai.application.system.command.BatchUpdateSettingsCommand;
import com.cloud.baseai.application.system.command.CreateSystemTaskCommand;
import com.cloud.baseai.application.system.command.RetryTaskCommand;
import com.cloud.baseai.application.system.command.UpdateSystemSettingCommand;
import com.cloud.baseai.application.system.dto.*;
import com.cloud.baseai.application.system.service.SystemApplicationService;
import com.cloud.baseai.infrastructure.web.response.ApiResult;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <h1>系统管理REST控制器</h1>
 *
 * <p>系统管理控制器是整个平台的"控制中心"。
 * 它为管理员提供了监控和控制整个系统的能力。通过这个控制器，管理员可以：</p>
 *
 * <ul>
 * <li><b>配置管理：</b>调整系统的各种参数，优化运行效果</li>
 * <li><b>任务监控：</b>查看和管理后台任务的执行情况</li>
 * <li><b>健康检查：</b>实时监控系统各组件的运行状态</li>
 * <li><b>统计分析：</b>获取系统运行的关键指标和趋势</li>
 * </ul>
 *
 * <p><b>安全性考虑：</b></p>
 * <p>系统管理功能具有极高的权限要求，因为错误的配置可能导致整个系统的异常。
 * 因此，我们对每个接口都进行了严格的权限控制，只有经过授权的系统管理员
 * 才能执行相关操作。同时，所有的操作都会被详细记录，以便事后审计。</p>
 */
@RestController
@RequestMapping("/api/v1/system")
@Validated
@Tag(name = "系统管理", description = "System Management APIs - 提供系统配置、任务管理和监控功能")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SystemController {

    private static final Logger log = LoggerFactory.getLogger(SystemController.class);

    private final SystemApplicationService appService;

    public SystemController(SystemApplicationService appService) {
        this.appService = appService;
    }

    // =================== 系统设置管理接口 ===================

    /**
     * 获取所有系统设置
     *
     * <p>系统设置是平台运行的基础，涵盖了从性能参数到功能开关的各个方面。
     * 这个接口为管理员提供了系统配置的全景视图，让他们能够了解当前的配置状态。</p>
     */
    @GetMapping("/settings")
    @Operation(
            summary = "获取系统设置列表",
            description = "获取所有系统配置项，敏感配置将被掩码处理。只有系统管理员可以访问。"
    )
    @ApiResponse(responseCode = "200", description = "获取成功",
            content = @Content(schema = @Schema(implementation = SystemSettingDTO.class)))
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResult<List<SystemSettingDTO>>> getAllSettings() {

        log.debug("获取所有系统设置");

        List<SystemSettingDTO> result = appService.getAllSettings();

        return ResponseEntity.ok(ApiResult.success(result,
                String.format("获取到 %d 个系统设置", result.size())));
    }

    /**
     * 获取指定设置
     */
    @GetMapping("/settings/{key}")
    @Operation(summary = "获取指定设置", description = "根据键获取特定的系统设置项。")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResult<SystemSettingDTO>> getSettingByKey(
            @PathVariable String key) {

        log.debug("获取系统设置: key={}", key);

        Optional<SystemSettingDTO> result = appService.getSettingByKey(key);

        return result.map(systemSettingDTO -> ResponseEntity.ok(ApiResult.success(systemSettingDTO)))
                .orElseGet(() -> ResponseEntity.ok(ApiResult.success(null, "设置不存在")));
    }

    /**
     * 更新系统设置
     *
     * <p>更新系统设置是一个高风险操作，因为错误的配置可能导致系统异常。
     * 我们提供了完善的验证机制和回滚策略，确保配置变更的安全性。</p>
     */
    @PutMapping(value = "/settings/{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "更新系统设置",
            description = "更新指定的系统配置项。会根据值的格式自动推断数据类型，并进行格式验证。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "配置值格式无效"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResult<SystemSettingDTO>> updateSetting(
            @PathVariable String key,
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "系统设置更新信息",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "更新邮件服务配置",
                                    value = """
                                            {
                                              "key": "email.smtp.host",
                                              "value": "smtp.example.com",
                                              "remark": "邮件服务器地址",
                                              "operatorId": 1
                                            }
                                            """
                            )
                    )
            ) UpdateSystemSettingCommand cmd) {

        log.info("更新系统设置: key={}, operatorId={}", key, cmd.operatorId());

        // 确保路径参数和请求体中的key一致
        UpdateSystemSettingCommand actualCmd = new UpdateSystemSettingCommand(
                key, cmd.value(), cmd.remark(), cmd.operatorId());

        SystemSettingDTO result = appService.updateSetting(actualCmd);

        return ResponseEntity.ok(ApiResult.success(result, "系统设置更新成功"));
    }

    /**
     * 批量更新设置
     */
    @PutMapping(value = "/settings", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "批量更新设置", description = "一次性更新多个系统配置项，采用最大努力策略。")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResult<Map<String, SystemSettingDTO>>> batchUpdateSettings(
            @Valid @RequestBody BatchUpdateSettingsCommand cmd) {

        log.info("批量更新系统设置: 设置数量={}, operatorId={}", cmd.settings().size(), cmd.operatorId());

        Map<String, SystemSettingDTO> result = appService.batchUpdateSettings(cmd);

        return ResponseEntity.ok(ApiResult.success(result,
                String.format("批量更新完成，成功更新 %d 个设置", result.size())));
    }

    // =================== 系统任务管理接口 ===================

    /**
     * 创建系统任务
     *
     * <p>系统任务是处理异步操作的重要机制。通过这个接口，管理员可以手动创建
     * 各种类型的后台任务，比如数据清理、报告生成、系统维护等。</p>
     */
    @PostMapping(value = "/tasks", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "创建系统任务",
            description = "创建新的后台任务。任务将被加入队列等待执行，支持重试机制。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "任务创建成功"),
            @ApiResponse(responseCode = "400", description = "任务参数无效")
    })
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResult<SystemTaskDTO>> createTask(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "系统任务创建信息",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "创建数据清理任务",
                                    value = """
                                            {
                                              "tenantId": 1,
                                              "taskType": "DATA_CLEANUP",
                                              "payload": {
                                                "targetTable": "audit_logs",
                                                "retentionDays": 90,
                                                "batchSize": 1000
                                              },
                                              "createdBy": 1
                                            }
                                            """
                            )
                    )
            ) CreateSystemTaskCommand cmd) {

        log.info("创建系统任务: taskType={}, tenantId={}", cmd.taskType(), cmd.tenantId());

        SystemTaskDTO result = appService.createTask(cmd);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(result, "系统任务创建成功"));
    }

    /**
     * 获取任务列表
     *
     * <p>任务列表是监控系统运行状况的重要窗口。管理员可以通过这个界面
     * 了解当前的任务执行情况，及时发现和处理异常任务。</p>
     */
    @GetMapping("/tasks")
    @Operation(
            summary = "获取系统任务列表",
            description = "分页获取系统任务，支持按租户和状态过滤。提供任务执行的详细统计信息。"
    )
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResult<PageResultDTO<SystemTaskDTO>>> listTasks(
            @Parameter(description = "租户ID过滤", example = "1")
            @RequestParam(required = false) Long tenantId,

            @Parameter(description = "任务状态过滤", example = "FAILED")
            @RequestParam(required = false) String status,

            @Parameter(description = "页码（从0开始）", example = "0")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "页码不能小于0")
            Integer page,

            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "每页大小不能小于1")
            @Max(value = 100, message = "每页大小不能超过100")
            Integer size) {

        log.debug("查询系统任务列表: tenantId={}, status={}, page={}, size={}",
                tenantId, status, page, size);

        PageResultDTO<SystemTaskDTO> result = appService.listTasks(tenantId, status, page, size);

        return ResponseEntity.ok(ApiResult.success(result,
                String.format("查询完成，共找到 %d 个任务", result.totalElements())));
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "获取任务详情", description = "获取指定任务的完整信息，包括执行参数和结果。")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResult<SystemTaskDetailDTO>> getTaskDetail(
            @PathVariable Long taskId) {

        log.debug("获取任务详情: taskId={}", taskId);

        SystemTaskDetailDTO result = appService.getTaskDetail(taskId);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 重试失败任务
     *
     * <p>重试机制是提高系统可靠性的重要手段。当任务因为临时性问题失败时，
     * 管理员可以通过这个接口手动触发重试，让系统自动恢复处理。</p>
     */
    @PostMapping("/tasks/{taskId}/retry")
    @Operation(summary = "重试失败任务", description = "手动重试失败的任务。只有满足重试条件的任务才能重试。")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResult<SystemTaskDTO>> retryTask(
            @PathVariable Long taskId,
            @RequestParam Long operatorId) {

        log.info("重试系统任务: taskId={}, operatorId={}", taskId, operatorId);

        RetryTaskCommand cmd = new RetryTaskCommand(taskId, operatorId);
        SystemTaskDTO result = appService.retryTask(cmd);

        return ResponseEntity.ok(ApiResult.success(result, "任务重试成功"));
    }

    // =================== 系统监控和统计接口 ===================

    /**
     * 获取系统统计信息
     *
     * <p>系统统计是运维监控的核心数据，它为管理员提供了系统运行状况的量化指标。
     * 通过这些数据，可以及时发现性能瓶颈、容量问题和异常趋势。</p>
     */
    @GetMapping("/statistics")
    @Operation(
            summary = "获取系统统计信息",
            description = "获取系统运行的全面统计数据，包括配置数量、任务状态、成功率等关键指标。"
    )
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResult<SystemStatisticsDTO>> getSystemStatistics() {

        SystemStatisticsDTO result = appService.getSystemStatistics();
        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 系统健康检查
     *
     * <p>健康检查是系统监控的第一道防线。它会检查各个关键组件的状态，
     * 包括数据库连接、任务队列、外部服务等，帮助运维人员及时发现问题。</p>
     */
    @GetMapping("/health")
    @Operation(
            summary = "系统健康检查",
            description = "检查系统各组件的健康状态，包括数据库、任务队列、外部服务等。提供详细的诊断信息。"
    )
    public ResponseEntity<ApiResult<SystemHealthDTO>> checkSystemHealth() {

        SystemHealthDTO status = appService.checkSystemHealth();

        HttpStatus httpStatus = "healthy".equals(status.status()) ?
                HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(httpStatus)
                .body(ApiResult.success(status, "健康检查完成"));
    }
}