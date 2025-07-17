package com.cloud.baseai.infrastructure.flow.executor;

import com.cloud.baseai.domain.flow.model.NodeTypes;
import com.cloud.baseai.infrastructure.flow.model.FlowExecutionContext;
import com.cloud.baseai.infrastructure.flow.model.NodeExecutionInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>流程控制节点执行器</h2>
 *
 * <p>这个执行器是流程的"交通指挥官"，负责控制数据在流程中的流向和处理逻辑。
 * 它实现了条件分支、循环处理、并行执行等高级控制结构，
 * 让简单的线性流程变成复杂的智能决策系统。</p>
 *
 * <p><b>支持的控制结构：</b></p>
 * <ul>
 * <li><b>条件判断：</b>根据数据或规则决定执行路径</li>
 * <li><b>循环处理：</b>对数组或条件进行重复处理</li>
 * <li><b>分支选择：</b>多条件路由，类似switch语句</li>
 * <li><b>并行执行：</b>同时处理多个任务，提高效率</li>
 * </ul>
 */
@Component
public class ControlNodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(ControlNodeExecutor.class);

    private final ObjectMapper objectMapper;
    private final ScriptEngine scriptEngine;

    public ControlNodeExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // 初始化JavaScript引擎用于条件表达式求值
        ScriptEngineManager manager = new ScriptEngineManager();
        this.scriptEngine = manager.getEngineByName("JavaScript");

        if (scriptEngine == null) {
            log.warn("JavaScript引擎不可用，条件表达式功能将受限");
        }
    }

    @Override
    public List<String> getSupportedNodeTypes() {
        return List.of(NodeTypes.CONDITION, NodeTypes.LOOP, NodeTypes.SWITCH, NodeTypes.PARALLEL);
    }

    @Override
    public Map<String, Object> execute(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                       FlowExecutionContext context) {
        log.debug("执行控制节点: type={}, key={}", nodeInfo.nodeTypeCode(), nodeInfo.nodeKey());

        return switch (nodeInfo.nodeTypeCode()) {
            case NodeTypes.CONDITION -> executeCondition(nodeInfo, input, context);
            case NodeTypes.LOOP -> executeLoop(nodeInfo, input, context);
            case NodeTypes.SWITCH -> executeSwitch(nodeInfo, input, context);
            case NodeTypes.PARALLEL -> executeParallel(nodeInfo, input, context);
            default -> throw new IllegalArgumentException("不支持的控制节点类型: " + nodeInfo.nodeTypeCode());
        };
    }

    /**
     * 执行条件判断节点
     *
     * <p>条件节点是流程分支的核心，它根据数据或业务规则来决定后续的执行路径。
     * 就像十字路口的红绿灯，根据当前状况指引数据流向不同的方向。</p>
     */
    private Map<String, Object> executeCondition(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                                 FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());
            String expression = (String) config.get("expression");

            if (expression == null || expression.trim().isEmpty()) {
                throw new IllegalArgumentException("条件节点需要配置expression参数");
            }

            boolean conditionResult = evaluateCondition(expression, input, context);

            Map<String, Object> output = new HashMap<>(input);
            output.put("_condition_result", conditionResult);
            output.put("_condition_expression", expression);
            output.put("_evaluated_at", System.currentTimeMillis());

            log.info("条件判断完成: expression={}, result={}", expression, conditionResult);
            return output;

        } catch (Exception e) {
            log.error("条件节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("条件节点执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行循环节点
     *
     * <p>循环节点能够对数组进行迭代处理，或者根据条件进行重复执行。
     * 它就像一个勤奋的工人，按照指定的规则重复执行相同的任务。</p>
     */
    private Map<String, Object> executeLoop(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                            FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());
            String loopType = (String) config.getOrDefault("loopType", "forEach");

            Map<String, Object> output = new HashMap<>(input);

            switch (loopType) {
                case "forEach" -> executeForEachLoop(config, input, output, context);
                case "while" -> executeWhileLoop(config, input, output, context);
                case "range" -> executeRangeLoop(config, input, output, context);
                default -> throw new IllegalArgumentException("不支持的循环类型: " + loopType);
            }

            return output;

        } catch (Exception e) {
            log.error("循环节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("循环节点执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行分支选择节点
     *
     * <p>分支节点实现了多路选择逻辑，类似于编程语言中的switch语句。
     * 它根据输入值选择对应的执行分支，提供了比简单条件判断更丰富的控制能力。</p>
     */
    private Map<String, Object> executeSwitch(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                              FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());
            String switchKey = (String) config.get("switchOn");

            if (switchKey == null) {
                throw new IllegalArgumentException("分支节点需要配置switchOn参数");
            }

            Object switchValue = input.get(switchKey);
            String selectedBranch = "default";

            // 查找匹配的分支
            if (config.containsKey("branches")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> branches = (List<Map<String, Object>>) config.get("branches");

                for (Map<String, Object> branch : branches) {
                    String branchValue = String.valueOf(branch.get("value"));
                    if (branchValue.equals(String.valueOf(switchValue))) {
                        selectedBranch = (String) branch.get("name");
                        break;
                    }
                }
            }

            Map<String, Object> output = new HashMap<>(input);
            output.put("_selected_branch", selectedBranch);
            output.put("_switch_value", switchValue);
            output.put("_switch_key", switchKey);

            log.info("分支选择完成: switchOn={}, value={}, branch={}", switchKey, switchValue, selectedBranch);
            return output;

        } catch (Exception e) {
            log.error("分支节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("分支节点执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行并行节点
     *
     * <p>并行节点标记数据可以进行并行处理，为后续的并行执行做准备。
     * 注意：真正的并行执行需要在流程引擎层面实现，这里主要是标记和配置。</p>
     */
    private Map<String, Object> executeParallel(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                                FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());

            Map<String, Object> output = new HashMap<>(input);
            output.put("_parallel_execution", true);
            output.put("_parallel_config", config);
            output.put("_parallel_node", nodeInfo.nodeKey());

            log.debug("并行节点标记完成: nodeKey={}", nodeInfo.nodeKey());
            return output;

        } catch (Exception e) {
            log.error("并行节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("并行节点执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void validateConfig(String configJson) {
        try {
            Map<String, Object> config = parseConfig(configJson);

            // 根据不同的控制节点类型进行验证
            if (config.containsKey("expression") && scriptEngine == null) {
                log.warn("JavaScript引擎不可用，条件表达式可能无法正常工作");
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("控制节点配置验证失败: " + e.getMessage());
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(String expression, Map<String, Object> input, FlowExecutionContext context) {
        if (scriptEngine == null) {
            log.warn("JavaScript引擎不可用，条件表达式使用简化评估");
            return evaluateSimpleCondition(expression, input);
        }

        try {
            // 将输入数据注入到脚本引擎
            for (Map.Entry<String, Object> entry : input.entrySet()) {
                scriptEngine.put(entry.getKey(), entry.getValue());
            }

            // 注入上下文变量
            for (Map.Entry<String, Object> entry : context.getAllGlobalVariables().entrySet()) {
                scriptEngine.put("ctx_" + entry.getKey(), entry.getValue());
            }

            // 执行表达式
            Object result = scriptEngine.eval(expression);
            return Boolean.TRUE.equals(result);

        } catch (Exception e) {
            log.error("条件表达式执行失败: {}", expression, e);
            throw new RuntimeException("条件表达式执行失败: " + expression, e);
        }
    }

    /**
     * 简化的条件评估（当JavaScript引擎不可用时）
     */
    private boolean evaluateSimpleCondition(String expression, Map<String, Object> input) {
        // 实现一些基本的条件判断
        if (expression.contains("==")) {
            String[] parts = expression.split("==");
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim().replace("\"", "").replace("'", "");
                Object leftValue = input.get(left);
                return right.equals(String.valueOf(leftValue));
            }
        }

        // 默认返回true
        log.warn("无法解析条件表达式，默认返回true: {}", expression);
        return true;
    }

    /**
     * 执行forEach循环
     */
    private void executeForEachLoop(Map<String, Object> config, Map<String, Object> input,
                                    Map<String, Object> output, FlowExecutionContext context) {
        String itemsKey = (String) config.get("items");
        Object itemsObj = input.get(itemsKey);

        if (!(itemsObj instanceof List)) {
            throw new IllegalArgumentException("forEach循环的items必须是数组");
        }

        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) itemsObj;
        String loopVar = (String) config.getOrDefault("loopVar", "item");

        List<Object> results = new java.util.ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);

            // 为每次迭代创建上下文
            Map<String, Object> loopContext = new HashMap<>();
            loopContext.put(loopVar, item);
            loopContext.put("_loop_index", i);
            loopContext.put("_loop_total", items.size());

            // 这里应该触发子流程执行，暂时简化处理
            results.add(item);
        }

        output.put("_loop_results", results);
        output.put("_loop_count", results.size());
    }

    /**
     * 执行while循环
     */
    private void executeWhileLoop(Map<String, Object> config, Map<String, Object> input,
                                  Map<String, Object> output, FlowExecutionContext context) {
        String condition = (String) config.get("condition");
        Integer maxIterations = (Integer) config.getOrDefault("maxIterations", 100);

        List<Object> results = new java.util.ArrayList<>();
        int iterations = 0;

        while (iterations < maxIterations && evaluateCondition(condition, input, context)) {
            // 这里应该触发子流程执行，暂时简化处理
            results.add(Map.of("iteration", iterations, "timestamp", System.currentTimeMillis()));
            iterations++;

            // 防止无限循环
            if (iterations >= maxIterations) {
                log.warn("while循环达到最大迭代次数限制: {}", maxIterations);
                break;
            }
        }

        output.put("_loop_results", results);
        output.put("_loop_count", iterations);
    }

    /**
     * 执行范围循环
     */
    private void executeRangeLoop(Map<String, Object> config, Map<String, Object> input,
                                  Map<String, Object> output, FlowExecutionContext context) {
        Integer start = (Integer) config.getOrDefault("start", 0);
        Integer end = (Integer) config.get("end");
        Integer step = (Integer) config.getOrDefault("step", 1);

        if (end == null) {
            throw new IllegalArgumentException("范围循环需要配置end参数");
        }

        List<Object> results = new java.util.ArrayList<>();
        for (int i = start; i < end; i += step) {
            // 这里应该触发子流程执行，暂时简化处理
            results.add(Map.of("index", i, "value", i));
        }

        output.put("_loop_results", results);
        output.put("_loop_count", results.size());
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
}