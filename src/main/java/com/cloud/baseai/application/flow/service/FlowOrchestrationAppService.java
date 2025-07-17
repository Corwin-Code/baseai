package com.cloud.baseai.application.flow.service;

import com.cloud.baseai.application.flow.command.*;
import com.cloud.baseai.application.flow.dto.*;
import com.cloud.baseai.application.metrics.service.MetricsService;
import com.cloud.baseai.application.user.service.UserInfoService;
import com.cloud.baseai.domain.flow.model.*;
import com.cloud.baseai.domain.flow.repository.*;
import com.cloud.baseai.domain.flow.service.FlowBuildService;
import com.cloud.baseai.domain.flow.service.FlowExecutionService;
import com.cloud.baseai.infrastructure.config.FlowProperties;
import com.cloud.baseai.infrastructure.exception.FlowException;
import com.cloud.baseai.infrastructure.exception.FlowValidationException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * <h2>流程编排应用服务增强版</h2>
 *
 * <p>这是流程编排系统的核心应用服务，负责协调复杂的业务流程和领域逻辑。
 * 它就像一个经验丰富的项目经理，不仅要理解每个任务的细节，还要掌控全局，
 * 确保整个项目按计划顺利进行。</p>
 *
 * <p><b>设计理念：</b></p>
 * <p>应用服务层是业务用例的实现者，它将复杂的业务流程分解为一系列协调的领域操作。
 * 这一层不包含业务规则（那是领域层的职责），而是专注于编排这些规则的执行顺序，
 * 管理事务边界，处理异常情况。</p>
 *
 * <p><b>流程生命周期管理：</b></p>
 * <p>从流程的设计、测试、发布到执行、监控的整个生命周期，这个服务都提供了
 * 完整的支持。它确保每个环节都有适当的验证、权限检查和审计记录。</p>
 */
@Service
public class FlowOrchestrationAppService {

    private static final Logger log = LoggerFactory.getLogger(FlowOrchestrationAppService.class);

    // 领域仓储
    private final FlowProjectRepository projectRepo;
    private final FlowDefinitionRepository definitionRepo;
    private final FlowNodeRepository nodeRepo;
    private final FlowEdgeRepository edgeRepo;
    private final FlowSnapshotRepository snapshotRepo;
    private final FlowRunRepository runRepo;
    private final FlowRunLogRepository runLogRepo;

    // 领域服务
    private final FlowBuildService buildService;
    private final FlowExecutionService executionService;

    // 配置
    private final FlowProperties config;

    // 异步执行器
    private final ExecutorService asyncExecutor;

    // 可选服务
    @Autowired(required = false)
    private UserInfoService userInfoService;

    @Autowired(required = false)
    private MetricsService metricsService;

    public FlowOrchestrationAppService(
            FlowProjectRepository projectRepo,
            FlowDefinitionRepository definitionRepo,
            FlowNodeRepository nodeRepo,
            FlowEdgeRepository edgeRepo,
            FlowSnapshotRepository snapshotRepo,
            FlowRunRepository runRepo,
            FlowRunLogRepository runLogRepo,
            FlowBuildService buildService,
            FlowExecutionService executionService,
            FlowProperties config) {

        this.projectRepo = projectRepo;
        this.definitionRepo = definitionRepo;
        this.nodeRepo = nodeRepo;
        this.edgeRepo = edgeRepo;
        this.snapshotRepo = snapshotRepo;
        this.runRepo = runRepo;
        this.runLogRepo = runLogRepo;
        this.buildService = buildService;
        this.executionService = executionService;
        this.config = config;

        // 创建异步执行器
        this.asyncExecutor = Executors.newFixedThreadPool(
                config.getExecution() != null ?
                        config.getExecution().getAsyncPoolSize() : 10
        );
    }

    // =================== 项目管理接口实现 ===================

