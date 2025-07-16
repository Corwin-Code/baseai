package com.clinflash.baseai.infrastructure.event;

import com.clinflash.baseai.domain.audit.event.DataChangeAuditEvent;
import com.clinflash.baseai.domain.audit.event.SecurityAuditEvent;
import com.clinflash.baseai.domain.audit.event.SystemAuditEvent;
import com.clinflash.baseai.domain.audit.event.UserActionAuditEvent;
import com.clinflash.baseai.infrastructure.exception.AuditServiceException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * <h2>审计事件发布器</h2>
 *
 * <p>这个事件发布器就像系统的"新闻广播电台"，它负责将重要的审计事件
 * 广播给整个系统。当系统中发生重要操作时，发布器会创建相应的事件，
 * 让其他组件能够响应这些事件，执行如通知、分析、告警等后续处理。</p>
 *
 * <p><b>核心职责：</b></p>
 * <ul>
 * <li><b>事件创建：</b>根据业务操作创建标准化的审计事件</li>
 * <li><b>事件发布：</b>将事件安全可靠地发布到系统事件总线</li>
 * <li><b>上下文增强：</b>自动收集请求上下文信息，丰富事件内容</li>
 * <li><b>异步处理：</b>确保事件发布不会阻塞主业务流程</li>
 * <li><b>错误处理：</b>处理发布过程中可能出现的各种异常情况</li>
 * </ul>
 *
 * <p><b>设计理念：</b></p>
 * <p>我们遵循"发布-订阅"模式的设计理念，让系统的各个组件之间保持松耦合。
 * 这就像报纸的发行系统，报社只负责印刷和发行，而读者可以根据自己的
 * 兴趣选择订阅哪些内容。</p>
 */
@Component
public class AuditEventPublisher implements ApplicationEventPublisherAware {

    private static final Logger log = LoggerFactory.getLogger(AuditEventPublisher.class);

    private ApplicationEventPublisher eventPublisher;

    // 事件发布统计
    private volatile long totalEventsPublished = 0;
    private volatile long totalPublishErrors = 0;
    private volatile long lastPublishTime = 0;

