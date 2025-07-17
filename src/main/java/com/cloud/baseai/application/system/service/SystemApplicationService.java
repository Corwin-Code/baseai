package com.cloud.baseai.application.system.service;

import com.cloud.baseai.application.system.command.BatchUpdateSettingsCommand;
import com.cloud.baseai.application.system.command.CreateSystemTaskCommand;
import com.cloud.baseai.application.system.command.RetryTaskCommand;
import com.cloud.baseai.application.system.command.UpdateSystemSettingCommand;
import com.cloud.baseai.application.system.dto.*;
import com.cloud.baseai.application.user.service.UserInfoService;
import com.cloud.baseai.domain.system.model.SystemSetting;
import com.cloud.baseai.domain.system.model.SystemTask;
import com.cloud.baseai.domain.system.model.enums.SettingValueType;
import com.cloud.baseai.domain.system.model.enums.TaskStatus;
import com.cloud.baseai.domain.system.repository.SystemSettingRepository;
import com.cloud.baseai.domain.system.repository.SystemTaskRepository;
import com.cloud.baseai.infrastructure.exception.SystemBusinessException;
import com.cloud.baseai.infrastructure.exception.SystemTechnicalException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * <h2>系统管理应用服务</h2>
 *
 * <p>系统管理服务是整个平台的"中枢神经系统"。它不仅管理着各种配置参数，
 * 还协调着异步任务的执行，监控着系统的健康状态。就像一个经验丰富的系统管理员，
 * 它需要确保系统的稳定运行，及时发现和处理各种问题。</p>
 *
 * <p><b>核心职责：</b></p>
 * <p>1. <b>配置管理：</b>提供安全、可靠的系统配置管理能力</p>
 * <p>2. <b>任务调度：</b>协调各种异步任务的创建、执行和监控</p>
 * <p>3. <b>健康监控：</b>实时监控系统各组件的健康状态</p>
 * <p>4. <b>统计分析：</b>提供系统运行的统计数据和趋势分析</p>
 *
 * <p><b>设计理念：</b></p>
 * <p>系统服务必须做到"稳如磐石"——即使在高并发、大压力的情况下，
 * 也要保证配置的一致性和任务的可靠执行。同时要具备良好的可观测性，
 * 让运维人员能够及时了解系统状态。</p>
 */
@Service
public class SystemApplicationService {

    private static final Logger log = LoggerFactory.getLogger(SystemApplicationService.class);

    // 领域仓储
    private final SystemSettingRepository settingRepo;
    private final SystemTaskRepository taskRepo;

    // 异步执行器
    private final ExecutorService asyncExecutor;

    // 可选的用户信息服务
    @Autowired(required = false)
    private UserInfoService userInfoService;

    public SystemApplicationService(
            SystemSettingRepository settingRepo,
            SystemTaskRepository taskRepo) {
        this.settingRepo = settingRepo;
        this.taskRepo = taskRepo;
        this.asyncExecutor = Executors.newFixedThreadPool(5);
    }

    // =================== 系统设置管理 ===================

