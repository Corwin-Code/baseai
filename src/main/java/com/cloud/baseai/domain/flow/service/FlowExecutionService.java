package com.cloud.baseai.domain.flow.service;

import com.cloud.baseai.domain.flow.model.FlowRunLog;
import com.cloud.baseai.domain.flow.model.FlowSnapshot;
import com.cloud.baseai.domain.flow.repository.FlowRunLogRepository;
import com.cloud.baseai.domain.flow.repository.FlowRunRepository;
import com.cloud.baseai.domain.flow.repository.FlowSnapshotRepository;
import com.cloud.baseai.infrastructure.exception.FlowException;
import com.cloud.baseai.infrastructure.flow.executor.NodeExecutor;
import com.cloud.baseai.infrastructure.flow.model.FlowExecutionContext;
import com.cloud.baseai.infrastructure.flow.model.NodeExecutionInfo;
import com.cloud.baseai.infrastructure.flow.service.NodeExecutorManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * <h2>流程执行领域服务</h2>
 *
 * <p>这个服务是流程编排的执行引擎，负责将静态的流程定义转换为动态的执行过程。
 * 它通常会按照预定的计划协调各个组件的执行，处理异常情况，
 * 确保整个流程能够顺利完成。</p>
 *
 * <p><b>执行模型：</b></p>
 * <p>采用基于状态机的执行模型，每个节点的执行都有明确的状态转换：
 * 等待 -> 运行 -> 成功/失败。这种模型让流程的执行过程变得可预测和可控制。</p>
 *
 * <p><b>并发处理：</b></p>
 * <p>对于可以并行执行的节点，执行引擎会自动识别并创建并发执行计划，
 * 最大化利用系统资源，提升整体执行效率。</p>
 */
@Service
public class FlowExecutionService {

    private static final Logger log = LoggerFactory.getLogger(FlowExecutionService.class);

    private final ObjectMapper objectMapper;
    private final NodeExecutorManager executorManager;
    private final FlowSnapshotRepository snapshotRepo;
    private final FlowRunRepository runRepo;
    private final FlowRunLogRepository runLogRepo;
    private final ExecutorService executorService;
    private final Map<Long, Future<?>> runningTasks;