    /**
     * 设置Spring的事件发布器
     *
     * <p>Spring会自动调用这个方法，为我们注入事件发布器。这就像为广播员
     * 配备专业的广播设备，让他能够将消息传达给整个系统。</p>
     */
    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        log.info("审计事件发布器初始化完成");
    }

    /**
     * 发布用户操作审计事件
     *
     * <p>这是最常用的事件发布方法，用于记录用户的各种操作。它会自动
     * 从当前请求上下文中提取用户信息、IP地址等重要信息。</p>
     *
     * <p><b>使用场景：</b></p>
     * <p>当用户执行登录、修改数据、删除文件等重要操作时，业务代码
     * 可以调用这个方法来发布审计事件。事件发布后，系统的其他组件
     * 可以响应这些事件执行相应的处理。</p>
     *
     * @param action         操作类型，如"USER_LOGIN"、"DATA_UPDATE"等
     * @param targetType     操作目标类型，如"USER"、"DOCUMENT"等
     * @param targetId       操作目标的ID
     * @param description    操作描述信息
     * @param additionalData 额外的上下文数据
     */
    @Async("auditTaskExecutor")
    @Retryable(value = {Exception.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    public CompletableFuture<Void> publishUserActionEvent(String action, String targetType,
                                                          Long targetId, String description,
                                                          Map<String, Object> additionalData) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("发布用户操作审计事件: action={}, targetType={}, targetId={}",
                        action, targetType, targetId);

                // 从当前请求上下文提取信息
                RequestContext requestContext = extractRequestContext();

                // 构建审计事件
                UserActionAuditEvent event = new UserActionAuditEvent(
                        this,
                        requestContext.getUserId(),
                        requestContext.getTenantId(),
                        action,
                        targetType,
                        targetId,
                        description,
                        requestContext.getIpAddress(),
                        requestContext.getUserAgent(),
                        mergeContextData(requestContext, additionalData),
                        OffsetDateTime.now()
                );

                // 发布事件
                publishEventSafely(event);

                log.debug("用户操作审计事件发布成功: action={}", action);

            } catch (Exception e) {
                log.error("发布用户操作审计事件失败: action={}", action, e);
                recordPublishError();
                throw new AuditServiceException("EVENT_PUBLISH_FAILED",
                        "发布审计事件失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 发布安全事件
     *
     * <p>安全事件需要特殊的关注和处理。这个方法专门用于发布安全相关的事件，
     * 如登录失败、权限拒绝、可疑活动等。安全事件通常会触发告警和通知机制。</p>
     *
     * @param eventType         安全事件类型
     * @param riskLevel         风险级别
     * @param description       事件描述
     * @param affectedResources 受影响的资源列表
     * @param threatIndicators  威胁指标
     */
    @Async("auditTaskExecutor")
    @Retryable(value = {Exception.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public CompletableFuture<Void> publishSecurityEvent(String eventType, String riskLevel,
                                                        String description,
                                                        Map<String, Object> affectedResources,
                                                        Map<String, Object> threatIndicators) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.warn("发布安全审计事件: eventType={}, riskLevel={}", eventType, riskLevel);

                RequestContext requestContext = extractRequestContext();

                // 构建安全事件特有的上下文数据
                Map<String, Object> securityContext = new HashMap<>();
                securityContext.put("riskLevel", riskLevel);
                securityContext.put("detectionTime", OffsetDateTime.now());
                securityContext.put("sourceIp", requestContext.getIpAddress());
                securityContext.put("userAgent", requestContext.getUserAgent());
                securityContext.put("sessionId", requestContext.getSessionId());

                if (affectedResources != null) {
                    securityContext.putAll(affectedResources);
                }
                if (threatIndicators != null) {
                    securityContext.put("threatIndicators", threatIndicators);
                }

                SecurityAuditEvent event = new SecurityAuditEvent(
                        this,
                        requestContext.getUserId(),
                        requestContext.getTenantId(),
                        eventType,
                        description,
                        riskLevel,
                        requestContext.getIpAddress(),
                        securityContext,
                        OffsetDateTime.now()
                );

                // 发布事件
                publishEventSafely(event);

                log.warn("安全审计事件发布成功: eventType={}, riskLevel={}", eventType, riskLevel);

            } catch (Exception e) {
                log.error("发布安全审计事件失败: eventType={}", eventType, e);
                recordPublishError();
                throw new AuditServiceException("SECURITY_EVENT_PUBLISH_FAILED",
                        "发布安全审计事件失败: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 发布系统事件
     *
     * <p>系统事件记录那些由系统自动执行的重要操作，如定时任务执行、
     * 系统配置变更、服务启停等。这些事件对于系统运维和问题诊断很重要。</p>
     *
     * @param eventType   系统事件类型
     * @param component   触发事件的系统组件
     * @param description 事件描述
     * @param severity    事件严重程度
     * @param metadata    事件元数据
     */
    @Async("auditTaskExecutor")
    public CompletableFuture<Void> publishSystemEvent(String eventType, String component,
                                                      String description, String severity,
                                                      Map<String, Object> metadata) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("发布系统审计事件: eventType={}, component={}, severity={}",
                        eventType, component, severity);

                // 系统事件的上下文信息
                Map<String, Object> systemContext = new HashMap<>();
                systemContext.put("component", component);
                systemContext.put("severity", severity);
                systemContext.put("hostname", getHostname());
                systemContext.put("processId", getProcessId());
                systemContext.put("threadInfo", getThreadInfo());

                if (metadata != null) {
                    systemContext.putAll(metadata);
                }

                SystemAuditEvent event = new SystemAuditEvent(
                        this,
                        eventType,
                        component,
                        description,
                        severity,
                        systemContext,
                        OffsetDateTime.now()
                );

                publishEventSafely(event);

                log.info("系统审计事件发布成功: eventType={}", eventType);

            } catch (Exception e) {
                log.error("发布系统审计事件失败: eventType={}", eventType, e);
                recordPublishError();
                // 系统事件发布失败通常不应该抛出异常，避免影响系统运行
                log.warn("系统事件发布失败，但继续执行: {}", e.getMessage());
            }
        });
    }

    /**
     * 发布数据变更事件
     *
     * <p>数据变更事件记录重要数据的创建、修改、删除操作。这些事件
     * 对于数据审计、合规检查和变更追踪都很重要。</p>
     *
     * @param operationType 操作类型：CREATE、UPDATE、DELETE
     * @param entityType    实体类型
     * @param entityId      实体ID
     * @param changedFields 变更的字段信息
     * @param oldValues     变更前的值
     * @param newValues     变更后的值
     */
    @Async("auditTaskExecutor")
    public CompletableFuture<Void> publishDataChangeEvent(String operationType, String entityType,
                                                          Long entityId,
                                                          Map<String, Object> changedFields,
                                                          Map<String, Object> oldValues,
                                                          Map<String, Object> newValues) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("发布数据变更审计事件: operationType={}, entityType={}, entityId={}",
                        operationType, entityType, entityId);

                RequestContext requestContext = extractRequestContext();

                // 构建数据变更的详细信息
                Map<String, Object> changeDetails = new HashMap<>();
                changeDetails.put("operationType", operationType);
                changeDetails.put("entityType", entityType);
                changeDetails.put("entityId", entityId);
                changeDetails.put("changedFields", changedFields);
                changeDetails.put("oldValues", maskSensitiveData(oldValues));
                changeDetails.put("newValues", maskSensitiveData(newValues));
                changeDetails.put("changeTime", OffsetDateTime.now());

                DataChangeAuditEvent event = new DataChangeAuditEvent(
                        this,
                        requestContext.getUserId(),
                        requestContext.getTenantId(),
                        operationType,
                        entityType,
                        entityId,
                        changeDetails,
                        requestContext.getIpAddress(),
                        OffsetDateTime.now()
                );

                publishEventSafely(event);

                log.debug("数据变更审计事件发布成功: operationType={}", operationType);

            } catch (Exception e) {
                log.error("发布数据变更审计事件失败: operationType={}", operationType, e);
                recordPublishError();
                throw new AuditServiceException("DATA_CHANGE_EVENT_PUBLISH_FAILED",
                        "发布数据变更审计事件失败: " + e.getMessage(), e);
            }
        });
    }

    // =================== 私有辅助方法 ===================

    /**
     * 安全地发布事件
     *
     * <p>这个方法提供了统一的事件发布逻辑，包括错误处理、统计记录等。
     * 它确保事件发布过程的稳定性和可观察性。</p>
     */
    private void publishEventSafely(Object event) {
        try {
            if (eventPublisher == null) {
                log.warn("事件发布器尚未初始化，无法发布事件: {}", event.getClass().getSimpleName());
                return;
            }

            eventPublisher.publishEvent(event);
            recordSuccessfulPublish();

        } catch (Exception e) {
            log.error("事件发布失败: eventType={}", event.getClass().getSimpleName(), e);
            recordPublishError();
            throw e;
        }
    }

    /**
     * 提取当前请求的上下文信息
     *
     * <p>这个方法从当前的HTTP请求中提取重要的上下文信息，如用户ID、
     * IP地址、用户代理等。这些信息对于审计事件的完整性很重要。</p>
     */
    private RequestContext extractRequestContext() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                // 如果没有HTTP请求上下文（如异步任务），返回默认值
                return RequestContext.createDefault();
            }

            HttpServletRequest request = attributes.getRequest();

            Long userId = extractUserIdFromRequest(request);
            Long tenantId = extractTenantIdFromRequest(request);
            String ipAddress = extractIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            String sessionId = request.getSession(false) != null ?
                    request.getSession(false).getId() : null;

            Map<String, String> headers = new HashMap<>();
            // 只收集非敏感的请求头
            String[] allowedHeaders = {"Accept", "Accept-Language", "Accept-Encoding", "Referer"};
            for (String headerName : allowedHeaders) {
                String headerValue = request.getHeader(headerName);
                if (headerValue != null) {
                    headers.put(headerName, headerValue);
                }
            }

            return new RequestContext(userId, tenantId, ipAddress, userAgent, sessionId, headers);

        } catch (Exception e) {
            log.debug("提取请求上下文失败，使用默认值", e);
            return RequestContext.createDefault();
        }
    }

    /**
     * 从HTTP请求中提取用户ID
     *
     * <p>这个方法需要根据具体的认证机制来实现。不同的项目可能使用
     * 不同的方式来存储和传递用户身份信息。</p>
     */
    private Long extractUserIdFromRequest(HttpServletRequest request) {
        // 这里应该根据实际的认证机制来提取用户ID
        // 可能从JWT Token、Session、或者请求头中获取

        // 简化实现：从请求属性中获取
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr instanceof Long) {
            return (Long) userIdAttr;
        }

        // 也可以从认证上下文获取
        try {
            // 这里可以添加从Spring Security获取用户信息的逻辑
            return 1L; // 临时返回值
        } catch (Exception e) {
            log.debug("无法提取用户ID", e);
            return null;
        }
    }

    /**
     * 从HTTP请求中提取租户ID
     */
    private Long extractTenantIdFromRequest(HttpServletRequest request) {
        // 租户ID可能来自请求头、URL参数或者用户的认证信息
        String tenantHeader = request.getHeader("X-Tenant-ID");
        if (tenantHeader != null) {
            try {
                return Long.parseLong(tenantHeader);
            } catch (NumberFormatException e) {
                log.debug("租户ID格式错误: {}", tenantHeader);
            }
        }

        // 也可以从用户的认证信息中获取默认租户
        return 1L; // 临时返回值
    }

    /**
     * 从HTTP请求中提取真实的IP地址
     *
     * <p>在使用负载均衡器或代理的环境中，需要从特定的HTTP头中获取真实IP。</p>
     */
    private String extractIpAddress(HttpServletRequest request) {
        String[] ipHeaders = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // 如果有多个IP，取第一个
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 合并上下文数据和额外数据
     */
    private Map<String, Object> mergeContextData(RequestContext context,
                                                 Map<String, Object> additionalData) {
        Map<String, Object> merged = new HashMap<>();

        // 添加请求上下文信息
        merged.put("sessionId", context.getSessionId());
        merged.put("requestHeaders", context.getHeaders());

        // 添加额外数据
        if (additionalData != null) {
            merged.putAll(additionalData);
        }

        return merged;
    }

    /**
     * 对敏感数据进行脱敏处理
     */
    private Map<String, Object> maskSensitiveData(Map<String, Object> data) {
        if (data == null) return null;

        Map<String, Object> masked = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey().toLowerCase();
            Object value = entry.getValue();

            // 对敏感字段进行脱敏
            if (key.contains("password") || key.contains("secret") ||
                    key.contains("token") || key.contains("key")) {
                masked.put(entry.getKey(), "***MASKED***");
            } else {
                masked.put(entry.getKey(), value);
            }
        }
        return masked;
    }

    // =================== 系统信息获取方法 ===================

    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String getProcessId() {
        try {
            return java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private Map<String, Object> getThreadInfo() {
        Map<String, Object> threadInfo = new HashMap<>();
        Thread currentThread = Thread.currentThread();
        threadInfo.put("threadName", currentThread.getName());
        threadInfo.put("threadId", currentThread.threadId());
        threadInfo.put("threadPriority", currentThread.getPriority());
        return threadInfo;
    }

    // =================== 统计方法 ===================

    private void recordSuccessfulPublish() {
        totalEventsPublished++;
        lastPublishTime = System.currentTimeMillis();
    }

    private void recordPublishError() {
        totalPublishErrors++;
    }

    /**
     * 获取发布统计信息
     */
    public Map<String, Object> getPublishStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEventsPublished", totalEventsPublished);
        stats.put("totalPublishErrors", totalPublishErrors);
        stats.put("lastPublishTime", lastPublishTime);
        stats.put("errorRate", totalEventsPublished > 0 ?
                (double) totalPublishErrors / totalEventsPublished * 100 : 0);
        return stats;
    }

    // =================== 内部类定义 ===================

    /**
     * 请求上下文信息封装
     */
    @Getter
    private static class RequestContext {

        private final Long userId;
        private final Long tenantId;
        private final String ipAddress;
        private final String userAgent;
        private final String sessionId;
        private final Map<String, String> headers;

        public RequestContext(Long userId, Long tenantId, String ipAddress,
                              String userAgent, String sessionId, Map<String, String> headers) {
            this.userId = userId;
            this.tenantId = tenantId;
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
            this.sessionId = sessionId;
            this.headers = headers != null ? headers : new HashMap<>();
        }

        public static RequestContext createDefault() {
            return new RequestContext(null, null, "127.0.0.1", "System", null, new HashMap<>());
        }
    }
}