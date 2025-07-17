package com.cloud.baseai.application.mcp.dto;

import java.util.Map;

/**
 * <h2>MCP系统健康状态检查结果</h2>
 *
 * <p>提供了系统各个关键组件的健康状况评估。
 * 在复杂的分布式AI系统中，工具服务的稳定性直接影响AI模型的能力表现，
 * 因此实时的健康监控就像是系统的"生命体征监测器"，确保服务始终处于最佳状态。</p>
 *
 * <p><b>监控维度：</b></p>
 * <ul>
 * <li><b>数据存储层：</b>工具注册信息、调用日志等数据的存储和查询能力</li>
 * <li><b>执行引擎：</b>工具调用执行服务的可用性和响应能力</li>
 * <li><b>异步处理：</b>后台任务处理器的运行状态和处理能力</li>
 * <li><b>外部依赖：</b>第三方API、网络连接等外部服务的可达性</li>
 * <li><b>资源状况：</b>内存、CPU、连接池等系统资源的使用情况</li>
 * </ul>
 *
 * <p><b>快速诊断策略：</b></p>
 * <p>健康检查采用"快进快出"的策略，每个组件都有严格的超时限制，
 * 避免健康检查本身成为系统负担。同时提供分层的状态报告，
 * 既有整体的健康判断，也有组件级的详细状态。</p>
 *
 * <p><b>运维支持：</b>详细的组件状态信息为运维团队提供了
 * 精确的问题定位能力，配合响应时间数据，
 * 可以快速判断系统的整体性能水平。</p>
 *
 * @param status         系统整体健康状态，"healthy"表示所有组件正常，"unhealthy"表示存在问题
 * @param components     各组件的详细状态映射表，键为组件名称，值为状态描述信息
 * @param responseTimeMs 健康检查的总响应时间（毫秒），反映系统的整体响应能力
 */
public record McpHealthStatus(
        String status,
        Map<String, String> components,
        long responseTimeMs
) {
}