    /**
     * 创建流程项目
     *
     * <p>项目是流程组织的最高层级，为一组相关的业务流程提供逻辑边界。
     * 创建项目时，我们需要确保项目名称在租户内的唯一性，这避免了管理上的混乱。</p>
     */
    @Transactional
    public FlowProjectDTO createProject(CreateFlowProjectCommand cmd) {
        long startTime = System.currentTimeMillis();
        log.info("创建流程项目: tenantId={}, name={}", cmd.tenantId(), cmd.name());

        try {
            // 验证项目名称唯一性
            if (projectRepo.existsByTenantIdAndName(cmd.tenantId(), cmd.name())) {
                throw new FlowException(
                        "DUPLICATE_PROJECT_NAME",
                        "租户下已存在同名项目：" + cmd.name()
                );
            }

            // 创建项目
            FlowProject project = FlowProject.create(
                    cmd.tenantId(),
                    cmd.name(),
                    cmd.operatorId()
            );

            project = projectRepo.save(project);

            recordMetrics("project.create", startTime, true);
            return toProjectDTO(project);

        } catch (Exception e) {
            recordMetrics("project.create", startTime, false);
            if (e instanceof FlowException) {
                throw e;
            }
            throw new FlowException("PROJECT_CREATE_ERROR", "项目创建失败", e);
        }
    }

    /**
     * 获取项目列表
     */
    public PageResultDTO<FlowProjectDTO> listProjects(Long tenantId, int page, int size, String search) {
        log.debug("查询项目列表: tenantId={}, page={}, size={}, search={}",
                tenantId, page, size, search);

        try {
            List<FlowProject> projects;
            long total;

            if (search != null && !search.trim().isEmpty()) {
                projects = projectRepo.searchByName(tenantId, search.trim(), page, size);
                total = projectRepo.countByTenantIdAndNameContaining(tenantId, search.trim());
            } else {
                projects = projectRepo.findByTenantId(tenantId, page, size);
                total = projectRepo.countByTenantId(tenantId);
            }

            List<FlowProjectDTO> projectDTOs = projects.stream()
                    .map(this::toProjectDTO)
                    .collect(Collectors.toList());

            return PageResultDTO.of(projectDTOs, total, page, size);

        } catch (Exception e) {
            throw new FlowException("PROJECT_LIST_ERROR", "项目列表查询失败", e);
        }
    }

    /**
     * 获取项目详情
     */
    public ProjectDetailDTO getProjectDetail(Long projectId) {
        log.debug("获取项目详情: projectId={}", projectId);

        try {
            FlowProject project = projectRepo.findById(projectId)
                    .orElseThrow(() -> new FlowException(
                            "PROJECT_NOT_FOUND",
                            "项目不存在，ID: " + projectId
                    ));

            // 获取项目统计信息
            int totalFlows = definitionRepo.countByProjectId(projectId);
            int publishedFlows = definitionRepo.countByProjectIdAndStatus(projectId, FlowStatus.PUBLISHED);
            int totalRuns = runRepo.countByProjectId(projectId);
            int runningFlows = runRepo.countByProjectIdAndStatus(projectId, RunStatus.RUNNING);

            String creatorName = null;
            if (userInfoService != null) {
                creatorName = userInfoService.getUserName(project.createdBy());
            }

            return new ProjectDetailDTO(
                    project.id(),
                    project.name(),
                    totalFlows,
                    publishedFlows,
                    totalRuns,
                    runningFlows,
                    project.createdBy(),
                    creatorName,
                    project.createdAt(),
                    project.updatedAt()
            );

        } catch (Exception e) {
            if (e instanceof FlowException) {
                throw e;
            }
            throw new FlowException("PROJECT_DETAIL_ERROR", "获取项目详情失败", e);
        }
    }

    // =================== 流程定义管理实现 ===================

    /**
     * 创建流程定义
     *
     * <p>这是流程设计的起点。新创建的流程定义处于草稿状态，用户可以自由地
     * 添加节点、设置连接、配置参数。草稿状态的流程不能执行，但可以进行充分的测试。</p>
     */
    @Transactional
    public FlowDefinitionDTO createFlowDefinition(CreateFlowDefinitionCommand cmd) {
        long startTime = System.currentTimeMillis();
        log.info("创建流程定义: projectId={}, name={}", cmd.projectId(), cmd.name());

        try {
            // 验证项目存在
            FlowProject project = projectRepo.findById(cmd.projectId())
                    .orElseThrow(() -> new FlowException(
                            "PROJECT_NOT_FOUND",
                            "项目不存在，ID: " + cmd.projectId()
                    ));

            // 验证流程名称在项目内的唯一性
            if (definitionRepo.existsByProjectIdAndName(cmd.projectId(), cmd.name())) {
                throw new FlowException(
                        "DUPLICATE_FLOW_NAME",
                        "项目内已存在同名流程：" + cmd.name()
                );
            }

            // 创建流程定义
            FlowDefinition definition = FlowDefinition.create(
                    cmd.projectId(),
                    cmd.name(),
                    cmd.description(),
                    cmd.diagramJson(),
                    cmd.operatorId()
            );

            definition = definitionRepo.save(definition);

            recordMetrics("flow.create", startTime, true);
            return toDefinitionDTO(definition);

        } catch (Exception e) {
            recordMetrics("flow.create", startTime, false);
            if (e instanceof FlowException) {
                throw e;
            }
            throw new FlowException("FLOW_CREATE_ERROR", "流程定义创建失败", e);
        }
    }

