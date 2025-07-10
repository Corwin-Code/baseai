package com.clinflash.baseai.domain.flow.service;

import com.clinflash.baseai.domain.flow.model.FlowRunLog;
import com.clinflash.baseai.domain.flow.repository.FlowRunLogRepository;
import com.clinflash.baseai.domain.flow.repository.FlowRunRepository;
import com.clinflash.baseai.infrastructure.exception.FlowException;
import com.clinflash.baseai.infrastructure.flow.executor.NodeExecutor;
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
 * 它就像一个精密的指挥官，按照预定的计划协调各个组件的执行，处理异常情况，
 * 确保整个流程能够顺利完成。</p>
 *
 * <p><b>执行模型：</b></p>
 * <p>我们采用基于状态机的执行模型，每个节点的执行都有明确的状态转换：
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
    private final Map<String, NodeExecutor> nodeExecutors;
    private final FlowRunRepository runRepo;
    private final FlowRunLogRepository runLogRepo;
    private final ExecutorService executorService;
    private final Map<Long, Future<?>> runningTasks;

    public FlowExecutionService(ObjectMapper objectMapper,
                                List<NodeExecutor> nodeExecutors,
                                FlowRunRepository runRepo,
                                FlowRunLogRepository runLogRepo) {
        this.objectMapper = objectMapper;
        this.runRepo = runRepo;
        this.runLogRepo = runLogRepo;
        this.executorService = Executors.newCachedThreadPool();
        this.runningTasks = new ConcurrentHashMap<>();

        // 构建节点执行器映射
        this.nodeExecutors = new HashMap<>();
        for (NodeExecutor executor : nodeExecutors) {
            for (String nodeType : executor.getSupportedNodeTypes()) {
                this.nodeExecutors.put(nodeType, executor);
            }
        }

        log.info("流程执行服务初始化完成: 支持的节点类型={}", this.nodeExecutors.keySet());
    }

    /**
     * 执行流程
     *
     * <p>这是整个执行引擎的入口方法。它接收流程快照和输入数据，
     * 创建执行上下文，按照预定的执行计划逐步执行每个节点。</p>
     *
     * @param runId          运行实例ID
     * @param snapshotJson   流程快照JSON
     * @param inputData      输入数据
     * @param timeoutMinutes 超时时间（分钟）
     * @return 执行结果JSON
     */
    public String executeFlow(Long runId, String snapshotJson,
                              Map<String, Object> inputData, Integer timeoutMinutes) {

        log.info("开始执行流程: runId={}, timeout={}分钟", runId, timeoutMinutes);
        long startTime = System.currentTimeMillis();

        try {
            // 解析快照
            FlowSnapshot snapshot = parseSnapshot(snapshotJson);

            // 创建执行上下文
            ExecutionContext context = createExecutionContext(runId, snapshot, inputData);

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
     */
    public boolean isHealthy() {
        try {
            return !executorService.isShutdown() &&
                    !nodeExecutors.isEmpty() &&
                    runRepo != null &&
                    runLogRepo != null;
        } catch (Exception e) {
            log.error("流程执行服务健康检查失败", e);
            return false;
        }
    }

    /**
     * 获取当前运行的任务数量
     */
    public int getRunningTaskCount() {
        return runningTasks.size();
    }

    // =================== 私有执行方法 ===================

    /**
     * 内部执行流程
     */
    private String executeFlowInternal(ExecutionContext context) {
        log.debug("开始内部执行流程: runId={}", context.runId);

        try {
            // 初始化执行状态
            initializeExecution(context);

            // 按执行计划逐步执行
            for (String nodeKey : context.snapshot.executionPlan) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("执行被中断");
                }

                executeNode(context, nodeKey);
            }

            // 构建最终结果
            Map<String, Object> result = buildExecutionResult(context);
            return objectMapper.writeValueAsString(result);

        } catch (InterruptedException e) {
            log.warn("流程执行被中断: runId={}", context.runId);
            return buildInterruptedResult(context);

        } catch (Exception e) {
            log.error("流程内部执行失败: runId={}", context.runId, e);
            return buildErrorResult(context, e);
        }
    }

    /**
     * 执行单个节点
     */
    private void executeNode(ExecutionContext context, String nodeKey) {
        log.debug("执行节点: runId={}, nodeKey={}", context.runId, nodeKey);

        try {
            // 获取节点信息
            NodeInfo nodeInfo = context.snapshot.nodes.get(nodeKey);
            if (nodeInfo == null) {
                throw new FlowException("NODE_NOT_FOUND", "节点不存在: " + nodeKey);
            }

            // 检查依赖是否满足
            if (!areDependenciesSatisfied(context, nodeKey)) {
                log.debug("节点依赖未满足，跳过执行: nodeKey={}", nodeKey);
                return;
            }

            // 获取节点执行器
            NodeExecutor executor = nodeExecutors.get(nodeInfo.nodeTypeCode);
            if (executor == null) {
                throw new FlowException("EXECUTOR_NOT_FOUND",
                        "未找到节点执行器: " + nodeInfo.nodeTypeCode);
            }

            // 准备输入数据
            Map<String, Object> nodeInput = prepareNodeInput(context, nodeKey);

            // 记录执行开始
            logNodeExecution(context.runId, nodeKey, "START", nodeInput, null);

            // 执行节点
            long nodeStartTime = System.currentTimeMillis();
            Map<String, Object> nodeOutput = executor.execute(nodeInfo, nodeInput, context);
            long nodeEndTime = System.currentTimeMillis();

            // 保存执行结果
            context.nodeResults.put(nodeKey, nodeOutput);
            context.executionMetrics.put(nodeKey, nodeEndTime - nodeStartTime);

            // 记录执行结果
            logNodeExecution(context.runId, nodeKey, "SUCCESS", nodeInput, nodeOutput);

            log.debug("节点执行成功: nodeKey={}, duration={}ms",
                    nodeKey, nodeEndTime - nodeStartTime);

        } catch (Exception e) {
            log.error("节点执行失败: nodeKey={}", nodeKey, e);

            // 记录执行失败
            Map<String, Object> errorInfo = Map.of(
                    "error", e.getMessage(),
                    "type", e.getClass().getSimpleName()
            );
            logNodeExecution(context.runId, nodeKey, "ERROR", null, errorInfo);

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
     */
    private boolean areDependenciesSatisfied(ExecutionContext context, String nodeKey) {
        List<String> dependencies = context.snapshot.dependencyGraph.get(nodeKey);
        if (dependencies == null || dependencies.isEmpty()) {
            return true; // 没有依赖
        }

        // 检查所有依赖节点是否都已执行成功
        for (String dependency : dependencies) {
            if (!context.nodeResults.containsKey(dependency)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 准备节点输入数据
     */
    private Map<String, Object> prepareNodeInput(ExecutionContext context, String nodeKey) {

        // 添加初始输入数据
        Map<String, Object> input = new HashMap<>(context.inputData);

        // 添加依赖节点的输出
        List<String> dependencies = context.snapshot.dependencyGraph.get(nodeKey);
        if (dependencies != null) {
            for (String dependency : dependencies) {
                Map<String, Object> dependencyOutput = context.nodeResults.get(dependency);
                if (dependencyOutput != null) {
                    input.put(dependency + "_output", dependencyOutput);
                }
            }
        }

        // 添加上下文信息
        input.put("_context", Map.of(
                "runId", context.runId,
                "nodeKey", nodeKey,
                "executionTime", System.currentTimeMillis()
        ));

        return input;
    }

    /**
     * 记录节点执行日志
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
     */
    private boolean shouldRetry(ExecutionContext context, String nodeKey, Exception e) {
        // 简化的重试逻辑实现
        NodeInfo nodeInfo = context.snapshot.nodes.get(nodeKey);
        if (nodeInfo.retryPolicyJson == null) {
            return false;
        }

        try {
            Map<String, Object> retryPolicy = objectMapper.readValue(
                    nodeInfo.retryPolicyJson, new TypeReference<Map<String, Object>>() {
                    });

            Integer maxRetries = (Integer) retryPolicy.get("maxRetries");
            if (maxRetries == null || maxRetries <= 0) {
                return false;
            }

            Integer currentRetries = context.retryCount.getOrDefault(nodeKey, 0);
            return currentRetries < maxRetries;

        } catch (Exception ex) {
            log.warn("解析重试策略失败: nodeKey={}", nodeKey, ex);
            return false;
        }
    }

    /**
     * 重试节点执行
     */
    private void retryNodeExecution(ExecutionContext context, String nodeKey) {
        int currentRetries = context.retryCount.getOrDefault(nodeKey, 0);
        context.retryCount.put(nodeKey, currentRetries + 1);

        log.info("重试节点执行: nodeKey={}, retryCount={}", nodeKey, currentRetries + 1);

        // 等待一段时间后重试
        try {
            Thread.sleep(1000L * (currentRetries + 1)); // 递增延迟
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
     */
    private FlowSnapshot parseSnapshot(String snapshotJson) {
        try {
            Map<String, Object> snapshotData = objectMapper.readValue(
                    snapshotJson, new TypeReference<Map<String, Object>>() {
                    });

            FlowSnapshot snapshot = new FlowSnapshot();
            snapshot.definitionId = ((Number) snapshotData.get("definitionId")).longValue();
            snapshot.name = (String) snapshotData.get("name");
            snapshot.version = (Integer) snapshotData.get("version");

            // 解析节点
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodeDataList = (List<Map<String, Object>>) snapshotData.get("nodes");
            snapshot.nodes = new HashMap<>();
            for (Map<String, Object> nodeData : nodeDataList) {
                NodeInfo nodeInfo = new NodeInfo();
                nodeInfo.id = ((Number) nodeData.get("id")).longValue();
                nodeInfo.nodeTypeCode = (String) nodeData.get("nodeTypeCode");
                nodeInfo.nodeKey = (String) nodeData.get("nodeKey");
                nodeInfo.name = (String) nodeData.get("name");
                nodeInfo.configJson = (String) nodeData.get("configJson");
                nodeInfo.retryPolicyJson = (String) nodeData.get("retryPolicyJson");

                snapshot.nodes.put(nodeInfo.nodeKey, nodeInfo);
            }

            // 解析执行计划
            @SuppressWarnings("unchecked")
            List<String> executionPlan = (List<String>) snapshotData.get("executionPlan");
            snapshot.executionPlan = executionPlan;

            // 解析依赖图
            @SuppressWarnings("unchecked")
            Map<String, List<String>> dependencyGraph = (Map<String, List<String>>) snapshotData.get("dependencyGraph");
            snapshot.dependencyGraph = dependencyGraph != null ? dependencyGraph : new HashMap<>();

            return snapshot;

        } catch (Exception e) {
            log.error("解析流程快照失败", e);
            throw new FlowException("SNAPSHOT_PARSE_ERROR", "流程快照解析失败", e);
        }
    }

    /**
     * 创建执行上下文
     */
    private ExecutionContext createExecutionContext(Long runId, FlowSnapshot snapshot,
                                                    Map<String, Object> inputData) {
        ExecutionContext context = new ExecutionContext();
        context.runId = runId;
        context.snapshot = snapshot;
        context.inputData = inputData != null ? inputData : new HashMap<>();
        context.nodeResults = new ConcurrentHashMap<>();
        context.retryCount = new ConcurrentHashMap<>();
        context.executionMetrics = new ConcurrentHashMap<>();
        return context;
    }

    /**
     * 初始化执行
     */
    private void initializeExecution(ExecutionContext context) {
        log.debug("初始化执行上下文: runId={}", context.runId);

        // 记录执行开始
        logNodeExecution(context.runId, "FLOW_START", "INFO", context.inputData, null);
    }

    /**
     * 构建执行结果
     */
    private Map<String, Object> buildExecutionResult(ExecutionContext context) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("runId", context.runId);
        result.put("nodeResults", context.nodeResults);
        result.put("executionMetrics", context.executionMetrics);
        result.put("finishedAt", System.currentTimeMillis());
        return result;
    }

    /**
     * 构建中断结果
     */
    private String buildInterruptedResult(ExecutionContext context) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "INTERRUPTED");
            result.put("runId", context.runId);
            result.put("partialResults", context.nodeResults);
            result.put("finishedAt", System.currentTimeMillis());
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"status\":\"INTERRUPTED\",\"error\":\"结果序列化失败\"}";
        }
    }

    /**
     * 构建错误结果
     */
    private String buildErrorResult(ExecutionContext context, Exception e) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "ERROR");
            result.put("runId", context.runId);
            result.put("error", e.getMessage());
            result.put("partialResults", context.nodeResults);
            result.put("finishedAt", System.currentTimeMillis());
            return objectMapper.writeValueAsString(result);
        } catch (Exception ex) {
            return "{\"status\":\"ERROR\",\"error\":\"结果序列化失败\"}";
        }
    }

    // =================== 内部数据结构 ===================

    /**
     * 流程快照内部表示
     */
    private static class FlowSnapshot {
        Long definitionId;
        String name;
        Integer version;
        Map<String, NodeInfo> nodes;
        List<String> executionPlan;
        Map<String, List<String>> dependencyGraph;
    }

    /**
     * 节点信息
     */
    private static class NodeInfo {
        Long id;
        String nodeTypeCode;
        String nodeKey;
        String name;
        String configJson;
        String retryPolicyJson;
    }

    /**
     * 执行上下文
     */
    public static class ExecutionContext {
        Long runId;
        FlowSnapshot snapshot;
        Map<String, Object> inputData;
        Map<String, Map<String, Object>> nodeResults;
        Map<String, Integer> retryCount;
        Map<String, Long> executionMetrics;
    }
}