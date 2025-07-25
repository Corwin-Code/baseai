package com.cloud.baseai.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@Validated
@ConfigurationProperties(prefix = "error.handling")
public class ErrorHandlingProperties {
    /**
     * 是否在响应里包含堆栈信息
     */
    private boolean includeStackTrace = false;
    /**
     * 是否在响应里包含异常全名
     */
    private boolean includeException = false;
    /**
     * 是否在响应里包含 traceId
     */
    private boolean includeTraceId = true;
    /**
     * 是否记录安全事件日志
     */
    private boolean securityEventLogging = true;
}