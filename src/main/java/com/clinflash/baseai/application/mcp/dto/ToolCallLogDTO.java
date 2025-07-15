package com.clinflash.baseai.application.mcp.dto;

import com.clinflash.baseai.infrastructure.persistence.mcp.entity.enums.ToolCallStatus;

import java.time.OffsetDateTime;

/**
 * <h2>工具调用日志数据传输对象</h2>
 *
 * <p>记录了每一次工具调用的关键信息。
 * 这些日志不仅用于审计追踪，更是系统优化、问题诊断和用户行为分析的重要数据源。
 * 就像银行的交易记录一样，每个调用都有完整的上下文信息。</p>
 *
 * <p><b>审计追踪价值：</b></p>
 * <ul>
 * <li><b>责任追溯：</b>明确记录谁在什么时候调用了哪个工具</li>
 * <li><b>使用统计：</b>为工具使用频率分析提供原始数据</li>
 * <li><b>性能分析：</b>响应时间数据帮助识别性能瓶颈</li>
 * <li><b>故障诊断：</b>失败的调用记录有助于问题定位和解决</li>
 * </ul>
 *
 * <p><b>多维度关联：</b></p>
 * <p>通过工具ID、租户ID、用户ID等多个维度，可以进行灵活的数据分析：
 * 查看特定工具的使用情况、特定租户的调用模式、特定用户的操作历史等。</p>
 *
 * <p><b>性能监控：</b>延迟时间（latencyMs）是系统性能的重要指标，
 * 通过分析这些数据可以发现性能趋势，制定优化策略。</p>
 *
 * <p><b>数据隐私：</b>日志记录遵循最小化原则，只保存必要的元数据，
 * 不记录敏感的调用参数和结果内容，确保用户隐私安全。</p>
 *
 * @param id        日志记录的唯一标识符，用于精确定位特定的调用记录
 * @param toolId    被调用工具的标识符，用于关联具体的工具信息
 * @param tenantId  发起调用的租户标识符，支持多租户环境下的数据隔离
 * @param userId    发起调用的用户标识符，用于用户行为分析和审计
 * @param status    调用执行的状态，如"SUCCESS"、"FAILED"、"TIMEOUT"等
 * @param latencyMs 调用的响应延迟时间（毫秒），用于性能监控和分析
 * @param createdAt 调用发生的时间戳，按时间维度进行数据分析的基础
 */
public record ToolCallLogDTO(
        Long id,
        Long toolId,
        Long tenantId,
        Long userId,
        ToolCallStatus status,
        Integer latencyMs,
        OffsetDateTime createdAt
) {
}