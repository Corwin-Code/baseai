package com.clinflash.baseai.domain.flow.service;

import com.clinflash.baseai.application.flow.command.UpdateFlowStructureCommand;
import com.clinflash.baseai.domain.flow.model.FlowDefinition;
import com.clinflash.baseai.domain.flow.model.FlowEdge;
import com.clinflash.baseai.domain.flow.model.FlowNode;
import com.clinflash.baseai.domain.flow.model.NodeTypes;
import com.clinflash.baseai.infrastructure.exception.FlowValidationException;
import com.clinflash.baseai.infrastructure.flow.service.NodeTypeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <h2>流程构建领域服务</h2>
 *
 * <p>这个服务负责流程的构建、验证和快照创建。它就像一个经验丰富的架构师，
 * 不仅要确保设计图的正确性，还要保证建筑的可施工性和安全性。</p>
 *
 * <p><b>核心职责：</b></p>
 * <ul>
 * <li><b>结构验证：</b>检查流程图的拓扑结构是否合理</li>
 * <li><b>依赖分析：</b>发现循环依赖和孤立节点</li>
 * <li><b>配置校验：</b>验证每个节点的配置是否完整和正确</li>
 * <li><b>快照生成：</b>将流程定义转换为可执行的快照格式</li>
 * </ul>
 *
 * <p><b>设计理念：</b></p>
 * <p>我们采用"层层递进"的验证策略：首先检查基本的语法正确性，
 * 然后验证语义的合理性，最后确保运行时的可执行性。这种方法
 * 能够在问题早期发现并给出明确的错误提示。</p>
 */
@Service
public class FlowBuildService {

    private static final Logger log = LoggerFactory.getLogger(FlowBuildService.class);

    private final ObjectMapper objectMapper;
    private final NodeTypeService nodeTypeService;

    public FlowBuildService(ObjectMapper objectMapper, NodeTypeService nodeTypeService) {
        this.objectMapper = objectMapper;
        this.nodeTypeService = nodeTypeService;
    }

