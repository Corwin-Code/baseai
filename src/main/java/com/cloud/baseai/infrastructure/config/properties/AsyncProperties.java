package com.cloud.baseai.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <h2>异步处理配置属性类</h2>
 *
 * <p>该类管理应用的异步处理配置，包括线程池设置、队列管理
 * 和拒绝策略等关键异步处理参数。</p>
 */
@Data
@ConfigurationProperties(prefix = "baseai.async")
public class AsyncProperties {

    /**
     * 线程池核心大小
     */
    private Integer corePoolSize = 2;

    /**
     * 线程池最大大小
     */
    private Integer maxPoolSize = 8;

    /**
     * 队列容量
     */
    private Integer queueCapacity = 500;

    /**
     * 线程名称前缀
     */
    private String threadNamePrefix = "BaseAI-Async-";

    /**
     * 线程保活时间（秒）
     */
    private Integer keepAliveSeconds = 60;

    /**
     * 是否允许核心线程超时
     */
    private Boolean allowCoreThreadTimeout = true;

    /**
     * 拒绝策略：ABORT、CALLER_RUNS、DISCARD、DISCARD_OLDEST
     */
    private String rejectionPolicy = "CALLER_RUNS";
}