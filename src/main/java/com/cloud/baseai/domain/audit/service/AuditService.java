package com.cloud.baseai.domain.audit.service;

import com.cloud.baseai.application.audit.dto.*;
import com.cloud.baseai.infrastructure.exception.AuditServiceException;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * <h2>审计服务接口</h2>
 *
 * <p>审计服务的设计遵循"记录一切、分析智能、查询高效"的原则。我们不仅要记录操作事件，
 * 还要记录操作的上下文信息，如用户身份、操作时间、IP地址、设备信息等。
 * 同时，考虑到审计数据的海量特性，我们需要设计高效的存储和检索机制。</p>
 *
 * <p><b>核心功能领域：</b></p>
 * <ul>
 * <li><b>用户行为审计：</b>记录用户的登录、操作、配置变更等行为</li>
 * <li><b>系统事件审计：</b>记录系统内部的重要事件和状态变化</li>
 * <li><b>安全事件审计：</b>记录安全相关的事件，如失败的登录尝试、权限变更等</li>
 * <li><b>业务操作审计：</b>记录关键业务操作，如数据修改、流程执行等</li>
 * <li><b>合规性审计：</b>按照行业标准和法规要求记录特定类型的事件</li>
 * </ul>
 *
 * <p><b>技术挑战：</b></p>
 * <p>审计服务面临着独特的技术挑战：高并发写入、海量数据存储、复杂条件查询、
 * 长期数据保留等。我们的接口设计考虑了这些挑战，提供了异步写入、批量处理、
 * 分层存储等解决方案。</p>
 */
public interface AuditService {

    /**
     * 记录用户操作审计
     *
     * <p>这是审计服务的核心功能，用于记录用户在系统中的各种操作。
     * 每一次用户操作都应该被准确记录，包括操作的详细信息和上下文环境。</p>
     *
     * <p><b>最佳实践：</b></p>
     * <p>建议在关键业务操作的前后都进行记录，形成完整的操作链路。
     * 同时，敏感操作（如权限变更、数据删除）应该记录更详细的信息。</p>
     *
     * @param action   操作动作，如"USER_LOGIN"、"DATA_UPDATE"等，建议使用标准化的动作码
     * @param targetId 操作目标ID，可以是用户ID、数据ID等，便于后续关联查询
     * @param detail   操作详情，包含操作的具体描述和关键参数
     * @throws AuditServiceException 当记录失败时抛出，但不应该影响主业务流程
     */
    void recordUserAction(String action, Long targetId, String detail) throws Exception;

    /**
     * 记录用户操作审计（详细版本）
     *
     * <p>这个重载方法提供了更完整的参数，适用于需要记录详细上下文信息的场景。
     * 特别是在安全敏感的操作中，详细的上下文信息对后续的安全分析非常重要。</p>
     *
     * @param userId         操作用户ID，用于标识操作主体
     * @param action         操作动作代码
     * @param targetType     目标类型，如"USER"、"TENANT"、"DOCUMENT"等
     * @param targetId       目标对象ID
     * @param detail         操作详情描述
     * @param ipAddress      用户IP地址，用于地理位置分析和安全监控
     * @param userAgent      用户代理字符串，包含浏览器和设备信息
     * @param additionalData 额外的上下文数据，以键值对形式存储
     * @throws AuditServiceException 当记录失败时抛出
     */
    void recordUserAction(Long userId, String action, String targetType, Long targetId,
                          String detail, String ipAddress, String userAgent,
                          Map<String, Object> additionalData) throws Exception;

    /**
     * 记录系统事件审计
     *
     * <p>系统事件记录那些由系统自动触发的重要事件，如定时任务执行、
     * 系统状态变更、自动化流程等。这些事件虽然不是用户直接操作，
     * 但对系统运行状态的监控和问题诊断非常重要。</p>
     *
     * @param eventType   事件类型，如"SYSTEM_STARTUP"、"SCHEDULED_TASK"等
     * @param eventSource 事件源，标识产生事件的系统组件
     * @param description 事件描述，详细说明事件的内容和影响
     * @param severity    事件严重程度，用于事件分级处理
     * @param metadata    事件元数据，包含事件的详细参数和状态信息
     * @throws AuditServiceException 当记录失败时抛出
     */
    void recordSystemEvent(String eventType, String eventSource, String description,
                           EventSeverity severity, Map<String, Object> metadata) throws Exception;

