package com.cloud.baseai.adapter.web.mcp;

import com.cloud.baseai.application.mcp.command.AuthorizeTenantForToolCommand;
import com.cloud.baseai.application.mcp.command.ExecuteToolCommand;
import com.cloud.baseai.application.mcp.command.RegisterToolCommand;
import com.cloud.baseai.application.mcp.dto.*;
import com.cloud.baseai.application.mcp.service.McpApplicationService;
import com.cloud.baseai.infrastructure.exception.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <h1>MCP工具管理REST控制器</h1>
 *
 * <p>MCP (Model Context Protocol) 工具系统是现代AI应用的重要组成部分。
 * 它允许AI模型通过标准化的接口调用外部工具和服务，极大地扩展了AI的能力边界。
 * 想象AI不再只是一个会说话的大脑，而是拥有了手和眼睛，能够与真实世界交互。</p>
 *
 * <p><b>MCP的核心价值：</b></p>
 * <p>传统的AI模型只能基于训练数据进行推理，无法获取实时信息或执行具体操作。
 * MCP工具系统打破了这个限制，让AI能够调用搜索引擎、访问数据库、操作文件、
 * 发送邮件等，真正成为能够解决实际问题的智能助手。</p>
 *
 * <p><b>完整功能模块：</b></p>
 * <ul>
 * <li><b>工具注册：</b>支持HTTP API、本地脚本、第三方服务等多种工具类型</li>
 * <li><b>权限管理：</b>细粒度的租户级工具授权和配额控制</li>
 * <li><b>执行引擎：</b>安全可靠的工具调用执行和结果处理</li>
 * <li><b>监控审计：</b>完整的调用日志和性能监控</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/mcp")
