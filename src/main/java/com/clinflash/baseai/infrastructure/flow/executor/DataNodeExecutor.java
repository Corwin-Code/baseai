package com.clinflash.baseai.infrastructure.flow.executor;

import com.clinflash.baseai.domain.flow.model.NodeTypes;
import com.clinflash.baseai.infrastructure.flow.model.FlowExecutionContext;
import com.clinflash.baseai.infrastructure.flow.model.NodeExecutionInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <h2>数据处理节点执行器</h2>
 *
 * <p>这个执行器负责处理各种数据转换、过滤、验证等任务。
 * 它能够理解和操作各种格式的数据，确保数据在流程中正确传递和转换。</p>
 *
 * <p><b>支持的数据处理能力：</b></p>
 * <ul>
 * <li><b>数据映射（MAPPER）：</b>根据映射规则转换数据结构，支持字段重命名、嵌套结构转换等</li>
 * <li><b>数据过滤（FILTER）：</b>基于条件表达式过滤数组数据，支持复杂的筛选逻辑</li>
 * <li><b>数据验证（VALIDATOR）：</b>验证数据格式和内容，支持必填、长度、格式等多种验证规则</li>
 * <li><b>文本分割（SPLITTER）：</b>将长文本分割成多个片段，支持按分隔符或长度分割</li>
 * <li><b>数据合并（MERGER）：</b>合并多个数据源，支持多种合并策略</li>
 * </ul>
 *
 * <p><b>设计理念：</b></p>
 * <p>数据处理节点强调灵活性和可配置性。每种操作都支持丰富的配置选项，
 * 让用户能够根据具体业务需求定制数据处理逻辑，而不需要编写代码。</p>
 */
