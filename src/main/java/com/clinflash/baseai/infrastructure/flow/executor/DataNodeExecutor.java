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
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * <h2>数据处理节点执行器</h2>
// *
// * <p>处理数据转换、过滤、验证等节点。
// * 这些节点负责流程中的数据处理和转换。</p>
// */
//@Component
//public class DataNodeExecutor implements NodeExecutor {
//
//    private static final Logger log = LoggerFactory.getLogger(DataNodeExecutor.class);
//
//    private final ObjectMapper objectMapper;
//
//    public DataNodeExecutor(ObjectMapper objectMapper) {
//        this.objectMapper = objectMapper;
//    }
//
//    @Override
//    public List<String> getSupportedTypes() {
//        return List.of(
//                NodeTypes.MAPPER,
//                NodeTypes.FILTER,
//                NodeTypes.VALIDATOR,
//                NodeTypes.SPLITTER,
//                NodeTypes.MERGER
//        );
//    }
//
//    @Override
//    public Map<String, Object> execute(FlowExecutionService.FlowGraph.Node node,
//                                       Map<String, Object> input,
//                                       FlowExecutionService.ExecutionContext context) {
//        log.debug("执行数据处理节点: type={}, key={}", node.type(), node.key());
//
//        return switch (node.type()) {
//            case NodeTypes.MAPPER -> executeMapper(node, input, context);
//            case NodeTypes.FILTER -> executeFilter(node, input, context);
//            case NodeTypes.VALIDATOR -> executeValidator(node, input, context);
//            case NodeTypes.SPLITTER -> executeSplitter(node, input, context);
//            case NodeTypes.MERGER -> executeMerger(node, input, context);
//            default -> throw new IllegalArgumentException("不支持的节点类型: " + node.type());
//        };
//    }
//
//    /**
//     * 执行数据映射节点
//     *
//     * <p>根据映射规则转换数据结构。</p>
//     */
//    private Map<String, Object> executeMapper(FlowExecutionService.FlowGraph.Node node,
//                                              Map<String, Object> input,
//                                              FlowExecutionService.ExecutionContext context) {
//        JsonNode config = node.config();
//        if (config == null || !config.has("mapping")) {
//            throw new IllegalArgumentException("映射节点需要配置mapping");
//        }
//
//        Map<String, Object> output = new HashMap<>();
//
//        // 应用映射规则
//        JsonNode mapping = config.get("mapping");
//        mapping.fields().forEachRemaining(entry -> {
//            String targetKey = entry.getKey();
//            JsonNode mappingRule = entry.getValue();
//
//            if (mappingRule.isTextual()) {
//                // 简单映射：直接从输入复制
//                String sourceKey = mappingRule.asText();
//                if (input.containsKey(sourceKey)) {
//                    output.put(targetKey, input.get(sourceKey));
//                }
//            } else if (mappingRule.isObject()) {
//                // 复杂映射：可能包含转换规则
//                if (mappingRule.has("source")) {
//                    String sourceKey = mappingRule.get("source").asText();
//                    Object value = input.get(sourceKey);
//
//                    // 应用转换
//                    if (mappingRule.has("transform")) {
//                        String transform = mappingRule.get("transform").asText();
//                        value = applyTransform(value, transform);
//                    }
//
//                    output.put(targetKey, value);
//                }
//            }
//        });
//
//        // 保留未映射的字段（如果配置了）
//        if (config.has("preserveUnmapped") && config.get("preserveUnmapped").asBoolean()) {
//            input.forEach((key, value) -> {
//                if (!output.containsKey(key)) {
//                    output.put(key, value);
//                }
//            });
//        }
//
//        return output;
//    }
//
//    /**
//     * 执行数据过滤节点
//     *
//     * <p>根据条件过滤数据。</p>
//     */
//    private Map<String, Object> executeFilter(FlowExecutionService.FlowGraph.Node node,
//                                              Map<String, Object> input,
//                                              FlowExecutionService.ExecutionContext context) {
//        JsonNode config = node.config();
//        if (config == null || !config.has("items")) {
//            throw new IllegalArgumentException("过滤节点需要配置items");
//        }
//
//        String itemsKey = config.get("items").asText();
//        Object itemsObj = input.get(itemsKey);
//
//        if (!(itemsObj instanceof List)) {
//            throw new IllegalArgumentException("过滤节点的items必须是数组");
//        }
//
//        @SuppressWarnings("unchecked")
//        List<Object> items = (List<Object>) itemsObj;
//
//        // 获取过滤条件
//        JsonNode condition = config.get("condition");
//
//        // 执行过滤
//        List<Object> filtered = items.stream()
//                .filter(item -> evaluateCondition(item, condition))
//                .collect(Collectors.toList());
//
//        Map<String, Object> output = new HashMap<>(input);
//        output.put(itemsKey, filtered);
//        output.put("_filtered_count", filtered.size());
//
//        log.info("数据过滤完成: 原始数量={}, 过滤后={}", items.size(), filtered.size());
//
//        return output;
//    }
//
//    /**
//     * 执行数据验证节点
//     *
//     * <p>验证数据是否符合规则。</p>
//     */
//    private Map<String, Object> executeValidator(FlowExecutionService.FlowGraph.Node node,
//                                                 Map<String, Object> input,
//                                                 FlowExecutionService.ExecutionContext context) {
//        JsonNode config = node.config();
//        if (config == null || !config.has("rules")) {
//            throw new IllegalArgumentException("验证节点需要配置rules");
//        }
//
//        List<ValidationError> errors = new ArrayList<>();
//
//        // 执行验证规则
//        JsonNode rules = config.get("rules");
//        rules.forEach(rule -> {
//            String field = rule.get("field").asText();
//            String type = rule.get("type").asText();
//
//            Object value = input.get(field);
//
//            switch (type) {
//                case "required":
//                    if (value == null || (value instanceof String && ((String) value).isEmpty())) {
//                        errors.add(new ValidationError(field, "字段不能为空"));
//                    }
//                    break;
//
//                case "minLength":
//                    int minLength = rule.get("value").asInt();
//                    if (value instanceof String && ((String) value).length() < minLength) {
//                        errors.add(new ValidationError(field, "长度不能小于" + minLength));
//                    }
//                    break;
//
//                case "pattern":
//                    String pattern = rule.get("value").asText();
//                    if (value instanceof String && !((String) value).matches(pattern)) {
//                        errors.add(new ValidationError(field, "格式不正确"));
//                    }
//                    break;
//
//                // 添加更多验证类型...
//            }
//        });
//
//        Map<String, Object> output = new HashMap<>(input);
//        output.put("_validation_errors", errors);
//        output.put("_validation_passed", errors.isEmpty());
//
//        if (!errors.isEmpty()) {
//            log.warn("数据验证失败: errors={}", errors);
//        }
//
//        return output;
//    }
//
//    /**
//     * 执行文本分割节点
//     *
//     * <p>将文本分割成多个部分。</p>
//     */
//    private Map<String, Object> executeSplitter(FlowExecutionService.FlowGraph.Node node,
//                                                Map<String, Object> input,
//                                                FlowExecutionService.ExecutionContext context) {
//        JsonNode config = node.config();
//        if (config == null || !config.has("text")) {
//            throw new IllegalArgumentException("分割节点需要配置text");
//        }
//
//        String textKey = config.get("text").asText();
//        String text = (String) input.get(textKey);
//
//        if (text == null) {
//            throw new IllegalArgumentException("找不到要分割的文本");
//        }
//
//        // 获取分割参数
//        String delimiter = config.has("delimiter") ?
//                config.get("delimiter").asText() : "\n";
//        int maxLength = config.has("maxLength") ?
//                config.get("maxLength").asInt() : 1000;
//
//        // 执行分割
//        List<String> parts = new ArrayList<>();
//
//        if (delimiter != null && !delimiter.isEmpty()) {
//            // 按分隔符分割
//            String[] split = text.split(delimiter);
//            parts.addAll(Arrays.asList(split));
//        } else {
//            // 按长度分割
//            for (int i = 0; i < text.length(); i += maxLength) {
//                int end = Math.min(i + maxLength, text.length());
//                parts.add(text.substring(i, end));
//            }
//        }
//
//        Map<String, Object> output = new HashMap<>(input);
//        output.put("parts", parts);
//        output.put("partCount", parts.size());
//
//        return output;
//    }
//
//    /**
//     * 执行数据合并节点
//     *
//     * <p>合并多个数据源。</p>
//     */
//    private Map<String, Object> executeMerger(FlowExecutionService.FlowGraph.Node node,
//                                              Map<String, Object> input,
//                                              FlowExecutionService.ExecutionContext context) {
//        JsonNode config = node.config();
//        if (config == null || !config.has("sources")) {
//            throw new IllegalArgumentException("合并节点需要配置sources");
//        }
//
//        Map<String, Object> output = new HashMap<>();
//
//        // 合并指定的数据源
//        JsonNode sources = config.get("sources");
//        sources.forEach(source -> {
//            String key = source.asText();
//            if (input.containsKey(key)) {
//                Object value = input.get(key);
//
//                if (value instanceof Map) {
//                    // 合并Map
//                    @SuppressWarnings("unchecked")
//                    Map<String, Object> map = (Map<String, Object>) value;
//                    output.putAll(map);
//                } else {
//                    // 直接添加
//                    output.put(key, value);
//                }
//            }
//        });
//
//        return output;
//    }
//
//    /**
//     * 应用数据转换
//     */
//    private Object applyTransform(Object value, String transform) {
//        if (value == null) return null;
//
//        return switch (transform) {
//            case "uppercase" -> value instanceof String ?
//                    ((String) value).toUpperCase() : value;
//            case "lowercase" -> value instanceof String ?
//                    ((String) value).toLowerCase() : value;
//            case "trim" -> value instanceof String ?
//                    ((String) value).trim() : value;
//            case "toString" -> String.valueOf(value);
//            case "toNumber" -> value instanceof String ?
//                    Double.parseDouble((String) value) : value;
//            default -> value;
//        };
//    }
//
//    /**
//     * 评估过滤条件
//     */
//    private boolean evaluateCondition(Object item, JsonNode condition) {
//        // 简化的条件评估逻辑
//        // 实际应该支持更复杂的条件表达式
//        if (condition == null || !condition.has("field")) {
//            return true;
//        }
//
//        String field = condition.get("field").asText();
//        String operator = condition.get("operator").asText();
//        JsonNode value = condition.get("value");
//
//        // TODO: 实现完整的条件评估逻辑
//
//        return true;
//    }
//
//    /**
//     * 验证错误
//     */
//    private record ValidationError(String field, String message) {
//    }
//}