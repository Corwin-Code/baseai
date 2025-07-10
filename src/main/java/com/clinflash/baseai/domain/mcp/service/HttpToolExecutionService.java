package com.clinflash.baseai.domain.mcp.service;

import com.clinflash.baseai.domain.mcp.model.Tool;
import com.clinflash.baseai.domain.mcp.model.ToolAuth;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * <h2>HTTP工具执行服务实现</h2>
 */
@Service
public class HttpToolExecutionService implements ToolExecutionService {

    private static final Logger log = LoggerFactory.getLogger(HttpToolExecutionService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HttpToolExecutionService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> execute(Tool tool, ToolAuth auth, Map<String, Object> params, Integer timeoutSeconds) {
        if (!tool.isHttpTool()) {
            throw new IllegalArgumentException("只支持HTTP类型工具");
        }

        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 添加认证信息
            if (tool.requiresAuth() && auth.apiKey() != null) {
                addAuthHeader(headers, tool.authType(), auth.apiKey());
            }

            // 构建请求体
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(params, headers);

            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tool.endpoint(), requestEntity, Map.class);

            // 处理响应
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("statusCode", response.getStatusCode().value());
            result.put("data", response.getBody());
            result.put("headers", response.getHeaders().toSingleValueMap());

            return result;

        } catch (Exception e) {
            log.error("HTTP工具执行失败: toolCode={}, endpoint={}", tool.code(), tool.endpoint(), e);

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("error", e.getMessage());
            errorResult.put("type", e.getClass().getSimpleName());

            return errorResult;
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            return restTemplate != null && objectMapper != null;
        } catch (Exception e) {
            log.error("HTTP工具执行服务健康检查失败", e);
            return false;
        }
    }

    private void addAuthHeader(HttpHeaders headers, String authType, String apiKey) {
        switch (authType.toUpperCase()) {
            case "API_KEY":
                headers.set("X-API-Key", apiKey);
                break;
            case "BEARER":
                headers.set("Authorization", "Bearer " + apiKey);
                break;
            case "BASIC":
                headers.set("Authorization", "Basic " + apiKey);
                break;
            default:
                log.warn("未知的认证类型: {}", authType);
        }
    }
}