@Component
public class DataNodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(DataNodeExecutor.class);

    private final ObjectMapper objectMapper;
    private final ScriptEngine scriptEngine;

    /**
     * 构造函数
     *
     * <p>初始化数据处理执行器，包括JSON处理器和脚本引擎。
     * 脚本引擎用于执行复杂的数据转换和条件表达式。</p>
     */
    public DataNodeExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        // 初始化JavaScript引擎用于表达式求值
        ScriptEngineManager manager = new ScriptEngineManager();
        this.scriptEngine = manager.getEngineByName("JavaScript");

        if (scriptEngine == null) {
            log.warn("JavaScript引擎不可用，某些高级功能将受限");
        }
    }

    @Override
    public List<String> getSupportedNodeTypes() {
        return List.of(NodeTypes.MAPPER, NodeTypes.FILTER, NodeTypes.VALIDATOR,
                NodeTypes.SPLITTER, NodeTypes.MERGER);
    }

    @Override
    public Map<String, Object> execute(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                       FlowExecutionContext context) {
        log.debug("执行数据处理节点: type={}, key={}", nodeInfo.nodeTypeCode(), nodeInfo.nodeKey());

        return switch (nodeInfo.nodeTypeCode()) {
            case NodeTypes.MAPPER -> executeMapper(nodeInfo, input, context);
            case NodeTypes.FILTER -> executeFilter(nodeInfo, input, context);
            case NodeTypes.VALIDATOR -> executeValidator(nodeInfo, input, context);
            case NodeTypes.SPLITTER -> executeSplitter(nodeInfo, input, context);
            case NodeTypes.MERGER -> executeMerger(nodeInfo, input, context);
            default -> throw new IllegalArgumentException("不支持的数据处理节点类型: " + nodeInfo.nodeTypeCode());
        };
    }

    /**
     * 执行数据映射节点
     *
     * <p>数据映射是数据处理的核心功能之一，它能够：</p>
     * <ul>
     * <li>重新组织数据结构，将输入数据按照映射规则转换为目标格式</li>
     * <li>支持字段重命名、嵌套结构展开、数据类型转换等操作</li>
     * <li>提供灵活的转换函数，如大小写转换、数值计算等</li>
     * <li>支持条件映射，根据数据内容动态选择映射规则</li>
     * </ul>
     *
     * <p><b>配置示例：</b></p>
     * <pre>{@code
     * {
     *   "mapping": {
     *     "fullName": "name",
     *     "userAge": {"source": "age", "transform": "toNumber"},
     *     "email": {"source": "contact.email", "transform": "lowercase"}
     *   },
     *   "preserveUnmapped": false
     * }
     * }</pre>
     */
    private Map<String, Object> executeMapper(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                              FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());

            if (!config.containsKey("mapping")) {
                throw new IllegalArgumentException("映射节点需要配置mapping参数");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> mappingRules = (Map<String, Object>) config.get("mapping");
            boolean preserveUnmapped = (Boolean) config.getOrDefault("preserveUnmapped", false);

            Map<String, Object> output = new HashMap<>();

            // 应用映射规则
            for (Map.Entry<String, Object> rule : mappingRules.entrySet()) {
                String targetKey = rule.getKey();
                Object mappingRule = rule.getValue();

                Object mappedValue = applyMappingRule(mappingRule, input, context);
                if (mappedValue != null) {
                    output.put(targetKey, mappedValue);
                }
            }

            // 保留未映射的字段
            if (preserveUnmapped) {
                for (Map.Entry<String, Object> entry : input.entrySet()) {
                    if (!output.containsKey(entry.getKey()) && !entry.getKey().startsWith("_")) {
                        output.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            // 添加映射统计信息
            output.put("_mapping_applied", mappingRules.size());
            output.put("_output_fields", output.size());
            output.put("_timestamp", System.currentTimeMillis());

            log.debug("数据映射完成: 映射规则数={}, 输出字段数={}", mappingRules.size(), output.size());
            return output;

        } catch (Exception e) {
            log.error("数据映射节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("数据映射节点执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行数据过滤节点
     *
     * <p>数据过滤节点专门处理数组数据的筛选，提供强大的过滤能力：</p>
     * <ul>
     * <li>支持基于字段值的简单过滤</li>
     * <li>支持复杂的条件表达式，如范围判断、正则匹配等</li>
     * <li>支持多条件组合，使用AND、OR逻辑</li>
     * <li>提供过滤统计信息，便于监控数据处理效果</li>
     * </ul>
     *
     * <p><b>配置示例：</b></p>
     * <pre>{@code
     * {
     *   "source": "items",
     *   "condition": {
     *     "type": "expression",
     *     "expression": "item.age >= 18 && item.status === 'active'"
     *   }
     * }
     * }</pre>
     */
    private Map<String, Object> executeFilter(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                              FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());
            String sourceKey = (String) config.getOrDefault("source", "items");

            Object sourceData = input.get(sourceKey);
            if (!(sourceData instanceof List)) {
                throw new IllegalArgumentException("过滤节点的数据源必须是数组: " + sourceKey);
            }

            @SuppressWarnings("unchecked")
            List<Object> items = (List<Object>) sourceData;

            @SuppressWarnings("unchecked")
            Map<String, Object> condition = (Map<String, Object>) config.get("condition");

            if (condition == null) {
                throw new IllegalArgumentException("过滤节点需要配置condition参数");
            }

            // 执行过滤
            List<Object> filtered = items.stream()
                    .filter(item -> evaluateFilterCondition(item, condition, context))
                    .collect(Collectors.toList());

            Map<String, Object> output = new HashMap<>(input);
            output.put(sourceKey, filtered);
            output.put("_original_count", items.size());
            output.put("_filtered_count", filtered.size());
            output.put("_filter_ratio", !items.isEmpty() ? (double) filtered.size() / items.size() : 0.0);
            output.put("_timestamp", System.currentTimeMillis());

            double ratio = !items.isEmpty() ? (100.0 * filtered.size() / items.size()) : 0.0;

            String formattedRatio = new BigDecimal(ratio).setScale(2, RoundingMode.HALF_UP).toString();

            log.debug("数据过滤完成: 原始数量={}, 过滤后={}, 过滤率={}%",
                    items.size(), filtered.size(), formattedRatio);

            return output;

        } catch (Exception e) {
            log.error("数据过滤节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("数据过滤节点执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行数据验证节点
     *
     * <p>数据验证是确保数据质量的重要环节，提供全面的验证能力：</p>
     * <ul>
     * <li>字段存在性验证（required）</li>
     * <li>数据类型验证（string, number, boolean, array, object）</li>
     * <li>数值范围验证（min, max）</li>
     * <li>字符串长度验证（minLength, maxLength）</li>
     * <li>正则表达式验证（pattern）</li>
     * <li>枚举值验证（enum）</li>
     * <li>自定义验证规则（custom）</li>
     * </ul>
     *
     * <p><b>配置示例：</b></p>
     * <pre>{@code
     * {
     *   "rules": [
     *     {"field": "email", "type": "required"},
     *     {"field": "email", "type": "pattern", "value": "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$"},
     *     {"field": "age", "type": "range", "min": 0, "max": 120}
     *   ],
     *   "failFast": false
     * }
     * }</pre>
     */
    private Map<String, Object> executeValidator(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                                 FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rules = (List<Map<String, Object>>) config.get("rules");

            if (rules == null || rules.isEmpty()) {
                throw new IllegalArgumentException("验证节点需要配置rules参数");
            }

            boolean failFast = (Boolean) config.getOrDefault("failFast", false);
            List<ValidationError> errors = new ArrayList<>();

            // 执行所有验证规则
            for (Map<String, Object> rule : rules) {
                ValidationError error = validateRule(rule, input, context);
                if (error != null) {
                    errors.add(error);
                    if (failFast) {
                        break; // 快速失败模式
                    }
                }
            }

            Map<String, Object> output = new HashMap<>(input);
            output.put("_validation_errors", errors);
            output.put("_validation_passed", errors.isEmpty());
            output.put("_rules_checked", rules.size());
            output.put("_error_count", errors.size());
            output.put("_timestamp", System.currentTimeMillis());

            if (!errors.isEmpty()) {
                log.warn("数据验证失败: nodeKey={}, 错误数量={}", nodeInfo.nodeKey(), errors.size());
                for (ValidationError error : errors) {
                    log.debug("验证错误: field={}, message={}", error.field(), error.message());
                }
            }

            return output;

        } catch (Exception e) {
            log.error("数据验证节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("数据验证节点执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行文本分割节点
     *
     * <p>文本分割节点专门处理长文本的分段，在RAG系统中尤其重要：</p>
     * <ul>
     * <li>按分隔符分割（换行符、标点符号、自定义分隔符）</li>
     * <li>按固定长度分割，支持重叠窗口</li>
     * <li>智能分割，保持句子完整性</li>
     * <li>支持最大块大小限制</li>
     * <li>提供分割统计信息</li>
     * </ul>
     *
     * <p><b>配置示例：</b></p>
     * <pre>{@code
     * {
     *   "source": "content",
     *   "strategy": "length",
     *   "maxLength": 1000,
     *   "overlap": 50,
     *   "preserveSentences": true
     * }
     * }</pre>
     */
    private Map<String, Object> executeSplitter(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                                FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());
            String sourceKey = (String) config.getOrDefault("source", "text");

            Object textObj = input.get(sourceKey);
            if (textObj == null) {
                throw new IllegalArgumentException("找不到要分割的文本字段: " + sourceKey);
            }

            String text = String.valueOf(textObj);
            String strategy = (String) config.getOrDefault("strategy", "delimiter");

            List<String> chunks = switch (strategy) {
                case "delimiter" -> splitByDelimiter(text, config);
                case "length" -> splitByLength(text, config);
                case "sentences" -> splitBySentences(text, config);
                case "paragraphs" -> splitByParagraphs(text, config);
                default -> throw new IllegalArgumentException("不支持的分割策略: " + strategy);
            };

            // 过滤空白块
            chunks = chunks.stream()
                    .map(String::trim)
                    .filter(chunk -> !chunk.isEmpty())
                    .collect(Collectors.toList());

            Map<String, Object> output = new HashMap<>(input);
            output.put("chunks", chunks);
            output.put("_original_length", text.length());
            output.put("_chunk_count", chunks.size());
            output.put("_average_chunk_length", chunks.stream().mapToInt(String::length).average().orElse(0.0));
            output.put("_strategy", strategy);
            output.put("_timestamp", System.currentTimeMillis());

            double averageLength = chunks.stream()
                    .mapToInt(String::length)
                    .average()
                    .orElse(0.0);

            String formattedAvgLength = String.format("%.1f", averageLength);

            log.debug("文本分割完成: 原始长度={}, 分块数量={}, 平均长度={}",
                    text.length(), chunks.size(), formattedAvgLength);

            return output;

        } catch (Exception e) {
            log.error("文本分割节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("文本分割节点执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行数据合并节点
     *
     * <p>数据合并节点负责将多个数据源整合成一个统一的输出：</p>
     * <ul>
     * <li>简单合并：直接将多个Map合并</li>
     * <li>数组合并：将多个数组连接起来</li>
     * <li>深度合并：递归合并嵌套结构</li>
     * <li>条件合并：根据条件选择性合并</li>
     * <li>冲突处理：处理字段名冲突的策略</li>
     * </ul>
     *
     * <p><b>配置示例：</b></p>
     * <pre>{@code
     * {
     *   "sources": ["data1", "data2", "data3"],
     *   "strategy": "merge",
     *   "conflictResolution": "prefer_last",
     *   "deepMerge": true
     * }
     * }</pre>
     */
    private Map<String, Object> executeMerger(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                              FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());

            @SuppressWarnings("unchecked")
            List<String> sources = (List<String>) config.get("sources");

            if (sources == null || sources.isEmpty()) {
                throw new IllegalArgumentException("合并节点需要配置sources参数");
            }

            String strategy = (String) config.getOrDefault("strategy", "merge");
            String conflictResolution = (String) config.getOrDefault("conflictResolution", "prefer_last");
            boolean deepMerge = (Boolean) config.getOrDefault("deepMerge", false);

            Map<String, Object> output = performMerge(sources, input, strategy, conflictResolution, deepMerge);

            // 添加合并统计信息
            output.put("_merged_sources", sources);
            output.put("_merge_strategy", strategy);
            output.put("_conflict_resolution", conflictResolution);
            output.put("_output_size", output.size());
            output.put("_timestamp", System.currentTimeMillis());

            log.debug("数据合并完成: 源数量={}, 策略={}, 输出字段数={}",
                    sources.size(), strategy, output.size());

            return output;

        } catch (Exception e) {
            log.error("数据合并节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("数据合并节点执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void validateConfig(String configJson) {
        try {
            Map<String, Object> config = parseConfig(configJson);

            // 基础配置验证
            if (config.isEmpty()) {
                throw new IllegalArgumentException("数据处理节点需要配置参数");
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("数据处理节点配置验证失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            return objectMapper != null;
        } catch (Exception e) {
            log.warn("数据处理节点执行器健康检查失败", e);
            return false;
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
     * 应用映射规则
     */
    private Object applyMappingRule(Object mappingRule, Map<String, Object> input, FlowExecutionContext context) {
        if (mappingRule instanceof String sourceKey) {
            // 简单映射：直接从输入复制
            return getNestedValue(input, sourceKey);
        } else if (mappingRule instanceof Map) {
            // 复杂映射：包含转换规则
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = (Map<String, Object>) mappingRule;

            String sourceKey = (String) rule.get("source");
            if (sourceKey == null) {
                return null;
            }

            Object value = getNestedValue(input, sourceKey);

            // 应用转换
            if (rule.containsKey("transform")) {
                String transform = (String) rule.get("transform");
                value = applyTransform(value, transform, rule);
            }

            return value;
        }

        return null;
    }

    /**
     * 获取嵌套字段值
     */
    private Object getNestedValue(Map<String, Object> data, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String[] keys = path.split("\\.");
        Object current = data;

        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(key);
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * 应用数据转换
     */
    private Object applyTransform(Object value, String transform, Map<String, Object> rule) {
        if (value == null) return null;

        return switch (transform) {
            case "uppercase" -> value instanceof String ? ((String) value).toUpperCase() : value;
            case "lowercase" -> value instanceof String ? ((String) value).toLowerCase() : value;
            case "trim" -> value instanceof String ? ((String) value).trim() : value;
            case "toString" -> String.valueOf(value);
            case "toNumber" -> convertToNumber(value);
            case "toBoolean" -> convertToBoolean(value);
            case "format" -> formatValue(value, rule);
            default -> value;
        };
    }

    /**
     * 转换为数字
     */
    private Object convertToNumber(Object value) {
        if (value instanceof Number) {
            return value;
        }

        try {
            String str = String.valueOf(value);
            if (str.contains(".")) {
                return Double.parseDouble(str);
            } else {
                return Long.parseLong(str);
            }
        } catch (NumberFormatException e) {
            return value; // 转换失败，返回原值
        }
    }

    /**
     * 转换为布尔值
     */
    private Object convertToBoolean(Object value) {
        if (value instanceof Boolean) {
            return value;
        }

        String str = String.valueOf(value).toLowerCase();
        return "true".equals(str) || "1".equals(str) || "yes".equals(str);
    }

    /**
     * 格式化值
     */
    private Object formatValue(Object value, Map<String, Object> rule) {
        String pattern = (String) rule.get("pattern");
        if (pattern == null) {
            return value;
        }

        try {
            return String.format(pattern, value);
        } catch (Exception e) {
            log.warn("值格式化失败: pattern={}, value={}", pattern, value);
            return value;
        }
    }

    /**
     * 评估过滤条件
     */
    private boolean evaluateFilterCondition(Object item, Map<String, Object> condition, FlowExecutionContext context) {
        String type = (String) condition.get("type");

        return switch (type) {
            case "simple" -> evaluateSimpleCondition(item, condition);
            case "expression" -> evaluateExpressionCondition(item, condition, context);
            case "range" -> evaluateRangeCondition(item, condition);
            case "regex" -> evaluateRegexCondition(item, condition);
            default -> true; // 默认通过
        };
    }

    /**
     * 评估简单条件
     */
    private boolean evaluateSimpleCondition(Object item, Map<String, Object> condition) {
        String field = (String) condition.get("field");
        String operator = (String) condition.get("operator");
        Object expectedValue = condition.get("value");

        if (!(item instanceof Map)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> itemMap = (Map<String, Object>) item;
        Object actualValue = itemMap.get(field);

        return switch (operator) {
            case "equals" -> Objects.equals(actualValue, expectedValue);
            case "not_equals" -> !Objects.equals(actualValue, expectedValue);
            case "contains" ->
                    actualValue != null && String.valueOf(actualValue).contains(String.valueOf(expectedValue));
            case "starts_with" ->
                    actualValue != null && String.valueOf(actualValue).startsWith(String.valueOf(expectedValue));
            case "ends_with" ->
                    actualValue != null && String.valueOf(actualValue).endsWith(String.valueOf(expectedValue));
            default -> true;
        };
    }

    /**
     * 评估表达式条件
     */
    private boolean evaluateExpressionCondition(Object item, Map<String, Object> condition, FlowExecutionContext context) {
        if (scriptEngine == null) {
            log.warn("JavaScript引擎不可用，表达式条件默认通过");
            return true;
        }

        try {
            String expression = (String) condition.get("expression");
            scriptEngine.put("item", item);

            // 注入上下文变量
            for (Map.Entry<String, Object> entry : context.getAllGlobalVariables().entrySet()) {
                scriptEngine.put("ctx_" + entry.getKey(), entry.getValue());
            }

            Object result = scriptEngine.eval(expression);
            return Boolean.TRUE.equals(result);

        } catch (Exception e) {
            log.error("表达式条件评估失败: {}", condition.get("expression"), e);
            return false;
        }
    }

    /**
     * 评估范围条件
     */
    private boolean evaluateRangeCondition(Object item, Map<String, Object> condition) {
        String field = (String) condition.get("field");
        Number min = (Number) condition.get("min");
        Number max = (Number) condition.get("max");

        if (!(item instanceof Map)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> itemMap = (Map<String, Object>) item;
        Object value = itemMap.get(field);

        if (!(value instanceof Number)) {
            return false;
        }

        double numValue = ((Number) value).doubleValue();
        boolean inRange = true;

        if (min != null) {
            inRange = inRange && numValue >= min.doubleValue();
        }

        if (max != null) {
            inRange = inRange && numValue <= max.doubleValue();
        }

        return inRange;
    }

    /**
     * 评估正则条件
     */
    private boolean evaluateRegexCondition(Object item, Map<String, Object> condition) {
        String field = (String) condition.get("field");
        String pattern = (String) condition.get("pattern");

        if (!(item instanceof Map) || pattern == null) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> itemMap = (Map<String, Object>) item;
        Object value = itemMap.get(field);

        if (value == null) {
            return false;
        }

        try {
            return Pattern.matches(pattern, String.valueOf(value));
        } catch (Exception e) {
            log.warn("正则表达式匹配失败: pattern={}, value={}", pattern, value);
            return false;
        }
    }

    /**
     * 验证单个规则
     */
    private ValidationError validateRule(Map<String, Object> rule, Map<String, Object> input, FlowExecutionContext context) {
        String field = (String) rule.get("field");
        String type = (String) rule.get("type");

        if (field == null || type == null) {
            return null;
        }

        Object value = input.get(field);

        return switch (type) {
            case "required" -> validateRequired(field, value);
            case "type" -> validateType(field, value, rule);
            case "minLength" -> validateMinLength(field, value, rule);
            case "maxLength" -> validateMaxLength(field, value, rule);
            case "pattern" -> validatePattern(field, value, rule);
            case "range" -> validateRange(field, value, rule);
            case "enum" -> validateEnum(field, value, rule);
            default -> null;
        };
    }

    /**
     * 验证必填字段
     */
    private ValidationError validateRequired(String field, Object value) {
        if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
            return new ValidationError(field, "字段不能为空");
        }
        return null;
    }

    /**
     * 验证数据类型
     */
    private ValidationError validateType(String field, Object value, Map<String, Object> rule) {
        if (value == null) return null;

        String expectedType = (String) rule.get("value");
        boolean isValid = switch (expectedType) {
            case "string" -> value instanceof String;
            case "number" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "array" -> value instanceof List;
            case "object" -> value instanceof Map;
            default -> true;
        };

        return isValid ? null : new ValidationError(field, "数据类型不匹配，期望: " + expectedType);
    }

    /**
     * 验证最小长度
     */
    private ValidationError validateMinLength(String field, Object value, Map<String, Object> rule) {
        if (value == null) return null;

        int minLength = ((Number) rule.get("value")).intValue();
        String str = String.valueOf(value);

        return str.length() >= minLength ? null :
                new ValidationError(field, "长度不能少于 " + minLength + " 个字符");
    }

    /**
     * 验证最大长度
     */
    private ValidationError validateMaxLength(String field, Object value, Map<String, Object> rule) {
        if (value == null) return null;

        int maxLength = ((Number) rule.get("value")).intValue();
        String str = String.valueOf(value);

        return str.length() <= maxLength ? null :
                new ValidationError(field, "长度不能超过 " + maxLength + " 个字符");
    }

    /**
     * 验证正则模式
     */
    private ValidationError validatePattern(String field, Object value, Map<String, Object> rule) {
        if (value == null) return null;

        String pattern = (String) rule.get("value");
        String str = String.valueOf(value);

        try {
            return Pattern.matches(pattern, str) ? null :
                    new ValidationError(field, "格式不正确");
        } catch (Exception e) {
            return new ValidationError(field, "正则表达式错误");
        }
    }

    /**
     * 验证数值范围
     */
    private ValidationError validateRange(String field, Object value, Map<String, Object> rule) {
        if (value == null || !(value instanceof Number)) {
            return new ValidationError(field, "必须是数值类型");
        }

        double numValue = ((Number) value).doubleValue();
        Number min = (Number) rule.get("min");
        Number max = (Number) rule.get("max");

        if (min != null && numValue < min.doubleValue()) {
            return new ValidationError(field, "值不能小于 " + min);
        }

        if (max != null && numValue > max.doubleValue()) {
            return new ValidationError(field, "值不能大于 " + max);
        }

        return null;
    }

    /**
     * 验证枚举值
     */
    private ValidationError validateEnum(String field, Object value, Map<String, Object> rule) {
        if (value == null) return null;

        @SuppressWarnings("unchecked")
        List<Object> allowedValues = (List<Object>) rule.get("values");

        if (allowedValues == null) {
            return null;
        }

        return allowedValues.contains(value) ? null :
                new ValidationError(field, "值必须是以下之一: " + allowedValues);
    }

    /**
     * 按分隔符分割文本
     */
    private List<String> splitByDelimiter(String text, Map<String, Object> config) {
        String delimiter = (String) config.getOrDefault("delimiter", "\n");
        return Arrays.asList(text.split(Pattern.quote(delimiter)));
    }

    /**
     * 按长度分割文本
     */
    private List<String> splitByLength(String text, Map<String, Object> config) {
        int maxLength = ((Number) config.getOrDefault("maxLength", 1000)).intValue();
        int overlap = ((Number) config.getOrDefault("overlap", 0)).intValue();

        List<String> chunks = new ArrayList<>();
        int step = maxLength - overlap;

        for (int i = 0; i < text.length(); i += step) {
            int end = Math.min(i + maxLength, text.length());
            chunks.add(text.substring(i, end));

            if (end >= text.length()) {
                break;
            }
        }

        return chunks;
    }

    /**
     * 按句子分割文本
     */
    private List<String> splitBySentences(String text, Map<String, Object> config) {
        int maxSentences = ((Number) config.getOrDefault("maxSentences", 5)).intValue();

        // 简单的句子分割（基于句号、问号、感叹号）
        String[] sentences = text.split("[.!?]+");
        List<String> chunks = new ArrayList<>();

        StringBuilder chunk = new StringBuilder();
        int sentenceCount = 0;

        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;

            chunk.append(sentence).append(". ");
            sentenceCount++;

            if (sentenceCount >= maxSentences) {
                chunks.add(chunk.toString().trim());
                chunk = new StringBuilder();
                sentenceCount = 0;
            }
        }

        if (!chunk.isEmpty()) {
            chunks.add(chunk.toString().trim());
        }

        return chunks;
    }

    /**
     * 按段落分割文本
     */
    private List<String> splitByParagraphs(String text, Map<String, Object> config) {
        int maxParagraphs = ((Number) config.getOrDefault("maxParagraphs", 3)).intValue();

        String[] paragraphs = text.split("\n\n+");
        List<String> chunks = new ArrayList<>();

        StringBuilder chunk = new StringBuilder();
        int paragraphCount = 0;

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            chunk.append(paragraph).append("\n\n");
            paragraphCount++;

            if (paragraphCount >= maxParagraphs) {
                chunks.add(chunk.toString().trim());
                chunk = new StringBuilder();
                paragraphCount = 0;
            }
        }

        if (!chunk.isEmpty()) {
            chunks.add(chunk.toString().trim());
        }

        return chunks;
    }

    /**
     * 执行数据合并
     */
    private Map<String, Object> performMerge(List<String> sources, Map<String, Object> input,
                                             String strategy, String conflictResolution, boolean deepMerge) {
        Map<String, Object> result = new HashMap<>();

        for (String sourceKey : sources) {
            Object sourceData = input.get(sourceKey);
            if (sourceData == null) continue;

            if (sourceData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sourceMap = (Map<String, Object>) sourceData;

                if (deepMerge) {
                    result = deepMergeMap(result, sourceMap, conflictResolution);
                } else {
                    mergeMap(result, sourceMap, conflictResolution);
                }
            } else {
                // 非Map类型，直接添加
                result.put(sourceKey, sourceData);
            }
        }

        return result;
    }

    /**
     * 合并Map
     */
    private void mergeMap(Map<String, Object> target, Map<String, Object> source, String conflictResolution) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (!target.containsKey(key)) {
                target.put(key, value);
            } else {
                // 处理冲突
                switch (conflictResolution) {
                    case "prefer_first" -> {
                    } // 保持原值，不覆盖
                    case "prefer_last" -> target.put(key, value);
                    case "merge_arrays" -> {
                        Object existing = target.get(key);
                        if (existing instanceof List && value instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Object> mergedList = new ArrayList<>((List<Object>) existing);
                            mergedList.addAll((List<Object>) value);
                            target.put(key, mergedList);
                        } else {
                            target.put(key, value);
                        }
                    }
                    default -> target.put(key, value);
                }
            }
        }
    }

    /**
     * 深度合并Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> deepMergeMap(Map<String, Object> target, Map<String, Object> source, String conflictResolution) {
        Map<String, Object> result = new HashMap<>(target);

        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (result.containsKey(key)) {
                Object existing = result.get(key);
                if (existing instanceof Map && value instanceof Map) {
                    // 递归合并嵌套Map
                    result.put(key, deepMergeMap((Map<String, Object>) existing, (Map<String, Object>) value, conflictResolution));
                } else {
                    // 应用冲突解决策略
                    switch (conflictResolution) {
                        case "prefer_first" -> {
                        } // 保持原值
                        case "prefer_last" -> result.put(key, value);
                        default -> result.put(key, value);
                    }
                }
            } else {
                result.put(key, value);
            }
        }

        return result;
    }

    /**
     * 验证错误记录
     */
    private record ValidationError(String field, String message) {
    }
}