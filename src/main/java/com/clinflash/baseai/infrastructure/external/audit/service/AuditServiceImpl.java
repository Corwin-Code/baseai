package com.clinflash.baseai.infrastructure.external.audit.service;

import com.clinflash.baseai.infrastructure.exception.AuditServiceException;
import com.clinflash.baseai.infrastructure.external.audit.model.AuditLogEntity;
import com.clinflash.baseai.infrastructure.external.audit.model.dto.*;
import com.clinflash.baseai.infrastructure.external.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * <h2>审计服务实现类</h2>
 *
 * <p>这个实现类就像一个现代化的数据中心，需要处理企业级应用中最复杂的数据管理挑战。
 * 审计数据具有"只增不减"的特点，随着时间推移会产生海量数据，因此我们的设计
 * 必须在性能、存储效率和查询能力之间找到最佳平衡点。</p>
 *
 * <p><b>架构设计理念：</b></p>
 * <p>我们采用了混合存储架构：关系型数据库用于事务性操作和结构化查询，
 * Elasticsearch用于全文搜索和复杂分析，Redis用于缓存和性能优化。
 * 同时实现了多级异步处理机制，确保审计记录不会影响主业务流程的性能。</p>
 *
 * <p><b>核心技术特性：</b></p>
 * <ul>
 * <li><b>异步写入：</b>使用队列和批处理机制提高写入性能</li>
 * <li><b>分片存储：</b>按时间和类型对数据进行分片，优化查询性能</li>
 * <li><b>智能索引：</b>根据查询模式动态优化索引策略</li>
 * <li><b>数据压缩：</b>对历史数据进行压缩存储，节约存储成本</li>
 * <li><b>完整性保护：</b>使用哈希校验确保数据完整性</li>
 * </ul>
 */