    /**
     * 验证流程结构的完整性和正确性
     *
     * <p>这是流程设计阶段最重要的验证步骤。我们需要确保用户设计的流程图
     * 在逻辑上是合理的，在技术上是可执行的。</p>
     *
     * @param nodes 流程节点列表
     * @param edges 流程连接列表
     * @throws FlowValidationException 当发现结构问题时抛出
     */
    public void validateFlowStructure(List<UpdateFlowStructureCommand.NodeCommand> nodes,
                                      List<UpdateFlowStructureCommand.EdgeCommand> edges) {
        log.debug("开始验证流程结构: nodes={}, edges={}", nodes.size(), edges.size());

        try {
            // 第一层验证：基本结构检查
            validateBasicStructure(nodes, edges);

            // 第二层验证：拓扑结构检查
            validateTopology(nodes, edges);

            // 第三层验证：节点配置检查
            validateNodeConfigurations(nodes);

            // 第四层验证：连接配置检查
            validateEdgeConfigurations(edges);

            log.info("流程结构验证通过: nodes={}, edges={}", nodes.size(), edges.size());

        } catch (FlowValidationException e) {
            log.warn("流程结构验证失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("流程结构验证异常", e);
            throw new FlowValidationException("流程结构验证过程中发生异常: " + e.getMessage());
        }
    }

    /**
     * 验证流程是否可以发布
     *
     * <p>发布前的最终检查，确保流程完整且可执行。这是进入生产环境前的最后一道防线。</p>
     */
    public void validateForPublication(FlowDefinition definition, List<FlowNode> nodes, List<FlowEdge> edges) {
        log.debug("验证流程发布条件: definitionId={}", definition.id());

        try {
            // 检查流程必须有开始节点
            boolean hasStartNode = nodes.stream()
                    .anyMatch(node -> NodeTypes.START.equals(node.nodeTypeCode()));
            if (!hasStartNode) {
                throw new FlowValidationException("流程必须包含一个开始节点");
            }

            // 检查流程必须有结束节点
            boolean hasEndNode = nodes.stream()
                    .anyMatch(node -> NodeTypes.END.equals(node.nodeTypeCode()));
            if (!hasEndNode) {
                throw new FlowValidationException("流程必须包含至少一个结束节点");
            }

            // 检查所有节点都必须可达
            validateReachability(nodes, edges);

            // 检查关键节点的配置完整性
            validateCriticalNodeConfigurations(nodes);

            log.info("流程发布验证通过: definitionId={}", definition.id());

        } catch (FlowValidationException e) {
            log.warn("流程发布验证失败: definitionId={}, error={}", definition.id(), e.getMessage());
            throw e;
        }
    }

    /**
     * 创建流程快照JSON
     *
     * <p>快照是流程执行时使用的完整配置，它包含了所有必要的信息，
     * 确保无论原始定义如何变化，运行实例都能保持一致性。</p>
     */
    public String createSnapshotJson(FlowDefinition definition, List<FlowNode> nodes, List<FlowEdge> edges) {
        log.debug("创建流程快照: definitionId={}", definition.id());

        try {
            // 构建快照数据结构
            Map<String, Object> snapshot = new HashMap<>();

            // 基本信息
            snapshot.put("definitionId", definition.id());
            snapshot.put("name", definition.name());
            snapshot.put("version", definition.version());
            snapshot.put("createdAt", System.currentTimeMillis());

            // 节点信息
            List<Map<String, Object>> nodeData = nodes.stream()
                    .map(this::nodeToMap)
                    .collect(Collectors.toList());
            snapshot.put("nodes", nodeData);

            // 连接信息
            List<Map<String, Object>> edgeData = edges.stream()
                    .map(this::edgeToMap)
                    .collect(Collectors.toList());
            snapshot.put("edges", edgeData);

            // 执行计划（优化后的执行顺序）
            List<String> executionPlan = buildExecutionPlan(nodes, edges);
            snapshot.put("executionPlan", executionPlan);

            // 依赖图（用于运行时优化）
            Map<String, List<String>> dependencyGraph = buildDependencyGraph(edges);
            snapshot.put("dependencyGraph", dependencyGraph);

            // 元数据
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("totalNodes", nodes.size());
            metadata.put("totalEdges", edges.size());
            metadata.put("hasParallelNodes", hasParallelExecution(nodes));
            metadata.put("estimatedComplexity", calculateComplexity(nodes, edges));
            snapshot.put("metadata", metadata);

            String snapshotJson = objectMapper.writeValueAsString(snapshot);
            log.info("流程快照创建成功: definitionId={}, size={} bytes",
                    definition.id(), snapshotJson.length());

            return snapshotJson;

        } catch (JsonProcessingException e) {
            log.error("快照JSON序列化失败: definitionId={}", definition.id(), e);
            throw new FlowValidationException("快照创建失败: JSON序列化错误");
        } catch (Exception e) {
            log.error("快照创建失败: definitionId={}", definition.id(), e);
            throw new FlowValidationException("快照创建失败: " + e.getMessage());
        }
    }

    /**
     * 检查服务健康状态
     */
    public boolean isHealthy() {
        try {
            // 简单的健康检查：验证核心组件是否正常
            return objectMapper != null && nodeTypeService != null && nodeTypeService.isAvailable();
        } catch (Exception e) {
            log.error("流程构建服务健康检查失败", e);
            return false;
        }
    }

    // =================== 私有验证方法 ===================

    /**
     * 验证基本结构
     */
    private void validateBasicStructure(List<UpdateFlowStructureCommand.NodeCommand> nodes,
                                        List<UpdateFlowStructureCommand.EdgeCommand> edges) {

        // 检查节点Key的唯一性
        Set<String> nodeKeys = new HashSet<>();
        List<String> duplicateKeys = new ArrayList<>();

        for (UpdateFlowStructureCommand.NodeCommand node : nodes) {
            if (!nodeKeys.add(node.nodeKey())) {
                duplicateKeys.add(node.nodeKey());
            }
        }

        if (!duplicateKeys.isEmpty()) {
            throw new FlowValidationException("发现重复的节点Key: " + String.join(", ", duplicateKeys));
        }

        // 检查边引用的节点是否存在
        for (UpdateFlowStructureCommand.EdgeCommand edge : edges) {
            if (!nodeKeys.contains(edge.sourceKey())) {
                throw new FlowValidationException("边引用了不存在的源节点: " + edge.sourceKey());
            }
            if (!nodeKeys.contains(edge.targetKey())) {
                throw new FlowValidationException("边引用了不存在的目标节点: " + edge.targetKey());
            }
        }

        // 检查节点类型是否有效
        for (UpdateFlowStructureCommand.NodeCommand node : nodes) {
            if (!nodeTypeService.isValidNodeType(node.nodeTypeCode())) {
                throw new FlowValidationException("无效的节点类型: " + node.nodeTypeCode());
            }
        }
    }

    /**
     * 验证拓扑结构
     */
    private void validateTopology(List<UpdateFlowStructureCommand.NodeCommand> nodes,
                                  List<UpdateFlowStructureCommand.EdgeCommand> edges) {

        // 检查循环依赖
        detectCycles(nodes, edges);

        // 检查孤立节点
        detectIsolatedNodes(nodes, edges);

        // 检查特殊节点规则
        validateSpecialNodeRules(nodes, edges);
    }

    /**
     * 检测循环依赖
     */
    private void detectCycles(List<UpdateFlowStructureCommand.NodeCommand> nodes,
                              List<UpdateFlowStructureCommand.EdgeCommand> edges) {

        Map<String, List<String>> graph = new HashMap<>();
        Set<String> nodeKeys = nodes.stream()
                .map(UpdateFlowStructureCommand.NodeCommand::nodeKey)
                .collect(Collectors.toSet());

        // 构建邻接表
        for (String key : nodeKeys) {
            graph.put(key, new ArrayList<>());
        }

        for (UpdateFlowStructureCommand.EdgeCommand edge : edges) {
            graph.get(edge.sourceKey()).add(edge.targetKey());
        }

        // DFS检测循环
        Set<String> visited = new HashSet<>();
        Set<String> inStack = new HashSet<>();

        for (String node : nodeKeys) {
            if (!visited.contains(node)) {
                if (hasCycleDFS(node, graph, visited, inStack)) {
                    throw new FlowValidationException("流程中存在循环依赖");
                }
            }
        }
    }

    /**
     * DFS检测循环的辅助方法
     */
    private boolean hasCycleDFS(String node, Map<String, List<String>> graph,
                                Set<String> visited, Set<String> inStack) {
        visited.add(node);
        inStack.add(node);

        for (String neighbor : graph.get(node)) {
            if (!visited.contains(neighbor)) {
                if (hasCycleDFS(neighbor, graph, visited, inStack)) {
                    return true;
                }
            } else if (inStack.contains(neighbor)) {
                return true;
            }
        }

        inStack.remove(node);
        return false;
    }

    /**
     * 检测孤立节点
     */
    private void detectIsolatedNodes(List<UpdateFlowStructureCommand.NodeCommand> nodes,
                                     List<UpdateFlowStructureCommand.EdgeCommand> edges) {

        Set<String> connectedNodes = new HashSet<>();

        for (UpdateFlowStructureCommand.EdgeCommand edge : edges) {
            connectedNodes.add(edge.sourceKey());
            connectedNodes.add(edge.targetKey());
        }

        List<String> isolatedNodes = nodes.stream()
                .map(UpdateFlowStructureCommand.NodeCommand::nodeKey)
                .filter(key -> !connectedNodes.contains(key))
                .filter(key -> !isSpecialNode(key, nodes)) // 开始和结束节点可以是孤立的
                .collect(Collectors.toList());

        if (!isolatedNodes.isEmpty()) {
            throw new FlowValidationException("发现孤立节点: " + String.join(", ", isolatedNodes));
        }
    }

    /**
     * 检查是否为特殊节点
     */
    private boolean isSpecialNode(String nodeKey, List<UpdateFlowStructureCommand.NodeCommand> nodes) {
        return nodes.stream()
                .filter(node -> node.nodeKey().equals(nodeKey))
                .anyMatch(node -> NodeTypes.START.equals(node.nodeTypeCode()) ||
                        NodeTypes.END.equals(node.nodeTypeCode()));
    }

    /**
     * 验证特殊节点规则
     */
    private void validateSpecialNodeRules(List<UpdateFlowStructureCommand.NodeCommand> nodes,
                                          List<UpdateFlowStructureCommand.EdgeCommand> edges) {

        // 统计特殊节点
        long startNodeCount = nodes.stream()
                .filter(node -> NodeTypes.START.equals(node.nodeTypeCode()))
                .count();

        long endNodeCount = nodes.stream()
                .filter(node -> NodeTypes.END.equals(node.nodeTypeCode()))
                .count();

        // 检查开始节点数量
        if (startNodeCount > 1) {
            throw new FlowValidationException("流程只能有一个开始节点");
        }

        // 检查结束节点数量
        if (endNodeCount == 0) {
            throw new FlowValidationException("流程必须至少有一个结束节点");
        }

        // 检查开始节点不能有输入边
        if (startNodeCount > 0) {
            String startNodeKey = nodes.stream()
                    .filter(node -> NodeTypes.START.equals(node.nodeTypeCode()))
                    .map(UpdateFlowStructureCommand.NodeCommand::nodeKey)
                    .findFirst()
                    .orElse(null);

            boolean hasInputEdge = edges.stream()
                    .anyMatch(edge -> edge.targetKey().equals(startNodeKey));

            if (hasInputEdge) {
                throw new FlowValidationException("开始节点不能有输入连接");
            }
        }

        // 检查结束节点不能有输出边
        Set<String> endNodeKeys = nodes.stream()
                .filter(node -> NodeTypes.END.equals(node.nodeTypeCode()))
                .map(UpdateFlowStructureCommand.NodeCommand::nodeKey)
                .collect(Collectors.toSet());

        boolean hasOutputEdge = edges.stream()
                .anyMatch(edge -> endNodeKeys.contains(edge.sourceKey()));

        if (hasOutputEdge) {
            throw new FlowValidationException("结束节点不能有输出连接");
        }
    }

    /**
     * 验证节点配置
     */
    private void validateNodeConfigurations(List<UpdateFlowStructureCommand.NodeCommand> nodes) {
        for (UpdateFlowStructureCommand.NodeCommand node : nodes) {
            try {
                nodeTypeService.validateNodeConfig(node.nodeTypeCode(), node.configJson());
            } catch (Exception e) {
                throw new FlowValidationException(
                        String.format("节点 %s 配置无效: %s", node.nodeKey(), e.getMessage()));
            }
        }
    }

    /**
     * 验证连接配置
     */
    private void validateEdgeConfigurations(List<UpdateFlowStructureCommand.EdgeCommand> edges) {
        for (UpdateFlowStructureCommand.EdgeCommand edge : edges) {
            if (!edge.isValidEdge()) {
                throw new FlowValidationException(
                        String.format("无效的连接: %s -> %s", edge.sourceKey(), edge.targetKey()));
            }
        }
    }

    /**
     * 验证节点可达性
     */
    private void validateReachability(List<FlowNode> nodes, List<FlowEdge> edges) {
        // 从开始节点出发，检查是否所有节点都可达
        String startNodeKey = nodes.stream()
                .filter(node -> NodeTypes.START.equals(node.nodeTypeCode()))
                .map(FlowNode::nodeKey)
                .findFirst()
                .orElse(null);

        if (startNodeKey == null) {
            return; // 没有开始节点，跳过可达性检查
        }

        Map<String, List<String>> graph = new HashMap<>();
        Set<String> allNodeKeys = nodes.stream()
                .map(FlowNode::nodeKey)
                .collect(Collectors.toSet());

        // 构建邻接表
        for (String key : allNodeKeys) {
            graph.put(key, new ArrayList<>());
        }

        for (FlowEdge edge : edges) {
            graph.get(edge.sourceKey()).add(edge.targetKey());
        }

        // BFS查找可达节点
        Set<String> reachable = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.offer(startNodeKey);
        reachable.add(startNodeKey);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String neighbor : graph.get(current)) {
                if (!reachable.contains(neighbor)) {
                    reachable.add(neighbor);
                    queue.offer(neighbor);
                }
            }
        }

