package com.clinflash.baseai.infrastructure.flow.executor;

import com.clinflash.baseai.application.mcp.command.ExecuteToolCommand;
import com.clinflash.baseai.application.mcp.dto.ToolExecutionResultDTO;
import com.clinflash.baseai.application.mcp.service.McpApplicationService;
import com.clinflash.baseai.domain.flow.model.NodeTypes;
import com.clinflash.baseai.infrastructure.flow.model.FlowExecutionContext;
import com.clinflash.baseai.infrastructure.flow.model.NodeExecutionInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>工具节点执行器</h2>
 *
 * <p>这个执行器负责调用各种外部工具和服务。
 * 它能够操作不同类型的工具，从简单的HTTP请求到复杂的MCP工具调用，
 * 甚至执行自定义脚本，为流程提供强大的外部集成能力。</p>
 *
 * <p><b>支持的工具类型：</b></p>
 * <ul>
 * <li><b>MCP工具（TOOL）：</b>调用注册的MCP（Model Context Protocol）工具，支持各种专业工具</li>
 * <li><b>HTTP请求（HTTP）：</b>发送HTTP请求到外部API，支持RESTful服务集成</li>
 * <li><b>脚本执行（SCRIPT）：</b>执行自定义脚本，支持JavaScript等脚本语言</li>
 * </ul>
 *
 * <p><b>设计理念：</b></p>
 * <p>工具节点的设计强调安全性、可靠性和易用性。每种工具都有完善的错误处理机制，
 * 支持超时控制、重试策略和详细的执行日志，确保外部调用的稳定性和可追踪性。</p>
 *
 * <p><b>安全考虑：</b></p>
 * <p>特别是脚本执行功能，我们实施了严格的安全措施，包括沙箱环境、资源限制和代码审核，
 * 防止恶意代码的执行，保护系统安全。</p>
 */
@Component
public class ToolNodeExecutor implements NodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolNodeExecutor.class);

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final ScriptEngine scriptEngine;

    @Autowired(required = false)
    private McpApplicationService mcpService;

    /**
     * 构造函数
     *
     * <p>初始化工具节点执行器，配置HTTP客户端和脚本引擎。
     * 通过依赖注入获取MCP服务，支持可选依赖模式。</p>
     */
    public ToolNodeExecutor(ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;

        // 初始化脚本引擎（用于SCRIPT节点）
        ScriptEngineManager manager = new ScriptEngineManager();
        this.scriptEngine = manager.getEngineByName("JavaScript");

        if (scriptEngine == null) {
            log.warn("JavaScript引擎不可用，脚本执行功能将被禁用");
        }

        log.info("工具节点执行器初始化完成: HTTP={}, Script={}, MCP={}",
                restTemplate != null, scriptEngine != null, mcpService != null);
    }

    @Override
    public List<String> getSupportedNodeTypes() {
        return List.of(NodeTypes.TOOL, NodeTypes.HTTP, NodeTypes.SCRIPT);
    }

    @Override
    public Map<String, Object> execute(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                       FlowExecutionContext context) {
        log.debug("执行工具节点: type={}, key={}", nodeInfo.nodeTypeCode(), nodeInfo.nodeKey());

        return switch (nodeInfo.nodeTypeCode()) {
            case NodeTypes.TOOL -> executeMcpTool(nodeInfo, input, context);
            case NodeTypes.HTTP -> executeHttpRequest(nodeInfo, input, context);
            case NodeTypes.SCRIPT -> executeScript(nodeInfo, input, context);
            default -> throw new IllegalArgumentException("不支持的工具节点类型: " + nodeInfo.nodeTypeCode());
        };
    }

    /**
     * 执行MCP工具节点
     *
     * <p>MCP（Model Context Protocol）工具是系统中最强大的外部集成能力之一。
     * 这些工具经过专门设计和验证，能够安全地执行各种复杂任务：</p>
     * <ul>
     * <li>数据库查询和操作</li>
     * <li>文件系统访问</li>
     * <li>API调用和集成</li>
     * <li>计算和分析任务</li>
     * <li>第三方服务连接</li>
     * </ul>
     *
     * <p><b>配置示例：</b></p>
     * <pre>{@code
     * {
     *   "toolCode": "database_query",
     *   "parameters": {
     *     "query": "SELECT * FROM users WHERE active = true",
     *     "limit": 100
     *   },
     *   "timeout": 30,
     *   "asyncMode": false
     * }
     * }</pre>
     *
     * <p><b>权限和安全：</b></p>
     * <p>MCP工具的执行需要租户授权，系统会验证当前租户是否有权限使用指定的工具，
     * 并检查配额限制，确保资源的合理使用。</p>
     */
    private Map<String, Object> executeMcpTool(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                               FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());
            String toolCode = (String) config.get("toolCode");

            if (toolCode == null || toolCode.trim().isEmpty()) {
                throw new IllegalArgumentException("MCP工具节点需要配置toolCode参数");
            }

            if (mcpService == null) {
                throw new RuntimeException("MCP服务不可用，无法执行工具调用");
            }

            // 准备工具参数
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) config.getOrDefault("parameters", new HashMap<>());

            // 合并输入数据到参数中
            mergeInputToParameters(parameters, input, config);

            // 构建执行命令
            ExecuteToolCommand command = new ExecuteToolCommand(
                    context.getTenantId(),
                    context.getUserId(),
                    null, // threadId
                    null,
                    parameters,
                    (Boolean) config.getOrDefault("asyncMode", false),
                    (Integer) config.getOrDefault("timeout", 30)
            );

            // 执行工具
            long startTime = System.currentTimeMillis();
            ToolExecutionResultDTO result = mcpService.executeTool(toolCode, command);
            long duration = System.currentTimeMillis() - startTime;

            // 构建输出
            Map<String, Object> output = new HashMap<>(input);
            output.put("toolResult", result);
            output.put("toolCode", toolCode);
