package com.cloud.baseai.infrastructure.flow.service;

import com.cloud.baseai.domain.flow.model.NodeTypes;
import com.cloud.baseai.infrastructure.flow.model.NodeTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * <h2>节点类型服务</h2>
 *
 * <p>这个服务管理系统中所有可用的节点类型及其配置规范。它就像一个详细的说明书，
 * 告诉系统每种节点需要什么样的配置，有什么样的能力，应该如何验证。</p>
 *
 * <p><b>节点类型分类：</b></p>
 * <ul>
 * <li><b>基础节点：</b>START、END、INPUT、OUTPUT - 流程的基本控制点</li>
 * <li><b>AI节点：</b>LLM、RETRIEVER、EMBEDDER - 人工智能相关的处理节点</li>
 * <li><b>控制节点：</b>CONDITION、LOOP、PARALLEL - 流程逻辑控制</li>
 * <li><b>工具节点：</b>HTTP、TOOL、SCRIPT - 外部服务和工具集成</li>
 * <li><b>数据节点：</b>MAPPER、FILTER、VALIDATOR - 数据处理和转换</li>
 * </ul>
 */
@Service
public class NodeTypeService {

    private static final Logger log = LoggerFactory.getLogger(NodeTypeService.class);

    private final ObjectMapper objectMapper;
    private final Map<String, NodeTypeInfo> nodeTypeRegistry;