    /**
     * 获取所有系统设置
     *
     * <p>获取系统设置时需要特别注意敏感信息的处理。密码、密钥等敏感配置
     * 不应该以明文形式返回给前端，而应该进行适当的掩码处理。</p>
     */
    public List<SystemSettingDTO> getAllSettings() {
        log.debug("获取所有系统设置");

        try {
            List<SystemSetting> settings = settingRepo.findAll();

            return settings.stream()
                    .map(this::toSystemSettingDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new SystemTechnicalException("GET_SETTINGS_ERROR", "获取系统设置失败", e);
        }
    }

    /**
     * 根据键获取设置
     */
    public Optional<SystemSettingDTO> getSettingByKey(String key) {
        log.debug("获取系统设置: key={}", key);

        try {
            return settingRepo.findByKey(key)
                    .map(this::toSystemSettingDTO);

        } catch (Exception e) {
            throw new SystemTechnicalException("GET_SETTING_ERROR", "获取系统设置失败", e);
        }
    }

    /**
     * 更新系统设置
     *
     * <p>系统设置的更新是一个关键操作，需要进行严格的权限控制和格式验证。
     * 某些核心配置的修改甚至可能需要重启服务才能生效。</p>
     */
    @Transactional
    public SystemSettingDTO updateSetting(UpdateSystemSettingCommand cmd) {
        log.info("更新系统设置: key={}, operatorId={}", cmd.key(), cmd.operatorId());

        try {
            // 验证操作权限（这里简化处理，实际应该检查具体权限）
            validateSystemAdminPermission(cmd.operatorId());

            // 查找现有设置或创建新设置
            Optional<SystemSetting> existingSetting = settingRepo.findByKey(cmd.key());

            SystemSetting setting;
            if (existingSetting.isPresent()) {
                // 更新现有设置
                setting = existingSetting.get().updateValue(cmd.value(), cmd.operatorId());
            } else {
                // 创建新设置（根据值的格式推断类型）
                SettingValueType valueType = inferValueType(cmd.value());
                setting = SystemSetting.create(cmd.key(), cmd.value(), valueType, cmd.remark(), cmd.operatorId());
            }

            // 保存设置
            SystemSetting savedSetting = settingRepo.save(setting);

            // 记录敏感操作日志
            if (savedSetting.isSensitive()) {
                log.warn("敏感系统设置被修改: key={}, operatorId={}", cmd.key(), cmd.operatorId());
            }

            return toSystemSettingDTO(savedSetting);

        } catch (Exception e) {
            log.error("更新系统设置失败: key={}", cmd.key(), e);
            if (e instanceof SystemBusinessException) {
                throw e;
            }
            throw new SystemTechnicalException("UPDATE_SETTING_ERROR", "更新系统设置失败", e);
        }
    }

    /**
     * 批量更新设置
     */
    @Transactional
    public Map<String, SystemSettingDTO> batchUpdateSettings(BatchUpdateSettingsCommand cmd) {
        log.info("批量更新系统设置: 设置数量={}, operatorId={}", cmd.settings().size(), cmd.operatorId());

        try {
            // 验证批量大小
            if (!cmd.isValidBatchSize()) {
                throw new SystemBusinessException("BATCH_SIZE_EXCEEDED", "批量更新设置数量超出限制");
            }

            validateSystemAdminPermission(cmd.operatorId());

            Map<String, SystemSettingDTO> results = new HashMap<>();

            for (Map.Entry<String, String> entry : cmd.settings().entrySet()) {
                try {
                    UpdateSystemSettingCommand singleCmd = new UpdateSystemSettingCommand(
                            entry.getKey(), entry.getValue(), null, cmd.operatorId());
                    SystemSettingDTO result = updateSetting(singleCmd);
                    results.put(entry.getKey(), result);
                } catch (Exception e) {
                    log.warn("批量更新中单个设置失败: key={}", entry.getKey(), e);
                    // 批量更新采用最大努力策略，单个失败不影响其他设置
                }
            }

            return results;

        } catch (Exception e) {
            if (e instanceof SystemBusinessException) {
                throw e;
            }
            throw new SystemTechnicalException("BATCH_UPDATE_SETTINGS_ERROR", "批量更新设置失败", e);
        }
    }

    // =================== 系统任务管理 ===================

    /**
     * 创建系统任务
     *
     * <p>任务的创建是异步处理流程的起点。我们需要确保任务参数的完整性和正确性，
     * 同时为任务分配合适的优先级和执行策略。</p>
     */
    @Transactional
    public SystemTaskDTO createTask(CreateSystemTaskCommand cmd) {
        log.info("创建系统任务: taskType={}, tenantId={}", cmd.taskType(), cmd.tenantId());

        try {
            // 验证任务参数
            if (!cmd.hasValidPayloadSize()) {
                throw new SystemBusinessException("INVALID_PAYLOAD_SIZE", "任务参数过大");
            }

            // 创建任务
            SystemTask task = SystemTask.create(cmd.tenantId(), cmd.taskType(), cmd.payload(), cmd.createdBy());
            SystemTask savedTask = taskRepo.save(task);

            log.info("系统任务创建成功: taskId={}, taskType={}", savedTask.id(), savedTask.taskType());

            return toSystemTaskDTO(savedTask);

        } catch (Exception e) {
            log.error("创建系统任务失败: taskType={}", cmd.taskType(), e);
            if (e instanceof SystemBusinessException) {
                throw e;
            }
            throw new SystemTechnicalException("CREATE_TASK_ERROR", "创建系统任务失败", e);
        }
    }

    /**
     * 获取任务列表
     */
    public PageResultDTO<SystemTaskDTO> listTasks(Long tenantId, String status, int page, int size) {
        log.debug("查询系统任务列表: tenantId={}, status={}, page={}, size={}", tenantId, status, page, size);

        try {
            List<SystemTask> tasks;
            long total;

            if (tenantId != null) {
                tasks = taskRepo.findByTenantId(tenantId, page, size);
                total = taskRepo.countByTenantId(tenantId);
            } else if (status != null) {
                TaskStatus taskStatus = TaskStatus.valueOf(status.toUpperCase());
                tasks = taskRepo.findByStatus(taskStatus, page, size);
                total = taskRepo.countByStatus(taskStatus);
            } else {
                // 简化实现：返回空列表，实际应该实现分页查询所有任务
                tasks = List.of();
                total = 0;
            }

            List<SystemTaskDTO> taskDTOs = tasks.stream()
                    .map(this::toSystemTaskDTO)
                    .collect(Collectors.toList());

            return PageResultDTO.of(taskDTOs, total, page, size);

        } catch (Exception e) {
            throw new SystemTechnicalException("LIST_TASKS_ERROR", "查询任务列表失败", e);
        }
    }

    /**
     * 获取任务详情
     */
    public SystemTaskDetailDTO getTaskDetail(Long taskId) {
        log.debug("获取任务详情: taskId={}", taskId);

        try {
            SystemTask task = taskRepo.findById(taskId)
                    .orElseThrow(() -> new SystemBusinessException("TASK_NOT_FOUND", "任务不存在"));

            return toSystemTaskDetailDTO(task);

        } catch (Exception e) {
            if (e instanceof SystemBusinessException) {
                throw e;
            }
            throw new SystemTechnicalException("GET_TASK_DETAIL_ERROR", "获取任务详情失败", e);
        }
    }

    /**
     * 重试失败任务
     */
    @Transactional
    public SystemTaskDTO retryTask(RetryTaskCommand cmd) {
        log.info("重试任务: taskId={}, operatorId={}", cmd.taskId(), cmd.operatorId());

        try {
            SystemTask task = taskRepo.findById(cmd.taskId())
                    .orElseThrow(() -> new SystemBusinessException("TASK_NOT_FOUND", "任务不存在"));

            if (!task.canRetry()) {
                throw new SystemBusinessException("TASK_CANNOT_RETRY", "任务无法重试");
            }

            SystemTask retriedTask = task.retry();
            SystemTask savedTask = taskRepo.save(retriedTask);

            log.info("任务重试成功: taskId={}, retryCount={}", savedTask.id(), savedTask.retryCount());

            return toSystemTaskDTO(savedTask);

        } catch (Exception e) {
            if (e instanceof SystemBusinessException) {
                throw e;
            }
            throw new SystemTechnicalException("RETRY_TASK_ERROR", "重试任务失败", e);
        }
    }

    // =================== 系统统计和监控 ===================

    /**
     * 获取系统统计信息
     *
     * <p>系统统计为管理员提供了系统运行状况的全貌。通过这些数据，
     * 可以及时发现性能瓶颈、容量问题和异常情况。</p>
     */
    public SystemStatisticsDTO getSystemStatistics() {
        log.debug("获取系统统计信息");

        try {
            // 统计设置数量
            int totalSettings = settingRepo.findAll().size();

            // 统计任务数量
            long totalTasks = taskRepo.countByStatus(TaskStatus.SUCCESS) +
                    taskRepo.countByStatus(TaskStatus.FAILED) +
                    taskRepo.countByStatus(TaskStatus.PENDING) +
                    taskRepo.countByStatus(TaskStatus.PROCESSING);

            long pendingTasks = taskRepo.countByStatus(TaskStatus.PENDING);
            long processingTasks = taskRepo.countByStatus(TaskStatus.PROCESSING);
            long successTasks = taskRepo.countByStatus(TaskStatus.SUCCESS);
            long failedTasks = taskRepo.countByStatus(TaskStatus.FAILED);

            // 计算成功率
            double successRate = totalTasks > 0 ? (double) successTasks / totalTasks : 0.0;

            // 按类型统计任务（简化实现）
            Map<String, Long> tasksByType = Map.of(
                    "EMAIL_SEND", successTasks / 4,
                    "DOCUMENT_PARSE", successTasks / 4,
                    "VECTOR_GENERATION", successTasks / 4,
                    "DATA_CLEANUP", successTasks / 4
            );

            // 获取系统健康状态
            SystemHealthDTO systemHealth = checkSystemHealth();

            return new SystemStatisticsDTO(
                    totalSettings,
                    totalTasks,
                    pendingTasks,
                    processingTasks,
                    successTasks,
                    failedTasks,
                    successRate,
                    tasksByType,
                    systemHealth
            );

        } catch (Exception e) {
            throw new SystemTechnicalException("GET_STATISTICS_ERROR", "获取系统统计失败", e);
        }
    }

    /**
     * 系统健康检查
     *
     * <p>健康检查是系统监控的核心功能。它会检查各个关键组件的状态，
     * 包括数据库连接、任务队列、外部服务等，及时发现潜在问题。</p>
     */
    public SystemHealthDTO checkSystemHealth() {
        log.debug("执行系统健康检查");

        Map<String, SystemHealthDTO.ComponentHealth> components = new HashMap<>();

        try {
            // 检查数据库连接
            try {
                settingRepo.findAll();
                components.put("database", new SystemHealthDTO.ComponentHealth(
                        "healthy", Map.of("connection", "active")
                ));
            } catch (Exception e) {
                components.put("database", new SystemHealthDTO.ComponentHealth(
                        "unhealthy", Map.of("error", e.getMessage())
                ));
            }

            // 检查任务队列状态
            try {
                long pendingTasks = taskRepo.countByStatus(TaskStatus.PENDING);
                String status = pendingTasks > 1000 ? "warning" : "healthy";
                components.put("task_queue", new SystemHealthDTO.ComponentHealth(
                        status, Map.of("pending_tasks", pendingTasks)
                ));
            } catch (Exception e) {
                components.put("task_queue", new SystemHealthDTO.ComponentHealth(
                        "unhealthy", Map.of("error", e.getMessage())
                ));
            }

            // 检查异步执行器状态
            try {
                String status = asyncExecutor.isShutdown() ? "unhealthy" : "healthy";
                components.put("async_executor", new SystemHealthDTO.ComponentHealth(
                        status, Map.of("shutdown", asyncExecutor.isShutdown())
                ));
            } catch (Exception e) {
                components.put("async_executor", new SystemHealthDTO.ComponentHealth(
                        "unhealthy", Map.of("error", e.getMessage())
                ));
            }

            // 判断整体健康状态
            boolean allHealthy = components.values().stream()
                    .allMatch(health -> "healthy".equals(health.status()));

            String overallStatus = allHealthy ? "healthy" : "unhealthy";

            return new SystemHealthDTO(overallStatus, components, OffsetDateTime.now());

        } catch (Exception e) {
            return new SystemHealthDTO(
                    "unhealthy",
                    Map.of("system", new SystemHealthDTO.ComponentHealth(
                            "unhealthy", Map.of("error", e.getMessage())
                    )),
                    OffsetDateTime.now()
            );
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 验证系统管理员权限
     */
    private void validateSystemAdminPermission(Long operatorId) {
        // 简化实现，实际应该检查用户的系统管理员权限
        if (operatorId == null) {
            throw new SystemBusinessException("UNAUTHORIZED", "需要系统管理员权限");
        }
        log.debug("验证系统管理员权限: operatorId={}", operatorId);
    }

    /**
     * 推断设置值的类型
     */
    private SettingValueType inferValueType(String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return SettingValueType.BOOLEAN;
        }

        try {
            Integer.parseInt(value);
            return SettingValueType.INTEGER;
        } catch (NumberFormatException e) {
            // 不是整数，继续检查
        }

        // 简单的JSON检查
        if ((value.startsWith("{") && value.endsWith("}")) ||
                (value.startsWith("[") && value.endsWith("]"))) {
            return SettingValueType.JSON;
        }

        return SettingValueType.STRING;
    }

    // =================== DTO转换方法 ===================

    private SystemSettingDTO toSystemSettingDTO(SystemSetting setting) {
        return SystemSettingDTO.createMasked(
                setting.key(),
                setting.getMaskedValue(), // 使用掩码值
                setting.valueType().name(),
                setting.remark(),
                setting.updatedBy(),
                setting.updatedAt(),
                setting.isSensitive()
        );
    }

    private SystemTaskDTO toSystemTaskDTO(SystemTask task) {
        return new SystemTaskDTO(
                task.id(),
                task.tenantId(),
                task.taskType(),
                task.status().name(),
                task.retryCount(),
                task.lastError(),
                task.createdBy(),
                task.createdAt(),
                task.executedAt(),
                task.finishedAt(),
                task.getExecutionDurationMs(),
                task.canRetry()
        );
    }

    private SystemTaskDetailDTO toSystemTaskDetailDTO(SystemTask task) {
        String creatorName = null;
        if (userInfoService != null && task.createdBy() != null) {
            creatorName = userInfoService.getUserName(task.createdBy());
        }

        return new SystemTaskDetailDTO(
                task.id(),
                task.tenantId(),
                task.taskType(),
                task.payload(),
                task.status().name(),
                task.retryCount(),
                task.lastError(),
                task.createdBy(),
                creatorName,
                task.createdAt(),
                task.executedAt(),
                task.finishedAt(),
                task.getExecutionDurationMs(),
                task.canRetry()
        );
    }
}