    /**
     * 更新流程结构
     *
     * <p>这是流程设计的核心功能。用户通过可视化编辑器设计流程图后，
     * 系统需要将图形表示转换为可执行的节点和边的集合。这个过程包括
     * 结构验证、循环检测、节点配置校验等多个步骤。</p>
     */
    @Transactional
    public FlowStructureDTO updateFlowStructure(UpdateFlowStructureCommand cmd) {
        long startTime = System.currentTimeMillis();
        log.info("更新流程结构: definitionId={}, nodes={}, edges={}",
                cmd.definitionId(), cmd.nodes().size(), cmd.edges().size());

        try {
            // 验证流程定义存在且为草稿状态
            FlowDefinition definition = definitionRepo.findById(cmd.definitionId())
                    .orElseThrow(() -> new FlowException(
                            "FLOW_NOT_FOUND",
                            "流程定义不存在，ID: " + cmd.definitionId()
                    ));

            if (definition.status() != FlowStatus.DRAFT) {
                throw new FlowException(
                        "INVALID_FLOW_STATUS",
                        "只有草稿状态的流程才能修改结构"
                );
            }

            // 验证流程结构
            buildService.validateFlowStructure(cmd.nodes(), cmd.edges());

            // 删除现有的节点和边
            nodeRepo.deleteByDefinitionId(cmd.definitionId());
            edgeRepo.deleteByDefinitionId(cmd.definitionId());

            // 创建新的节点
            List<FlowNode> nodes = cmd.nodes().stream()
                    .map(nodeCmd -> FlowNode.create(
                            cmd.definitionId(),
                            nodeCmd.nodeTypeCode(),
                            nodeCmd.nodeKey(),
                            nodeCmd.name(),
                            nodeCmd.configJson(),
                            nodeCmd.retryPolicyJson(),
                            cmd.operatorId()
                    ))
                    .collect(Collectors.toList());

            nodes = nodeRepo.saveAll(nodes);

            // 创建新的边
            List<FlowEdge> edges = cmd.edges().stream()
                    .map(edgeCmd -> FlowEdge.create(
                            cmd.definitionId(),
                            edgeCmd.sourceKey(),
                            edgeCmd.targetKey(),
                            edgeCmd.configJson(),
                            cmd.operatorId()
                    ))
                    .collect(Collectors.toList());

            edges = edgeRepo.saveAll(edges);

            // 更新流程定义的图表JSON
            definition = definition.update(
                    definition.name(),
                    definition.description(),
                    cmd.diagramJson(),
                    cmd.operatorId()
            );
            definitionRepo.save(definition);

            recordMetrics("flow.structure.update", startTime, true);

            return new FlowStructureDTO(
                    cmd.definitionId(),
                    nodes.stream().map(this::toNodeDTO).collect(Collectors.toList()),
                    edges.stream().map(this::toEdgeDTO).collect(Collectors.toList()),
                    cmd.diagramJson()
            );

        } catch (Exception e) {
            recordMetrics("flow.structure.update", startTime, false);
            if (e instanceof FlowException || e instanceof FlowValidationException) {
                throw e;
            }
            throw new FlowException("FLOW_STRUCTURE_UPDATE_ERROR", "流程结构更新失败", e);
        }
    }