    /**
     * 记录安全事件审计
     *
     * <p>安全事件是审计系统的重要组成部分，用于记录所有与安全相关的事件。
     * 这些记录对于安全分析、威胁检测和合规审查都具有重要价值。</p>
     *
     * @param securityEventType 安全事件类型，如"LOGIN_FAILED"、"PERMISSION_ESCALATION"
     * @param userId            相关用户ID，可能为null（如匿名攻击）
     * @param description       事件描述，包含安全事件的详细信息
     * @param riskLevel         风险等级，用于安全事件的优先级处理
     * @param sourceIp          源IP地址，用于地理位置分析和威胁情报关联
     * @param affectedResources 受影响的资源列表，用于影响范围评估
     * @throws AuditServiceException 当记录失败时抛出
     */
    void recordSecurityEvent(String securityEventType, Long userId, String description,
                             RiskLevel riskLevel, String sourceIp,
                             List<String> affectedResources) throws Exception;

    /**
     * 记录业务操作审计
     *
     * <p>业务操作审计专注于记录具有业务意义的操作，如订单创建、
     * 合同签署、财务操作等。这类审计通常需要满足特定的合规要求。</p>
     *
     * @param businessOperation 业务操作类型，如"ORDER_CREATE"、"PAYMENT_PROCESS"
     * @param operatorId        操作员ID
     * @param businessObjectId  业务对象ID，如订单ID、合同ID等
     * @param operationDetails  操作详情，包含业务操作的关键参数
     * @param businessContext   业务上下文，如所属部门、项目等
     * @throws AuditServiceException 当记录失败时抛出
     */
    void recordBusinessOperation(String businessOperation, Long operatorId,
                                 String businessObjectId, Map<String, Object> operationDetails,
                                 Map<String, String> businessContext) throws Exception;

    /**
     * 批量记录审计事件
     *
     * <p>批量记录功能用于高并发场景下的性能优化。通过批量提交，
     * 可以显著提高审计记录的写入效率，减少对主业务流程的影响。</p>
     *
     * @param auditEvents 审计事件列表，每个事件包含完整的审计信息
     * @return 批量记录结果，包含成功和失败的统计信息
     * @throws AuditServiceException 当批量记录失败时抛出
     */
    BatchAuditResult recordBatchEvents(List<AuditEvent> auditEvents) throws AuditServiceException;

    /**
     * 查询用户操作历史
     *
     * <p>提供灵活的查询接口，支持按用户、时间范围、操作类型等条件
     * 查询历史操作记录。这是审计服务对外提供价值的重要接口。</p>
     *
     * <p><b>查询优化考虑：</b></p>
     * <p>由于审计数据量庞大，查询接口需要支持分页、索引优化、缓存等
     * 性能优化手段。同时要注意权限控制，确保用户只能查询有权限的数据。</p>
     *
     * @param userId    用户ID，null表示查询所有用户
     * @param startTime 开始时间，null表示不限制开始时间
     * @param endTime   结束时间，null表示不限制结束时间
     * @param actions   操作类型列表，null表示所有操作类型
     * @param page      页码，从0开始
     * @param size      页大小，建议不超过100
     * @return 查询结果，包含审计记录和分页信息
     * @throws AuditServiceException 当查询失败时抛出
     */
    PagedAuditResult queryUserActions(Long userId, OffsetDateTime startTime, OffsetDateTime endTime,
                                      List<String> actions, int page, int size) throws AuditServiceException;