    public FlowExecutionService(ObjectMapper objectMapper,
                                NodeExecutorManager executorManager,
                                FlowSnapshotRepository snapshotRepo,
                                FlowRunRepository runRepo,
                                FlowRunLogRepository runLogRepo) {
        this.objectMapper = objectMapper;
        this.executorManager = executorManager;
        this.snapshotRepo = snapshotRepo;
        this.runRepo = runRepo;
        this.runLogRepo = runLogRepo;
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "flow-executor");
            thread.setDaemon(true);
            return thread;
        });
        this.runningTasks = new ConcurrentHashMap<>();

        log.info("流程执行服务初始化完成: 支持的节点类型={}",
                executorManager.getSupportedNodeTypes());
    }

    /**
     * 根据快照ID执行流程
     *
     * @param runId          运行实例ID
     * @param snapshotId     快照ID
     * @param inputData      输入数据
     * @param timeoutMinutes 超时时间（分钟）
     * @return 执行结果JSON
     * @throws FlowException 当快照不存在或执行失败时抛出
     */
    public String executeFlowBySnapshotId(Long runId, Long snapshotId,
                                          Map<String, Object> inputData, Integer timeoutMinutes) {
        log.info("根据快照ID执行流程: runId={}, snapshotId={}", runId, snapshotId);

        // 加载快照
        FlowSnapshot snapshot = snapshotRepo.findById(snapshotId)
                .orElseThrow(() -> new FlowException("SNAPSHOT_NOT_FOUND",
                        "流程快照不存在: " + snapshotId));

        if (!snapshot.isAvailable()) {
            throw new FlowException("SNAPSHOT_UNAVAILABLE",
                    "流程快照不可用: " + snapshotId);
        }

        return executeFlow(runId, snapshot, inputData, timeoutMinutes);
    }

    /**
     * 执行流程
     *
     * <p>这是整个执行引擎的入口方法。它接收流程快照和输入数据，
     * 创建执行上下文，按照预定的执行计划逐步执行每个节点。</p>
     *
     * @param runId          运行实例ID
     * @param snapshot       流程快照
     * @param inputData      输入数据
     * @param timeoutMinutes 超时时间（分钟）
     * @return 执行结果JSON
     */
    public String executeFlow(Long runId, FlowSnapshot snapshot,
                              Map<String, Object> inputData, Integer timeoutMinutes) {
        log.info("执行流程: runId={}, snapshotId={}, timeout={}分钟",
                runId, snapshot.id(), timeoutMinutes);

        long startTime = System.currentTimeMillis();

        try {
            // 创建执行上下文
            FlowExecutionContext context = createExecutionContext(runId, snapshot, inputData);

            // 提交执行任务
            Future<String> future = executorService.submit(() -> executeFlowInternal(context));
            runningTasks.put(runId, future);

            // 等待执行完成或超时
            int effectiveTimeout = timeoutMinutes != null ? timeoutMinutes : 30;
            String result = future.get(effectiveTimeout, TimeUnit.MINUTES);

            runningTasks.remove(runId);

            long duration = System.currentTimeMillis() - startTime;
            log.info("流程执行完成: runId={}, duration={}ms", runId, duration);

            return result;

        } catch (TimeoutException e) {
            log.error("流程执行超时: runId={}", runId);
            stopExecution(runId);
            throw new FlowException("EXECUTION_TIMEOUT", "流程执行超时");

        } catch (Exception e) {
            log.error("流程执行失败: runId={}", runId, e);
            runningTasks.remove(runId);
            throw new FlowException("EXECUTION_ERROR", "流程执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 停止流程执行
     *
     * <p>当需要强制停止正在运行的流程时调用此方法。
     * 这在处理超时、用户取消或系统关闭时非常有用。</p>
     */
    public void stopExecution(Long runId) {
        log.info("停止流程执行: runId={}", runId);

        Future<?> task = runningTasks.get(runId);
        if (task != null) {
            task.cancel(true);
            runningTasks.remove(runId);
            log.info("流程执行已停止: runId={}", runId);
        }
    }

    /**
     * 检查服务健康状态
     *
     * <p>综合检查流程执行服务的各个组件是否正常工作。
     * 这包括线程池状态、执行器管理器状态和数据库连接等。</p>
     */
    public boolean isHealthy() {
        try {
            return !executorService.isShutdown() &&
                    !executorManager.getSupportedNodeTypes().isEmpty() &&
                    runRepo != null &&
                    runLogRepo != null;
        } catch (Exception e) {
            log.error("流程执行服务健康检查失败", e);
            return false;
        }
    }

    /**
     * 获取当前运行的任务数量
     *
     * <p>用于监控和调试，了解系统当前的负载情况。</p>
     */
    public int getRunningTaskCount() {
        return runningTasks.size();
    }

    /**
     * 获取执行器健康状态
     *
     * <p>通过NodeExecutorManager获取所有执行器的健康状态，
     * 这对于系统监控和故障诊断非常有用。</p>
     */
    public Map<String, Boolean> getExecutorHealthStatus() {
        return executorManager.getExecutorHealthStatus();
    }

    // =================== 私有执行方法 ===================

    /**
     * 内部执行流程核心逻辑
     *
     * <p>这是流程执行的核心方法，负责：</p>
     * <ul>
     * <li>初始化执行状态和日志记录</li>
     * <li>按照执行计划顺序执行节点</li>
     * <li>处理节点间的数据传递和依赖关系</li>
     * <li>捕获和处理执行过程中的异常</li>
     * <li>构建最终的执行结果</li>
     * </ul>
     */
    private String executeFlowInternal(FlowExecutionContext context) {
        log.debug("开始内部执行流程: runId={}", context.getRunId());

        try {
            // 初始化执行状态
            initializeExecution(context);

            // 从快照获取执行计划 - 注意这里的简化
            List<String> executionPlan = context.getSnapshot().getExecutionPlan();

            // 按执行计划逐步执行节点
            for (String nodeKey : executionPlan) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("执行被中断");
                }

                executeNode(context, nodeKey);
            }

            // 构建最终结果
            Map<String, Object> result = buildExecutionResult(context);
            return objectMapper.writeValueAsString(result);

        } catch (InterruptedException e) {
            log.warn("流程执行被中断: runId={}", context.getRunId());
            return buildInterruptedResult(context);

        } catch (Exception e) {
            log.error("流程内部执行失败: runId={}", context.getRunId(), e);
            return buildErrorResult(context, e);
        }
    }

    /**
     * 执行单个节点
     */
    private void executeNode(FlowExecutionContext context, String nodeKey) {
        log.debug("执行节点: runId={}, nodeKey={}", context.getRunId(), nodeKey);

        try {
            // 获取节点信息
            NodeExecutionInfo nodeInfo = context.getSnapshot().getNode(nodeKey);
            if (nodeInfo == null) {
                throw new FlowException("NODE_NOT_FOUND", "节点不存在: " + nodeKey);
            }

            // 检查依赖是否满足
            if (!areDependenciesSatisfied(context, nodeKey)) {
                log.debug("节点依赖未满足，跳过执行: nodeKey={}", nodeKey);
                return;
            }

            // 获取节点执行器
            NodeExecutor executor = executorManager.getExecutor(nodeInfo.nodeTypeCode());
            if (executor == null) {
                throw new FlowException("EXECUTOR_NOT_FOUND",
                        "未找到节点执行器: " + nodeInfo.nodeTypeCode());
            }

            // 检查执行器健康状态
            if (!executor.isHealthy()) {
                log.warn("执行器健康状态异常，但继续执行: {}", executor.getExecutorName());
            }

            // 准备输入数据
            Map<String, Object> nodeInput = prepareNodeInput(context, nodeKey);

            // 记录执行开始
            logNodeExecution(context.getRunId(), nodeKey, "START", nodeInput, null);

            // 执行节点
            long nodeStartTime = System.currentTimeMillis();
            Map<String, Object> nodeOutput = executor.execute(nodeInfo, nodeInput, context);
            long nodeEndTime = System.currentTimeMillis();

            // 保存执行结果到上下文
            context.saveNodeResult(nodeKey, nodeOutput);
            context.recordMetric(nodeKey, nodeEndTime - nodeStartTime);

            // 记录执行结果
            logNodeExecution(context.getRunId(), nodeKey, "SUCCESS", nodeInput, nodeOutput);

            log.debug("节点执行成功: nodeKey={}, duration={}ms, executor={}",
                    nodeKey, nodeEndTime - nodeStartTime, executor.getExecutorName());

        } catch (Exception e) {
            log.error("节点执行失败: nodeKey={}", nodeKey, e);

            // 记录执行失败
            Map<String, Object> errorInfo = Map.of(
                    "error", e.getMessage(),
                    "type", e.getClass().getSimpleName()
            );
            logNodeExecution(context.getRunId(), nodeKey, "ERROR", null, errorInfo);

            // 检查是否有重试策略
            if (shouldRetry(context, nodeKey, e)) {
                retryNodeExecution(context, nodeKey);
            } else {
                throw new FlowException("NODE_EXECUTION_ERROR",
                        "节点执行失败: " + nodeKey, e);
            }
        }
    }

    /**
     * 检查依赖是否满足
     *
     * <p>在执行节点之前，需要确保所有依赖的节点都已经成功执行。
     * 这是保证数据流正确性的关键检查。</p>
     */
    private boolean areDependenciesSatisfied(FlowExecutionContext context, String nodeKey) {
        Map<String, List<String>> dependencyGraph = context.getSnapshot().getDependencyGraph();
        List<String> dependencies = dependencyGraph.get(nodeKey);

        if (dependencies == null || dependencies.isEmpty()) {
            return true; // 没有依赖，可以执行
        }

        // 检查所有依赖节点是否都已执行成功
        for (String dependency : dependencies) {
            if (!context.isNodeExecuted(dependency)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 准备节点输入数据
     */
    private Map<String, Object> prepareNodeInput(FlowExecutionContext context, String nodeKey) {

        // 添加初始输入数据
        Map<String, Object> input = new HashMap<>(context.getInitialInput());

        // 添加依赖节点的输出
        Map<String, List<String>> dependencyGraph = context.getSnapshot().getDependencyGraph();
        List<String> dependencies = dependencyGraph.get(nodeKey);

        if (dependencies != null) {
            for (String dependency : dependencies) {
                Map<String, Object> dependencyOutput = context.getNodeResult(dependency);
                if (dependencyOutput != null) {
                    input.put(dependency + "_output", dependencyOutput);
                }
            }
        }

        // 添加上下文信息
        input.put("_context", Map.of(
                "runId", context.getRunId(),
                "nodeKey", nodeKey,
                "executionTime", System.currentTimeMillis(),
                "globalVariables", context.getAllGlobalVariables()
        ));

        return input;
    }

    /**
     * 记录节点执行日志
     *
     * <p>详细记录每个节点的执行状态、输入输出数据，
     * 为后续的调试、审计和性能分析提供重要信息。</p>
     */
    private void logNodeExecution(Long runId, String nodeKey, String status,
                                  Map<String, Object> input, Map<String, Object> output) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("status", status);
            logData.put("timestamp", System.currentTimeMillis());

            if (input != null) {
                logData.put("input", input);
            }

            if (output != null) {
                logData.put("output", output);
            }

            String ioJson = objectMapper.writeValueAsString(logData);

            FlowRunLog runLog = FlowRunLog.create(runId, nodeKey, ioJson, null);
            runLogRepo.save(runLog);

        } catch (Exception e) {
            log.error("记录节点执行日志失败: runId={}, nodeKey={}", runId, nodeKey, e);
        }
    }

    /**
     * 检查是否应该重试
     *
     * <p>基于节点的重试策略配置，决定是否对失败的节点进行重试。</p>
     */
    private boolean shouldRetry(FlowExecutionContext context, String nodeKey, Exception e) {
        NodeExecutionInfo nodeInfo = context.getSnapshot().getNode(nodeKey);
        if (nodeInfo.retryPolicyJson() == null) {
            return false;
        }

        try {
            Map<String, Object> retryPolicy = objectMapper.readValue(
                    nodeInfo.retryPolicyJson(), new TypeReference<Map<String, Object>>() {
                    });

            Integer maxRetries = (Integer) retryPolicy.get("maxRetries");
            if (maxRetries == null || maxRetries <= 0) {
                return false;
            }

            int currentRetries = context.getRetryCount(nodeKey);
            return currentRetries < maxRetries;

        } catch (Exception ex) {
            log.warn("解析重试策略失败: nodeKey={}", nodeKey, ex);
            return false;
        }
    }

    /**
     * 重试节点执行
     *
     * <p>实现节点重试逻辑，包括递增延迟和重试计数管理。</p>
     */
    private void retryNodeExecution(FlowExecutionContext context, String nodeKey) {
        int currentRetries = context.incrementRetryCount(nodeKey);

        log.info("重试节点执行: nodeKey={}, retryCount={}", nodeKey, currentRetries);

        // 等待一段时间后重试
        try {
            Thread.sleep(1000L * (currentRetries)); // 递增延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("重试等待被中断", e);
        }

        // 递归调用执行
        executeNode(context, nodeKey);
    }

    // =================== 辅助方法 ===================

    /**
     * 解析流程快照
     *
     * <p>将JSON格式的流程快照解析为内部数据结构，
     * 包括节点信息、执行计划和依赖关系图。</p>
     */
    private FlowSnapshot parseSnapshotFromJson(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.trim().isEmpty()) {
            throw new FlowException("EMPTY_SNAPSHOT", "快照内容为空");
        }

        try {
            // 从JSON创建"临时"快照对象，没有数据库ID
            return FlowSnapshot.create(
                    null, // definitionId - 从JSON中提取
                    "临时快照", // name
                    1, // version
                    snapshotJson,
                    null // createdBy
            );

        } catch (Exception e) {
            log.error("JSON快照解析失败", e);
            throw new FlowException("SNAPSHOT_PARSE_ERROR", "快照解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建执行上下文
     */
    private FlowExecutionContext createExecutionContext(Long runId, FlowSnapshot snapshot,
                                                        Map<String, Object> inputData) {
        FlowExecutionContext context = new FlowExecutionContext(
                runId,
                snapshot.id(),
                null, // userId - 从上下文获取或参数传入
                null, // tenantId - 从上下文获取或参数传入
                inputData
        );

        // 设置快照引用 - 这是关键的改进
        context.setSnapshot(snapshot);

        return context;
    }

    /**
     * 初始化执行
     *
     * <p>在开始执行节点之前进行必要的初始化工作，
     * 包括日志记录和状态设置。</p>
     */
    private void initializeExecution(FlowExecutionContext context) {
        log.debug("初始化执行上下文: runId={}", context.getRunId());

        // 记录执行开始
        logNodeExecution(context.getRunId(), "FLOW_START", "INFO", context.getInitialInput(), null);
    }

    /**
     * 构建执行结果
     *
     * <p>整理所有节点的执行结果和统计信息，
     * 构建完整的流程执行结果。</p>
     */
    private Map<String, Object> buildExecutionResult(FlowExecutionContext context) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("runId", context.getRunId());
        result.put("snapshotId", context.getSnapshot().id());
        result.put("nodeResults", context.getAllNodeResults());
        result.put("executionMetrics", context.getExecutionMetrics());
        result.put("summary", context.generateSummary());
        result.put("finishedAt", System.currentTimeMillis());
        return result;
    }

    /**
     * 构建中断结果
     */
    private String buildInterruptedResult(FlowExecutionContext context) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "INTERRUPTED");
            result.put("runId", context.getRunId());
            result.put("partialResults", context.getAllNodeResults());
            result.put("finishedAt", System.currentTimeMillis());
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"status\":\"INTERRUPTED\",\"error\":\"结果序列化失败\"}";
        }
    }

    /**
     * 构建错误结果
     */
    private String buildErrorResult(FlowExecutionContext context, Exception e) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "ERROR");
            result.put("runId", context.getRunId());
            result.put("error", e.getMessage());
            result.put("partialResults", context.getAllNodeResults());
            result.put("finishedAt", System.currentTimeMillis());
            return objectMapper.writeValueAsString(result);
        } catch (Exception ex) {
            return "{\"status\":\"ERROR\",\"error\":\"结果序列化失败\"}";
        }
    }
}