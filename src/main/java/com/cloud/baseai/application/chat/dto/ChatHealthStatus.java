package com.cloud.baseai.application.chat.dto;

import java.util.Map;

/**
 * <h2>智能对话系统健康状态检查结果</h2>
 *
 * <p>提供了各个关键组件的健康状况评估。</p>
 *
 * @param status         系统整体健康状态，"healthy"表示所有组件正常，"unhealthy"表示存在问题
 * @param components     各个组件的详细健康状态映射表，键为组件名称，值为状态描述
 * @param responseTimeMs 健康检查的总响应时间（毫秒），反映系统的整体响应能力
 */
public record ChatHealthStatus(
        String status,
        Map<String, String> components,
        long responseTimeMs
) {
}