    /**
     * 发布流程定义
     *
     * <p>发布是流程从设计阶段转向生产阶段的重要步骤。发布前，系统会进行
     * 全面的验证，确保流程的完整性和可执行性。发布后的流程会创建快照，
     * 以确保执行时的版本一致性。</p>
     */
    @Transactional
    public FlowDefinitionDTO publishFlow(PublishFlowCommand cmd) {
        long startTime = System.currentTimeMillis();
        log.info("发布流程定义: definitionId={}", cmd.definitionId());

        try {
            // 验证流程定义存在且为草稿状态
            FlowDefinition definition = definitionRepo.findById(cmd.definitionId())
                    .orElseThrow(() -> new FlowException(
                            "FLOW_NOT_FOUND",
                            "流程定义不存在，ID: " + cmd.definitionId()
                    ));

            if (definition.status() != FlowStatus.DRAFT) {
                throw new FlowException(
                        "INVALID_FLOW_STATUS",
                        "只有草稿状态的流程才能发布"
                );
            }

            // 获取流程结构并验证
            List<FlowNode> nodes = nodeRepo.findByDefinitionId(cmd.definitionId());
            List<FlowEdge> edges = edgeRepo.findByDefinitionId(cmd.definitionId());

            if (nodes.isEmpty()) {
                throw new FlowValidationException("流程至少需要包含一个节点");
            }

            // 使用构建服务验证流程完整性
            buildService.validateForPublication(definition, nodes, edges);

            // 发布流程定义
            definition = definition.publish(cmd.operatorId());
            definition = definitionRepo.save(definition);

            // 创建流程快照
            String snapshotJson = buildService.createSnapshotJson(definition, nodes, edges);
            FlowSnapshot snapshot = FlowSnapshot.create(
                    definition.id(),
                    null,
                    definition.version(),
                    snapshotJson,
                    cmd.operatorId()
            );
            snapshotRepo.save(snapshot);

            recordMetrics("flow.publish", startTime, true);
            return toDefinitionDTO(definition);

        } catch (Exception e) {
            recordMetrics("flow.publish", startTime, false);
            if (e instanceof FlowException || e instanceof FlowValidationException) {
                throw e;
            }
            throw new FlowException("FLOW_PUBLISH_ERROR", "流程发布失败", e);
        }
    }

    /**
     * 获取流程定义列表
     */
    public PageResultDTO<FlowDefinitionDTO> listFlowDefinitions(Long projectId, int page, int size, String status) {
        log.debug("查询流程定义列表: projectId={}, page={}, size={}, status={}",
                projectId, page, size, status);

        try {
            List<FlowDefinition> definitions;
            long total;

            if (status != null && !status.trim().isEmpty()) {
                FlowStatus flowStatus = FlowStatus.valueOf(status.toUpperCase());
                definitions = definitionRepo.findByProjectIdAndStatus(projectId, flowStatus, page, size);
                total = definitionRepo.countByProjectIdAndStatus(projectId, flowStatus);
            } else {
                definitions = definitionRepo.findByProjectId(projectId, page, size);
                total = definitionRepo.countByProjectId(projectId);
            }

            List<FlowDefinitionDTO> definitionDTOs = definitions.stream()
                    .map(this::toDefinitionDTO)
                    .collect(Collectors.toList());

            return PageResultDTO.of(definitionDTOs, total, page, size);

        } catch (Exception e) {
            throw new FlowException("FLOW_LIST_ERROR", "流程定义列表查询失败", e);
        }
    }

    /**
     * 获取流程结构
     */
    public FlowStructureDTO getFlowStructure(Long definitionId) {
        log.debug("获取流程结构: definitionId={}", definitionId);

        try {
            FlowDefinition definition = definitionRepo.findById(definitionId)
                    .orElseThrow(() -> new FlowException(
                            "FLOW_NOT_FOUND",
                            "流程定义不存在，ID: " + definitionId
                    ));

            List<FlowNode> nodes = nodeRepo.findByDefinitionId(definitionId);
            List<FlowEdge> edges = edgeRepo.findByDefinitionId(definitionId);

            return new FlowStructureDTO(
                    definitionId,
                    nodes.stream().map(this::toNodeDTO).collect(Collectors.toList()),
                    edges.stream().map(this::toEdgeDTO).collect(Collectors.toList()),
                    definition.diagramJson()
            );

        } catch (Exception e) {
            if (e instanceof FlowException) {
                throw e;
            }
            throw new FlowException("FLOW_STRUCTURE_GET_ERROR", "获取流程结构失败", e);
        }
    }

    // =================== 流程执行实现 ===================

