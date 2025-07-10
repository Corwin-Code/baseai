package com.clinflash.baseai.domain.mcp.service;

import com.clinflash.baseai.domain.mcp.model.Tool;
import com.clinflash.baseai.domain.mcp.model.ToolAuth;

import java.util.Map;

/**
 * <h2>工具执行服务接口</h2>
 */
public interface ToolExecutionService {

    /**
     * 执行工具
     */
    Map<String, Object> execute(Tool tool, ToolAuth auth, Map<String, Object> params, Integer timeoutSeconds);

    /**
     * 检查服务健康状态
     */
    boolean isHealthy();
}