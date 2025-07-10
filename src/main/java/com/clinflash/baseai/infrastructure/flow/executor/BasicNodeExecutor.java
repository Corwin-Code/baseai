package com.clinflash.baseai.infrastructure.flow.executor;

import com.clinflash.baseai.domain.flow.model.NodeTypes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>基础节点执行器</h2>
 *
 * <p>处理START、END、INPUT、OUTPUT等基础流程控制节点的执行逻辑。
 * 这些节点虽然简单，但是流程执行的重要控制点。</p>
 */
@Component
public class BasicNodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(BasicNodeExecutor.class);

    private final ObjectMapper objectMapper;

    public BasicNodeExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<String> getSupportedNodeTypes() {
        return List.of(NodeTypes.START, NodeTypes.END, NodeTypes.INPUT, NodeTypes.OUTPUT);
    }

    @Override
    public Map<String, Object> execute(Object nodeInfo, Map<String, Object> input, Object context) {
        // 这里需要从nodeInfo中提取实际的节点信息
        // 为简化示例，假设nodeInfo有相应的方法
        String nodeTypeCode = extractNodeTypeCode(nodeInfo);
        String nodeKey = extractNodeKey(nodeInfo);
        String configJson = extractConfigJson(nodeInfo);

        log.debug("执行基础节点: type={}, key={}", nodeTypeCode, nodeKey);

        return switch (nodeTypeCode) {
            case NodeTypes.START -> executeStartNode(input, configJson);
            case NodeTypes.END -> executeEndNode(input, configJson);
            case NodeTypes.INPUT -> executeInputNode(input, configJson);
            case NodeTypes.OUTPUT -> executeOutputNode(input, configJson);
            default -> throw new IllegalArgumentException("不支持的节点类型: " + nodeTypeCode);
        };
    }

    /**
     * 执行开始节点
     */
    private Map<String, Object> executeStartNode(Map<String, Object> input, String configJson) {
        Map<String, Object> output = new HashMap<>();
        output.put("status", "started");
        output.put("timestamp", System.currentTimeMillis());
        output.putAll(input); // 传递所有输入数据

        log.debug("开始节点执行完成");
        return output;
    }

    /**
     * 执行结束节点
     */
    private Map<String, Object> executeEndNode(Map<String, Object> input, String configJson) {
        Map<String, Object> output = new HashMap<>();
        output.put("status", "completed");
        output.put("timestamp", System.currentTimeMillis());
        output.put("finalResult", input);

        log.debug("结束节点执行完成");
        return output;
    }

    /**
     * 执行输入节点
     */
    private Map<String, Object> executeInputNode(Map<String, Object> input, String configJson) {
        try {
            Map<String, Object> config = parseConfig(configJson);
            Map<String, Object> output = new HashMap<>();

            // 根据配置提取或转换输入数据
            String inputKey = (String) config.getOrDefault("inputKey", "data");
            Object inputData = input.get(inputKey);

            output.put("inputData", inputData);
            output.put("timestamp", System.currentTimeMillis());

            log.debug("输入节点执行完成: inputKey={}", inputKey);
            return output;

        } catch (Exception e) {
            log.error("输入节点执行失败", e);
            throw new RuntimeException("输入节点执行失败: " + e.getMessage());
        }
    }

    /**
     * 执行输出节点
     */
    private Map<String, Object> executeOutputNode(Map<String, Object> input, String configJson) {
        try {
            Map<String, Object> config = parseConfig(configJson);
            Map<String, Object> output = new HashMap<>();

            // 根据配置格式化输出数据
            String outputFormat = (String) config.getOrDefault("format", "json");
            String outputKey = (String) config.getOrDefault("outputKey", "result");

            Object outputData = input.get(outputKey);
            output.put("outputData", outputData);
            output.put("format", outputFormat);
            output.put("timestamp", System.currentTimeMillis());

            log.debug("输出节点执行完成: format={}, outputKey={}", outputFormat, outputKey);
            return output;

        } catch (Exception e) {
            log.error("输出节点执行失败", e);
            throw new RuntimeException("输出节点执行失败: " + e.getMessage());
        }
    }

    /**
     * 解析配置JSON
     */
    private Map<String, Object> parseConfig(String configJson) {
        if (configJson == null || configJson.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("配置JSON解析失败，使用默认配置: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    // 这些方法需要根据实际的nodeInfo对象类型来实现
    private String extractNodeTypeCode(Object nodeInfo) {
        // 简化实现，实际应该从nodeInfo对象中提取
        return "START"; // 占位实现
    }

    private String extractNodeKey(Object nodeInfo) {
        return "start_node"; // 占位实现
    }

    private String extractConfigJson(Object nodeInfo) {
        return "{}"; // 占位实现
    }
}