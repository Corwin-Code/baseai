package com.cloud.baseai.application.mcp.service;

import com.cloud.baseai.application.mcp.command.AuthorizeTenantForToolCommand;
import com.cloud.baseai.application.mcp.command.ExecuteToolCommand;
import com.cloud.baseai.application.mcp.command.RegisterToolCommand;
import com.cloud.baseai.application.mcp.dto.*;
import com.cloud.baseai.domain.mcp.model.Tool;
import com.cloud.baseai.domain.mcp.model.ToolAuth;
import com.cloud.baseai.domain.mcp.model.ToolCallLog;
import com.cloud.baseai.domain.mcp.model.ToolType;
import com.cloud.baseai.domain.mcp.repository.ToolAuthRepository;
import com.cloud.baseai.domain.mcp.repository.ToolCallLogRepository;
import com.cloud.baseai.domain.mcp.repository.ToolRepository;
import com.cloud.baseai.domain.mcp.service.ToolExecutionService;
import com.cloud.baseai.infrastructure.constants.McpConstants;
import com.cloud.baseai.infrastructure.exception.BusinessException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.exception.McpToolException;
import com.cloud.baseai.infrastructure.persistence.mcp.entity.enums.ToolCallStatus;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * <h2>MCP应用服务</h2>
 *
 * <p>MCP系统的核心应用服务，负责工具的完整生命周期管理。
 * 从工具注册、租户授权到执行调用，提供完整的工具管理解决方案。</p>
 */
@Service
public class McpApplicationService {

    private static final Logger log = LoggerFactory.getLogger(McpApplicationService.class);

    private final ToolRepository toolRepo;
    private final ToolAuthRepository toolAuthRepo;
    private final ToolCallLogRepository toolCallLogRepo;
    private final ToolExecutionService toolExecutionService;

    // MCP工具管理专用异步执行器
    private final AsyncTaskExecutor mcpToolAsyncExecutor;

    public McpApplicationService(ToolRepository toolRepo,
                                 ToolAuthRepository toolAuthRepo,
                                 ToolCallLogRepository toolCallLogRepo,
                                 ToolExecutionService toolExecutionService,
                                 AsyncTaskExecutor mcpToolAsyncExecutor) {
        this.toolRepo = toolRepo;
        this.toolAuthRepo = toolAuthRepo;
        this.toolCallLogRepo = toolCallLogRepo;
        this.toolExecutionService = toolExecutionService;
        this.mcpToolAsyncExecutor = mcpToolAsyncExecutor;
    }

    // =================== 工具注册管理 ===================