    /**
     * 查询安全事件历史
     *
     * <p>专门用于安全事件的查询，支持按风险级别、事件类型、时间范围等
     * 条件进行筛选。安全团队可以使用这个接口进行威胁分析和安全审查。</p>
     *
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @param riskLevels 风险级别列表，用于筛选特定级别的安全事件
     * @param eventTypes 事件类型列表
     * @param sourceIp   源IP地址，用于查询特定IP的安全事件
     * @param page       页码
     * @param size       页大小
     * @return 安全事件查询结果
     * @throws AuditServiceException 当查询失败时抛出
     */
    PagedAuditResult querySecurityEvents(OffsetDateTime startTime, OffsetDateTime endTime,
                                         List<RiskLevel> riskLevels, List<String> eventTypes,
                                         String sourceIp, int page, int size) throws AuditServiceException;

    /**
     * 生成审计报告
     *
     * <p>根据指定的条件和模板生成审计报告，用于合规检查、安全分析等目的。
     * 报告可以是PDF、Excel等多种格式，支持定制化的报告模板。</p>
     *
     * @param reportType   报告类型，如"COMPLIANCE_REPORT"、"SECURITY_SUMMARY"
     * @param reportParams 报告参数，包含时间范围、筛选条件等
     * @param outputFormat 输出格式，如"PDF"、"EXCEL"、"JSON"
     * @return 报告生成结果，包含报告ID和下载链接
     * @throws AuditServiceException 当报告生成失败时抛出
     */
    AuditReportResult generateAuditReport(String reportType, Map<String, Object> reportParams,
                                          String outputFormat) throws AuditServiceException;

    /**
     * 获取审计统计信息
     *
     * <p>提供审计数据的统计信息，如操作频率、用户活跃度、安全事件趋势等。
     * 这些统计信息对于系统监控和业务分析很有价值。</p>
     *
     * @param statisticsType 统计类型，如"USER_ACTIVITY"、"OPERATION_FREQUENCY"
     * @param timeRange      时间范围，如"LAST_7_DAYS"、"LAST_MONTH"
     * @param filters        过滤条件，用于细化统计范围
     * @return 统计结果，包含各种维度的统计数据
     * @throws AuditServiceException 当统计计算失败时抛出
     */
    AuditStatistics getAuditStatistics(String statisticsType, String timeRange,
                                       Map<String, Object> filters) throws AuditServiceException;

    /**
     * 数据保留策略管理
     *
     * <p>审计数据需要按照法规要求进行长期保存，但也要考虑存储成本。
     * 这个方法用于配置和执行数据保留策略，如自动归档、定期清理等。</p>
     *
     * @param retentionPolicy 保留策略配置，包含保留期限、归档规则等
     * @throws AuditServiceException 当策略配置失败时抛出
     */
    void configureRetentionPolicy(RetentionPolicy retentionPolicy) throws AuditServiceException;

    /**
     * 验证审计数据完整性
     *
     * <p>定期验证审计数据的完整性，确保数据没有被篡改或丢失。
     * 这是满足某些合规要求的重要功能。</p>
     *
     * @param startTime 验证开始时间
     * @param endTime   验证结束时间
     * @return 完整性验证结果，包含验证状态和详细信息
     * @throws AuditServiceException 当验证失败时抛出
     */
    IntegrityCheckResult verifyDataIntegrity(OffsetDateTime startTime, OffsetDateTime endTime) throws AuditServiceException;

    // =================== 枚举和数据结构定义 ===================

    /**
     * 事件严重程度枚举
     */
    @Getter
    enum EventSeverity {
        LOW("低"),
        MEDIUM("中"),
        HIGH("高"),
        CRITICAL("严重");

        private final String description;

        EventSeverity(String description) {
            this.description = description;
        }
    }

    /**
     * 风险级别枚举
     */
    @Getter
    enum RiskLevel {
        MINIMAL("极低"),
        LOW("低"),
        MEDIUM("中"),
        HIGH("高"),
        CRITICAL("极高");

        private final String description;

        RiskLevel(String description) {
            this.description = description;
        }
    }

    /**
     * 审计事件数据结构
     *
     * @param eventType Getters
     */
    record AuditEvent(String eventType, Long userId, String action, String targetType, Long targetId,
                      String description, OffsetDateTime timestamp, String ipAddress, String userAgent,
                      Map<String, Object> metadata) {

    }


}