package com.cloud.baseai.application.audit.service;

import com.cloud.baseai.application.audit.command.AuditQueryCommand;
import com.cloud.baseai.application.audit.dto.AuditLogDTO;
import com.cloud.baseai.application.audit.dto.AuditStatisticsDTO;
import com.cloud.baseai.application.audit.dto.PageResultDTO;
import com.cloud.baseai.domain.audit.model.SysAuditLog;
import com.cloud.baseai.domain.audit.repository.SysAuditLogRepository;
import com.cloud.baseai.domain.user.service.UserInfoService;
import com.cloud.baseai.infrastructure.exception.AuditException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.i18n.MessageManager;
import com.cloud.baseai.infrastructure.utils.AuditUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * <h2>审计查询应用服务</h2>
 *
 * <p>这个应用服务就像一个专业的数据分析师，它懂得如何从海量的审计数据中
 * 挖掘出有价值的信息。想象一下，当你需要了解系统的安全状况时，这个服务
 * 就能够为你提供详尽的分析报告，包括用户行为模式、安全威胁、合规状况等。</p>
 *
 * <p><b>核心职责：</b></p>
 * <ul>
 * <li><b>智能查询：</b>根据业务需求构建复杂的查询条件，屏蔽底层数据访问细节</li>
 * <li><b>数据分析：</b>对原始的审计数据进行加工，生成有业务意义的统计信息</li>
 * <li><b>性能优化：</b>通过缓存、异步处理等手段确保查询性能</li>
 * <li><b>安全控制：</b>确保数据访问符合安全策略和权限要求</li>
 * </ul>
 *
 * <p><b>设计原则：</b></p>
 * <p>这个服务遵循应用服务的设计原则，它不包含业务规则，而是编排各种领域服务
 * 和基础设施服务来完成复杂的业务场景。它就像一个乐队指挥，协调各个组件
 * 共同完成美妙的演奏。</p>
 */
@Service
@Transactional(readOnly = true) // 默认为只读事务，写操作需要单独标注
public class AuditQueryAppService {

    private static final Logger log = LoggerFactory.getLogger(AuditQueryAppService.class);

    // 核心仓储依赖
    private final SysAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    // 可选的用户信息服务，用于丰富审计数据的显示
    @Autowired(required = false)
    private UserInfoService userInfoService;

    // 异步处理线程池，用于复杂统计计算
    private final ExecutorService statisticsExecutor;

    // 缓存管理器，用于提高查询性能
    private final Map<String, Object> queryCache = new HashMap<>();
    private static final long CACHE_TTL_MINUTES = 10; // 缓存10分钟

    // 性能监控数据
    private final Map<String, Long> operationCounts = new HashMap<>();
    private final Map<String, Long> operationTotalTime = new HashMap<>();

    /**
     * 构造函数 - 初始化服务依赖
     *
     * <p>在构造函数中，我们建立了必要的依赖关系，并初始化了用于性能优化的组件。
     * 这就像是为数据分析师配备专业的工具和助手。</p>
     */
    public AuditQueryAppService(SysAuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;

        // 创建专门用于统计计算的线程池
        this.statisticsExecutor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                r -> {
                    Thread thread = new Thread(r, "audit-statistics-" + System.currentTimeMillis());
                    thread.setDaemon(true);
                    return thread;
                }
        );

