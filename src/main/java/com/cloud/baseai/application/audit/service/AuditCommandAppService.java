package com.cloud.baseai.application.audit.service;

import com.cloud.baseai.application.audit.dto.AuditLogDTO;
import com.cloud.baseai.domain.audit.model.SysAuditLog;
import com.cloud.baseai.domain.audit.repository.SysAuditLogRepository;
import com.cloud.baseai.domain.audit.service.AuditService;
import com.cloud.baseai.infrastructure.event.AuditEventPublisher;
import com.cloud.baseai.infrastructure.exception.AuditException;
import com.cloud.baseai.infrastructure.exception.ErrorCode;
import com.cloud.baseai.infrastructure.i18n.MessageManager;
import com.cloud.baseai.infrastructure.utils.AuditUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * <h2>审计命令应用服务</h2>
 *
 * <p>这个应用服务专门处理审计系统中的写操作，如创建审计记录、批量处理等。
 * 它遵循CQRS模式，将命令操作与查询操作分离，确保系统的职责清晰。</p>
 *
 * <p><b>设计理念：</b></p>
 * <p>命令服务关注的是改变系统状态的操作，它需要确保数据的一致性和完整性。
 * 就像银行的转账系统，每一个操作都必须是原子的、一致的、隔离的和持久的。</p>
 *
 * <p><b>核心职责：</b></p>
 * <ul>
 * <li><b>审计记录创建：</b>接收业务事件，转换为标准化的审计记录</li>
 * <li><b>批量处理：</b>高效处理大量审计事件，优化系统性能</li>
 * <li><b>事件发布：</b>在适当的时机发布领域事件，通知其他系统组件</li>
 * <li><b>数据验证：</b>确保审计数据的完整性和准确性</li>
 * </ul>
 */
@Service
@Transactional
public class AuditCommandAppService {

    private static final Logger logger = LoggerFactory.getLogger(AuditCommandAppService.class);

    private final SysAuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final AuditEventPublisher eventPublisher;

    /**
     * 构造函数 - 依赖注入核心服务
     *
     * <p>通过构造函数注入确保所有依赖在对象创建时就已经准备就绪，
     * 这是依赖注入的最佳实践，能够确保对象的不可变性和线程安全性。</p>
     */
    public AuditCommandAppService(SysAuditLogRepository auditLogRepository,
                                  AuditService auditService,
                                  AuditEventPublisher eventPublisher) {
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;

        logger.info("审计命令应用服务初始化完成");
    }

    /**
     * 创建审计记录
     *
     * <p>这是最基础的命令操作，用于创建单个审计记录。它会进行完整的
     * 数据验证、业务规则检查，并在成功创建后发布相应的领域事件。</p>
     *
     * <p><b>处理流程：</b></p>
     * <p>首先验证输入数据的完整性和正确性，然后创建领域对象，
     * 接着保存到仓储，最后发布创建成功事件。这个流程确保了
     * 数据的一致性和业务规则的执行。</p>
     *
     * @param tenantId    租户ID，用于多租户数据隔离
     * @param userId      操作用户ID，null表示系统操作
     * @param action      操作类型，如"USER_LOGIN"、"DATA_UPDATE"等
     * @param targetType  操作目标类型，如"USER"、"DOCUMENT"等
     * @param targetId    操作目标ID，可以为null
     * @param description 操作描述信息
     * @param ipAddress   用户IP地址，用于安全分析
     * @param userAgent   用户代理信息
     * @param contextData 额外的上下文数据
     * @return 创建成功的审计日志DTO
     * @throws AuditException 当创建操作失败时抛出
     */
    public AuditLogDTO createAuditLog(Long tenantId, Long userId, String action,
                                      String targetType, Long targetId,
                                      String description, String ipAddress,
                                      String userAgent, Map<String, Object> contextData) {
        long startTime = System.currentTimeMillis();

        try {
            logger.debug("创建审计记录: tenantId={}, userId={}, action={}", tenantId, userId, action);

            // 第一步：参数验证
            validateCreateParameters(tenantId, action, description);

            // 第二步：数据脱敏处理
            Map<String, Object> sanitizedData = AuditUtils.maskSensitiveData(contextData);

            // 第三步：构建审计日志领域对象
            SysAuditLog auditLog = SysAuditLog.create(
                    tenantId, userId, action, targetType, targetId,
                    ipAddress, userAgent, AuditUtils.safeSerializeToJson(sanitizedData),
                    SysAuditLog.ResultStatus.SUCCESS, SysAuditLog.LogLevel.INFO
            );

            // 第四步：业务规则验证
            auditLog.validate();

            // 第五步：保存到仓储
            SysAuditLog savedLog = auditLogRepository.save(auditLog);

            // 第六步：发布领域事件
            publishAuditCreatedEvent(savedLog, sanitizedData);

            // 第七步：转换为DTO返回
            AuditLogDTO result = convertToDTO(savedLog);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("审计记录创建成功: id={}, action={}, 耗时={}ms",
                    savedLog.id(), action, duration);

            return result;

        } catch (Exception e) {
            logger.error("创建审计记录失败: tenantId={}, action={}", tenantId, action, e);
            throw AuditException.createFailed(e.getMessage());
        }
    }