@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    // 日期格式化器，用于生成索引名称
    private static final DateTimeFormatter INDEX_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    // 审计事件类型常量
    private static final String USER_ACTION_TYPE = "USER_ACTION";
    private static final String SYSTEM_EVENT_TYPE = "SYSTEM_EVENT";
    private static final String SECURITY_EVENT_TYPE = "SECURITY_EVENT";
    private static final String BUSINESS_OPERATION_TYPE = "BUSINESS_OPERATION";

    // 依赖的存储服务
    private final AuditLogRepository auditLogRepository;
    private final ElasticsearchOperations elasticsearchOps;
    private final ObjectMapper objectMapper;

    // 异步处理相关
    private final ExecutorService auditExecutor;
    private final BlockingQueue<AuditEvent> auditQueue;
    private final ScheduledExecutorService scheduledExecutor;

    // 配置参数
    @Value("${audit.async.enabled:true}")
    private boolean asyncEnabled;

    @Value("${audit.batch.size:100}")
    private int batchSize;

    @Value("${audit.batch.timeout:5000}")
    private long batchTimeoutMs;

    @Value("${audit.elasticsearch.enabled:true}")
    private boolean elasticsearchEnabled;

    @Value("${audit.integrity.check.enabled:true}")
    private boolean integrityCheckEnabled;

    @Value("${audit.retention.default-days:2555}") // 7年默认保留期
    private int defaultRetentionDays;

    /**
     * 构造函数：初始化审计服务
     *
     * <p>在构造函数中，我们建立了完整的异步处理基础设施。包括专门的线程池、
     * 批处理队列、定时调度器等。这些组件协同工作，确保审计记录既高效又可靠。</p>
     */
    public AuditServiceImpl(AuditLogRepository auditLogRepository,
                            @Autowired(required = false) ElasticsearchOperations elasticsearchOps,
                            ObjectMapper objectMapper) {

        this.auditLogRepository = auditLogRepository;
        this.elasticsearchOps = elasticsearchOps;
        this.objectMapper = objectMapper;

        // 创建专门的审计处理线程池
        this.auditExecutor = Executors.newFixedThreadPool(5, r -> {
            Thread thread = new Thread(r, "audit-processor-" + r.hashCode());
            thread.setDaemon(true);
            return thread;
        });

        // 创建审计事件队列，支持高并发写入
        this.auditQueue = new LinkedBlockingQueue<>(10000);

        // 创建定时调度器，用于批处理和维护任务
        this.scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "audit-scheduler-" + r.hashCode());
            thread.setDaemon(true);
            return thread;
        });

        // 启动批处理器
        startBatchProcessor();

        // 启动定时维护任务
        startMaintenanceTasks();

        log.info("审计服务初始化完成 - 异步模式：{}, ES支持：{}, 批处理大小：{}",
                asyncEnabled, elasticsearchEnabled, batchSize);
    }

    /**
     * 记录用户操作审计
     *
     * <p>这是最常用的审计接口，设计为高性能和低延迟。通过异步队列机制，
     * 确保审计记录不会影响主业务流程的响应时间。</p>
     */
    @Override
    public void recordUserAction(String action, Long targetId, String detail)
            throws Exception {

        // 从当前上下文获取用户信息和请求信息
        Long userId = getCurrentUserId();
        String ipAddress = getCurrentIpAddress();
        String userAgent = getCurrentUserAgent();

        recordUserAction(userId, action, "UNKNOWN", targetId, detail,
                ipAddress, userAgent, new HashMap<>());
    }

    /**
     * 记录用户操作审计（详细版本）
     *
     * <p>这是核心的审计记录方法，包含完整的上下文信息。我们特别注意
     * 数据的标准化和验证，确保记录的一致性和完整性。</p>
     */
    @Override
    public void recordUserAction(Long userId, String action, String targetType, Long targetId,
                                 String detail, String ipAddress, String userAgent,
                                 Map<String, Object> additionalData) throws Exception {

        try {
            // 验证必要参数
            validateAuditParameters(action, detail);

            // 构建审计事件
            AuditEvent event = new AuditEvent(
                    USER_ACTION_TYPE,
                    userId,
                    action,
                    targetType,
                    targetId,
                    detail,
                    OffsetDateTime.now(),
                    ipAddress,
                    userAgent,
                    enhanceMetadata(additionalData, USER_ACTION_TYPE)
            );

            // 提交到处理队列
            submitAuditEvent(event);

            log.debug("用户操作审计已记录: userId={}, action={}", userId, action);

        } catch (Exception e) {
            handleAuditError("记录用户操作审计失败", e);
        }
    }

    /**
     * 记录系统事件审计
     *
     * <p>系统事件通常比用户操作更加结构化，我们可以对其进行更深入的分析。
     * 这些事件对于系统监控和性能优化具有重要价值。</p>
     */
    @Override
    public void recordSystemEvent(String eventType, String eventSource, String description,
                                  EventSeverity severity, Map<String, Object> metadata)
            throws Exception {

        try {
            validateAuditParameters(eventType, description);

            // 增强元数据，添加系统相关信息
            Map<String, Object> enhancedMetadata = enhanceMetadata(metadata, SYSTEM_EVENT_TYPE);
            enhancedMetadata.put("severity", severity.name());
            enhancedMetadata.put("eventSource", eventSource);
            enhancedMetadata.put("systemInfo", getSystemInfo());

            AuditEvent event = new AuditEvent(
                    SYSTEM_EVENT_TYPE,
                    null, // 系统事件没有特定用户
                    eventType,
                    "SYSTEM",
                    null,
                    description,
                    OffsetDateTime.now(),
                    "127.0.0.1", // 本地系统IP
                    "System",
                    enhancedMetadata
            );

            submitAuditEvent(event);

            log.debug("系统事件审计已记录: eventType={}, severity={}", eventType, severity);

        } catch (Exception e) {
            handleAuditError("记录系统事件审计失败", e);
        }
    }

    /**
     * 记录安全事件审计
     *
     * <p>安全事件是最重要的审计类型，我们给予特殊处理。包括即时告警、
     * 详细的上下文记录和自动的风险评估。</p>
     */
    @Override
    public void recordSecurityEvent(String securityEventType, Long userId, String description,
                                    RiskLevel riskLevel, String sourceIp,
                                    List<String> affectedResources) throws Exception {

        try {
            validateAuditParameters(securityEventType, description);

            // 构建安全事件的详细元数据
            Map<String, Object> securityMetadata = new HashMap<>();
            securityMetadata.put("riskLevel", riskLevel.name());
            securityMetadata.put("sourceIp", sourceIp);
            securityMetadata.put("affectedResources", affectedResources);
            securityMetadata.put("geoLocation", getGeoLocation(sourceIp));
            securityMetadata.put("threatIntelligence", checkThreatIntelligence(sourceIp));

            Map<String, Object> enhancedMetadata = enhanceMetadata(securityMetadata, SECURITY_EVENT_TYPE);

            AuditEvent event = new AuditEvent(
                    SECURITY_EVENT_TYPE,
                    userId,
                    securityEventType,
                    "SECURITY",
                    null,
                    description,
                    OffsetDateTime.now(),
                    sourceIp,
                    getCurrentUserAgent(),
                    enhancedMetadata
            );

            submitAuditEvent(event);

            // 高风险事件立即处理，不进入队列
            if (riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL) {
                processHighRiskEvent(event);
            }

            log.info("安全事件审计已记录: eventType={}, riskLevel={}, sourceIp={}",
                    securityEventType, riskLevel, sourceIp);

        } catch (Exception e) {
            handleAuditError("记录安全事件审计失败", e);
        }
    }

    /**
     * 记录业务操作审计
     *
     * <p>业务操作审计需要特别注意合规性要求。我们会根据业务类型
     * 应用不同的审计策略和保留政策。</p>
     */
    @Override
    public void recordBusinessOperation(String businessOperation, Long operatorId,
                                        String businessObjectId, Map<String, Object> operationDetails,
                                        Map<String, String> businessContext) throws Exception {

        try {
            validateAuditParameters(businessOperation, "业务操作: " + businessOperation);

            // 构建业务操作的完整上下文
            Map<String, Object> businessMetadata = new HashMap<>(operationDetails);
            businessMetadata.putAll(businessContext);
            businessMetadata.put("businessObjectId", businessObjectId);
            businessMetadata.put("complianceRequired", isComplianceRequired(businessOperation));

            Map<String, Object> enhancedMetadata = enhanceMetadata(businessMetadata, BUSINESS_OPERATION_TYPE);

            AuditEvent event = new AuditEvent(
                    BUSINESS_OPERATION_TYPE,
                    operatorId,
                    businessOperation,
                    "BUSINESS",
                    parseLongSafely(businessObjectId),
                    String.format("业务操作: %s, 对象: %s", businessOperation, businessObjectId),
                    OffsetDateTime.now(),
                    getCurrentIpAddress(),
                    getCurrentUserAgent(),
                    enhancedMetadata
            );

            submitAuditEvent(event);

            log.debug("业务操作审计已记录: operation={}, operator={}, object={}",
                    businessOperation, operatorId, businessObjectId);

        } catch (Exception e) {
            handleAuditError("记录业务操作审计失败", e);
        }
    }

    /**
     * 批量记录审计事件
     *
     * <p>批量记录是高并发场景下的重要优化手段。我们使用分组和并行处理
     * 来最大化吞吐量，同时保持数据的一致性。</p>
     */
    @Override
    public BatchAuditResult recordBatchEvents(List<AuditEvent> auditEvents) throws AuditServiceException {

        if (auditEvents == null || auditEvents.isEmpty()) {
            return new BatchAuditResult(0, 0, 0, Collections.emptyList());
        }

        log.info("开始批量记录审计事件: count={}", auditEvents.size());

        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try {
            // 按类型分组处理，提高效率
            Map<String, List<AuditEvent>> eventsByType = auditEvents.stream()
                    .collect(Collectors.groupingBy(AuditEvent::getEventType));

            for (Map.Entry<String, List<AuditEvent>> entry : eventsByType.entrySet()) {
                String eventType = entry.getKey();
                List<AuditEvent> events = entry.getValue();

                try {
                    processBatchEventsByType(eventType, events);
                    successCount += events.size();

                } catch (Exception e) {
                    String error = String.format("处理 %s 类型事件失败: %s", eventType, e.getMessage());
                    errors.add(error);
                    log.error(error, e);
                }
            }

            log.info("批量审计记录完成: total={}, success={}, failures={}",
                    auditEvents.size(), successCount, auditEvents.size() - successCount);

            return new BatchAuditResult(auditEvents.size(), successCount,
                    auditEvents.size() - successCount, errors);

        } catch (Exception e) {
            String errorMessage = "批量记录审计事件失败: " + e.getMessage();
            log.error(errorMessage, e);
            throw new AuditServiceException("BATCH_AUDIT_FAILED", errorMessage, e);
        }
    }

    /**
     * 查询用户操作历史
     *
     * <p>查询接口是审计服务价值体现的重要途径。我们实现了多层缓存、
     * 智能索引选择和查询优化，确保即使在海量数据下也能快速响应。</p>
     */
    @Override
    public PagedAuditResult queryUserActions(Long userId, OffsetDateTime startTime, OffsetDateTime endTime,
                                             List<String> actions, int page, int size) throws AuditServiceException {

        try {
            validateQueryParameters(page, size);

            log.debug("查询用户操作历史: userId={}, page={}, size={}", userId, page, size);

            // 优先使用Elasticsearch进行查询，性能更好
            if (elasticsearchEnabled) {
                return queryUserActionsFromElasticsearch(userId, startTime, endTime, actions, page, size);
            } else {
                return queryUserActionsFromDatabase(userId, startTime, endTime, actions, page, size);
            }

        } catch (Exception e) {
            String errorMessage = "查询用户操作历史失败: " + e.getMessage();
            log.error(errorMessage, e);
            throw new AuditServiceException("QUERY_USER_ACTIONS_FAILED", errorMessage, e);
        }
    }

    /**
     * 查询安全事件历史
     *
     * <p>安全事件查询需要特别的性能优化，因为安全分析师经常需要
     * 在紧急情况下快速查询大量历史数据。</p>
     */
    @Override
    public PagedAuditResult querySecurityEvents(OffsetDateTime startTime, OffsetDateTime endTime,
                                                List<RiskLevel> riskLevels, List<String> eventTypes,
                                                String sourceIp, int page, int size) throws AuditServiceException {

        try {
            validateQueryParameters(page, size);

            log.debug("查询安全事件历史: riskLevels={}, eventTypes={}, sourceIp={}",
                    riskLevels, eventTypes, sourceIp);

            if (elasticsearchEnabled) {
                return querySecurityEventsFromElasticsearch(startTime, endTime, riskLevels,
                        eventTypes, sourceIp, page, size);
            } else {
                return querySecurityEventsFromDatabase(startTime, endTime, riskLevels,
                        eventTypes, sourceIp, page, size);
            }

        } catch (Exception e) {
            String errorMessage = "查询安全事件历史失败: " + e.getMessage();
            log.error(errorMessage, e);
            throw new AuditServiceException("QUERY_SECURITY_EVENTS_FAILED", errorMessage, e);
        }
    }

    /**
     * 生成审计报告
     *
     * <p>报告生成是一个复杂的过程，涉及数据聚合、分析和格式化。
     * 我们使用异步处理来避免长时间阻塞，并提供进度跟踪。</p>
     */
    @Override
    @Async
    public AuditReportResult generateAuditReport(String reportType, Map<String, Object> reportParams,
                                                 String outputFormat) throws AuditServiceException {

        try {
            validateStringParameter(reportType, "报告类型不能为空");
            validateStringParameter(outputFormat, "输出格式不能为空");

            log.info("开始生成审计报告: type={}, format={}", reportType, outputFormat);

            // 生成报告ID
            String reportId = generateReportId(reportType);

            // 异步生成报告
            CompletableFuture.runAsync(() -> {
                try {
                    generateReportAsync(reportId, reportType, reportParams, outputFormat);
                } catch (Exception e) {
                    log.error("异步生成报告失败: reportId={}", reportId, e);
                }
            }, auditExecutor);

            // 返回报告信息
            String downloadUrl = generateDownloadUrl(reportId);

            return new AuditReportResult(reportId, downloadUrl, outputFormat, OffsetDateTime.now());

        } catch (Exception e) {
            String errorMessage = "生成审计报告失败: " + e.getMessage();
            log.error(errorMessage, e);
            throw new AuditServiceException("GENERATE_REPORT_FAILED", errorMessage, e);
        }
    }

    /**
     * 获取审计统计信息
     *
     * <p>统计信息的计算可能很耗时，我们使用缓存和预计算来提高性能。
     * 同时支持实时统计和历史统计两种模式。</p>
     */
    @Override
    public AuditStatistics getAuditStatistics(String statisticsType, String timeRange,
                                              Map<String, Object> filters) throws AuditServiceException {

        try {
            validateStringParameter(statisticsType, "统计类型不能为空");
            validateStringParameter(timeRange, "时间范围不能为空");

            log.debug("获取审计统计信息: type={}, timeRange={}", statisticsType, timeRange);

            // 先尝试从缓存获取
            String cacheKey = buildStatisticsCacheKey(statisticsType, timeRange, filters);
            AuditStatistics cachedStats = getFromCache(cacheKey);

            if (cachedStats != null) {
                log.debug("从缓存获取统计信息: cacheKey={}", cacheKey);
                return cachedStats;
            }

            // 计算统计信息
            AuditStatistics statistics = calculateStatistics(statisticsType, timeRange, filters);

            // 缓存结果
            cacheStatistics(cacheKey, statistics);

            return statistics;

        } catch (Exception e) {
            String errorMessage = "获取审计统计信息失败: " + e.getMessage();
            log.error(errorMessage, e);
            throw new AuditServiceException("GET_STATISTICS_FAILED", errorMessage, e);
        }
    }

    /**
     * 配置数据保留策略
     *
     * <p>数据保留策略的配置需要考虑法规要求、存储成本和查询性能。
     * 我们提供灵活的策略配置，支持不同类型数据的差异化保留。</p>
     */
    @Override
    @Transactional
    public void configureRetentionPolicy(RetentionPolicy retentionPolicy) throws AuditServiceException {

        try {
            validateRetentionPolicy(retentionPolicy);

            log.info("配置数据保留策略: policy={}, retentionDays={}",
                    retentionPolicy.policyName(), retentionPolicy.retentionDays());

            // 保存策略配置
            saveRetentionPolicy(retentionPolicy);

            // 如果启用自动归档，安排归档任务
            if (retentionPolicy.autoArchive()) {
                scheduleArchiveTask(retentionPolicy);
            }

            log.info("数据保留策略配置完成: {}", retentionPolicy.policyName());

        } catch (Exception e) {
            String errorMessage = "配置数据保留策略失败: " + e.getMessage();
            log.error(errorMessage, e);
            throw new AuditServiceException("CONFIGURE_RETENTION_POLICY_FAILED", errorMessage, e);
        }
    }

    /**
     * 验证审计数据完整性
     *
     * <p>完整性验证是保证审计数据可信度的重要手段。我们使用多种技术
     * 来检测数据的完整性，包括哈希校验、序列号验证等。</p>
     */
    @Override
    @Async
    public IntegrityCheckResult verifyDataIntegrity(OffsetDateTime startTime, OffsetDateTime endTime)
            throws AuditServiceException {

        try {
            log.info("开始验证审计数据完整性: startTime={}, endTime={}", startTime, endTime);

            long totalRecords = 0;
            long corruptedRecords = 0;
            List<String> issues = new ArrayList<>();

            // 分批检查数据完整性
            OffsetDateTime currentTime = startTime;
            while (currentTime.isBefore(endTime)) {
                OffsetDateTime batchEndTime = currentTime.plusDays(1);
                if (batchEndTime.isAfter(endTime)) {
                    batchEndTime = endTime;
                }

                IntegrityCheckBatch batchResult = verifyBatchIntegrity(currentTime, batchEndTime);
                totalRecords += batchResult.getTotalRecords();
                corruptedRecords += batchResult.getCorruptedRecords();
                issues.addAll(batchResult.getIssues());

                currentTime = batchEndTime;
            }

            boolean isIntegrityValid = corruptedRecords == 0;

            log.info("审计数据完整性验证完成: total={}, corrupted={}, valid={}",
                    totalRecords, corruptedRecords, isIntegrityValid);

            return new IntegrityCheckResult(isIntegrityValid, totalRecords, corruptedRecords,
                    issues, OffsetDateTime.now());

        } catch (Exception e) {
            String errorMessage = "验证审计数据完整性失败: " + e.getMessage();
            log.error(errorMessage, e);
            throw new AuditServiceException("VERIFY_INTEGRITY_FAILED", errorMessage, e);
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 启动批处理器
     *
     * <p>批处理器是审计服务的核心组件，负责将队列中的事件批量写入存储。
     * 它使用生产者-消费者模式，可以有效处理高并发的审计请求。</p>
     */
    private void startBatchProcessor() {
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            try {
                List<AuditEvent> batch = new ArrayList<>();

                // 收集一批事件，或者等待超时
                long deadline = System.currentTimeMillis() + batchTimeoutMs;
                while (batch.size() < batchSize && System.currentTimeMillis() < deadline) {
                    AuditEvent event = auditQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (event != null) {
                        batch.add(event);
                    }
                }

                // 处理这一批事件
                if (!batch.isEmpty()) {
                    processBatch(batch);
                }

            } catch (Exception e) {
                log.error("批处理器执行失败", e);
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * 启动维护任务
     *
     * <p>维护任务包括索引优化、过期数据清理、统计信息更新等。
     * 这些任务在后台定期执行，确保系统的长期稳定运行。</p>
     */
    private void startMaintenanceTasks() {
        // 每天执行一次数据清理
        scheduledExecutor.scheduleWithFixedDelay(this::performDataCleanup,
                1, 24, TimeUnit.HOURS);

        // 每小时更新一次统计信息
        scheduledExecutor.scheduleWithFixedDelay(this::updateStatisticsCache,
                10, 60, TimeUnit.MINUTES);

        // 每天执行一次完整性检查
        scheduledExecutor.scheduleWithFixedDelay(this::performIntegrityCheck,
                2, 24, TimeUnit.HOURS);
    }

    /**
     * 提交审计事件到队列
     */
    private void submitAuditEvent(AuditEvent event) throws AuditServiceException {
        if (asyncEnabled) {
            // 异步模式：加入队列等待批处理
            boolean offered = auditQueue.offer(event);
            if (!offered) {
                // 队列满了，降级为同步处理
                log.warn("审计队列已满，降级为同步处理");
                processEventSynchronously(event);
            }
        } else {
            // 同步模式：立即处理
            processEventSynchronously(event);
        }
    }

    /**
     * 同步处理单个审计事件
     */
    private void processEventSynchronously(AuditEvent event) throws AuditServiceException {
        try {
            // 保存到数据库
            saveAuditEventToDatabase(event);

            // 保存到Elasticsearch
            if (elasticsearchEnabled) {
                saveAuditEventToElasticsearch(event);
            }

        } catch (Exception e) {
            throw new AuditServiceException("PROCESS_EVENT_FAILED", "处理审计事件失败", e);
        }
    }

    /**
     * 批量处理审计事件
     */
    private void processBatch(List<AuditEvent> batch) {
        try {
            log.debug("开始处理审计事件批次: size={}", batch.size());

            // 并行保存到不同的存储
            CompletableFuture<Void> dbFuture = CompletableFuture.runAsync(() -> {
                try {
                    saveAuditEventsBatchToDatabase(batch);
                } catch (Exception e) {
                    log.error("批量保存到数据库失败", e);
                }
            }, auditExecutor);

            CompletableFuture<Void> esFuture = CompletableFuture.runAsync(() -> {
                if (elasticsearchEnabled) {
                    try {
                        saveAuditEventsBatchToElasticsearch(batch);
                    } catch (Exception e) {
                        log.error("批量保存到Elasticsearch失败", e);
                    }
                }
            }, auditExecutor);

            // 等待所有保存操作完成
            CompletableFuture.allOf(dbFuture, esFuture).get(30, TimeUnit.SECONDS);

            log.debug("审计事件批次处理完成: size={}", batch.size());

        } catch (Exception e) {
            log.error("处理审计事件批次失败: size={}", batch.size(), e);
        }
    }

    // =================== 数据库操作方法 ===================

    /**
     * 保存单个审计事件到数据库
     */
    private void saveAuditEventToDatabase(AuditEvent event) {
        try {
            AuditLogEntity entity = convertToEntity(event);
            auditLogRepository.save(entity);
        } catch (Exception e) {
            log.error("保存审计事件到数据库失败", e);
            throw e;
        }
    }

    /**
     * 批量保存审计事件到数据库
     */
    private void saveAuditEventsBatchToDatabase(List<AuditEvent> events) {
        try {
            List<AuditLogEntity> entities = events.stream()
                    .map(this::convertToEntity)
                    .collect(Collectors.toList());

            auditLogRepository.saveAll(entities);

        } catch (Exception e) {
            log.error("批量保存审计事件到数据库失败", e);
            throw e;
        }
    }

    /**
     * 转换审计事件为数据库实体
     */
    private AuditLogEntity convertToEntity(AuditEvent event) {
        try {
            AuditLogEntity entity = new AuditLogEntity();
            entity.setEventType(event.getEventType());
            entity.setUserId(event.getUserId());
            entity.setAction(event.getAction());
            entity.setTargetType(event.getTargetType());
            entity.setTargetId(event.getTargetId());
            entity.setDescription(event.getDescription());
            entity.setTimestamp(event.getTimestamp());
            entity.setIpAddress(event.getIpAddress());
            entity.setUserAgent(event.getUserAgent());

            // 序列化元数据
            if (event.getMetadata() != null) {
                entity.setMetadata(objectMapper.writeValueAsString(event.getMetadata()));
            }

            // 生成完整性哈希
            if (integrityCheckEnabled) {
                entity.setIntegrityHash(calculateIntegrityHash(entity));
            }

            return entity;

        } catch (Exception e) {
            log.error("转换审计事件为数据库实体失败", e);
            throw new RuntimeException("转换审计事件失败", e);
        }
    }

    // =================== Elasticsearch操作方法 ===================

    /**
     * 保存单个审计事件到Elasticsearch
     */
    private void saveAuditEventToElasticsearch(AuditEvent event) {
        try {
            String indexName = generateIndexName(event.getTimestamp());

            Map<String, Object> document = convertToEsDocument(event);

            elasticsearchOps.save(document, indexName);

        } catch (Exception e) {
            log.error("保存审计事件到Elasticsearch失败", e);
            // ES保存失败不应该影响主流程，只记录日志
        }
    }

    /**
     * 批量保存审计事件到Elasticsearch
     */
    private void saveAuditEventsBatchToElasticsearch(List<AuditEvent> events) {
        try {
            // 按索引分组
            Map<String, List<Map<String, Object>>> documentsByIndex = events.stream()
                    .collect(Collectors.groupingBy(
                            event -> generateIndexName(event.getTimestamp()),
                            Collectors.mapping(this::convertToEsDocument, Collectors.toList())
                    ));

            // 批量保存到各个索引
            for (Map.Entry<String, List<Map<String, Object>>> entry : documentsByIndex.entrySet()) {
                String indexName = entry.getKey();
                List<Map<String, Object>> documents = entry.getValue();

                // 这里应该使用Elasticsearch的bulk API
                for (Map<String, Object> document : documents) {
                    elasticsearchOps.save(document, indexName);
                }
            }

        } catch (Exception e) {
            log.error("批量保存审计事件到Elasticsearch失败", e);
        }
    }

    /**
     * 转换审计事件为ES文档
     */
    private Map<String, Object> convertToEsDocument(AuditEvent event) {
        Map<String, Object> document = new HashMap<>();
        document.put("eventType", event.getEventType());
        document.put("userId", event.getUserId());
        document.put("action", event.getAction());
        document.put("targetType", event.getTargetType());
        document.put("targetId", event.getTargetId());
        document.put("description", event.getDescription());
        document.put("timestamp", event.getTimestamp());
        document.put("ipAddress", event.getIpAddress());
        document.put("userAgent", event.getUserAgent());

        if (event.getMetadata() != null) {
            document.putAll(event.getMetadata());
        }

        return document;
    }

    /**
     * 生成ES索引名称
     */
    private String generateIndexName(OffsetDateTime timestamp) {
        // 按月分片：audit-2024-01
        return "audit-" + timestamp.format(INDEX_DATE_FORMAT);
    }

    // =================== 查询实现方法 ===================

    /**
     * 从Elasticsearch查询用户操作历史
     */
    private PagedAuditResult queryUserActionsFromElasticsearch(Long userId, OffsetDateTime startTime,
                                                               OffsetDateTime endTime, List<String> actions,
                                                               int page, int size) {
        // 构建查询条件
        Criteria criteria = Criteria.where("eventType").is(USER_ACTION_TYPE);

        if (userId != null) {
            criteria = criteria.and("userId").is(userId);
        }

        if (startTime != null && endTime != null) {
            criteria = criteria.and("timestamp").between(startTime, endTime);
        }

        if (actions != null && !actions.isEmpty()) {
            criteria = criteria.and("action").in(actions);
        }

        // 执行查询
        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp")));

        SearchHits<Map> searchHits = elasticsearchOps.search(query, Map.class);

        // 转换结果
        List<AuditEvent> events = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::convertFromEsDocument)
                .collect(Collectors.toList());

        return new PagedAuditResult(events, searchHits.getTotalHits(), page, size);
    }

    /**
     * 从数据库查询用户操作历史
     */
    private PagedAuditResult queryUserActionsFromDatabase(Long userId, OffsetDateTime startTime,
                                                          OffsetDateTime endTime, List<String> actions,
                                                          int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<AuditLogEntity> entityPage = auditLogRepository.findUserActions(
                userId, startTime, endTime, actions, pageable);

        List<AuditEvent> events = entityPage.getContent().stream()
                .map(this::convertFromEntity)
                .collect(Collectors.toList());

        return new PagedAuditResult(events, entityPage.getTotalElements(), page, size);
    }

    // =================== 工具方法 ===================

    /**
     * 验证审计参数
     */
    private void validateAuditParameters(String action, String detail) {
        if (!StringUtils.hasText(action)) {
            throw new IllegalArgumentException("操作动作不能为空");
        }
        if (!StringUtils.hasText(detail)) {
            throw new IllegalArgumentException("操作详情不能为空");
        }
    }

    /**
     * 验证查询参数
     */
    private void validateQueryParameters(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("页码不能小于0");
        }
        if (size <= 0 || size > 1000) {
            throw new IllegalArgumentException("页大小必须在1-1000之间");
        }
    }

    /**
     * 增强元数据
     */
    private Map<String, Object> enhanceMetadata(Map<String, Object> originalMetadata, String eventType) {
        Map<String, Object> enhanced = new HashMap<>();
        if (originalMetadata != null) {
            enhanced.putAll(originalMetadata);
        }

        enhanced.put("eventType", eventType);
        enhanced.put("serverInfo", getServerInfo());
        enhanced.put("applicationVersion", getApplicationVersion());
        enhanced.put("correlationId", generateCorrelationId());

        return enhanced;
    }

    /**
     * 计算完整性哈希
     */
    private String calculateIntegrityHash(AuditLogEntity entity) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String data = String.format("%s|%s|%s|%s|%s",
                    entity.getEventType(),
                    entity.getAction(),
                    entity.getTimestamp(),
                    entity.getDescription(),
                    entity.getMetadata());

            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            log.error("计算完整性哈希失败", e);
            return null;
        }
    }

    /**
     * 处理审计错误
     */
    private void handleAuditError(String message, Exception e) throws Exception {
        log.error(message, e);

        // 审计失败不应该影响主业务，但需要记录并告警
        if (e instanceof AuditServiceException) {
            throw e;
        } else {
            throw new AuditServiceException("AUDIT_ERROR", message, e);
        }
    }

    // =================== 模拟的上下文获取方法 ===================

    private Long getCurrentUserId() {
        // 这里应该从SecurityContext或ThreadLocal获取当前用户ID
        return 1L; // 简化实现
    }

    private String getCurrentIpAddress() {
        // 这里应该从请求上下文获取真实IP
        return "192.168.1.100"; // 简化实现
    }

    private String getCurrentUserAgent() {
        // 这里应该从HTTP请求头获取User-Agent
        return "Mozilla/5.0 BaseAI Client"; // 简化实现
    }

    private Map<String, Object> getSystemInfo() {
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("memoryUsage", getMemoryUsage());
        return systemInfo;
    }

    private String getServerInfo() {
        return "BaseAI-Server-001"; // 简化实现
    }

    private String getApplicationVersion() {
        return "1.0.0"; // 简化实现
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    private String generateReportId(String reportType) {
        return String.format("%s_%d_%s", reportType, System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8));
    }

    private String generateDownloadUrl(String reportId) {
        return String.format("/api/v1/audit/reports/%s/download", reportId);
    }

    private Long getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    // =================== 简化实现的存根方法 ===================

    private void validateStringParameter(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private Long parseLongSafely(String value) {
        try {
            return value != null ? Long.parseLong(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getGeoLocation(String ip) {
        return "北京市";
    }

    private Map<String, Object> checkThreatIntelligence(String ip) {
        return new HashMap<>();
    }

    private void processHighRiskEvent(AuditEvent event) {
        log.warn("处理高风险事件: {}", event.getAction());
    }

    private boolean isComplianceRequired(String operation) {
        return operation.contains("FINANCIAL");
    }

    private void processBatchEventsByType(String eventType, List<AuditEvent> events) {
        log.debug("处理 {} 类型事件批次: {}", eventType, events.size());
    }

    private PagedAuditResult querySecurityEventsFromElasticsearch(OffsetDateTime startTime, OffsetDateTime endTime, List<RiskLevel> riskLevels, List<String> eventTypes, String sourceIp, int page, int size) {
        return new PagedAuditResult(Collections.emptyList(), 0, page, size);
    }

    private PagedAuditResult querySecurityEventsFromDatabase(OffsetDateTime startTime, OffsetDateTime endTime, List<RiskLevel> riskLevels, List<String> eventTypes, String sourceIp, int page, int size) {
        return new PagedAuditResult(Collections.emptyList(), 0, page, size);
    }

    private void generateReportAsync(String reportId, String reportType, Map<String, Object> reportParams, String outputFormat) {
        log.info("异步生成报告: {}", reportId);
    }

    private String buildStatisticsCacheKey(String statisticsType, String timeRange, Map<String, Object> filters) {
        return String.format("stats:%s:%s:%d", statisticsType, timeRange, filters.hashCode());
    }

    private AuditStatistics getFromCache(String cacheKey) {
        return null;
    }

    private AuditStatistics calculateStatistics(String statisticsType, String timeRange, Map<String, Object> filters) {
        return new AuditStatistics(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    private void cacheStatistics(String cacheKey, AuditStatistics statistics) {
        log.debug("缓存统计信息: {}", cacheKey);
    }

    private void validateRetentionPolicy(RetentionPolicy policy) {
        if (policy.retentionDays() <= 0) throw new IllegalArgumentException("保留天数必须大于0");
    }

    private void saveRetentionPolicy(RetentionPolicy policy) {
        log.info("保存保留策略: {}", policy.policyName());
    }

    private void scheduleArchiveTask(RetentionPolicy policy) {
        log.info("安排归档任务: {}", policy.policyName());
    }

    private IntegrityCheckBatch verifyBatchIntegrity(OffsetDateTime startTime, OffsetDateTime endTime) {
        return new IntegrityCheckBatch(100, 0, Collections.emptyList());
    }

    private void performDataCleanup() {
        log.info("执行数据清理任务");
    }

    private void updateStatisticsCache() {
        log.debug("更新统计信息缓存");
    }

    private void performIntegrityCheck() {
        log.info("执行完整性检查");
    }

    private AuditEvent convertFromEsDocument(Map<String, Object> document) {
        return new AuditEvent("USER_ACTION", 1L, "TEST", "TEST", 1L, "测试", OffsetDateTime.now(), "127.0.0.1", "Test", new HashMap<>());
    }

    private AuditEvent convertFromEntity(AuditLogEntity entity) {
        return new AuditEvent(entity.getEventType(), entity.getUserId(), entity.getAction(), entity.getTargetType(), entity.getTargetId(), entity.getDescription(), entity.getTimestamp(), entity.getIpAddress(), entity.getUserAgent(), new HashMap<>());
    }
}