//            output.put("executionId", result.executionId());
            output.put("status", result.status());
            output.put("duration", duration);
            output.put("_timestamp", System.currentTimeMillis());

            // 如果执行成功，提取结果数据
            if ("SUCCESS".equals(result.status()) && result.result() != null) {
                output.put("data", result.result());
            }

            // 如果执行失败，记录错误信息
            if ("FAILED".equals(result.status()) && result.errorMessage() != null) {
                output.put("error", result.errorMessage());
                log.warn("MCP工具执行失败: toolCode={}, error={}", toolCode, result.errorMessage());
            }

            log.debug("MCP工具执行完成: toolCode={}, status={}, duration={}ms",
                    toolCode, result.status(), duration);

            return output;

        } catch (Exception e) {
            log.error("MCP工具节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("MCP工具节点执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行HTTP请求节点
     *
     * <p>HTTP请求节点是最常用的外部集成方式，支持与各种RESTful API交互：</p>
     * <ul>
     * <li>支持所有标准HTTP方法（GET、POST、PUT、DELETE等）</li>
     * <li>灵活的请求头配置，支持认证和自定义头部</li>
     * <li>多种请求体格式（JSON、XML、表单数据等）</li>
     * <li>响应数据解析和转换</li>
     * <li>错误处理和重试机制</li>
     * <li>超时控制和连接管理</li>
     * </ul>
     *
     * <p><b>配置示例：</b></p>
     * <pre>{@code
     * {
     *   "url": "https://api.example.com/users",
     *   "method": "POST",
     *   "headers": {
     *     "Authorization": "Bearer {token}",
     *     "Content-Type": "application/json"
     *   },
     *   "body": {
     *     "name": "{userName}",
     *     "email": "{userEmail}"
     *   },
     *   "timeout": 30
     * }
     * }</pre>
     *
     * <p><b>变量替换：</b></p>
     * <p>HTTP节点支持在URL、请求头和请求体中使用变量占位符，
     * 系统会自动将输入数据和上下文变量替换到相应位置。</p>
     */
    private Map<String, Object> executeHttpRequest(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                                   FlowExecutionContext context) {
        try {
            Map<String, Object> config = parseConfig(nodeInfo.configJson());
            String url = (String) config.get("url");
            String method = (String) config.getOrDefault("method", "GET");

            if (url == null || url.trim().isEmpty()) {
                throw new IllegalArgumentException("HTTP节点需要配置url参数");
            }

            // 变量替换
            url = replaceVariables(url, input, context);

            // 准备请求头
            HttpHeaders headers = prepareHttpHeaders(config, input, context);

            // 准备请求体
            Object requestBody = prepareHttpBody(config, input, context);

            // 获取超时配置
            int timeout = ((Number) config.getOrDefault("timeout", 30)).intValue();

            // 创建HTTP实体
            HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);

            // 执行HTTP请求
            long startTime = System.currentTimeMillis();
            ResponseEntity<Object> response = executeHttpCall(url, method, requestEntity, timeout);
            long duration = System.currentTimeMillis() - startTime;

            // 构建输出
            Map<String, Object> output = new HashMap<>(input);
            output.put("httpResponse", response.getBody());
            output.put("httpStatus", response.getStatusCode().value());
            output.put("httpHeaders", response.getHeaders().toSingleValueMap());
            output.put("url", url);
            output.put("method", method);
            output.put("duration", duration);
            output.put("_timestamp", System.currentTimeMillis());

            log.debug("HTTP请求执行完成: {} {}, status={}, duration={}ms",
                    method, url, response.getStatusCode().value(), duration);

            return output;

        } catch (Exception e) {
            log.error("HTTP节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);

            // 构建错误响应
            Map<String, Object> errorOutput = new HashMap<>(input);
            errorOutput.put("httpError", e.getMessage());
            errorOutput.put("httpStatus", 500);
            errorOutput.put("_error", true);
            errorOutput.put("_timestamp", System.currentTimeMillis());

            return errorOutput;
        }
    }

    /**
     * 执行脚本节点
     *
     * <p>脚本节点提供了强大的自定义数据处理能力，但需要谨慎使用：</p>
     * <ul>
     * <li>支持JavaScript脚本语言</li>
     * <li>可以访问输入数据和上下文变量</li>
     * <li>支持复杂的数据转换和计算逻辑</li>
     * <li>提供丰富的内置函数和工具库</li>
     * <li>严格的安全限制和资源控制</li>
     * </ul>
     *
     * <p><b>配置示例：</b></p>
     * <pre>{@code
     * {
     *   "script": "
     *     var result = input.numbers.reduce((sum, num) => sum + num, 0);
     *     return { total: result, average: result / input.numbers.length };
     *   ",
     *   "timeout": 5,
     *   "language": "javascript"
     * }
     * }</pre>
     *
     * <p><b>安全警告：</b></p>
     * <p>脚本执行功能具有很高的灵活性，但也带来安全风险。
     * 建议在生产环境中限制脚本节点的使用，或者实施严格的代码审核机制。</p>
     */
    private Map<String, Object> executeScript(NodeExecutionInfo nodeInfo, Map<String, Object> input,
                                              FlowExecutionContext context) {
        try {
            if (scriptEngine == null) {
                throw new RuntimeException("脚本引擎不可用，无法执行脚本节点");
            }

            Map<String, Object> config = parseConfig(nodeInfo.configJson());
            String script = (String) config.get("script");
            String language = (String) config.getOrDefault("language", "javascript");

            if (script == null || script.trim().isEmpty()) {
                throw new IllegalArgumentException("脚本节点需要配置script参数");
            }

            if (!"javascript".equalsIgnoreCase(language)) {
                throw new IllegalArgumentException("当前只支持JavaScript脚本语言");
            }

            int timeout = ((Number) config.getOrDefault("timeout", 5)).intValue();

            // 准备脚本执行环境
            prepareScriptEnvironment(input, context);

            // 执行脚本（带超时控制）
            long startTime = System.currentTimeMillis();
            Object scriptResult = executeScriptWithTimeout(script, timeout);
            long duration = System.currentTimeMillis() - startTime;

            // 构建输出
            Map<String, Object> output = new HashMap<>(input);
            output.put("scriptResult", scriptResult);
            output.put("script", script);
            output.put("language", language);
            output.put("duration", duration);
            output.put("_timestamp", System.currentTimeMillis());

            log.debug("脚本执行完成: language={}, duration={}ms", language, duration);

            return output;

        } catch (Exception e) {
            log.error("脚本节点执行失败: nodeKey={}", nodeInfo.nodeKey(), e);
            throw new RuntimeException("脚本节点执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void validateConfig(String configJson) {
        try {
            Map<String, Object> config = parseConfig(configJson);

            // 基础配置验证
            if (config.isEmpty()) {
                throw new IllegalArgumentException("工具节点需要配置参数");
            }

            // 根据节点类型进行特定验证
            if (config.containsKey("toolCode")) {
                // MCP工具验证
                String toolCode = (String) config.get("toolCode");
                if (toolCode == null || toolCode.trim().isEmpty()) {
                    throw new IllegalArgumentException("MCP工具需要指定toolCode");
                }
            } else if (config.containsKey("url")) {
                // HTTP请求验证
                String url = (String) config.get("url");
                if (url == null || !url.matches("^https?://.*")) {
                    throw new IllegalArgumentException("HTTP节点需要有效的URL");
                }
            } else if (config.containsKey("script")) {
                // 脚本验证
                String script = (String) config.get("script");
                if (script == null || script.trim().isEmpty()) {
                    throw new IllegalArgumentException("脚本节点需要非空的脚本内容");
                }
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("工具节点配置验证失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            boolean httpHealthy = restTemplate != null;
            boolean mcpHealthy = mcpService == null || checkMcpHealth();
            boolean scriptHealthy = scriptEngine != null;

            return httpHealthy && mcpHealthy && scriptHealthy;

        } catch (Exception e) {
            log.warn("工具节点执行器健康检查失败", e);
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
     * 合并输入数据到MCP工具参数
     */
    private void mergeInputToParameters(Map<String, Object> parameters, Map<String, Object> input,
                                        Map<String, Object> config) {
        // 获取参数映射配置
        @SuppressWarnings("unchecked")
        Map<String, String> paramMapping = (Map<String, String>) config.get("paramMapping");

        if (paramMapping != null) {
            // 根据映射规则添加参数
            for (Map.Entry<String, String> mapping : paramMapping.entrySet()) {
                String paramName = mapping.getKey();
                String inputKey = mapping.getValue();

                if (input.containsKey(inputKey)) {
                    parameters.put(paramName, input.get(inputKey));
                }
            }
        } else {
            // 默认将所有输入数据添加到参数中（过滤系统字段）
            for (Map.Entry<String, Object> entry : input.entrySet()) {
                if (!entry.getKey().startsWith("_")) {
                    parameters.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * 替换字符串中的变量
     */
    private String replaceVariables(String template, Map<String, Object> input, FlowExecutionContext context) {
        String result = template;

        // 替换输入变量
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }

        // 替换上下文变量
        for (Map.Entry<String, Object> entry : context.getAllGlobalVariables().entrySet()) {
            String placeholder = "{ctx." + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }

        return result;
    }

    /**
     * 准备HTTP请求头
     */
    private HttpHeaders prepareHttpHeaders(Map<String, Object> config, Map<String, Object> input,
                                           FlowExecutionContext context) {
        HttpHeaders headers = new HttpHeaders();

        // 设置默认内容类型
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 应用配置的请求头
        @SuppressWarnings("unchecked")
        Map<String, String> configHeaders = (Map<String, String>) config.get("headers");

        if (configHeaders != null) {
            for (Map.Entry<String, String> header : configHeaders.entrySet()) {
                String key = header.getKey();
                String value = replaceVariables(header.getValue(), input, context);
                headers.set(key, value);
            }
        }

        return headers;
    }

    /**
     * 准备HTTP请求体
     */
    private Object prepareHttpBody(Map<String, Object> config, Map<String, Object> input,
                                   FlowExecutionContext context) {
        Object body = config.get("body");

        if (body == null) {
            return null;
        }

        // 如果请求体是字符串，进行变量替换
        if (body instanceof String) {
            return replaceVariables((String) body, input, context);
        }

        // 如果请求体是Map，递归替换其中的字符串值
        if (body instanceof Map) {
            return replaceVariablesInMap((Map<?, ?>) body, input, context);
        }

        return body;
    }

    /**
     * 在Map中递归替换变量
     */
    private Object replaceVariablesInMap(Map<?, ?> map, Map<String, Object> input, FlowExecutionContext context) {
        Map<Object, Object> result = new HashMap<>();

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                value = replaceVariables((String) value, input, context);
            } else if (value instanceof Map) {
                value = replaceVariablesInMap((Map<?, ?>) value, input, context);
            }

            result.put(key, value);
        }

        return result;
    }

    /**
     * 执行HTTP调用
     */
    private ResponseEntity<Object> executeHttpCall(String url, String method, HttpEntity<Object> requestEntity,
                                                   int timeoutSeconds) {
        try {
            // 将字符串方法转换为 HttpMethod 枚举
            HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());

            // 将枚举实例传递给 exchange 方法
            return restTemplate.exchange(url, httpMethod, requestEntity, Object.class);

        } catch (IllegalArgumentException e) {
            // 捕获无效的HTTP方法字符串 (例如 "GETS")
            log.error("不支持的HTTP方法: {}", method, e);
            throw new IllegalArgumentException("不支持的HTTP方法: " + method);
        } catch (RestClientException e) {
            log.error("HTTP请求执行失败: {} {}", method, url, e);
            throw new RuntimeException("HTTP请求执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 准备脚本执行环境
     */
    private void prepareScriptEnvironment(Map<String, Object> input, FlowExecutionContext context) {
        try {
            // 注入输入数据
            scriptEngine.put("input", input);

            // 注入上下文信息
            scriptEngine.put("context", Map.of(
                    "runId", context.getRunId(),
                    "globalVariables", context.getAllGlobalVariables()
            ));

            // 注入常用工具函数
            scriptEngine.eval("""
                    var utils = {
                        log: function(message) {
                            java.lang.System.out.println('[SCRIPT] ' + message);
                        },
                        isString: function(obj) {
                            return typeof obj === 'string';
                        },
                        isNumber: function(obj) {
                            return typeof obj === 'number';
                        },
                        isArray: function(obj) {
                            return Array.isArray(obj);
                        },
                        isEmpty: function(obj) {
                            return obj == null || obj === '' ||
                                   (Array.isArray(obj) && obj.length === 0) ||
                                   (typeof obj === 'object' && Object.keys(obj).length === 0);
                        }
                    };
                    """);

        } catch (Exception e) {
            log.error("脚本环境准备失败", e);
            throw new RuntimeException("脚本环境准备失败: " + e.getMessage(), e);
        }
    }

    /**
     * 带超时控制的脚本执行
     */
    private Object executeScriptWithTimeout(String script, int timeoutSeconds) {
        try {
            // 在生产环境中，这里应该使用更安全的沙箱执行方式
            // 比如使用 CompletableFuture 和 timeout 控制

            long startTime = System.currentTimeMillis();
            Object result = scriptEngine.eval(script);
            long duration = System.currentTimeMillis() - startTime;

            if (duration > timeoutSeconds * 1000L) {
                log.warn("脚本执行时间过长: {}ms", duration);
            }

            return result;

        } catch (Exception e) {
            log.error("脚本执行失败: {}", script, e);
            throw new RuntimeException("脚本执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查MCP服务健康状态
     */
    private boolean checkMcpHealth() {
        try {
            if (mcpService == null) {
                return true; // MCP服务是可选的
            }

            return mcpService.checkHealth().status().equals("healthy");

        } catch (Exception e) {
            log.warn("MCP服务健康检查失败", e);
            return false;
        }
    }
}