    /**
     * 批量创建审计记录
     *
     * <p>这个方法专门处理大量审计记录的批量创建，采用了优化的批处理策略
     * 来提高性能。在高并发场景下，批量处理比逐个处理要高效得多。</p>
     *
     * <p><b>批处理优势：</b></p>
     * <p>通过减少数据库交互次数、优化事务处理、使用批量插入等技术，
     * 能够显著提高大量数据处理的性能，同时保证数据的一致性。</p>
     *
     * @param auditEvents 审计事件列表
     * @return 批量处理结果，包含成功和失败的统计信息
     * @throws AuditException 当批量处理失败时抛出
     */
    public BatchCreateResult batchCreateAuditLogs(List<AuditCreateCommand> auditEvents) {
        long startTime = System.currentTimeMillis();

        try {
            logger.info("开始批量创建审计记录: 数量={}", auditEvents.size());

            // 第一步：验证批量参数
            validateBatchParameters(auditEvents);

            // 第二步：数据预处理和验证
            List<SysAuditLog> validLogs = preprocessAuditEvents(auditEvents);

            // 第三步：批量保存
            List<SysAuditLog> savedLogs = auditLogRepository.saveAll(validLogs);

            // 第四步：异步发布事件
            publishBatchCreatedEvents(savedLogs);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("批量创建审计记录完成: 成功={}, 耗时={}ms",
                    savedLogs.size(), duration);

            return new BatchCreateResult(
                    auditEvents.size(),
                    savedLogs.size(),
                    auditEvents.size() - savedLogs.size(),
                    List.of() // 简化实现，实际应该包含错误详情
            );

        } catch (Exception e) {
            logger.error("批量创建审计记录失败: 数量={}", auditEvents.size(), e);
            throw AuditException.batchCreateFailed(auditEvents.size(), e.getMessage());
        }
    }

