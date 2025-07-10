package com.clinflash.baseai.application.mcp.dto;

import java.util.Map;

/**
 * <h2>工具执行结果数据传输对象</h2>
 *
 * <p>记录了工具执行的完整结果信息。
 * 无论是成功还是失败，同步还是异步，这个对象都提供了统一的结果格式，
 * 让AI模型和调用方能够以标准化的方式处理工具执行的反馈。</p>
 *
 * <p><b>执行模式支持：</b></p>
 * <ul>
 * <li><b>同步执行：</b>立即返回完整结果，适合快速响应的工具调用</li>
 * <li><b>异步执行：</b>返回任务标识，适合耗时较长的工具操作</li>
 * <li><b>流式处理：</b>支持分批返回结果，适合大数据量的处理场景</li>
 * </ul>
 *
 * <p><b>状态管理：</b></p>
 * <p>通过状态字段，调用方可以清楚地了解工具执行的当前阶段：
 * "STARTED"表示异步任务已启动，"SUCCESS"表示执行成功，
 * "FAILED"表示执行失败，"TIMEOUT"表示执行超时等。</p>
 *
 * <p><b>性能监控：</b>执行时长信息为性能分析提供了重要数据，
 * 帮助识别性能瓶颈，优化工具响应速度。</p>
 *
 * <p><b>错误诊断：</b>详细的错误信息帮助开发者快速定位和解决问题，
 * 提升整个工具生态的稳定性。</p>
 *
 * @param logId        执行日志的唯一标识符，用于后续的状态查询和结果追踪
 * @param status       执行状态，如"SUCCESS"、"FAILED"、"STARTED"、"TIMEOUT"等
 * @param result       工具执行的具体结果数据，格式由工具的输出规范决定
 * @param errorMessage 错误信息描述，仅在执行失败时提供，便于问题诊断
 * @param durationMs   执行耗时（毫秒），用于性能监控和分析
 */
public record ToolExecutionResultDTO(
        Long logId,
        String status,
        Map<String, Object> result,
        String errorMessage,
        Long durationMs
) {
}