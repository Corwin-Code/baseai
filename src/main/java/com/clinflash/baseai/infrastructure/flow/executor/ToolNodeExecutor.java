//package com.clinflash.baseai.infrastructure.flow.executor;
//
//import com.clinflash.baseai.domain.flow.model.NodeTypes;
//import com.clinflash.baseai.domain.flow.service.FlowExecutionService;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.time.Duration;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * <h2>工具节点执行器</h2>
// *
// * <p>处理工具调用相关的节点，包括MCP工具、HTTP请求等。
// * 这是流程与外部系统集成的关键组件。</p>
// */
//@Component
//public class ToolNodeExecutor implements NodeExecutor {
//
//    private static final Logger log = LoggerFactory.getLogger(ToolNodeExecutor.class);
//
//    private final ObjectMapper objectMapper;
//    private final WebClient webClient;
//    // TODO: 注入MCP工具服务
//
//    public ToolNodeExecutor(ObjectMapper objectMapper) {
//        this.objectMapper = objectMapper;
//        this.webClient = WebClient.builder()
//                .build();
//    }
//
//    @Override
//    public List<String> getSupportedTypes() {
//        return List.of(
//                NodeTypes.TOOL,
//                NodeTypes.HTTP,
//                NodeTypes.SCRIPT
//        );
//    }
//
//    @Override
//    public Map<String, Object> execute(FlowExecutionService.FlowGraph.Node node,
//                                       Map<String, Object> input,
//                                       FlowExecutionService.ExecutionContext context) {
//        log.debug("执行工具节点: type={}, key={}", node.type(), node.key());
//
//        return switch (node.type()) {
//            case NodeTypes.TOOL -> executeTool(node, input, context);
//            case NodeTypes.HTTP -> executeHttp(node, input, context);
//            case NodeTypes.SCRIPT -> executeScript(node, input, context);
//            default -> throw new IllegalArgumentException("不支持的节点类型: " + node.type());
//        };
//    }
//
//    /**
//     * 执行MCP工具节点
//     *
//     * <p>调用注册的MCP工具，实现与外部系统的集成。</p>
//     */
//    private Map<String, Object> executeTool(FlowExecutionService.FlowGraph.Node node,
//                                            Map<String, Object> input,
//                                            FlowExecutionService.ExecutionContext context) {
//        JsonNode config = node.config();
//        if (config == null || !config.has("toolId")) {
//            throw new IllegalArgumentException("工具节点需要配置toolId");
//        }
//
//        Long toolId = config.get("toolId").asLong();
//
//        // 准备工具参数
//        Map<String, Object> toolParams = new HashMap<>();
//        if (config.has("paramMapping")) {
//            JsonNode paramMapping = config.get("paramMapping");
//            paramMapping.fields().forEachRemaining(entry -> {
//                String paramName = entry.getKey();
//                String inputKey = entry.getValue().asText();
//                if (input.containsKey(inputKey)) {
//                    toolParams.put(paramName, input.get(inputKey));
//                }
//            });
//        }
//
//        // TODO: 调用MCP工具服务
//        log.info("调用MCP工具: toolId={}, params={}", toolId, toolParams);
//
//        // 模拟工具调用结果
//        Map<String, Object> toolResult = Map.of(
//                "status", "success",
//                "data", Map.of("message", "工具执行成功")
//        );
//
//        Map<String, Object> output = new HashMap<>(input);
//        output.put("toolResult", toolResult);
//
//        return output;
//    }
//
//    /**
//     * 执行HTTP请求节点
//     *
//     * <p>发送HTTP请求到外部API。</p>
//     */
//    private Map<String, Object> executeHttp(FlowExecutionService.FlowGraph.Node node,
//                                            Map<String, Object> input,
//                                            FlowExecutionService.ExecutionContext context) {
//        JsonNode config = node.config();
//        if (config == null || !config.has("url")) {
//            throw new IllegalArgumentException("HTTP节点需要配置url");
//        }
//
//        String url = config.get("url").asText();
//        String method = config.has("method") ?
//                config.get("method").asText() : "GET";
//
//        // 构建请求体
//        Object requestBody = null;
//        if (config.has("body")) {
//            String bodyKey = config.get("body").asText();
//            requestBody = input.get(bodyKey);
//        }
//
//        try {
//            // 执行HTTP请求
//            WebClient.ResponseSpec response = webClient
//                    .method(org.springframework.http.HttpMethod.valueOf(method))
//                    .uri(url)
//                    .bodyValue(requestBody != null ? requestBody : "")
//                    .retrieve();
//
//            // 获取响应
//            String responseBody = response
//                    .bodyToMono(String.class)
//                    .timeout(Duration.ofSeconds(30))
//                    .block();
//
//            Map<String, Object> output = new HashMap<>(input);
//            output.put("httpResponse", responseBody);
//            output.put("httpStatus", 200); // 简化处理
//
//            log.info("HTTP请求成功: {} {}", method, url);
//
//            return output;
//
//        } catch (Exception e) {
//            log.error("HTTP请求失败: {} {}", method, url, e);
//
//            Map<String, Object> output = new HashMap<>(input);
//            output.put("httpError", e.getMessage());
//            output.put("httpStatus", 500);
//
//            return output;
//        }
//    }
//
//    /**
//     * 执行脚本节点
//     *
//     * <p>执行自定义脚本进行数据处理。</p>
//     */
//    private Map<String, Object> executeScript(FlowExecutionService.FlowGraph.Node node,
//                                              Map<String, Object> input,
//                                              FlowExecutionService.ExecutionContext context) {
//        JsonNode config = node.config();
//        if (config == null || !config.has("script")) {
//            throw new IllegalArgumentException("脚本节点需要配置script");
//        }
//
//        String script = config.get("script").asText();
//        String language = config.has("language") ?
//                config.get("language").asText() : "JavaScript";
//
//        // TODO: 实现脚本执行逻辑
//        // 需要考虑安全性，可能需要沙箱环境
//
//        log.warn("脚本执行功能尚未实现: language={}", language);
//
//        Map<String, Object> output = new HashMap<>(input);
//        output.put("scriptResult", "脚本执行结果");
//
//        return output;
//    }
//}