    @Transactional
    public ToolDTO registerTool(RegisterToolCommand cmd) {
        log.info("注册工具: code={}, type={}", cmd.code(), cmd.type());

        try {
            // 检查工具代码唯一性
            if (toolRepo.existsByCode(cmd.code())) {
                throw new McpToolException(ErrorCode.BIZ_MCP_002, cmd.code());
            }

            // 验证工具配置
            validateToolConfig(cmd);

            // 创建工具
            Tool tool = Tool.create(
                    cmd.code(),
                    cmd.name(),
                    ToolType.valueOf(cmd.type()),
                    cmd.description(),
                    cmd.iconUrl(),
                    cmd.paramSchema(),
                    cmd.resultSchema(),
                    cmd.endpoint(),
                    cmd.authType(),
                    cmd.operatorId()
            );

            tool = toolRepo.save(tool);
            return toToolDTO(tool);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_MCP_004)
                    .cause(e)
                    .context("operation", "registerTool")
                    .context("toolName", cmd.name())
                    .build();
        }
    }

    public PageResultDTO<ToolDTO> listTools(int page, int size, String type, Boolean enabled) {
        try {
            List<Tool> tools;
            long total;

            if (type != null || enabled != null) {
                tools = toolRepo.findByTypeAndEnabled(type, enabled, page, size);
                total = toolRepo.countByTypeAndEnabled(type, enabled);
            } else {
                tools = toolRepo.findAll(page, size);
                total = toolRepo.count();
            }

            List<ToolDTO> toolDTOs = tools.stream()
                    .map(this::toToolDTO)
                    .collect(Collectors.toList());

            return PageResultDTO.of(toolDTOs, total, page, size);

        } catch (Exception e) {
            throw BusinessException.builder(ErrorCode.BIZ_MCP_009)
                    .cause(e)
                    .context("operation", "listTools")
                    .context("toolType", type)
                    .build();
        }
    }

    public ToolDetailDTO getToolDetail(String toolCode) {
        try {
            Tool tool = toolRepo.findByCode(toolCode)
                    .orElseThrow(() -> McpToolException.toolNotFound(toolCode));

            // 获取授权租户数量
            int authorizedTenants = toolAuthRepo.countByToolId(tool.id());

            // 获取最近调用统计
            int recentCalls = toolCallLogRepo.countByToolIdSince(tool.id(),
                    OffsetDateTime.now().minusHours(24));

            return new ToolDetailDTO(
                    tool.id(),
                    tool.code(),
                    tool.name(),
                    tool.type().name(),
                    tool.description(),
                    tool.iconUrl(),
                    tool.paramSchema(),
                    tool.resultSchema(),
                    tool.endpoint(),
                    tool.authType(),
                    tool.enabled(),
                    authorizedTenants,
                    recentCalls,
                    tool.createdAt()
            );

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_MCP_010)
                    .cause(e)
                    .context("operation", "getToolDetail")
                    .context("toolCode", toolCode)
                    .build();
        }
    }

    @Transactional
    public ToolDTO updateTool(String toolCode, RegisterToolCommand cmd) {
        try {
            Tool tool = toolRepo.findByCode(toolCode)
                    .orElseThrow(() -> McpToolException.toolNotFound(toolCode));

            // 验证更新配置
            validateToolConfig(cmd);

            tool = tool.update(
                    cmd.name(),
                    cmd.description(),
                    cmd.iconUrl(),
                    cmd.paramSchema(),
                    cmd.resultSchema(),
                    cmd.endpoint(),
                    cmd.authType(),
                    cmd.operatorId()
            );

            tool = toolRepo.save(tool);
            return toToolDTO(tool);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_MCP_005)
                    .cause(e)
                    .context("operation", "updateTool")
                    .context("toolCode", toolCode)
                    .build();
        }
    }

    // =================== 租户授权管理 ===================

    @Transactional
    public ToolAuthDTO authorizeTenantForTool(AuthorizeTenantForToolCommand cmd) {
        try {
            Tool tool = toolRepo.findByCode(cmd.toolCode())
                    .orElseThrow(() -> McpToolException.toolNotFound(cmd.toolCode()));

            // 检查是否已经授权
            Optional<ToolAuth> existingAuth = toolAuthRepo.findByToolIdAndTenantId(tool.id(), cmd.tenantId());
            if (existingAuth.isPresent()) {
                throw new McpToolException(ErrorCode.BIZ_AUTH_001);
            }

            // 创建授权
            ToolAuth auth = ToolAuth.create(
                    tool.id(),
                    cmd.tenantId(),
                    cmd.apiKey(),
                    cmd.quotaLimit()
            );

            auth = toolAuthRepo.save(auth);
            return toToolAuthDTO(auth, tool);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_AUTH_005)
                    .cause(e)
                    .context("operation", "authorizeTenantForTool")
                    .context("toolCode", cmd.toolCode())
                    .build();
        }
    }

    public List<ToolAuthDTO> getTenantAuthorizedTools(Long tenantId) {
        try {
            List<ToolAuth> auths = toolAuthRepo.findByTenantId(tenantId);
            List<Long> toolIds = auths.stream().map(ToolAuth::toolId).collect(Collectors.toList());
            Map<Long, Tool> toolMap = toolRepo.findByIds(toolIds).stream()
                    .collect(Collectors.toMap(Tool::id, tool -> tool));

            return auths.stream()
                    .map(auth -> toToolAuthDTO(auth, toolMap.get(auth.toolId())))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw BusinessException.builder(ErrorCode.BIZ_AUTH_007)
                    .cause(e)
                    .context("operation", "getTenantAuthorizedTools")
                    .context("tenantId", tenantId)
                    .build();
        }
    }

    @Transactional
    public void revokeTenantAuthorization(String toolCode, Long tenantId) {
        try {
            Tool tool = toolRepo.findByCode(toolCode)
                    .orElseThrow(() -> McpToolException.toolNotFound(toolCode));

            ToolAuth auth = toolAuthRepo.findByToolIdAndTenantId(tool.id(), tenantId)
                    .orElseThrow(() -> new McpToolException(ErrorCode.BIZ_AUTH_002));

            toolAuthRepo.delete(auth.toolId(), auth.tenantId());

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_AUTH_006)
                    .cause(e)
                    .context("operation", "revokeTenantAuthorization")
                    .context("toolCode", toolCode)
                    .build();
        }
    }

    // =================== 工具执行 ===================

    public ToolExecutionResultDTO executeTool(String toolCode, ExecuteToolCommand cmd) {
        long startTime = System.currentTimeMillis();
        log.info("执行工具: toolCode={}, tenantId={}", toolCode, cmd.tenantId());

        try {
            // 验证工具和授权
            Tool tool = toolRepo.findByCode(toolCode)
                    .orElseThrow(() -> McpToolException.toolNotFound(toolCode));

            if (!tool.enabled()) {
                throw McpToolException.toolDisabled(toolCode, tool.code());
            }

            ToolAuth auth = toolAuthRepo.findByToolIdAndTenantId(tool.id(), cmd.tenantId())
                    .orElseThrow(() -> new McpToolException(ErrorCode.BIZ_AUTH_003));

            if (!auth.enabled()) {
                throw new McpToolException(ErrorCode.BIZ_AUTH_004);
            }

            // 检查配额
            if (auth.quotaLimit() != null && auth.quotaUsed() >= auth.quotaLimit()) {
                throw McpToolException.quotaExceeded(
                        String.valueOf(cmd.tenantId()),
                        auth.quotaUsed(),
                        auth.quotaLimit()
                );
            }

            // 创建调用日志
            ToolCallLog callLog = ToolCallLog.create(
                    tool.id(),
                    cmd.tenantId(),
                    cmd.userId(),
                    cmd.threadId(),
                    null, // flowRunId
                    cmd.params()
            );
            callLog = toolCallLogRepo.save(callLog);

            // 执行工具
            if (cmd.asyncMode()) {
                // 异步执行
                executeToolAsync(tool, auth, callLog, cmd);
                return new ToolExecutionResultDTO(
                        callLog.id(),
                        McpConstants.CallStatus.RUNNING,
                        null,
                        null,
                        System.currentTimeMillis() - startTime
                );
            } else {
                // 同步执行
                return executeToolSync(tool, auth, callLog, cmd, startTime);
            }

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_MCP_007)
                    .cause(e)
                    .context("operation", "executeTool")
                    .context("toolCode", toolCode)
                    .build();
        }
    }

    public PageResultDTO<ToolCallLogDTO> getExecutionHistory(String toolCode, Long tenantId, int page, int size) {
        try {
            Tool tool = toolRepo.findByCode(toolCode)
                    .orElseThrow(() -> McpToolException.toolNotFound(toolCode));

            List<ToolCallLog> logs;
            long total;

            if (tenantId != null) {
                logs = toolCallLogRepo.findByToolIdAndTenantId(tool.id(), tenantId, page, size);
                total = toolCallLogRepo.countByToolIdAndTenantId(tool.id(), tenantId);
            } else {
                logs = toolCallLogRepo.findByToolId(tool.id(), page, size);
                total = toolCallLogRepo.countByToolId(tool.id());
            }

            List<ToolCallLogDTO> logDTOs = logs.stream()
                    .map(this::toToolCallLogDTO)
                    .collect(Collectors.toList());

            return PageResultDTO.of(logDTOs, total, page, size);

        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw e;
            }
            throw BusinessException.builder(ErrorCode.BIZ_MCP_015)
                    .cause(e)
                    .context("operation", "getExecutionHistory")
                    .context("toolCode", toolCode)
                    .build();
        }
    }

    // =================== 统计和监控 ===================

    public ToolStatisticsDTO getToolStatistics(String timeRange, Long tenantId) {
        try {
            OffsetDateTime since = calculateSinceTime(timeRange);

            int totalTools = (int) toolRepo.count();
            int enabledTools = toolRepo.countByEnabled(true);
            int totalExecutions = toolCallLogRepo.countSince(since);
            int successfulExecutions = toolCallLogRepo.countByStatusSince(McpConstants.CallStatus.SUCCESS, since);
            int failedExecutions = toolCallLogRepo.countByStatusSince(McpConstants.CallStatus.FAILED, since);

            double successRate = totalExecutions > 0 ?
                    (double) successfulExecutions / totalExecutions : 0.0;

            // 获取热门工具
            List<ToolUsageDTO> topTools = toolCallLogRepo.getTopUsedTools(10, since);

            return new ToolStatisticsDTO(
                    tenantId,
                    totalTools,
                    enabledTools,
                    totalExecutions,
                    successfulExecutions,
                    failedExecutions,
                    successRate,
                    topTools,
                    timeRange
            );

        } catch (Exception e) {
            throw BusinessException.builder(ErrorCode.BIZ_MCP_016)
                    .cause(e)
                    .context("operation", "getToolStatistics")
                    .context("tenantId", tenantId)
                    .build();
        }
    }

    public McpHealthStatus checkHealth() {
        Map<String, String> components = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // 检查数据库连接
            try {
                toolRepo.count();
                components.put("database", "healthy");
            } catch (Exception e) {
                components.put("database", "unhealthy: " + e.getMessage());
            }

            // 检查执行服务
            try {
                boolean available = toolExecutionService.isHealthy();
                components.put("execution_service", available ? "healthy" : "unhealthy");
            } catch (Exception e) {
                components.put("execution_service", "unhealthy: " + e.getMessage());
            }

            boolean allHealthy = components.values().stream()
                    .allMatch(status -> status.equals("healthy"));

            long responseTime = System.currentTimeMillis() - startTime;

            return new McpHealthStatus(
                    allHealthy ? "healthy" : "unhealthy",
                    components,
                    responseTime
            );

        } catch (Exception e) {
            return new McpHealthStatus(
                    "unhealthy",
                    Map.of("error", e.getMessage()),
                    System.currentTimeMillis() - startTime
            );
        }
    }

    // =================== 私有辅助方法 ===================

    private void validateToolConfig(RegisterToolCommand cmd) {
        // 验证参数Schema
        if (cmd.paramSchema() != null && !isValidJsonSchema(cmd.paramSchema())) {
            throw new McpToolException(ErrorCode.BIZ_MCP_011);
        }

        // 验证结果Schema
        if (cmd.resultSchema() != null && !isValidJsonSchema(cmd.resultSchema())) {
            throw new McpToolException(ErrorCode.BIZ_MCP_012);
        }

        // 验证端点URL
        if ("HTTP".equals(cmd.type()) && (cmd.endpoint() == null || cmd.endpoint().trim().isEmpty())) {
            throw new McpToolException(ErrorCode.BIZ_MCP_013);
        }
    }

    private boolean isValidJsonSchema(String schema) {
        // 简化的JSON Schema验证
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.readTree(schema);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void executeToolAsync(Tool tool, ToolAuth auth, ToolCallLog callLog, ExecuteToolCommand cmd) {
        CompletableFuture.runAsync(() -> {
            try {
                executeToolSync(tool, auth, callLog, cmd, System.currentTimeMillis());
            } catch (Exception e) {
                log.error("异步工具执行失败: toolCode={}, logId={}", tool.code(), callLog.id(), e);
                // 更新调用日志状态
                updateCallLogStatus(callLog.id(), ToolCallStatus.FAILED, null, e.getMessage());
            }
        }, mcpToolAsyncExecutor);
    }

    private ToolExecutionResultDTO executeToolSync(Tool tool, ToolAuth auth, ToolCallLog callLog,
                                                   ExecuteToolCommand cmd, long startTime) {
        try {
            // 调用执行服务
            Map<String, Object> result = toolExecutionService.execute(
                    tool, auth, cmd.params(), cmd.timeoutSeconds());

            // 更新配额使用
            auth = auth.incrementQuotaUsed();
            toolAuthRepo.save(auth);

            // 更新调用日志
            long duration = System.currentTimeMillis() - startTime;
            updateCallLogStatus(callLog.id(), ToolCallStatus.SUCCESS, result, null);

            return new ToolExecutionResultDTO(
                    callLog.id(),
                    McpConstants.CallStatus.SUCCESS,
                    result,
                    null,
                    duration
            );

        } catch (Exception e) {
            log.error("工具执行失败: toolCode={}", tool.code(), e);
            long duration = System.currentTimeMillis() - startTime;
            updateCallLogStatus(callLog.id(), ToolCallStatus.FAILED, null, e.getMessage());

            return new ToolExecutionResultDTO(
                    callLog.id(),
                    McpConstants.CallStatus.FAILED,
                    null,
                    e.getMessage(),
                    duration
            );
        }
    }

    private void updateCallLogStatus(Long logId, ToolCallStatus status, Map<String, Object> result, String errorMsg) {
        try {
            ToolCallLog log = toolCallLogRepo.findById(logId).orElse(null);
            if (log != null) {
                log = log.updateStatus(status, result, errorMsg);
                toolCallLogRepo.save(log);
            }
        } catch (Exception e) {
            log.error("更新调用日志状态失败: logId={}", logId, e);
        }
    }

    private OffsetDateTime calculateSinceTime(String timeRange) {
        if (timeRange == null) {
            return OffsetDateTime.now().minusHours(24);
        }

        return switch (timeRange.toLowerCase()) {
            case "1h", "hour" -> OffsetDateTime.now().minusHours(1);
            case "1d", "day" -> OffsetDateTime.now().minusDays(1);
            case "7d", "week" -> OffsetDateTime.now().minusDays(7);
            case "30d", "month" -> OffsetDateTime.now().minusDays(30);
            default -> OffsetDateTime.now().minusHours(24);
        };
    }

    // =================== DTO转换方法 ===================

    private ToolDTO toToolDTO(Tool tool) {
        return new ToolDTO(
                tool.id(),
                tool.code(),
                tool.name(),
                tool.type().name(),
                tool.description(),
                tool.iconUrl(),
                tool.enabled(),
                tool.createdAt()
        );
    }

    private ToolAuthDTO toToolAuthDTO(ToolAuth auth, Tool tool) {
        return new ToolAuthDTO(
                auth.toolId(),
                tool != null ? tool.code() : null,
                tool != null ? tool.name() : null,
                auth.tenantId(),
                auth.quotaLimit(),
                auth.quotaUsed(),
                auth.enabled(),
                auth.grantedAt()
        );
    }

    private ToolCallLogDTO toToolCallLogDTO(ToolCallLog log) {
        return new ToolCallLogDTO(
                log.id(),
                log.toolId(),
                log.tenantId(),
                log.userId(),
                log.status(),
                log.latencyMs(),
                log.createdAt()
        );
    }
}