    /**
     * 执行流程
     *
     * <p>这是整个流程编排系统的核心功能。当用户触发流程执行时，系统会：
     * 1. 创建运行实例
     * 2. 根据执行模式选择同步或异步执行
     * 3. 实时跟踪执行状态
     * 4. 处理异常和超时情况</p>
     */
    @Transactional
    public FlowRunDTO executeFlow(ExecuteFlowCommand cmd) {
        long startTime = System.currentTimeMillis();
        log.info("执行流程: definitionId={}, asyncMode={}",
                cmd.definitionId(), cmd.asyncMode());

        try {
            // 验证流程定义存在且已发布
            FlowDefinition definition = definitionRepo.findById(cmd.definitionId())
                    .orElseThrow(() -> new FlowException(
                            "FLOW_NOT_FOUND",
                            "流程定义不存在，ID: " + cmd.definitionId()
                    ));

            if (!definition.canExecute()) {
                throw new FlowException(
                        "INVALID_FLOW_STATUS",
                        "只有已发布的流程才能执行"
                );
            }

            // 获取最新的流程快照
            FlowSnapshot snapshot = snapshotRepo.findLatestByDefinitionId(cmd.definitionId())
                    .orElseThrow(() -> new FlowException(
                            "SNAPSHOT_NOT_FOUND",
                            "流程快照不存在，请重新发布流程"
                    ));

            // 创建运行实例
            FlowRun run = FlowRun.create(
                    snapshot.id(),
                    cmd.operatorId(),
                    cmd.operatorId()
            );
            run = runRepo.save(run);

            // 根据执行模式选择同步或异步执行
            if (cmd.asyncMode()) {
                // 异步执行
                scheduleAsyncExecution(run, snapshot, cmd.inputData(), cmd.timeoutMinutes());
                recordMetrics("flow.execute.async", startTime, true);
            } else {
                // 同步执行
                run = executeSynchronously(run, snapshot, cmd.inputData(), cmd.timeoutMinutes());
                recordMetrics("flow.execute.sync", startTime, true);
            }

            return toRunDTO(run);

        } catch (Exception e) {
            recordMetrics("flow.execute", startTime, false);
            if (e instanceof FlowException) {
                throw e;
            }
            throw new FlowException("FLOW_EXECUTE_ERROR", "流程执行失败", e);
        }
    }

    /**
     * 获取运行详情
     */
    public FlowRunDetailDTO getRunDetail(Long runId) {
        log.debug("获取运行详情: runId={}", runId);

        try {
            FlowRun run = runRepo.findById(runId)
                    .orElseThrow(() -> new FlowException(
                            "RUN_NOT_FOUND",
                            "运行实例不存在，ID: " + runId
                    ));

            // 获取运行日志
            List<FlowRunLog> logs = runLogRepo.findByRunId(runId);

            // 获取快照信息
            FlowSnapshot snapshot = snapshotRepo.findById(run.snapshotId())
                    .orElse(null);

            String definitionName = null;
            if (snapshot != null) {
                FlowDefinition definition = definitionRepo.findById(snapshot.definitionId())
                        .orElse(null);
                if (definition != null) {
                    definitionName = definition.name();
                }
            }

            return new FlowRunDetailDTO(
                    run.id(),
                    run.snapshotId(),
                    definitionName,
                    run.status().getLabel(),
                    run.resultJson(),
                    logs.stream().map(this::toRunLogDTO).collect(Collectors.toList()),
                    run.startedAt(),
                    run.finishedAt(),
                    run.getDurationMs()
            );

        } catch (Exception e) {
            if (e instanceof FlowException) {
                throw e;
            }
            throw new FlowException("RUN_DETAIL_ERROR", "获取运行详情失败", e);
        }
    }

    /**
     * 获取运行历史
     */
    public PageResultDTO<FlowRunDTO> getRunHistory(Long snapshotId, int page, int size, String status) {
        log.debug("获取运行历史: definitionId={}, page={}, size={}, status={}",
                snapshotId, page, size, status);

        try {
            List<FlowRun> runs;
            long total;

            if (status != null && !status.trim().isEmpty()) {
                RunStatus runStatus = RunStatus.valueOf(status.toUpperCase());
                runs = runRepo.findBySnapshotIdAndStatus(snapshotId, runStatus, page, size);
                total = runRepo.countBySnapshotIdAndStatus(snapshotId, runStatus);
            } else {
                runs = runRepo.findBySnapshotId(snapshotId, page, size);
                total = runRepo.countBySnapshotId(snapshotId);
            }

            List<FlowRunDTO> runDTOs = runs.stream()
                    .map(this::toRunDTO)
                    .collect(Collectors.toList());

            return PageResultDTO.of(runDTOs, total, page, size);

        } catch (Exception e) {
            throw new FlowException("RUN_HISTORY_ERROR", "获取运行历史失败", e);
        }
    }