@Validated
@Tag(name = "MCP工具管理", description = "Model Context Protocol Tool Management - 提供AI工具的注册、授权和调用服务")
@CrossOrigin(origins = "*", maxAge = 3600)
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);

    private final McpApplicationService appService;

    public McpController(McpApplicationService appService) {
        this.appService = appService;
    }

    // =================== 工具注册管理 ===================

    /**
     * 注册新工具
     *
     * <p>工具注册是MCP系统的入口。开发者可以注册各种类型的工具：
     * HTTP API、本地脚本、数据库连接等。每个工具都有详细的参数规范，
     * 确保AI模型能够正确调用。</p>
     */
    @PostMapping(value = "/tools", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "注册新工具",
            description = "注册一个新的MCP工具，支持HTTP API、脚本、数据库等多种类型。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "工具注册成功"),
            @ApiResponse(responseCode = "400", description = "工具配置无效"),
            @ApiResponse(responseCode = "409", description = "工具代码已存在")
    })
    @PreAuthorize("hasRole('TOOL_ADMIN')")
    public ResponseEntity<ApiResult<ToolDTO>> registerTool(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "工具注册信息",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "天气查询API工具",
                                    value = """
                                            {
                                              "code": "weather_api",
                                              "name": "天气查询",
                                              "type": "HTTP",
                                              "description": "获取指定城市的天气信息",
                                              "endpoint": "https://api.weather.com/v1/current",
                                              "authType": "API_KEY",
                                              "paramSchema": {
                                                "type": "object",
                                                "properties": {
                                                  "city": {
                                                    "type": "string",
                                                    "description": "城市名称"
                                                  },
                                                  "unit": {
                                                    "type": "string",
                                                    "enum": ["celsius", "fahrenheit"],
                                                    "default": "celsius"
                                                  }
                                                },
                                                "required": ["city"]
                                              },
                                              "operatorId": 123
                                            }
                                            """
                            )
                    )
            ) RegisterToolCommand cmd) {

        log.info("注册新工具: code={}, type={}", cmd.code(), cmd.type());

        ToolDTO result = appService.registerTool(cmd);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(result, "工具注册成功"));
    }

    /**
     * 获取工具列表
     */
    @GetMapping("/tools")
    @Operation(summary = "获取工具列表", description = "分页获取所有已注册的MCP工具。")
    public ResponseEntity<ApiResult<PageResultDTO<ToolDTO>>> listTools(
            @Parameter(description = "页码", example = "0")
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") Integer size,

            @Parameter(description = "工具类型过滤", example = "HTTP")
            @RequestParam(required = false) String type,

            @Parameter(description = "启用状态过滤", example = "true")
            @RequestParam(required = false) Boolean enabled) {

        PageResultDTO<ToolDTO> result = appService.listTools(page, size, type, enabled);

        return ResponseEntity.ok(ApiResult.success(result,
                String.format("查询完成，共找到 %d 个工具", result.totalElements())));
    }

    /**
     * 获取工具详情
     */
    @GetMapping("/tools/{toolCode}")
    @Operation(summary = "获取工具详情", description = "获取指定工具的完整信息，包括参数规范和使用统计。")
    public ResponseEntity<ApiResult<ToolDetailDTO>> getToolDetail(
            @PathVariable String toolCode) {

        ToolDetailDTO result = appService.getToolDetail(toolCode);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 更新工具信息
     */
    @PutMapping("/tools/{toolCode}")
    @Operation(summary = "更新工具信息", description = "更新工具的配置信息，如端点地址、参数规范等。")
    @PreAuthorize("hasRole('TOOL_ADMIN')")
    public ResponseEntity<ApiResult<ToolDTO>> updateTool(
            @PathVariable String toolCode,
            @Valid @RequestBody RegisterToolCommand cmd) {

        ToolDTO result = appService.updateTool(toolCode, cmd);
        return ResponseEntity.ok(ApiResult.success(result, "工具更新成功"));
    }

    // =================== 租户授权管理 ===================

    /**
     * 授权租户使用工具
     *
     * <p>租户授权是MCP系统的安全机制。只有被明确授权的租户才能使用特定工具，
     * 并且可以设置调用配额，防止滥用。</p>
     */
    @PostMapping("/tools/{toolCode}/authorize")
    @Operation(
            summary = "授权租户使用工具",
            description = "为指定租户授权使用特定工具，可设置API密钥和调用配额。"
    )
    @PreAuthorize("hasRole('TOOL_ADMIN')")
    public ResponseEntity<ApiResult<ToolAuthDTO>> authorizeTenantForTool(
            @PathVariable String toolCode,
            @Valid @RequestBody AuthorizeTenantForToolCommand cmd) {

        log.info("授权租户使用工具: toolCode={}, tenantId={}", toolCode, cmd.tenantId());

        ToolAuthDTO result = appService.authorizeTenantForTool(cmd);

        return ResponseEntity.ok(ApiResult.success(result, "租户授权成功"));
    }

    /**
     * 获取租户工具授权列表
     */
    @GetMapping("/tenants/{tenantId}/tools")
    @Operation(summary = "获取租户工具授权", description = "获取指定租户已授权的所有工具列表。")
    @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'READ')")
    public ResponseEntity<ApiResult<List<ToolAuthDTO>>> getTenantAuthorizedTools(
            @PathVariable Long tenantId) {

        List<ToolAuthDTO> result = appService.getTenantAuthorizedTools(tenantId);
        return ResponseEntity.ok(ApiResult.success(result,
                String.format("租户拥有 %d 个已授权工具", result.size())));
    }

    /**
     * 撤销租户工具授权
     */
    @DeleteMapping("/tools/{toolCode}/tenants/{tenantId}")
    @Operation(summary = "撤销租户授权", description = "撤销指定租户对特定工具的使用授权。")
    @PreAuthorize("hasRole('TOOL_ADMIN')")
    public ResponseEntity<ApiResult<Void>> revokeTenantAuthorization(
            @PathVariable String toolCode,
            @PathVariable Long tenantId) {

        log.info("撤销租户工具授权: toolCode={}, tenantId={}", toolCode, tenantId);

        appService.revokeTenantAuthorization(toolCode, tenantId);
        return ResponseEntity.ok(ApiResult.success(null, "授权已撤销"));
    }

    // =================== 工具执行接口 ===================

    /**
     * 执行工具
     *
     * <p>这是MCP系统的核心功能。AI模型通过这个接口调用各种工具，
     * 扩展自己的能力。系统会验证权限、参数，安全地执行工具并返回结果。</p>
     */
    @PostMapping("/tools/{toolCode}/execute")
    @Operation(
            summary = "执行工具",
            description = "执行指定的MCP工具，传入参数并返回执行结果。支持同步和异步执行。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "工具执行成功"),
            @ApiResponse(responseCode = "400", description = "参数无效"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "429", description = "调用配额已用完")
    })
    @PreAuthorize("hasPermission(#cmd.tenantId, 'TENANT', 'EXECUTE_TOOL')")
    public ResponseEntity<ApiResult<ToolExecutionResultDTO>> executeTool(
            @PathVariable String toolCode,
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "工具执行参数",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "天气查询示例",
                                    value = """
                                            {
                                              "tenantId": 1,
                                              "userId": 123,
                                              "threadId": 456,
                                              "params": {
                                                "city": "北京",
                                                "unit": "celsius"
                                              },
                                              "asyncMode": false,
                                              "timeoutSeconds": 30
                                            }
                                            """
                            )
                    )
            ) ExecuteToolCommand cmd) {

        log.info("执行工具: toolCode={}, tenantId={}, userId={}",
                toolCode, cmd.tenantId(), cmd.userId());

        ToolExecutionResultDTO result = appService.executeTool(toolCode, cmd);

        String message = cmd.asyncMode() ? "工具已开始异步执行" : "工具执行完成";
        return ResponseEntity.ok(ApiResult.success(result, message));
    }

    /**
     * 获取工具执行历史
     */
    @GetMapping("/tools/{toolCode}/executions")
    @Operation(summary = "获取执行历史", description = "获取指定工具的执行历史记录。")
    @PreAuthorize("hasRole('TOOL_ADMIN') or hasPermission(#tenantId, 'TENANT', 'READ')")
    public ResponseEntity<ApiResult<PageResultDTO<ToolCallLogDTO>>> getExecutionHistory(
            @PathVariable String toolCode,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        PageResultDTO<ToolCallLogDTO> result = appService.getExecutionHistory(
                toolCode, tenantId, page, size);

        return ResponseEntity.ok(ApiResult.success(result));
    }

    // =================== 统计和监控接口 ===================

    /**
     * 获取工具使用统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取工具统计", description = "获取工具使用的统计信息，包括调用次数、成功率等。")
    @PreAuthorize("hasRole('TOOL_ADMIN')")
    public ResponseEntity<ApiResult<ToolStatisticsDTO>> getToolStatistics(
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) Long tenantId) {

        ToolStatisticsDTO result = appService.getToolStatistics(timeRange, tenantId);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "MCP系统健康检查", description = "检查MCP工具系统各组件的健康状态。")
    public ResponseEntity<ApiResult<McpHealthStatus>> healthCheck() {

        McpHealthStatus status = appService.checkHealth();

        HttpStatus httpStatus = "healthy".equals(status.status()) ?
                HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(httpStatus)
                .body(ApiResult.success(status, "健康检查完成"));
    }
}