        // 检查不可达节点
        Set<String> unreachable = new HashSet<>(allNodeKeys);
        unreachable.removeAll(reachable);

        if (!unreachable.isEmpty()) {
            throw new FlowValidationException("存在不可达的节点: " + String.join(", ", unreachable));
        }
    }

    /**
     * 验证关键节点配置完整性
     */
    private void validateCriticalNodeConfigurations(List<FlowNode> nodes) {
        for (FlowNode node : nodes) {
            if (node.isControlNode() || node.nodeTypeCode().equals(NodeTypes.LLM)) {
                if (node.configJson() == null || node.configJson().trim().isEmpty()) {
                    throw new FlowValidationException(
                            String.format("关键节点 %s 缺少必要配置", node.nodeKey()));
                }
            }
        }
    }

    // =================== 快照构建辅助方法 ===================

    /**
     * 将节点转换为Map
     */
    private Map<String, Object> nodeToMap(FlowNode node) {
        Map<String, Object> nodeMap = new HashMap<>();
        nodeMap.put("id", node.id());
        nodeMap.put("nodeTypeCode", node.nodeTypeCode());
        nodeMap.put("nodeKey", node.nodeKey());
        nodeMap.put("name", node.name());
        nodeMap.put("configJson", node.configJson());
        nodeMap.put("retryPolicyJson", node.retryPolicyJson());
        return nodeMap;
    }

    /**
     * 将边转换为Map
     */
    private Map<String, Object> edgeToMap(FlowEdge edge) {
        Map<String, Object> edgeMap = new HashMap<>();
        edgeMap.put("id", edge.id());
        edgeMap.put("sourceKey", edge.sourceKey());
        edgeMap.put("targetKey", edge.targetKey());
        edgeMap.put("configJson", edge.configJson());
        return edgeMap;
    }

    /**
     * 构建执行计划
     */
    private List<String> buildExecutionPlan(List<FlowNode> nodes, List<FlowEdge> edges) {
        // 拓扑排序生成执行计划
        Map<String, List<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        // 初始化
        for (FlowNode node : nodes) {
            graph.put(node.nodeKey(), new ArrayList<>());
            inDegree.put(node.nodeKey(), 0);
        }

        // 构建图和入度
        for (FlowEdge edge : edges) {
            graph.get(edge.sourceKey()).add(edge.targetKey());
            inDegree.put(edge.targetKey(), inDegree.get(edge.targetKey()) + 1);
        }

        // 拓扑排序
        Queue<String> queue = new LinkedList<>();
        List<String> executionPlan = new ArrayList<>();

        // 找到所有入度为0的节点
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            executionPlan.add(current);

            for (String neighbor : graph.get(current)) {
                int newInDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, newInDegree);
                if (newInDegree == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        return executionPlan;
    }

    /**
     * 构建依赖图
     */
    private Map<String, List<String>> buildDependencyGraph(List<FlowEdge> edges) {
        Map<String, List<String>> dependencies = new HashMap<>();

        for (FlowEdge edge : edges) {
            dependencies.computeIfAbsent(edge.targetKey(), k -> new ArrayList<>())
                    .add(edge.sourceKey());
        }

        return dependencies;
    }

    /**
     * 检查是否有并行执行
     */
    private boolean hasParallelExecution(List<FlowNode> nodes) {
        return nodes.stream()
                .anyMatch(node -> NodeTypes.PARALLEL.equals(node.nodeTypeCode()));
    }

    /**
     * 计算流程复杂度
     */
    private int calculateComplexity(List<FlowNode> nodes, List<FlowEdge> edges) {
        // 简单的复杂度计算：节点数 + 边数 + 控制节点权重
        int baseComplexity = nodes.size() + edges.size();

        long controlNodes = nodes.stream()
                .filter(FlowNode::isControlNode)
                .count();

        return baseComplexity + (int) (controlNodes * 2);
    }
}