    /**
     * 异步创建审计记录
     *
     * <p>这个方法提供异步的审计记录创建功能，适用于不影响主业务流程的场景。
     * 它能够确保审计操作不会阻塞关键的业务操作。</p>
     *
     * <p><b>异步处理的价值：</b></p>
     * <p>在很多业务场景中，审计记录虽然重要，但不应该影响用户的正常操作。
     * 异步处理让我们可以在后台完成审计工作，同时保证用户体验的流畅性。</p>
     */
    @Async("auditTaskExecutor")
    public CompletableFuture<AuditLogDTO> createAuditLogAsync(Long tenantId, Long userId,
                                                              String action, String targetType,
                                                              Long targetId, String description,
                                                              String ipAddress, String userAgent,
                                                              Map<String, Object> contextData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return createAuditLog(tenantId, userId, action, targetType, targetId,
                        description, ipAddress, userAgent, contextData);
            } catch (Exception e) {
                logger.error("异步创建审计记录失败: action={}", action, e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 创建安全事件审计记录
     *
     * <p>安全事件需要特别的处理方式，包括立即通知、详细记录、风险评估等。
     * 这个方法专门处理安全相关的审计记录创建。</p>
     */
    public AuditLogDTO createSecurityAuditLog(Long tenantId, Long userId, String securityEventType,
                                              String description, String riskLevel,
                                              String sourceIp, Map<String, Object> securityContext) {
        try {
            logger.warn("创建安全审计记录: eventType={}, riskLevel={}, userId={}",
                    securityEventType, riskLevel, userId);

            // 增强安全上下文信息
            Map<String, Object> enhancedContext = enhanceSecurityContext(securityContext,
                    riskLevel, sourceIp);

            // 创建审计记录
            AuditLogDTO result = createAuditLog(tenantId, userId, securityEventType,
                    "SECURITY_EVENT", null, description, sourceIp, null, enhancedContext);

            // 高风险事件需要立即通知
            if ("HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel)) {
                notifySecurityTeam(result, enhancedContext);
            }

            return result;

        } catch (Exception e) {
            logger.error("创建安全审计记录失败: eventType={}", securityEventType, e);
            throw AuditException.securityAuditCreateFailed(securityEventType, riskLevel);
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 验证创建参数的有效性
     */
    private void validateCreateParameters(Long tenantId, String action, String description) {
        Assert.notNull(tenantId, MessageManager.getMessage(ErrorCode.PARAM_011));
        Assert.hasText(action, MessageManager.getMessage(ErrorCode.PARAM_012));
        Assert.hasText(description, MessageManager.getMessage(ErrorCode.PARAM_013));

        if (action.length() > 64) {
            throw new IllegalArgumentException(MessageManager.getMessage(ErrorCode.PARAM_014));
        }

        if (description.length() > 1000) {
            throw new IllegalArgumentException(MessageManager.getMessage(ErrorCode.PARAM_015));
        }
    }

    /**
     * 验证批量处理参数
     */
    private void validateBatchParameters(List<AuditCreateCommand> auditEvents) {
        Assert.notNull(auditEvents, MessageManager.getMessage(ErrorCode.PARAM_016));
        Assert.notEmpty(auditEvents, MessageManager.getMessage(ErrorCode.PARAM_017));

        if (auditEvents.size() > 1000) {
            throw new IllegalArgumentException(MessageManager.getMessage(ErrorCode.PARAM_018));
        }
    }

    /**
     * 预处理审计事件
     */
    private List<SysAuditLog> preprocessAuditEvents(List<AuditCreateCommand> auditEvents) {
        return auditEvents.stream()
                .map(this::convertToAuditLog)
                .filter(log -> {
                    try {
                        log.validate();
                        return true;
                    } catch (Exception e) {
                        logger.warn("审计事件验证失败，跳过: {}", e.getMessage());
                        return false;
                    }
                })
                .toList();
    }

    /**
     * 转换命令为审计日志
     */
    private SysAuditLog convertToAuditLog(AuditCreateCommand command) {
        return SysAuditLog.create(
                command.tenantId(),
                command.userId(),
                command.action(),
                command.targetType(),
                command.targetId(),
                command.ipAddress(),
                command.userAgent(),
                AuditUtils.safeSerializeToJson(command.contextData()),
                SysAuditLog.ResultStatus.SUCCESS,
                SysAuditLog.LogLevel.INFO
        );
    }

    /**
     * 转换为DTO
     */
    private AuditLogDTO convertToDTO(SysAuditLog auditLog) {
        return new AuditLogDTO(
                auditLog.id(),
                auditLog.tenantId(),
                auditLog.userId(),
                getUserDisplayName(auditLog.userId()),
                auditLog.action(),
                translateAction(auditLog.action()),
                auditLog.targetType(),
                auditLog.targetId(),
                auditLog.ipAddress(),
                auditLog.userAgent(),
                AuditUtils.safeDeserializeFromJson(auditLog.detail()),
                auditLog.resultStatus(),
                auditLog.logLevel(),
                auditLog.createdAt()
        );
    }

    /**
     * 发布审计创建事件
     */
    private void publishAuditCreatedEvent(SysAuditLog auditLog, Map<String, Object> contextData) {
        try {
            eventPublisher.publishUserActionEvent(
                    auditLog.action(),
                    auditLog.targetType(),
                    auditLog.targetId(),
                    "审计记录已创建",
                    contextData
            );
        } catch (Exception e) {
            logger.warn("发布审计创建事件失败: {}", e.getMessage());
        }
    }

    /**
     * 批量发布创建事件
     */
    @Async("auditTaskExecutor")
    protected void publishBatchCreatedEvents(List<SysAuditLog> savedLogs) {
        for (SysAuditLog log : savedLogs) {
            try {
                publishAuditCreatedEvent(log, Map.of("batchOperation", true));
            } catch (Exception e) {
                logger.debug("发布批量事件失败: logId={}", log.id(), e);
            }
        }
    }

    /**
     * 增强安全上下文信息
     */
    private Map<String, Object> enhanceSecurityContext(Map<String, Object> original,
                                                       String riskLevel, String sourceIp) {
        Map<String, Object> enhanced = new HashMap<>();
        if (original != null) {
            enhanced.putAll(original);
        }

        enhanced.put("riskLevel", riskLevel);
        enhanced.put("sourceIp", sourceIp);
        enhanced.put("detectionTime", OffsetDateTime.now());
        enhanced.put("threatScore", AuditUtils.calculateRiskScore("SECURITY_EVENT",
                "SUCCESS", sourceIp, null));

        return enhanced;
    }

    /**
     * 通知安全团队
     */
    @Async("auditTaskExecutor")
    protected void notifySecurityTeam(AuditLogDTO auditLog, Map<String, Object> context) {
        try {
            logger.warn("高风险安全事件需要关注: id={}, action={}",
                    auditLog.id(), auditLog.action());
            // 这里可以集成邮件、短信、钉钉等通知方式
        } catch (Exception e) {
            logger.error("通知安全团队失败", e);
        }
    }

    // 辅助方法存根
    private String getUserDisplayName(Long userId) {
        return userId != null ? "用户" + userId : "系统";
    }

    private String translateAction(String action) {
        return AuditUtils.generateOperationSummary(action, null, null, null);
    }

    // =================== 内部数据类 ===================

    /**
     * 审计创建命令
     */
    public record AuditCreateCommand(
            Long tenantId,
            Long userId,
            String action,
            String targetType,
            Long targetId,
            String description,
            String ipAddress,
            String userAgent,
            Map<String, Object> contextData
    ) {
    }

    /**
     * 批量创建结果
     */
    public record BatchCreateResult(
            int totalCount,
            int successCount,
            int failureCount,
            List<String> errors
    ) {
    }
}