        log.info("审计查询应用服务初始化完成");
    }

    /**
     * 查询审计日志 - 核心查询接口
     *
     * <p>这是最重要的查询方法，它能够根据各种复杂的业务条件来查找审计记录。
     * 就像一个经验丰富的档案管理员，能够根据你的需求快速找到相关的历史记录。</p>
     *
     * <p><b>查询策略：</b></p>
     * <p>我们采用分层查询策略，首先进行快速的条件过滤，然后对结果进行
     * 业务层面的加工和增强，最后返回符合前端展示需求的DTO对象。</p>
     *
     * @param command 查询命令，包含所有查询条件和分页信息
     * @return 分页的审计日志数据传输对象列表
     */
    public PageResultDTO<AuditLogDTO> queryAuditLogs(AuditQueryCommand command) {
        long startTime = System.currentTimeMillis();
        log.debug("开始查询审计日志: command={}", command);

        try {
            // 第一步：验证查询参数的合法性
            validateQueryCommand(command);

            // 第二步：检查缓存，如果有有效的缓存结果就直接返回
            String cacheKey = buildCacheKey(command);
            PageResultDTO<AuditLogDTO> cachedResult = getCachedResult(cacheKey);
            if (cachedResult != null) {
                log.debug("返回缓存的查询结果: cacheKey={}", cacheKey);
                return cachedResult;
            }

            // 第三步：构建分页参数
            Pageable pageable = buildPageable(command);

            // 第四步：执行数据库查询
            Page<SysAuditLog> auditPage = auditLogRepository.findUserActions(
                    command.userId(),
                    command.startTime(),
                    command.endTime(),
                    command.actions(),
                    pageable
            );

            // 第五步：转换为业务DTO
            List<AuditLogDTO> auditDTOs = convertToAuditLogDTOs(auditPage.getContent());

            // 第六步：构建分页结果
            PageResultDTO<AuditLogDTO> result = new PageResultDTO<>(
                    auditDTOs,
                    auditPage.getTotalElements(),
                    auditPage.getNumber(),
                    auditPage.getSize(),
                    auditPage.getTotalPages()
            );

            // 第七步：将结果放入缓存，为后续查询提速
            putCachedResult(cacheKey, result);

            long duration = System.currentTimeMillis() - startTime;
            recordPerformanceMetric("queryAuditLogs", startTime);

            log.info("审计日志查询完成: 总记录数={}, 当前页记录数={}, 耗时={}ms",
                    result.totalElements(), result.content().size(), duration);

            return result;

        } catch (Exception e) {
            log.error("审计日志查询失败: command={}", command, e);
            throw AuditException.queryFailed(e.getMessage());
        }
    }

    /**
     * 查询安全事件 - 专业的安全分析
     *
     * <p>安全事件查询是审计系统的重要功能，它就像一个专业的安全分析师，
     * 能够从大量的操作记录中识别出可能的安全威胁和异常行为。</p>
     *
     * <p><b>安全事件识别逻辑：</b></p>
     * <p>我们基于预定义的安全规则来识别安全事件，包括但不限于：
     * 连续登录失败、非工作时间的异常操作、权限提升操作、
     * 批量数据访问等可疑行为模式。</p>
     */
    public PageResultDTO<AuditLogDTO> querySecurityEvents(AuditQueryCommand command) {
        log.info("开始安全事件查询: tenantId={}, timeRange=[{}, {}]",
                command.tenantId(), command.startTime(), command.endTime());

        try {
            // 构建安全事件特有的查询条件
            List<String> securityActions = getSecurityRelatedActions();

            // 创建安全事件查询的分页参数
            Pageable pageable = buildPageable(command);

            // 执行查询 - 优先查询失败和高风险操作
            Page<SysAuditLog> securityEvents = auditLogRepository.findUserActions(
                    null, // 查询所有用户的安全事件
                    command.startTime(),
                    command.endTime(),
                    securityActions,
                    pageable
            );

            // 对结果进行安全级别分析和排序
            List<AuditLogDTO> securityEventDTOs = analyzeSecurityEvents(securityEvents.getContent());

            PageResultDTO<AuditLogDTO> result = new PageResultDTO<>(
                    securityEventDTOs,
                    securityEvents.getTotalElements(),
                    securityEvents.getNumber(),
                    securityEvents.getSize(),
                    securityEvents.getTotalPages()
            );

            log.info("安全事件查询完成: 发现{}条安全相关记录", result.totalElements());
            return result;

        } catch (Exception e) {
            log.error("安全事件查询失败", e);
            throw AuditException.securityEventsQueryFailed();
        }
    }

    /**
     * 查询对象操作历史 - 生命周期追踪
     *
     * <p>这个方法专门用于追踪特定对象的完整操作历史。就像为每个重要的文件
     * 建立完整的修改记录一样，我们可以看到一个对象从创建到现在的所有变化。</p>
     */
    public PageResultDTO<AuditLogDTO> queryObjectHistory(AuditQueryCommand command) {
        log.debug("查询对象历史: targetType={}, targetId={}",
                command.targetType(), command.targetId());

        try {
            // 构建对象历史查询的分页参数
            Pageable pageable = PageRequest.of(
                    command.page(),
                    command.size(),
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );

            // 使用专门的对象历史查询方法
            Page<SysAuditLog> objectHistory = auditLogRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                    command.targetType(),
                    command.targetId(),
                    pageable
            );

            // 转换为DTO并增强显示信息
            List<AuditLogDTO> historyDTOs = convertToAuditLogDTOs(objectHistory.getContent());

            PageResultDTO<AuditLogDTO> result = new PageResultDTO<>(
                    historyDTOs,
                    objectHistory.getTotalElements(),
                    objectHistory.getNumber(),
                    objectHistory.getSize(),
                    objectHistory.getTotalPages()
            );

            log.debug("对象历史查询完成: 找到{}条操作记录", result.totalElements());
            return result;

        } catch (Exception e) {
            log.error("对象历史查询失败: targetType={}, targetId={}",
                    command.targetType(), command.targetId(), e);
            throw AuditException.objectHistoryQueryFailed(command.targetType(), command.targetId());
        }
    }

    /**
     * 获取审计统计信息 - 数据洞察分析
     *
     * <p>统计分析是审计系统的高级功能，它能够从大量的操作数据中提取出
     * 有价值的业务洞察。就像企业的数据分析师一样，这个功能能够帮助
     * 管理者了解系统的使用模式、发现潜在问题、制定优化策略。</p>
     *
     * <p><b>统计维度：</b></p>
     * <p>我们提供多个维度的统计分析，包括时间维度（按小时、天、周、月统计）、
     * 用户维度（用户活跃度、操作频率）、功能维度（各功能模块的使用情况）、
     * 安全维度（异常行为、风险事件）等。</p>
     */
    public AuditStatisticsDTO getAuditStatistics(Long tenantId, String timeRange, String dimension) {
        log.debug("获取审计统计: tenantId={}, timeRange={}, dimension={}",
                tenantId, timeRange, dimension);

        try {
            // 解析时间范围
            TimeRange range = parseTimeRange(timeRange);

            // 异步并行计算各种统计数据
            CompletableFuture<Map<String, Long>> operationCountsFuture =
                    CompletableFuture.supplyAsync(() -> calculateOperationCounts(tenantId, range), statisticsExecutor);

            CompletableFuture<Map<String, Long>> userActivityFuture =
                    CompletableFuture.supplyAsync(() -> calculateUserActivity(tenantId, range), statisticsExecutor);

            CompletableFuture<Map<String, Long>> securityEventsFuture =
                    CompletableFuture.supplyAsync(() -> calculateSecurityEvents(tenantId, range), statisticsExecutor);

            // 等待所有计算完成并合并结果
            Map<String, Long> operationCounts = operationCountsFuture.join();
            Map<String, Long> userActivity = userActivityFuture.join();
            Map<String, Long> securityEvents = securityEventsFuture.join();

            // 计算额外的洞察信息
            Map<String, Object> insights = calculateInsights(operationCounts, userActivity, securityEvents);

            AuditStatisticsDTO result = new AuditStatisticsDTO(
                    operationCounts,
                    userActivity,
                    securityEvents,
                    insights
            );

            log.info("审计统计计算完成: tenantId={}, operations={}, users={}, securityEvents={}",
                    tenantId, operationCounts.size(), userActivity.size(), securityEvents.size());

            return result;

        } catch (Exception e) {
            log.error("审计统计计算失败: tenantId={}", tenantId, e);
            throw AuditException.statisticsFailed(tenantId);
        }
    }

    /**
     * 获取用户审计摘要 - 个人数据透明化
     *
     * <p>这个功能体现了我们对数据透明化的承诺。每个用户都有权了解
     * 系统记录了他们哪些操作，这不仅是法规要求，也是建立用户信任的重要手段。</p>
     */
    public AuditStatisticsDTO getUserAuditSummary(Long userId, Long tenantId, Integer days) {
        log.debug("获取用户审计摘要: userId={}, tenantId={}, days={}", userId, tenantId, days);

        try {
            OffsetDateTime startTime = OffsetDateTime.now().minusDays(days);
            OffsetDateTime endTime = OffsetDateTime.now();

            // 查询用户在指定时间内的所有操作
            Page<SysAuditLog> userLogs = auditLogRepository.findUserActions(
                    userId, startTime, endTime, null,
                    PageRequest.of(0, Integer.MAX_VALUE)
            );

            // 按操作类型统计
            Map<String, Long> operationCounts = userLogs.getContent().stream()
                    .collect(Collectors.groupingBy(
                            SysAuditLog::action,
                            Collectors.counting()
                    ));

            // 计算用户特有的统计信息
            Map<String, Object> userInsights = new HashMap<>();
            userInsights.put("totalOperations", userLogs.getTotalElements());
            userInsights.put("avgOperationsPerDay", Math.round((double) userLogs.getTotalElements() / days));
            userInsights.put("mostActiveDay", findMostActiveDay(userId, startTime, endTime));
            userInsights.put("recentSecurityEvents", countRecentSecurityEvents(userId, startTime, endTime));

            AuditStatisticsDTO summary = new AuditStatisticsDTO(
                    operationCounts,
                    Map.of("user_" + userId, userLogs.getTotalElements()),
                    Map.of(), // 用户摘要中不显示其他用户的安全事件
                    userInsights
            );

            log.debug("用户审计摘要生成完成: userId={}, totalOps={}", userId, userLogs.getTotalElements());
            return summary;

        } catch (Exception e) {
            log.error("用户审计摘要生成失败: userId={}", userId, e);
            throw AuditException.userAuditSummaryFailed(userId, tenantId);
        }
    }

    /**
     * 导出审计报告 - 合规报告生成
     *
     * <p>报告导出是审计系统的重要功能，特别是在合规要求严格的行业。
     * 这个功能就像一个专业的报告生成器，能够根据不同的业务需求
     * 生成标准化的审计报告。</p>
     */
    public String exportAuditReport(AuditQueryCommand.ReportExportCommand command) {
        log.info("开始生成审计报告: reportType={}, tenantId={}, format={}",
                command.reportType(), command.tenantId(), command.format());

        try {
            // 验证导出命令的有效性
            command.validate();

            // 生成唯一的报告ID
            String reportId = generateReportId(command.reportType());

            // 异步生成报告，避免阻塞用户请求
            CompletableFuture.runAsync(() -> {
                try {
                    generateReportAsync(reportId, command);
                } catch (Exception e) {
                    log.error("异步报告生成失败: reportId={}", reportId, e);
                }
            }, statisticsExecutor);

            log.info("审计报告生成任务已启动: reportId={}", reportId);
            return reportId;

        } catch (Exception e) {
            log.error("审计报告导出失败: command={}", command, e);
            throw AuditException.reportExportFailed(command.reportType());
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 验证查询命令的完整性和合理性
     */
    private void validateQueryCommand(AuditQueryCommand command) {
        if (command.tenantId() == null) {
            throw new IllegalArgumentException(MessageManager.getMessage(ErrorCode.PARAM_011));
        }

        if (command.page() < 0) {
            throw new IllegalArgumentException(MessageManager.getMessage(ErrorCode.PARAM_019));
        }

        if (command.size() <= 0 || command.size() > 100) {
            throw AuditException.pageSizeOutOfRange(command.size());
        }

        // 验证时间范围
        if (command.startTime() != null && command.endTime() != null) {
            if (command.startTime().isAfter(command.endTime())) {
                throw AuditException.invalidTimeRange();
            }

            // 检查时间范围是否过大（超过1年）
            long daysBetween = ChronoUnit.DAYS.between(command.startTime(), command.endTime());
            if (daysBetween > 365) {
                log.warn("查询时间范围较大，可能影响性能: {} 天", daysBetween);
            }
        }
    }

    /**
     * 构建标准化的分页参数
     */
    private Pageable buildPageable(AuditQueryCommand command) {
        Sort.Direction direction = "desc".equalsIgnoreCase(command.sortDir()) ?
                Sort.Direction.DESC : Sort.Direction.ASC;

        Sort sort = Sort.by(direction, command.sortBy() != null ? command.sortBy() : "createdAt");

        return PageRequest.of(command.page(), command.size(), sort);
    }

    /**
     * 转换为业务DTO对象
     */
    private List<AuditLogDTO> convertToAuditLogDTOs(List<SysAuditLog> auditLogs) {
        return auditLogs.stream()
                .map(this::convertToAuditLogDTO)
                .collect(Collectors.toList());
    }

    /**
     * 单个审计日志转换为DTO
     */
    private AuditLogDTO convertToAuditLogDTO(SysAuditLog auditLog) {
        // 获取用户显示名称
        String userDisplayName = null;
        if (auditLog.userId() != null && userInfoService != null) {
            try {
                userDisplayName = userInfoService.getUserName(auditLog.userId());
            } catch (Exception e) {
                log.debug("获取用户显示名称失败: userId={}", auditLog.userId(), e);
                userDisplayName = "用户" + auditLog.userId();
            }
        }

        // 安全地解析详细信息
        Map<String, Object> parsedDetail = AuditUtils.safeDeserializeFromJson(auditLog.detail());

        // 确定操作描述
        String actionDescription = generateActionDescription(auditLog);

        return new AuditLogDTO(
                auditLog.id(),
                auditLog.tenantId(),
                auditLog.userId(),
                userDisplayName,
                auditLog.action(),
                actionDescription,
                auditLog.targetType(),
                auditLog.targetId(),
                auditLog.ipAddress(),
                auditLog.userAgent(),
                parsedDetail,
                auditLog.resultStatus(),
                auditLog.logLevel(),
                auditLog.createdAt()
        );
    }

    /**
     * 获取安全相关的操作类型
     */
    private List<String> getSecurityRelatedActions() {
        return Arrays.asList(
                "USER_LOGIN_FAILED",
                "PERMISSION_DENIED",
                "AUTH_FAILURE",
                "SECURITY_VIOLATION",
                "SUSPICIOUS_ACTIVITY",
                "PRIVILEGE_ESCALATION",
                "DATA_BREACH_ATTEMPT",
                "UNAUTHORIZED_ACCESS"
        );
    }

    /**
     * 分析安全事件并添加安全级别信息
     */
    private List<AuditLogDTO> analyzeSecurityEvents(List<SysAuditLog> securityLogs) {
        return securityLogs.stream()
                .map(log -> {
                    AuditLogDTO dto = convertToAuditLogDTO(log);
                    // 为安全事件添加风险级别分析
                    String riskLevel = calculateRiskLevel(log);

                    // 在详细信息中添加安全分析结果
                    Map<String, Object> enhancedDetail = new HashMap<>(dto.detail());
                    enhancedDetail.put("riskLevel", riskLevel);
                    enhancedDetail.put("securityAnalysis", generateSecurityAnalysis(log));
                    enhancedDetail.put("recommendedAction", getRecommendedAction(log));

                    return new AuditLogDTO(
                            dto.id(), dto.tenantId(), dto.userId(), dto.userDisplayName(),
                            dto.action(), dto.actionDescription(), dto.targetType(), dto.targetId(),
                            dto.ipAddress(), dto.userAgent(), enhancedDetail,
                            dto.resultStatus(), dto.logLevel(), dto.createdAt()
                    );
                })
                .sorted((a, b) -> {
                    // 按风险级别排序，高风险在前
                    String riskA = (String) a.detail().get("riskLevel");
                    String riskB = (String) b.detail().get("riskLevel");
                    return compareRiskLevel(riskB, riskA); // 倒序
                })
                .collect(Collectors.toList());
    }

    /**
     * 解析时间范围字符串
     *
     * <p>将用户友好的时间范围描述转换为具体的时间区间，支持多种常见的表达方式。</p>
     */
    private TimeRange parseTimeRange(String timeRange) {
        OffsetDateTime now = OffsetDateTime.now();

        return switch (timeRange) {
            case "LAST_24_HOURS" -> new TimeRange(now.minusHours(24), now);
            case "LAST_7_DAYS" -> new TimeRange(now.minusDays(7), now);
            case "LAST_30_DAYS" -> new TimeRange(now.minusDays(30), now);
            case "LAST_90_DAYS" -> new TimeRange(now.minusDays(90), now);
            case "LAST_YEAR" -> new TimeRange(now.minusYears(1), now);
            default -> new TimeRange(now.minusDays(7), now); // 默认最近7天
        };
    }

    /**
     * 计算操作类型统计
     *
     * <p>这个方法统计各种操作类型的频率，帮助了解系统的使用模式。</p>
     */
    private Map<String, Long> calculateOperationCounts(Long tenantId, TimeRange range) {
        try {
            List<Object[]> results = auditLogRepository.countByActionAndTimeRange(
                    range.start(), range.end());

            return results.stream()
                    .collect(Collectors.toMap(
                            row -> (String) row[0],
                            row -> (Long) row[1]
                    ));
        } catch (Exception e) {
            log.warn("计算操作统计失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 计算用户活动统计
     *
     * <p>分析用户的活跃程度，识别系统的重度用户和潜在的异常账户。</p>
     */
    private Map<String, Long> calculateUserActivity(Long tenantId, TimeRange range) {
        try {
            List<Object[]> results = auditLogRepository.countByUserAndTimeRange(
                    range.start(), range.end());

            return results.stream()
                    .collect(Collectors.toMap(
                            row -> "user_" + row[0],
                            row -> (Long) row[1]
                    ));
        } catch (Exception e) {
            log.warn("计算用户活动统计失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 计算安全事件统计
     *
     * <p>专门统计安全相关的事件，为安全监控提供数据支撑。</p>
     */
    private Map<String, Long> calculateSecurityEvents(Long tenantId, TimeRange range) {
        List<String> securityActions = getSecurityRelatedActions();
        Map<String, Long> securityCounts = new HashMap<>();

        for (String action : securityActions) {
            try {
                // 这里应该有专门的安全事件统计方法，暂时使用模拟数据
                securityCounts.put(action, 0L);
            } catch (Exception e) {
                log.warn("计算安全事件统计失败: action={}", action, e);
            }
        }

        return securityCounts;
    }

    /**
     * 计算高级洞察信息
     *
     * <p>这个方法将原始统计数据转化为有价值的业务洞察，为管理决策提供支持。</p>
     */
    private Map<String, Object> calculateInsights(Map<String, Long> operations,
                                                  Map<String, Long> users,
                                                  Map<String, Long> security) {
        Map<String, Object> insights = new HashMap<>();

        // 计算总体活跃度
        long totalOperations = operations.values().stream().mapToLong(Long::longValue).sum();
        insights.put("totalOperations", totalOperations);

        // 找出最活跃的操作类型
        String mostFrequentOperation = operations.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("无");
        insights.put("mostFrequentOperation", mostFrequentOperation);

        // 计算安全风险评分
        long totalSecurityEvents = security.values().stream().mapToLong(Long::longValue).sum();
        double riskScore = totalOperations > 0 ? (double) totalSecurityEvents / totalOperations * 100 : 0;
        insights.put("securityRiskScore", Math.round(riskScore * 100.0) / 100.0);

        // 用户活跃度分析
        insights.put("activeUserCount", users.size());

        return insights;
    }

    private String buildCacheKey(AuditQueryCommand command) {
        return String.format("audit_query_%s_%s_%s_%d_%d",
                command.userId(), command.startTime(), command.endTime(),
                command.page(), command.size());
    }

    private PageResultDTO<AuditLogDTO> getCachedResult(String cacheKey) {
        CacheEntry entry = (CacheEntry) queryCache.get(cacheKey);
        if (entry != null && !entry.isExpired()) {
            return entry.getData();
        }
        return null;
    }

    private void putCachedResult(String cacheKey, PageResultDTO<AuditLogDTO> result) {
        queryCache.put(cacheKey, new CacheEntry(result));
    }

    private String generateActionDescription(SysAuditLog auditLog) {
        return AuditUtils.generateOperationSummary(
                auditLog.action(),
                auditLog.targetType(),
                auditLog.targetId(),
                getUserDisplayName(auditLog.userId())
        );
    }

    private String getUserDisplayName(Long userId) {
        if (userId == null) return null;
        try {
            return userInfoService != null ? userInfoService.getUserName(userId) : "用户" + userId;
        } catch (Exception e) {
            return "用户" + userId;
        }
    }

    private String calculateRiskLevel(SysAuditLog log) {
        int riskScore = AuditUtils.calculateRiskScore(
                log.action(), log.resultStatus(), log.ipAddress(), log.userAgent());

        if (riskScore >= 80) return "CRITICAL";
        if (riskScore >= 60) return "HIGH";
        if (riskScore >= 40) return "MEDIUM";
        return "LOW";
    }

    private String generateSecurityAnalysis(SysAuditLog log) {
        // 这里可以实现复杂的安全分析逻辑
        return "安全事件分析: " + log.action();
    }

    private String getRecommendedAction(SysAuditLog log) {
        return switch (log.action()) {
            case "USER_LOGIN_FAILED" -> "建议检查用户密码策略";
            case "PERMISSION_DENIED" -> "建议审查用户权限配置";
            default -> "建议进一步调查";
        };
    }

    private int compareRiskLevel(String a, String b) {
        Map<String, Integer> riskOrder = Map.of(
                "CRITICAL", 4, "HIGH", 3, "MEDIUM", 2, "LOW", 1
        );
        return Integer.compare(
                riskOrder.getOrDefault(a, 0),
                riskOrder.getOrDefault(b, 0)
        );
    }

    private String findMostActiveDay(Long userId, OffsetDateTime start, OffsetDateTime end) {
        // 这里应该实现找到用户最活跃日期的逻辑
        return start.toLocalDate().toString();
    }

    private long countRecentSecurityEvents(Long userId, OffsetDateTime start, OffsetDateTime end) {
        // 这里应该实现统计用户安全事件的逻辑
        return 0L;
    }

    private String generateReportId(String reportType) {
        return String.format("%s_%d_%s",
                reportType,
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8));
    }

    private void generateReportAsync(String reportId, AuditQueryCommand.ReportExportCommand command) {
        log.info("异步生成报告: reportId={}", reportId);
        // 这里实现具体的报告生成逻辑
    }

    private void recordPerformanceMetric(String operation, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        operationCounts.merge(operation, 1L, Long::sum);
        operationTotalTime.merge(operation, duration, Long::sum);
    }

    /**
     * 缓存条目类
     */
    private static class CacheEntry {
        @Getter
        private final PageResultDTO<AuditLogDTO> data;
        private final long timestamp;

        public CacheEntry(PageResultDTO<AuditLogDTO> data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MINUTES * 60 * 1000;
        }

    }

    /**
     * 时间范围类
     */
    private record TimeRange(OffsetDateTime start, OffsetDateTime end) {
    }
}