    /**
     * 停止流程执行
     */
    @Transactional
    public void stopExecution(Long runId, Long operatorId) {
        log.info("停止流程执行: runId={}", runId);

        try {
            FlowRun run = runRepo.findById(runId)
                    .orElseThrow(() -> new FlowException(
                            "RUN_NOT_FOUND",
                            "运行实例不存在，ID: " + runId
                    ));

            if (!run.isRunning()) {
                throw new FlowException(
                        "INVALID_RUN_STATUS",
                        "只有运行中的流程才能停止"
                );
            }

            // 通知执行服务停止执行
            executionService.stopExecution(runId);

            // 更新运行状态
            run = run.fail("{\"error\":\"用户手动停止\",\"stoppedBy\":" + operatorId + "}");
            runRepo.save(run);

        } catch (Exception e) {
            if (e instanceof FlowException) {
                throw e;
            }
            throw new FlowException("STOP_EXECUTION_ERROR", "停止执行失败", e);
        }
    }

    // =================== 版本管理实现 ===================

    /**
     * 创建新版本
     */
    @Transactional
    public FlowDefinitionDTO createNewVersion(Long definitionId, Long operatorId) {
        log.info("创建新版本: definitionId={}", definitionId);

        try {
            FlowDefinition currentDefinition = definitionRepo.findById(definitionId)
                    .orElseThrow(() -> new FlowException(
                            "FLOW_NOT_FOUND",
                            "流程定义不存在，ID: " + definitionId
                    ));

            // 将当前版本标记为非最新
            if (currentDefinition.isLatest()) {
                currentDefinition = currentDefinition.markAsNotLatest();
                definitionRepo.save(currentDefinition);
            }

            // 创建新版本
            FlowDefinition newVersion = currentDefinition.createNewVersion(operatorId);
            newVersion = definitionRepo.save(newVersion);

            // 复制节点和边
            List<FlowNode> currentNodes = nodeRepo.findByDefinitionId(definitionId);
            List<FlowEdge> currentEdges = edgeRepo.findByDefinitionId(definitionId);

            FlowDefinition finalNewVersion = newVersion;
            List<FlowNode> newNodes = currentNodes.stream()
                    .map(node -> FlowNode.create(
                            finalNewVersion.id(),
                            node.nodeTypeCode(),
                            node.nodeKey(),
                            node.name(),
                            node.configJson(),
                            node.retryPolicyJson(),
                            operatorId
                    ))
                    .collect(Collectors.toList());

            nodeRepo.saveAll(newNodes);

            FlowDefinition finalNewVersion1 = newVersion;
            List<FlowEdge> newEdges = currentEdges.stream()
                    .map(edge -> FlowEdge.create(
                            finalNewVersion1.id(),
                            edge.sourceKey(),
                            edge.targetKey(),
                            edge.configJson(),
                            operatorId
                    ))
                    .collect(Collectors.toList());

            edgeRepo.saveAll(newEdges);

            return toDefinitionDTO(newVersion);

        } catch (Exception e) {
            if (e instanceof FlowException) {
                throw e;
            }
            throw new FlowException("CREATE_VERSION_ERROR", "创建新版本失败", e);
        }
    }

