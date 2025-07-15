package com.clinflash.baseai.adapter.web.flow;

import com.clinflash.baseai.application.flow.command.*;
import com.clinflash.baseai.application.flow.dto.*;
import com.clinflash.baseai.application.flow.service.FlowOrchestrationAppService;
import com.clinflash.baseai.infrastructure.web.response.ApiResult;
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

/**
 * <h1>流程编排REST控制器完整版</h1>
 *
 * <p>这个控制器是整个流程编排系统的API入口，负责管理从简单的线性流程到复杂的分支、循环、
 * 并行处理的所有工作流场景。想象它是一个智能的项目经理，能够协调各种任务的执行顺序，
 * 确保每个步骤都在正确的时机以正确的方式执行。</p>
 *
 * <p><b>流程编排的核心价值：</b></p>
 * <p>在现代的AI应用中，单一的模型调用往往无法解决复杂的业务问题。我们需要将多个AI能力、
 * 数据处理步骤、外部服务调用等组合成一个完整的解决方案。流程编排就是实现这种组合的技术，
 * 它让我们能够构建真正智能的业务流程。</p>
 *
 * <p><b>设计哲学：</b></p>
 * <p>我们的流程编排遵循"可视化设计，代码化执行"的理念。用户通过拖拽的方式设计流程图，
 * 系统将其转换为可执行的工作流。这种设计降低了复杂业务逻辑的实现门槛，让业务专家
 * 也能参与到AI应用的构建中。</p>
 *
 * <p><b>完整功能模块：</b></p>
 * <ul>
 * <li><b>项目管理：</b>为相关流程提供组织和权限管理</li>
 * <li><b>流程设计：</b>支持可视化的流程图设计和编辑</li>
 * <li><b>版本控制：</b>完善的流程版本管理和历史追踪</li>
 * <li><b>流程执行：</b>高性能的异步流程执行引擎</li>
 * <li><b>监控调试：</b>实时的执行状态监控和详细的调试信息</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/flows")
@Validated
@Tag(name = "流程编排管理", description = "Flow Orchestration APIs - 提供完整的可视化流程设计和执行解决方案")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FlowOrchestrationController {

    private static final Logger log = LoggerFactory.getLogger(FlowOrchestrationController.class);

    private final FlowOrchestrationAppService appService;

    public FlowOrchestrationController(FlowOrchestrationAppService appService) {
        this.appService = appService;
    }

    // =================== 项目管理接口 ===================

    /**
     * 创建流程项目
     *
     * <p>流程项目是组织相关工作流的容器。在企业环境中，不同的部门或业务线
     * 可能有各自的流程项目，这样既保证了权限隔离，又便于管理和维护。</p>
     */
    @PostMapping(value = "/projects", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "创建流程项目",
            description = "创建新的流程项目来组织和管理相关的工作流定义。项目提供权限边界和逻辑分组。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "项目创建成功",
                    content = @Content(schema = @Schema(implementation = FlowProjectDTO.class))),
            @ApiResponse(responseCode = "400", description = "请求参数无效"),
            @ApiResponse(responseCode = "409", description = "项目名称已存在")
    })
    @PreAuthorize("hasPermission(#cmd.tenantId, 'TENANT', 'WRITE')")
    public ResponseEntity<ApiResult<FlowProjectDTO>> createProject(
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "项目创建信息",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "客户服务流程项目",
                                    value = """
                                            {
                                              "tenantId": 1,
                                              "name": "客户服务自动化流程",
                                              "description": "包含客户问询处理、工单创建、问题解答等相关流程",
                                              "operatorId": 123
                                            }
                                            """
                            )
                    )
            ) CreateFlowProjectCommand cmd) {

        log.info("创建流程项目: tenantId={}, name={}", cmd.tenantId(), cmd.name());

        FlowProjectDTO result = appService.createProject(cmd);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(result, "流程项目创建成功"));
    }

    /**
     * 获取项目列表
     *
     * <p>提供租户下所有流程项目的概览，支持分页和搜索。
     * 这让管理员能够快速了解组织内的所有流程项目情况。</p>
     */
    @GetMapping("/projects")
    @Operation(
            summary = "获取项目列表",
            description = "分页获取租户下的所有流程项目，支持按名称搜索过滤。"
    )
    @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'READ')")
    public ResponseEntity<ApiResult<PageResultDTO<FlowProjectDTO>>> listProjects(
            @Parameter(description = "租户ID", required = true)
            @RequestParam Long tenantId,

            @Parameter(description = "页码（从0开始）", example = "0")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "页码不能小于0")
            Integer page,

            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "每页大小不能小于1")
            @Max(value = 100, message = "每页大小不能超过100")
            Integer size,

            @Parameter(description = "项目名称搜索", example = "客户服务")
            @RequestParam(required = false) String search) {

        log.debug("查询项目列表: tenantId={}, page={}, size={}, search={}",
                tenantId, page, size, search);

        PageResultDTO<FlowProjectDTO> result = appService.listProjects(tenantId, page, size, search);

        return ResponseEntity.ok(ApiResult.success(result,
                String.format("查询完成，共找到 %d 个项目", result.totalElements())));
    }

    /**
     * 获取项目详情
     */
    @GetMapping("/projects/{projectId}")
    @Operation(summary = "获取项目详情", description = "获取指定流程项目的完整信息，包括统计数据。")
    @PreAuthorize("hasPermission(#projectId, 'PROJECT', 'READ')")
    public ResponseEntity<ApiResult<ProjectDetailDTO>> getProjectDetail(
            @PathVariable Long projectId) {

        log.debug("获取项目详情: projectId={}", projectId);

        ProjectDetailDTO result = appService.getProjectDetail(projectId);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    // =================== 流程定义管理接口 ===================

    /**
     * 创建流程定义
     *
     * <p>这是流程设计的起点。用户可以从空白流程开始，也可以基于模板创建。
     * 新创建的流程处于草稿状态，可以自由编辑和调试。</p>
     */
    @PostMapping(value = "/definitions", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "创建流程定义",
            description = "在指定项目下创建新的流程定义。新创建的流程为草稿状态，可以进行编辑和测试。"
    )
    @PreAuthorize("hasPermission(#cmd.projectId, 'PROJECT', 'WRITE')")
    public ResponseEntity<ApiResult<FlowDefinitionDTO>> createFlowDefinition(
            @Valid @RequestBody CreateFlowDefinitionCommand cmd) {

        log.info("创建流程定义: projectId={}, name={}", cmd.projectId(), cmd.name());

        FlowDefinitionDTO result = appService.createFlowDefinition(cmd);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResult.success(result, "流程定义创建成功"));
    }

    /**
     * 更新流程结构
     *
     * <p>这是流程设计的核心功能。用户通过可视化编辑器修改流程图后，
     * 系统会将新的结构保存。只有草稿状态的流程才能修改。</p>
     */
    @PutMapping("/definitions/{definitionId}/structure")
    @Operation(
            summary = "更新流程结构",
            description = "更新流程的节点和连接关系。只有草稿状态的流程才能修改结构。"
    )
    @PreAuthorize("hasPermission(#definitionId, 'FLOW_DEFINITION', 'WRITE')")
    public ResponseEntity<ApiResult<FlowStructureDTO>> updateFlowStructure(
            @PathVariable Long definitionId,
            @Valid @RequestBody UpdateFlowStructureCommand cmd) {

        log.info("更新流程结构: definitionId={}, nodes={}, edges={}",
                definitionId, cmd.nodes().size(), cmd.edges().size());

        FlowStructureDTO result = appService.updateFlowStructure(cmd);

        return ResponseEntity.ok(ApiResult.success(result, "流程结构更新成功"));
    }

    /**
     * 发布流程定义
     *
     * <p>发布操作将流程从草稿状态变为已发布状态。发布后的流程不能再修改，
     * 但可以被执行。这确保了生产环境的稳定性。</p>
     */
    @PostMapping("/definitions/{definitionId}/publish")
    @Operation(
            summary = "发布流程定义",
            description = "将草稿状态的流程发布为可执行版本。发布后的流程不能修改，但可以执行和复制。"
    )
    @PreAuthorize("hasPermission(#definitionId, 'FLOW_DEFINITION', 'PUBLISH')")
    public ResponseEntity<ApiResult<FlowDefinitionDTO>> publishFlow(
            @PathVariable Long definitionId,
            @Valid @RequestBody PublishFlowCommand cmd) {

        log.info("发布流程定义: definitionId={}", definitionId);

        FlowDefinitionDTO result = appService.publishFlow(cmd);

        return ResponseEntity.ok(ApiResult.success(result, "流程发布成功"));
    }

    /**
     * 获取流程定义列表
     */
    @GetMapping("/definitions")
    @Operation(summary = "获取流程定义列表", description = "分页获取项目下的所有流程定义。")
    @PreAuthorize("hasPermission(#projectId, 'PROJECT', 'READ')")
    public ResponseEntity<ApiResult<PageResultDTO<FlowDefinitionDTO>>> listFlowDefinitions(
            @RequestParam Long projectId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status) {

        PageResultDTO<FlowDefinitionDTO> result = appService.listFlowDefinitions(
                projectId, page, size, status);

        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 获取流程结构
     *
     * <p>返回流程的完整结构定义，包括所有节点、连接和配置信息。
     * 这个信息用于可视化编辑器的渲染和流程执行引擎的解析。</p>
     */
    @GetMapping("/definitions/{definitionId}/structure")
    @Operation(summary = "获取流程结构", description = "获取流程的完整结构定义，包括节点、连接和配置。")
    @PreAuthorize("hasPermission(#definitionId, 'FLOW_DEFINITION', 'READ')")
    public ResponseEntity<ApiResult<FlowStructureDTO>> getFlowStructure(
            @PathVariable Long definitionId) {

        FlowStructureDTO result = appService.getFlowStructure(definitionId);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    // =================== 流程执行接口 ===================

    /**
     * 执行流程
     *
     * <p>这是流程编排系统的核心功能。当用户触发流程执行时，系统会创建一个
     * 运行实例，按照定义的逻辑顺序执行各个节点，并实时跟踪执行状态。</p>
     */
    @PostMapping("/definitions/{definitionId}/execute")
    @Operation(
            summary = "执行流程",
            description = "创建并启动流程运行实例。支持同步和异步执行模式，可传入初始参数。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "流程执行成功启动"),
            @ApiResponse(responseCode = "400", description = "执行参数无效"),
            @ApiResponse(responseCode = "409", description = "流程状态不允许执行")
    })
    @PreAuthorize("hasPermission(#definitionId, 'FLOW_DEFINITION', 'EXECUTE')")
    public ResponseEntity<ApiResult<FlowRunDTO>> executeFlow(
            @PathVariable Long definitionId,
            @Valid @RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "流程执行参数",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "客户问询处理流程",
                                    value = """
                                            {
                                              "definitionId": 123,
                                              "inputData": {
                                                "customerQuery": "我想了解产品的技术规格",
                                                "customerId": "C001",
                                                "priority": "normal"
                                              },
                                              "asyncMode": true,
                                              "timeoutMinutes": 30,
                                              "operatorId": 456
                                            }
                                            """
                            )
                    )
            ) ExecuteFlowCommand cmd) {

        log.info("执行流程: definitionId={}, asyncMode={}",
                definitionId, cmd.asyncMode());

        FlowRunDTO result = appService.executeFlow(cmd);

        String message = cmd.asyncMode() ?
                "流程已开始异步执行" : "流程执行完成";

        return ResponseEntity.ok(ApiResult.success(result, message));
    }

    /**
     * 获取运行状态
     *
     * <p>实时查询流程运行实例的当前状态。对于长时间运行的流程，
     * 用户可以通过这个接口跟踪执行进度。</p>
     */
    @GetMapping("/runs/{runId}")
    @Operation(summary = "获取运行状态", description = "获取流程运行实例的详细状态信息，包括当前进度和执行结果。")
    @PreAuthorize("hasPermission(#runId, 'FLOW_RUN', 'READ')")
    public ResponseEntity<ApiResult<FlowRunDetailDTO>> getRunStatus(
            @PathVariable Long runId) {

        log.debug("查询运行状态: runId={}", runId);

        FlowRunDetailDTO result = appService.getRunDetail(runId);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 获取运行历史
     */
    @GetMapping("/definitions/{snapshotId}/runs")
    @Operation(summary = "获取运行历史", description = "分页获取流程定义的所有运行历史记录。")
    @PreAuthorize("hasPermission(#snapshotId, 'FLOW_DEFINITION', 'READ')")
    public ResponseEntity<ApiResult<PageResultDTO<FlowRunDTO>>> getRunHistory(
            @PathVariable Long snapshotId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status) {

        PageResultDTO<FlowRunDTO> result = appService.getRunHistory(
                snapshotId, page, size, status);

        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 停止流程执行
     *
     * <p>对于正在运行的流程，用户可以手动停止执行。系统会尝试优雅地
     * 终止当前执行，并保存已完成的步骤结果。</p>
     */
    @PostMapping("/runs/{runId}/stop")
    @Operation(summary = "停止流程执行", description = "停止正在运行的流程实例，保存当前执行状态。")
    @PreAuthorize("hasPermission(#runId, 'FLOW_RUN', 'CONTROL')")
    public ResponseEntity<ApiResult<Void>> stopExecution(
            @PathVariable Long runId,
            @RequestParam Long operatorId) {

        log.info("停止流程执行: runId={}, operatorId={}", runId, operatorId);

        appService.stopExecution(runId, operatorId);

        return ResponseEntity.ok(ApiResult.success(null, "流程执行已停止"));
    }

    // =================== 版本管理接口 ===================

    /**
     * 创建新版本
     *
     * <p>基于现有流程创建新版本。这对于流程的迭代和改进很重要，
     * 新版本可以修复问题或添加新功能，而不影响正在使用的旧版本。</p>
     */
    @PostMapping("/definitions/{definitionId}/versions")
    @Operation(summary = "创建新版本", description = "基于现有流程定义创建新版本，继承所有配置但重置为草稿状态。")
    @PreAuthorize("hasPermission(#definitionId, 'FLOW_DEFINITION', 'VERSION')")
    public ResponseEntity<ApiResult<FlowDefinitionDTO>> createNewVersion(
            @PathVariable Long definitionId,
            @RequestParam Long operatorId) {

        log.info("创建新版本: definitionId={}", definitionId);

        FlowDefinitionDTO result = appService.createNewVersion(definitionId, operatorId);

        return ResponseEntity.ok(ApiResult.success(result, "新版本创建成功"));
    }

    /**
     * 获取版本历史
     */
    @GetMapping("/definitions/{definitionId}/versions")
    @Operation(summary = "获取版本历史", description = "获取流程定义的所有版本历史记录。")
    @PreAuthorize("hasPermission(#definitionId, 'FLOW_DEFINITION', 'READ')")
    public ResponseEntity<ApiResult<List<FlowDefinitionDTO>>> getVersionHistory(
            @PathVariable Long definitionId) {

        List<FlowDefinitionDTO> result = appService.getVersionHistory(definitionId);

        return ResponseEntity.ok(ApiResult.success(result,
                String.format("找到 %d 个版本", result.size())));
    }

    // =================== 流程快照管理 ===================

    /**
     * 获取流程快照
     *
     * <p>流程快照是执行时使用的不可变版本。通过快照，我们能够确保
     * 即使流程定义发生变化，正在运行的实例也能保持一致性。</p>
     */
    @GetMapping("/snapshots/{snapshotId}")
    @Operation(summary = "获取流程快照", description = "获取流程快照的详细信息，包括完整的执行配置。")
    @PreAuthorize("hasPermission(#snapshotId, 'FLOW_SNAPSHOT', 'READ')")
    public ResponseEntity<ApiResult<FlowSnapshotDTO>> getSnapshot(
            @PathVariable Long snapshotId) {

        FlowSnapshotDTO result = appService.getSnapshot(snapshotId);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    // =================== 统计和监控接口 ===================

    /**
     * 获取流程统计
     *
     * <p>提供项目或租户级别的流程统计信息，包括执行次数、成功率、
     * 平均执行时间等关键指标。这些数据对于性能优化和业务分析很有价值。</p>
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取流程统计", description = "获取租户或项目级别的流程执行统计信息。")
    @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'READ')")
    public ResponseEntity<ApiResult<FlowStatisticsDTO>> getStatistics(
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String timeRange) {

        FlowStatisticsDTO result = appService.getStatistics(
                tenantId, projectId, timeRange);

        return ResponseEntity.ok(ApiResult.success(result));
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "流程引擎健康检查", description = "检查流程执行引擎和相关组件的健康状态。")
    public ResponseEntity<ApiResult<FlowHealthStatus>> healthCheck() {

        FlowHealthStatus status = appService.checkHealth();

        HttpStatus httpStatus = "healthy".equals(status.status()) ?
                HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(httpStatus)
                .body(ApiResult.success(status, "健康检查完成"));
    }
}