    public NodeTypeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.nodeTypeRegistry = new HashMap<>();
        initializeNodeTypes();
    }

    /**
     * 检查节点类型是否有效
     */
    public boolean isValidNodeType(String nodeTypeCode) {
        return nodeTypeRegistry.containsKey(nodeTypeCode);
    }

    /**
     * 验证节点配置
     */
    public void validateNodeConfig(String nodeTypeCode, String configJson) {
        NodeTypeInfo nodeTypeInfo = nodeTypeRegistry.get(nodeTypeCode);
        if (nodeTypeInfo == null) {
            throw new IllegalArgumentException("未知的节点类型: " + nodeTypeCode);
        }

        if (nodeTypeInfo.isConfigRequired() && (configJson == null || configJson.trim().isEmpty())) {
            throw new IllegalArgumentException("节点类型 " + nodeTypeCode + " 需要配置信息");
        }

        if (configJson != null && !configJson.trim().isEmpty()) {
            try {
                // 验证JSON格式
                objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {
                });

                // 进一步的配置验证可以在这里添加
                validateSpecificNodeConfig(nodeTypeCode, configJson);

            } catch (Exception e) {
                throw new IllegalArgumentException("节点配置JSON格式无效: " + e.getMessage());
            }
        }
    }

    /**
     * 获取节点类型信息
     */
    public NodeTypeInfo getNodeTypeInfo(String nodeTypeCode) {
        return nodeTypeRegistry.get(nodeTypeCode);
    }

    /**
     * 获取所有支持的节点类型
     */
    public Set<String> getSupportedNodeTypes() {
        return new HashSet<>(nodeTypeRegistry.keySet());
    }

    /**
     * 检查服务可用性
     */
    public boolean isAvailable() {
        return !nodeTypeRegistry.isEmpty();
    }

    /**
     * 按类别获取节点类型
     */
    public Map<String, List<NodeTypeInfo>> getNodeTypesByCategory() {
        Map<String, List<NodeTypeInfo>> categorized = new HashMap<>();

        for (NodeTypeInfo info : nodeTypeRegistry.values()) {
            categorized.computeIfAbsent(info.getCategory(), k -> new ArrayList<>()).add(info);
        }

        return categorized;
    }

    // =================== 私有初始化方法 ===================

    /**
     * 初始化所有节点类型
     */
    private void initializeNodeTypes() {
        log.info("初始化节点类型注册表");

        // 基础节点
        registerBasicNodes();

        // AI节点
        registerAINodes();

        // 控制节点
        registerControlNodes();

        // 工具节点
        registerToolNodes();

        // 数据处理节点
        registerDataNodes();

        log.info("节点类型注册完成，共注册 {} 种类型", nodeTypeRegistry.size());
    }

    /**
     * 注册基础节点
     */
    private void registerBasicNodes() {
        nodeTypeRegistry.put(NodeTypes.START, NodeTypeInfo.builder()
                .code(NodeTypes.START)
                .name("开始")
                .description("流程开始节点")
                .category("基础节点")
                .configRequired(false)
                .inputPorts(0)
                .outputPorts(1)
                .build());

        nodeTypeRegistry.put(NodeTypes.END, NodeTypeInfo.builder()
                .code(NodeTypes.END)
                .name("结束")
                .description("流程结束节点")
                .category("基础节点")
                .configRequired(false)
                .inputPorts(1)
                .outputPorts(0)
                .build());

        nodeTypeRegistry.put(NodeTypes.INPUT, NodeTypeInfo.builder()
                .code(NodeTypes.INPUT)
                .name("输入")
                .description("数据输入节点")
                .category("基础节点")
                .configRequired(true)
                .inputPorts(0)
                .outputPorts(1)
                .build());

        nodeTypeRegistry.put(NodeTypes.OUTPUT, NodeTypeInfo.builder()
                .code(NodeTypes.OUTPUT)
                .name("输出")
                .description("数据输出节点")
                .category("基础节点")
                .configRequired(true)
                .inputPorts(1)
                .outputPorts(0)
                .build());
    }

    /**
     * 注册AI节点
     */
    private void registerAINodes() {
        nodeTypeRegistry.put(NodeTypes.LLM, NodeTypeInfo.builder()
                .code(NodeTypes.LLM)
                .name("大语言模型")
                .description("调用大语言模型进行文本生成")
                .category("AI节点")
                .configRequired(true)
                .inputPorts(1)
                .outputPorts(1)
                .build());

        nodeTypeRegistry.put(NodeTypes.RETRIEVER, NodeTypeInfo.builder()
                .code(NodeTypes.RETRIEVER)
                .name("知识检索")
                .description("从知识库检索相关信息")
                .category("AI节点")
                .configRequired(true)
                .inputPorts(1)
                .outputPorts(1)
                .build());

        nodeTypeRegistry.put(NodeTypes.EMBEDDER, NodeTypeInfo.builder()
                .code(NodeTypes.EMBEDDER)
                .name("向量生成")
                .description("生成文本的向量表示")
                .category("AI节点")
                .configRequired(true)
                .inputPorts(1)
                .outputPorts(1)
                .build());

        nodeTypeRegistry.put(NodeTypes.CHAT, NodeTypeInfo.builder()
                .code(NodeTypes.CHAT)
                .name("对话")
                .description("多轮对话处理")
                .category("AI节点")
                .configRequired(true)
                .inputPorts(1)
                .outputPorts(1)
                .build());

        nodeTypeRegistry.put(NodeTypes.CLASSIFIER, NodeTypeInfo.builder()
                .code(NodeTypes.CLASSIFIER)
                .name("分类器")
                .description("文本分类和意图识别")
                .category("AI节点")
                .configRequired(true)
                .inputPorts(1)
                .outputPorts(1)
                .build());
    }

    /**
     * 注册控制节点
     */
    private void registerControlNodes() {
        nodeTypeRegistry.put(NodeTypes.CONDITION, NodeTypeInfo.builder()
                .code(NodeTypes.CONDITION)
                .name("条件判断")
                .description("基于条件进行分支控制")
                .category("控制节点")
                .configRequired(true)
                .inputPorts(1)
                .outputPorts(2)
                .build());

        nodeTypeRegistry.put(NodeTypes.LOOP, NodeTypeInfo.builder()
                .code(NodeTypes.LOOP)
                .name("循环")
                .description("重复执行子流程")
                .category("控制节点")
                .configRequired(true)
                .inputPorts(1)
                .outputPorts(2)
                .build());

        nodeTypeRegistry.put(NodeTypes.PARALLEL, NodeTypeInfo.builder()
                .code(NodeTypes.PARALLEL)
                .name("并行处理")
                .description("并行执行多个分支")
                .category("控制节点")
                .configRequired(true)
                .inputPorts(1)
                .outputPorts(1)
                .build());

        nodeTypeRegistry.put(NodeTypes.SWITCH, NodeTypeInfo.builder()
                .code(NodeTypes.SWITCH)
                .name("分支选择")
                .description("多条件分支选择")
                .category("控制节点")
                .configRequired(true)
                .inputPorts(1)
                .outputPorts(3)
                .build());
    }

    /**
     * 注册工具节点
     */
    private void registerToolNodes() {
        nodeTypeRegistry.put(NodeTypes.TOOL, NodeTypeInfo.builder()
                .code(NodeTypes.TOOL)
                .name("MCP工具")
                .description("调用MCP工具")
                .category("工具节点")
                .configRequired(true)
                .inputPorts(1)
                .outputPorts(1)
                .build());

        nodeTypeRegistry.put(NodeTypes.HTTP, NodeTypeInfo.builder()
                .code(NodeTypes.HTTP)
                .name("HTTP请求")
                .description("发送HTTP请求")
                .category("工具节点")
                .configRequired(true)
                .inputPorts(1)
                .outputPorts(1)
                .build());

        nodeTypeRegistry.put(NodeTypes.SCRIPT, NodeTypeInfo.builder()
                .code(NodeTypes.SCRIPT)
                .name("脚本执行")
                .description("执行自定义脚本")
                .category("工具节点")
                .configRequired(true)
                .inputPorts(1)
                .outputPorts(1)
                .build());
    }

    /**
     * 注册数据处理节点
     */
    private void registerDataNodes() {
        nodeTypeRegistry.put(NodeTypes.MAPPER, NodeTypeInfo.builder()
                .code(NodeTypes.MAPPER)
                .name("数据映射")
                .description("转换和映射数据结构")
                .category("数据节点")
                .configRequired(true)
                .inputPorts(1)
                .outputPorts(1)
                .build());

        nodeTypeRegistry.put(NodeTypes.FILTER, NodeTypeInfo.builder()
                .code(NodeTypes.FILTER)
                .name("数据过滤")
                .description("过滤和筛选数据")
                .category("数据节点")
                .configRequired(true)
                .inputPorts(1)
                .outputPorts(1)
                .build());

        nodeTypeRegistry.put(NodeTypes.VALIDATOR, NodeTypeInfo.builder()
                .code(NodeTypes.VALIDATOR)
                .name("数据验证")
                .description("验证数据格式和内容")
                .category("数据节点")
                .configRequired(true)
                .inputPorts(1)
                .outputPorts(2)
                .build());

        nodeTypeRegistry.put(NodeTypes.SPLITTER, NodeTypeInfo.builder()
                .code(NodeTypes.SPLITTER)
                .name("文本分割")
                .description("分割文本为多个片段")
                .category("数据节点")
                .configRequired(true)
                .inputPorts(1)
                .outputPorts(1)
                .build());

        nodeTypeRegistry.put(NodeTypes.MERGER, NodeTypeInfo.builder()
                .code(NodeTypes.MERGER)
                .name("数据合并")
                .description("合并多个数据源")
                .category("数据节点")
                .configRequired(true)
                .inputPorts(2)
                .outputPorts(1)
                .build());
    }

    /**
     * 验证特定节点类型的配置
     */
    private void validateSpecificNodeConfig(String nodeTypeCode, String configJson) {
        try {
            Map<String, Object> config = objectMapper.readValue(
                    configJson, new TypeReference<Map<String, Object>>() {
                    });

            switch (nodeTypeCode) {
                case NodeTypes.LLM:
                    validateLLMConfig(config);
                    break;
                case NodeTypes.HTTP:
                    validateHttpConfig(config);
                    break;
                case NodeTypes.CONDITION:
                    validateConditionConfig(config);
                    break;
                // 可以为其他节点类型添加特定验证
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("节点配置验证失败: " + e.getMessage());
        }
    }

    /**
     * 验证LLM节点配置
     */
    private void validateLLMConfig(Map<String, Object> config) {
        if (!config.containsKey("model")) {
            throw new IllegalArgumentException("LLM节点必须指定model参数");
        }

        if (!config.containsKey("prompt")) {
            throw new IllegalArgumentException("LLM节点必须指定prompt参数");
        }
    }

    /**
     * 验证HTTP节点配置
     */
    private void validateHttpConfig(Map<String, Object> config) {
        if (!config.containsKey("url")) {
            throw new IllegalArgumentException("HTTP节点必须指定url参数");
        }

        if (!config.containsKey("method")) {
            throw new IllegalArgumentException("HTTP节点必须指定method参数");
        }
    }

    /**
     * 验证条件节点配置
     */
    private void validateConditionConfig(Map<String, Object> config) {
        if (!config.containsKey("condition")) {
            throw new IllegalArgumentException("条件节点必须指定condition参数");
        }
    }
}