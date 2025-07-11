package com.clinflash.baseai.domain.flow.model;

import com.clinflash.baseai.infrastructure.flow.model.NodeExecutionInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>流程快照领域模型</h2>
 *
 * <p>流程快照是流程定义在某个时间点的完整快照，包含了执行流程所需的所有信息。
 * 这就像拍照一样，保存了流程在某个瞬间的完整状态。</p>
 *
 * <p><b>不可变性：</b></p>
 * <p>快照一旦创建就不可更改，这保证了流程执行的一致性和可重现性。
 * 无论何时基于同一个快照执行流程，结果都应该是可预期的。</p>
 */
public record FlowSnapshot(
        Long id,
        Long definitionId,
        String name,
        Integer version,
        String snapshotJson,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {

    private static final Logger log = LoggerFactory.getLogger(FlowSnapshot.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 解析后的缓存数据 - 使用ThreadLocal确保线程安全
    private static final ThreadLocal<Map<String, ParsedSnapshot>> parseCache =
            ThreadLocal.withInitial(HashMap::new);

    /**
     * 创建新的流程快照
     *
     * <p>这是创建快照的标准方式。它会验证输入数据的有效性，
     * 确保快照包含执行流程所需的完整信息。</p>
     *
     * @param definitionId 流程定义ID
     * @param name         快照名称
     * @param version      版本号
     * @param snapshotJson 快照JSON内容
     * @param createdBy    创建者ID
     * @return 新创建的流程快照
     * @throws IllegalArgumentException 当输入数据无效时抛出
     */
    public static FlowSnapshot create(Long definitionId, String name, Integer version,
                                      String snapshotJson, Long createdBy) {
        validateSnapshotData(definitionId, name, version, snapshotJson);

        return new FlowSnapshot(
                null,
                definitionId,
                name,
                version,
                snapshotJson,
                createdBy,
                OffsetDateTime.now(),
                null
        );
    }

    /**
     * 从持久化实体重建快照
     *
     * <p>这个方法用于从数据库或其他持久化存储中恢复快照对象。
     * 它假设输入数据已经经过验证，因此不进行额外的验证检查。</p>
     */
    public static FlowSnapshot fromPersistence(Long id, Long definitionId, String name,
                                               Integer version, String snapshotJson,
                                               Long createdBy, OffsetDateTime createdAt,
                                               OffsetDateTime deletedAt) {
        return new FlowSnapshot(id, definitionId, name, version, snapshotJson,
                createdBy, createdAt, deletedAt);
    }

    /**
     * 检查快照是否可用
     *
     * <p>只有未删除且包含有效JSON内容的快照才被认为是可用的。
     * 这个检查在执行流程之前非常重要。</p>
     *
     * @return 如果快照可用则返回true
     */
    public boolean isAvailable() {
        return this.deletedAt == null &&
                this.snapshotJson != null &&
                !this.snapshotJson.trim().isEmpty();
    }

    /**
     * 获取解析后的节点信息映射
     *
     * <p>这个方法返回快照中所有节点的结构化信息。节点信息以Map的形式组织，
     * 键是节点的唯一标识符（nodeKey），值是对应的节点执行信息。</p>
     *
     * <p><b>性能优化：</b></p>
     * <p>解析结果会被缓存，避免重复解析相同的快照。缓存使用快照的JSON内容
     * 作为键，确保数据的一致性。</p>
     *
     * @return 节点信息映射，键为nodeKey，值为NodeExecutionInfo
     * @throws FlowSnapshotParseException 当JSON解析失败时抛出
     */
    @JsonIgnore
    public Map<String, NodeExecutionInfo> getNodes() {
        return getParsedSnapshot().nodes();
    }

    /**
     * 获取执行计划
     *
     * <p>执行计划定义了节点的执行顺序。这是一个有序列表，包含了所有需要
     * 执行的节点的键值。流程执行引擎会按照这个顺序依次执行节点。</p>
     *
     * @return 节点执行顺序列表
     * @throws FlowSnapshotParseException 当JSON解析失败时抛出
     */
    @JsonIgnore
    public List<String> getExecutionPlan() {
        return getParsedSnapshot().executionPlan();
    }

    /**
     * 获取依赖关系图
     *
     * <p>依赖关系图描述了节点之间的依赖关系。Map的键是节点标识符，
     * 值是该节点依赖的其他节点列表。这个信息用于确保节点按正确的
     * 顺序执行，避免数据依赖问题。</p>
     *
     * @return 依赖关系映射，键为节点标识符，值为依赖节点列表
     * @throws FlowSnapshotParseException 当JSON解析失败时抛出
     */
    @JsonIgnore
    public Map<String, List<String>> getDependencyGraph() {
        return getParsedSnapshot().dependencyGraph();
    }

    /**
     * 获取流程元信息
     *
     * <p>元信息包含流程的基本描述信息，如描述、标签、配置参数等。
     * 这些信息主要用于流程管理和监控。</p>
     *
     * @return 流程元信息
     * @throws FlowSnapshotParseException 当JSON解析失败时抛出
     */
    @JsonIgnore
    public Map<String, Object> getMetadata() {
        return getParsedSnapshot().metadata();
    }

    /**
     * 获取指定节点的执行信息
     *
     * <p>这是一个便利方法，用于快速获取特定节点的执行信息。
     * 相比于先获取所有节点再查找特定节点，这种方式更加高效。</p>
     *
     * @param nodeKey 节点标识符
     * @return 节点执行信息，如果节点不存在则返回null
     * @throws FlowSnapshotParseException 当JSON解析失败时抛出
     */
    @JsonIgnore
    public NodeExecutionInfo getNode(String nodeKey) {
        if (nodeKey == null) {
            return null;
        }
        return getNodes().get(nodeKey);
    }

    /**
     * 检查是否包含指定节点
     *
     * @param nodeKey 节点标识符
     * @return 如果包含该节点则返回true
     */
    @JsonIgnore
    public boolean containsNode(String nodeKey) {
        return nodeKey != null && getNodes().containsKey(nodeKey);
    }

    /**
     * 获取节点总数
     *
     * @return 快照中包含的节点数量
     */
    @JsonIgnore
    public int getNodeCount() {
        return getNodes().size();
    }

    /**
     * 标记快照为已删除
     *
     * <p>这个方法创建一个新的快照实例，将删除时间设置为当前时间。
     * 由于record是不可变的，我们需要创建新实例来表示状态变更。</p>
     *
     * @return 标记为已删除的新快照实例
     */
    public FlowSnapshot markAsDeleted() {
        return new FlowSnapshot(
                this.id,
                this.definitionId,
                this.name,
                this.version,
                this.snapshotJson,
                this.createdBy,
                this.createdAt,
                OffsetDateTime.now()
        );
    }

    /**
     * 生成快照摘要信息
     *
     * <p>摘要信息包含快照的关键统计数据，用于快速了解快照的基本情况，
     * 比如节点数量、执行计划长度等。这对于监控和诊断非常有用。</p>
     *
     * @return 快照摘要信息
     */
    @JsonIgnore
    public SnapshotSummary getSummary() {
        try {
            ParsedSnapshot parsed = getParsedSnapshot();
            return new SnapshotSummary(
                    this.id,
                    this.definitionId,
                    this.name,
                    this.version,
                    parsed.nodes().size(),
                    parsed.executionPlan().size(),
                    parsed.dependencyGraph().size(),
                    this.createdAt,
                    this.isAvailable()
            );
        } catch (Exception e) {
            // 如果解析失败，返回基础摘要信息
            return new SnapshotSummary(
                    this.id,
                    this.definitionId,
                    this.name,
                    this.version,
                    0,
                    0,
                    0,
                    this.createdAt,
                    false
            );
        }
    }

    // =================== 私有方法 ===================

    /**
     * 获取解析后的快照数据
     *
     * <p>这个方法实现了惰性解析逻辑。它首先检查缓存中是否已有解析结果，
     * 如果没有则进行解析并缓存结果。使用快照JSON的哈希值作为缓存键，
     * 确保数据一致性。</p>
     */
    private ParsedSnapshot getParsedSnapshot() {
        if (!isAvailable()) {
            throw new FlowSnapshotParseException("快照不可用或JSON内容为空");
        }

        String cacheKey = this.id + "_" + this.snapshotJson.hashCode();
        Map<String, ParsedSnapshot> cache = parseCache.get();

        ParsedSnapshot parsed = cache.get(cacheKey);
        if (parsed == null) {
            synchronized (this) {
                // 双重检查锁定，避免重复解析
                parsed = cache.get(cacheKey);
                if (parsed == null) {
                    parsed = parseSnapshotJson();
                    cache.put(cacheKey, parsed);
                    log.debug("快照解析完成并缓存: id={}, nodeCount={}",
                            this.id, parsed.nodes().size());
                }
            }
        }

        return parsed;
    }

    /**
     * 解析快照JSON内容
     *
     * <p>这个方法负责将JSON字符串解析为结构化的Java对象。解析过程包括
     * 节点信息提取、执行计划构建和依赖关系图生成。</p>
     */
    private ParsedSnapshot parseSnapshotJson() {
        try {
            Map<String, Object> snapshotData = objectMapper.readValue(
                    this.snapshotJson, new TypeReference<Map<String, Object>>() {
                    }
            );

            // 解析节点信息
            Map<String, NodeExecutionInfo> nodes = parseNodes(snapshotData);

            // 解析执行计划
            @SuppressWarnings("unchecked")
            List<String> executionPlan = (List<String>) snapshotData.get("executionPlan");
            if (executionPlan == null) {
                throw new FlowSnapshotParseException("快照中缺少executionPlan");
            }

            // 解析依赖关系图
            @SuppressWarnings("unchecked")
            Map<String, List<String>> dependencyGraph =
                    (Map<String, List<String>>) snapshotData.get("dependencyGraph");
            if (dependencyGraph == null) {
                dependencyGraph = new HashMap<>();
            }

            // 解析元信息
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) snapshotData.get("metadata");
            if (metadata == null) {
                metadata = new HashMap<>();
            }

            return new ParsedSnapshot(nodes, executionPlan, dependencyGraph, metadata);

        } catch (Exception e) {
            log.error("快照JSON解析失败: id={}", this.id, e);
            throw new FlowSnapshotParseException("快照JSON解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析节点信息
     *
     * <p>从快照JSON中提取所有节点信息，并转换为NodeExecutionInfo对象。
     * 这个过程包括节点配置验证和格式转换。</p>
     */
    private Map<String, NodeExecutionInfo> parseNodes(Map<String, Object> snapshotData) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodeDataList = (List<Map<String, Object>>) snapshotData.get("nodes");

        if (nodeDataList == null) {
            throw new FlowSnapshotParseException("快照中缺少nodes信息");
        }

        Map<String, NodeExecutionInfo> nodes = new HashMap<>();
        for (Map<String, Object> nodeData : nodeDataList) {
            try {
                NodeExecutionInfo nodeInfo = parseNodeData(nodeData);
                nodes.put(nodeInfo.nodeKey(), nodeInfo);
            } catch (Exception e) {
                log.error("解析节点信息失败: nodeData={}", nodeData, e);
                throw new FlowSnapshotParseException("解析节点信息失败", e);
            }
        }

        return nodes;
    }

    /**
     * 解析单个节点数据
     */
    private NodeExecutionInfo parseNodeData(Map<String, Object> nodeData) {
        Long nodeId = ((Number) nodeData.get("id")).longValue();
        String nodeTypeCode = (String) nodeData.get("nodeTypeCode");
        String nodeKey = (String) nodeData.get("nodeKey");
        String name = (String) nodeData.get("name");
        String configJson = (String) nodeData.get("configJson");
        String retryPolicyJson = (String) nodeData.get("retryPolicyJson");

        // 验证必需字段
        if (nodeTypeCode == null || nodeKey == null) {
            throw new FlowSnapshotParseException(
                    "节点缺少必需字段: nodeTypeCode=" + nodeTypeCode + ", nodeKey=" + nodeKey);
        }

        return new NodeExecutionInfo(
                nodeId,
                nodeTypeCode,
                nodeKey,
                name,
                configJson,
                retryPolicyJson,
                new HashMap<>() // 执行上下文在运行时填充
        );
    }

    /**
     * 验证快照数据的有效性
     */
    private static void validateSnapshotData(Long definitionId, String name, Integer version, String snapshotJson) {
        if (definitionId == null) {
            throw new IllegalArgumentException("流程定义ID不能为空");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("快照名称不能为空");
        }

        if (version == null || version < 1) {
            throw new IllegalArgumentException("版本号必须是正整数");
        }

        if (snapshotJson == null || snapshotJson.trim().isEmpty()) {
            throw new IllegalArgumentException("快照JSON内容不能为空");
        }

        // 基础JSON格式验证
        try {
            objectMapper.readTree(snapshotJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("快照JSON格式无效: " + e.getMessage());
        }
    }

    // =================== 内部数据结构 ===================

    /**
     * 解析后的快照数据
     *
     * <p>这个record保存了从JSON解析出来的结构化数据。使用record确保
     * 数据的不可变性，避免意外修改。</p>
     */
    private record ParsedSnapshot(
            Map<String, NodeExecutionInfo> nodes,
            List<String> executionPlan,
            Map<String, List<String>> dependencyGraph,
            Map<String, Object> metadata
    ) {
    }

    /**
     * 快照摘要信息
     *
     * <p>包含快照的关键统计信息，用于快速了解快照状态。</p>
     */
    public record SnapshotSummary(
            Long id,
            Long definitionId,
            String name,
            Integer version,
            int nodeCount,
            int executionPlanSize,
            int dependencyGraphSize,
            OffsetDateTime createdAt,
            boolean available
    ) {
    }

    /**
     * 快照解析异常
     *
     * <p>当快照JSON解析失败时抛出的专用异常。这有助于区分不同类型的错误，
     * 提供更精确的错误处理。</p>
     */
    public static class FlowSnapshotParseException extends RuntimeException {
        public FlowSnapshotParseException(String message) {
            super(message);
        }

        public FlowSnapshotParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}