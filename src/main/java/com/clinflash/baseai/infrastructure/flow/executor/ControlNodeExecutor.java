//package com.clinflash.baseai.infrastructure.flow.executor;
//
//import com.clinflash.baseai.domain.flow.model.NodeTypes;
//import com.clinflash.baseai.domain.flow.service.FlowExecutionService;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Component;
//
//import javax.script.ScriptEngine;
//import javax.script.ScriptEngineManager;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * <h2>流程控制节点执行器</h2>
// *
// * <p>处理条件判断、循环、并行等流程控制节点。
// * 这些节点决定了流程的执行路径。</p>
// */
//@Component
//public class ControlNodeExecutor implements NodeExecutor {
//
//    private static final Logger log = LoggerFactory.getLogger(ControlNodeExecutor.class);
//
//    private final ObjectMapper objectMapper;
//    private final ScriptEngine scriptEngine;
//
//    public ControlNodeExecutor(ObjectMapper objectMapper) {
//        this.objectMapper = objectMapper;
//        // 初始化JavaScript引擎用于条件表达式求值
//        ScriptEngineManager manager = new ScriptEngineManager();
//        this.scriptEngine = manager.getEngineByName("JavaScript");
//    }
//
//    @Override
//    public List<String> getSupportedTypes() {
//        return List.of(
//                NodeTypes.CONDITION,
//                NodeTypes.LOOP,
//                NodeTypes.SWITCH,
//                NodeTypes.PARALLEL
//        );
//    }
//
//    @Override
//    public Map<String, Object> execute(FlowExecutionService.FlowGraph.Node node,
//                                       Map<String, Object> input,
//                                       FlowExecutionService.ExecutionContext context) {
//        log.debug("执行控制节点: type={}, key={}", node.type(), node.key());
//
//        return switch (node.type()) {
//            case NodeTypes.CONDITION -> executeCondition(node, input, context);
//            case NodeTypes.LOOP -> executeLoop(node, input, context);
//            case NodeTypes.SWITCH -> executeSwitch(node, input, context);
//            case NodeTypes.PARALLEL -> executeParallel(node, input, context);
//            default -> throw new IllegalArgumentException("不支持的节点类型: " + node.type());
//        };
//    }
//
//    /**
//     * 执行条件判断节点
//     *
//     * <p>根据条件表达式的结果决定执行路径。
//     * 输出中包含_condition_result字段，供后续边判断使用。</p>
//     */
//    private Map<String, Object> executeCondition(FlowExecutionService.FlowGraph.Node node,
//                                                 Map<String, Object> input,
//                                                 FlowExecutionService.ExecutionContext context) {
//        JsonNode config = node.config();
//        if (config == null || !config.has("expression")) {
//            throw new IllegalArgumentException("条件节点需要配置expression");
//        }
//
//        String expression = config.get("expression").asText();
//
//        try {
//            // 将输入数据注入到脚本引擎
//            for (Map.Entry<String, Object> entry : input.entrySet()) {
//                scriptEngine.put(entry.getKey(), entry.getValue());
//            }
//
//            // 执行表达式
//            Object result = scriptEngine.eval(expression);
//            boolean conditionResult = Boolean.TRUE.equals(result);
//
//            log.info("条件判断: expression={}, result={}", expression, conditionResult);
//
//            // 输出包含条件结果
//            Map<String, Object> output = new HashMap<>(input);
//            output.put("_condition_result", conditionResult);
//
//            return output;
//
//        } catch (Exception e) {
//            throw new RuntimeException("条件表达式执行失败: " + expression, e);
//        }
//    }
//
//    /**
//     * 执行循环节点
//     *
//     * <p>对数组或集合进行迭代处理。
//     * 注意：当前是同步执行，实际可能需要控制并发。</p>
//     */
//    private Map<String, Object> executeLoop(FlowExecutionService.FlowGraph.Node node,
//                                            Map<String, Object> input,
//                                            FlowExecutionService.ExecutionContext context) {
//        JsonNode config = node.config();
//        if (config == null || !config.has("items")) {
//            throw new IllegalArgumentException("循环节点需要配置items");
//        }
//
//        String itemsKey = config.get("items").asText();
//        Object itemsObj = input.get(itemsKey);
//
//        if (!(itemsObj instanceof List)) {
//            throw new IllegalArgumentException("循环节点的items必须是数组");
//        }
//
//        @SuppressWarnings("unchecked")
//        List<Object> items = (List<Object>) itemsObj;
//
//        // 获取循环变量名
//        String loopVar = config.has("loopVar") ?
//                config.get("loopVar").asText() : "item";
//
//        // 收集每次迭代的结果
//        List<Object> results = new java.util.ArrayList<>();
//
//        for (int i = 0; i < items.size(); i++) {
//            Object item = items.get(i);
//
//            // 为每次迭代创建新的输入
//            Map<String, Object> loopInput = new HashMap<>(input);
//            loopInput.put(loopVar, item);
//            loopInput.put("_loop_index", i);
//
//            // TODO: 这里应该递归执行子流程
//            // 当前简化处理，直接将item加入结果
//            results.add(item);
//        }
//
//        Map<String, Object> output = new HashMap<>(input);
//        output.put("_loop_results", results);
//
//        return output;
//    }
//
//    /**
//     * 执行分支选择节点
//     *
//     * <p>根据条件选择不同的执行分支。
//     * 类似于switch-case语句。</p>
//     */
//    private Map<String, Object> executeSwitch(FlowExecutionService.FlowGraph.Node node,
//                                              Map<String, Object> input,
//                                              FlowExecutionService.ExecutionContext context) {
//        JsonNode config = node.config();
//        if (config == null || !config.has("switchOn")) {
//            throw new IllegalArgumentException("分支节点需要配置switchOn");
//        }
//
//        String switchOnKey = config.get("switchOn").asText();
//        Object switchValue = input.get(switchOnKey);
//
//        // 查找匹配的分支
//        String selectedBranch = "default";
//
//        if (config.has("branches")) {
//            JsonNode branches = config.get("branches");
//            for (JsonNode branch : branches) {
//                String value = branch.get("value").asText();
//                if (value.equals(String.valueOf(switchValue))) {
//                    selectedBranch = branch.get("name").asText();
//                    break;
//                }
//            }
//        }
//
//        log.info("分支选择: switchOn={}, value={}, branch={}",
//                switchOnKey, switchValue, selectedBranch);
//
//        Map<String, Object> output = new HashMap<>(input);
//        output.put("_selected_branch", selectedBranch);
//
//        return output;
//    }
//
//    /**
//     * 执行并行节点
//     *
//     * <p>并行执行多个子任务。
//     * 注意：当前实现是串行的，真正的并行需要异步支持。</p>
//     */
//    private Map<String, Object> executeParallel(FlowExecutionService.FlowGraph.Node node,
//                                                Map<String, Object> input,
//                                                FlowExecutionService.ExecutionContext context) {
//        JsonNode config = node.config();
//
//        // TODO: 实现真正的并行执行
//        // 当前简化实现，标记为并行节点
//        Map<String, Object> output = new HashMap<>(input);
//        output.put("_parallel_execution", true);
//
//        return output;
//    }
//}