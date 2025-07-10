package com.clinflash.baseai.application.kb.dto;

import java.util.Map;

/**
 * <h2>系统健康状态检查结果</h2>
 *
 * <p>通过监控各个关键组件的状态，及时发现潜在问题，确保系统能够持续提供高质量的服务。</p>
 *
 * @param status         整体健康状态，"healthy"表示所有组件正常，"unhealthy"表示存在问题
 * @param components     各组件的详细状态映射，键为组件名称，值为状态描述
 * @param responseTimeMs 健康检查的响应时间，单位毫秒，反映系统整体性能
 */
public record HealthStatus(
        String status,
        Map<String, String> components,
        long responseTimeMs
) {
}