    /**
     * 获取版本历史
     */
    public List<FlowDefinitionDTO> getVersionHistory(Long definitionId) {
        log.debug("获取版本历史: definitionId={}", definitionId);

        try {
            FlowDefinition definition = definitionRepo.findById(definitionId)
                    .orElseThrow(() -> new FlowException(
                            "FLOW_NOT_FOUND",
                            "流程定义不存在，ID: " + definitionId
                    ));

            List<FlowDefinition> versions = definitionRepo.findVersionsByProjectIdAndName(
                    definition.projectId(), definition.name());

            return versions.stream()
                    .map(this::toDefinitionDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            if (e instanceof FlowException) {
                throw e;
            }
            throw new FlowException("VERSION_HISTORY_ERROR", "获取版本历史失败", e);
        }
    }

    // =================== 快照管理实现 ===================

    /**
     * 获取流程快照
     */
    public FlowSnapshotDTO getSnapshot(Long snapshotId) {
        log.debug("获取流程快照: snapshotId={}", snapshotId);

        try {
            FlowSnapshot snapshot = snapshotRepo.findById(snapshotId)
                    .orElseThrow(() -> new FlowException(
                            "SNAPSHOT_NOT_FOUND",
                            "流程快照不存在，ID: " + snapshotId
                    ));

            return new FlowSnapshotDTO(
                    snapshot.id(),
                    snapshot.definitionId(),
                    snapshot.version(),
                    snapshot.snapshotJson(),
                    snapshot.createdAt()
            );

        } catch (Exception e) {
            if (e instanceof FlowException) {
                throw e;
            }
            throw new FlowException("SNAPSHOT_GET_ERROR", "获取快照失败", e);
        }
    }

    // =================== 统计和监控实现 ===================

    /**
     * 获取流程统计
     */
    public FlowStatisticsDTO getStatistics(Long tenantId, Long projectId, String timeRange) {
        log.debug("获取流程统计: tenantId={}, projectId={}, timeRange={}",
                tenantId, projectId, timeRange);

        try {
            OffsetDateTime startTime = calculateStartTime(timeRange);

            int totalProjects = (int) projectRepo.countByTenantId(tenantId);
            int totalFlows;
            int publishedFlows;
            int totalRuns;
            int successfulRuns;
            int failedRuns;

            if (projectId != null) {
                // 项目级统计
                totalFlows = definitionRepo.countByProjectId(projectId);
                publishedFlows = definitionRepo.countByProjectIdAndStatus(projectId, FlowStatus.PUBLISHED);
                totalRuns = runRepo.countByProjectIdSince(projectId, startTime);
                successfulRuns = runRepo.countByProjectIdAndStatusSince(projectId, RunStatus.SUCCESS, startTime);
                failedRuns = runRepo.countByProjectIdAndStatusSince(projectId, RunStatus.FAILED, startTime);
            } else {
                // 租户级统计
                totalFlows = definitionRepo.countByTenantId(tenantId);
                publishedFlows = definitionRepo.countByTenantIdAndStatus(tenantId, FlowStatus.PUBLISHED);
                totalRuns = runRepo.countByTenantIdSince(tenantId, startTime);
                successfulRuns = runRepo.countByTenantIdAndStatusSince(tenantId, RunStatus.SUCCESS, startTime);
                failedRuns = runRepo.countByTenantIdAndStatusSince(tenantId, RunStatus.FAILED, startTime);
            }

            double successRate = totalRuns > 0 ? (double) successfulRuns / totalRuns : 0.0;

            return new FlowStatisticsDTO(
                    tenantId,
                    projectId,
                    totalProjects,
                    totalFlows,
                    publishedFlows,
                    totalRuns,
                    successfulRuns,
                    failedRuns,
                    successRate,
                    timeRange
            );

        } catch (Exception e) {
            throw new FlowException("STATISTICS_ERROR", "获取统计信息失败", e);
        }
    }

    /**
     * 健康检查
     */
    public FlowHealthStatus checkHealth() {
        log.debug("执行健康检查");

        long startTime = System.currentTimeMillis();
        Map<String, String> components = new HashMap<>();

        try {
            // 检查数据库连接
            try {
                projectRepo.countByTenantId(1L);
                components.put("database", "healthy");
            } catch (Exception e) {
                components.put("database", "unhealthy: " + e.getMessage());
            }

            // 检查执行服务
            try {
                boolean available = executionService.isHealthy();
                components.put("execution_service", available ? "healthy" : "unhealthy");
            } catch (Exception e) {
                components.put("execution_service", "unhealthy: " + e.getMessage());
            }

            // 检查构建服务
            try {
                boolean available = buildService.isHealthy();
                components.put("build_service", available ? "healthy" : "unhealthy");
            } catch (Exception e) {
                components.put("build_service", "unhealthy: " + e.getMessage());
            }

            // 检查异步执行器
            try {
                if (!asyncExecutor.isShutdown()) {
                    components.put("async_executor", "healthy");
                } else {
                    components.put("async_executor", "unhealthy: executor is shutdown");
                }
            } catch (Exception e) {
                components.put("async_executor", "unhealthy: " + e.getMessage());
            }

            boolean allHealthy = components.values().stream()
                    .allMatch(status -> status.equals("healthy"));

            long responseTime = System.currentTimeMillis() - startTime;

            return new FlowHealthStatus(
                    allHealthy ? "healthy" : "unhealthy",
                    components,
                    responseTime
            );

        } catch (Exception e) {
            return new FlowHealthStatus(
                    "unhealthy",
                    Map.of("error", e.getMessage()),
                    System.currentTimeMillis() - startTime
            );
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 异步执行流程
     */
    private void scheduleAsyncExecution(FlowRun run, FlowSnapshot snapshot,
                                        Map<String, Object> inputData, Integer timeoutMinutes) {
        CompletableFuture.runAsync(() -> {
            try {
                executeSynchronously(run, snapshot, inputData, timeoutMinutes);
            } catch (Exception e) {
                log.error("异步执行失败: runId={}", run.id(), e);
                // 标记运行失败
                FlowRun failedRun = run.fail("{\"error\":\"异步执行失败\",\"message\":\"" + e.getMessage() + "\"}");
                runRepo.save(failedRun);
            }
        }, asyncExecutor);
    }

    /**
     * 同步执行流程
     */
    private FlowRun executeSynchronously(FlowRun run, FlowSnapshot snapshot,
                                         Map<String, Object> inputData, Integer timeoutMinutes) {
        try {
            // 开始执行
            run = run.start();
            run = runRepo.save(run);

            // 调用执行服务
            String resultJson = executionService.executeFlow(
                    run.id(), snapshot, inputData, timeoutMinutes);

            // 标记成功
            run = run.success(resultJson);
            return runRepo.save(run);

        } catch (Exception e) {
            // 标记失败
            String errorJson = "{\"error\":\"执行失败\",\"message\":\"" + e.getMessage() + "\"}";
            run = run.fail(errorJson);
            return runRepo.save(run);
        }
    }

    /**
     * 计算时间范围的开始时间
     */
    private OffsetDateTime calculateStartTime(String timeRange) {
        if (timeRange == null) {
            return OffsetDateTime.now().minusDays(30);
        }

        return switch (timeRange.toLowerCase()) {
            case "1h", "hour" -> OffsetDateTime.now().minusHours(1);
            case "1d", "day" -> OffsetDateTime.now().minusDays(1);
            case "7d", "week" -> OffsetDateTime.now().minusDays(7);
            case "30d", "month" -> OffsetDateTime.now().minusDays(30);
            case "90d", "quarter" -> OffsetDateTime.now().minusDays(90);
            default -> OffsetDateTime.now().minusDays(30);
        };
    }

    /**
     * 记录指标
     */
    private void recordMetrics(String operation, long startTime, boolean success) {
        if (metricsService != null) {
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordOperation(operation, duration, success);
        }
    }

    // =================== DTO转换方法 ===================

    private FlowProjectDTO toProjectDTO(FlowProject project) {
        String creatorName = null;
        if (userInfoService != null) {
            creatorName = userInfoService.getUserName(project.createdBy());
        }

        return new FlowProjectDTO(
                project.id(),
                project.name(),
                project.createdBy(),
                creatorName,
                project.createdAt(),
                project.updatedAt()
        );
    }

    private FlowDefinitionDTO toDefinitionDTO(FlowDefinition definition) {
        String creatorName = null;
        if (userInfoService != null) {
            creatorName = userInfoService.getUserName(definition.createdBy());
        }

        return new FlowDefinitionDTO(
                definition.id(),
                definition.projectId(),
                definition.name(),
                definition.version(),
                definition.isLatest(),
                definition.status().getLabel(),
                definition.description(),
                definition.createdBy(),
                creatorName,
                definition.createdAt(),
                definition.updatedAt()
        );
    }

    private FlowNodeDTO toNodeDTO(FlowNode node) {
        return new FlowNodeDTO(
                node.id(),
                node.nodeTypeCode(),
                node.nodeKey(),
                node.name(),
                node.configJson(),
                node.retryPolicyJson()
        );
    }

    private FlowEdgeDTO toEdgeDTO(FlowEdge edge) {
        return new FlowEdgeDTO(
                edge.id(),
                edge.sourceKey(),
                edge.targetKey(),
                edge.configJson()
        );
    }

    private FlowRunDTO toRunDTO(FlowRun run) {
        return new FlowRunDTO(
                run.id(),
                run.snapshotId(),
                run.userId(),
                run.status().getLabel(),
                run.startedAt(),
                run.finishedAt(),
                run.getDurationMs()
        );
    }

    private FlowRunLogDTO toRunLogDTO(FlowRunLog log) {
        return new FlowRunLogDTO(
                log.id(),
                log.nodeKey(),
                log.ioJson(),
                log.createdAt()
        );
    }
}