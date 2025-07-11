package com.clinflash.baseai.infrastructure.flow.executor;

import com.clinflash.baseai.domain.flow.model.NodeTypes;
import com.clinflash.baseai.infrastructure.flow.model.FlowExecutionContext;
import com.clinflash.baseai.infrastructure.flow.model.NodeExecutionInfo;
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
 * <p>这个执行器负责处理流程的基础控制节点，它们虽然功能简单，但却是流程执行的重要支撑。
 * 就像建筑的地基和框架，这些基础节点为整个流程提供了结构性支持。</p>
 *
 * <p><b>支持的基础节点：</b></p>
 * <ul>
 * <li><b>START：</b>流程入口，初始化执行环境</li>
 * <li><b>END：</b>流程出口，整理最终结果</li>
 * <li><b>INPUT：</b>数据输入处理和验证</li>
 * <li><b>OUTPUT：</b>数据输出格式化和传递</li>
 * </ul>
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
    public Map<String, Object> execute(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                       FlowExecutionContext context) {
        log.debug("执行基础节点: type={}, key={}", nodeInfo.nodeTypeCode(), nodeInfo.nodeKey());

        return switch (nodeInfo.nodeTypeCode()) {
            case NodeTypes.START -> executeStartNode(nodeInfo, input, context);
            case NodeTypes.END -> executeEndNode(nodeInfo, input, context);
            case NodeTypes.INPUT -> executeInputNode(nodeInfo, input, context);
            case NodeTypes.OUTPUT -> executeOutputNode(nodeInfo, input, context);
            default -> throw new IllegalArgumentException("不支持的基础节点类型: " + nodeInfo.nodeTypeCode());
        };
    }

    /**
     * 执行开始节点
     *
     * <p>开始节点是流程的入口点，负责初始化执行环境和验证输入数据。
     * 它就像一个接待员，为后续的处理流程做好准备工作。</p>
     */
    private Map<String, Object> executeStartNode(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                                 FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());

            Map<String, Object> output = new HashMap<>();
            output.put("_node_type", "START");
            output.put("_execution_id", context.getRunId());
            output.put("_started_at", System.currentTimeMillis());

            // 将初始输入数据传递下去
            output.putAll(input);

            // 如果配置了初始化变量，设置到全局变量中
            if (config.containsKey("initVariables")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> initVars = (Map<String, Object>) config.get("initVariables");
                for (Map.Entry<String, Object> entry : initVars.entrySet()) {
                    context.setGlobalVariable(entry.getKey(), entry.getValue());
                }
            }

            log.debug("开始节点执行完成: runId={}", context.getRunId());
            return output;

        } catch (Exception e) {
            log.error("开始节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("开始节点执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行结束节点
     *
     * <p>结束节点是流程的出口点，负责整理最终结果和清理资源。
     * 它就像一个总结者，将整个流程的执行结果进行最后的整理。</p>
     */
    private Map<String, Object> executeEndNode(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                               FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());

            Map<String, Object> output = new HashMap<>();
            output.put("_node_type", "END");
            output.put("_execution_id", context.getRunId());
            output.put("_finished_at", System.currentTimeMillis());

            // 根据配置决定输出什么数据
            if (config.containsKey("outputFields")) {
                @SuppressWarnings("unchecked")
                List<String> outputFields = (List<String>) config.get("outputFields");

                // 只输出指定的字段
                for (String field : outputFields) {
                    if (input.containsKey(field)) {
                        output.put(field, input.get(field));
                    }
                }
            } else {
                // 输出所有输入数据
                output.putAll(input);
            }

            // 添加执行统计信息
            output.put("_execution_metrics", context.getExecutionMetrics());

            log.debug("结束节点执行完成: runId={}", context.getRunId());
            return output;

        } catch (Exception e) {
            log.error("结束节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("结束节点执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行输入节点
     *
     * <p>输入节点负责处理和验证外部输入数据，确保数据格式正确且符合业务规则。
     * 它就像一个质检员，对进入流程的数据进行检查和预处理。</p>
     */
    private Map<String, Object> executeInputNode(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                                 FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());
            Map<String, Object> output = new HashMap<>();

            // 获取输入字段配置
            String sourceKey = (String) config.getOrDefault("sourceKey", "data");
            String targetKey = (String) config.getOrDefault("targetKey", "input");

            // 提取输入数据
            Object inputData = input.get(sourceKey);

            // 数据验证
            if (config.containsKey("required") && (Boolean) config.get("required")) {
                if (inputData == null) {
                    throw new IllegalArgumentException("必需的输入数据不能为空: " + sourceKey);
                }
            }

            // 数据类型转换
            if (config.containsKey("dataType")) {
                String dataType = (String) config.get("dataType");
                inputData = convertDataType(inputData, dataType);
            }

            // 保存处理后的数据
            output.put(targetKey, inputData);
            output.put("_input_processed", true);
            output.put("_timestamp", System.currentTimeMillis());

            log.debug("输入节点执行完成: sourceKey={}, targetKey={}", sourceKey, targetKey);
            return output;

        } catch (Exception e) {
            log.error("输入节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("输入节点执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行输出节点
     *
     * <p>输出节点负责格式化和传递执行结果，确保下游系统能够正确理解和处理数据。
     * 它就像一个翻译员，将内部数据格式转换为外部可理解的格式。</p>
     */
    private Map<String, Object> executeOutputNode(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                                  FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());
            Map<String, Object> output = new HashMap<>();

            // 获取输出配置
            String sourceKey = (String) config.getOrDefault("sourceKey", "result");
            String outputFormat = (String) config.getOrDefault("format", "json");

            // 提取要输出的数据
            Object outputData = input.get(sourceKey);

            // 格式化输出数据
            Object formattedData = formatOutputData(outputData, outputFormat);

            output.put("outputData", formattedData);
            output.put("format", outputFormat);
            output.put("_output_processed", true);
            output.put("_timestamp", System.currentTimeMillis());

            log.debug("输出节点执行完成: format={}, sourceKey={}", outputFormat, sourceKey);
            return output;

        } catch (Exception e) {
            log.error("输出节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("输出节点执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void validateConfig(String configJson) {
        try {
            parseConfig(configJson);
            // 基础节点的配置比较简单，主要验证JSON格式即可
        } catch (Exception e) {
            throw new IllegalArgumentException("基础节点配置验证失败: " + e.getMessage());
        }
    }

    // =================== 私有辅助方法 ===================

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

    /**
     * 数据类型转换
     */
    private Object convertDataType(Object data, String targetType) {
        if (data == null) return null;

        try {
            return switch (targetType.toLowerCase()) {
                case "string" -> String.valueOf(data);
                case "integer" ->
                        data instanceof Number ? ((Number) data).intValue() : Integer.parseInt(String.valueOf(data));
                case "double" ->
                        data instanceof Number ? ((Number) data).doubleValue() : Double.parseDouble(String.valueOf(data));
                case "boolean" -> data instanceof Boolean ? data : Boolean.parseBoolean(String.valueOf(data));
                default -> data; // 不支持的类型，返回原始数据
            };
        } catch (Exception e) {
            log.warn("数据类型转换失败: {} -> {}", data, targetType);
            return data; // 转换失败，返回原始数据
        }
    }

    /**
     * 格式化输出数据
     */
    private Object formatOutputData(Object data, String format) {
        if (data == null) return null;

        try {
            return switch (format.toLowerCase()) {
                case "json" -> objectMapper.writeValueAsString(data);
                case "string" -> String.valueOf(data);
                case "raw" -> data;
                default -> data; // 不支持的格式，返回原始数据
            };
        } catch (Exception e) {
            log.warn("输出数据格式化失败: format={}", format);
            return data; // 格式化失败，返回原始数据
        }
    }
}