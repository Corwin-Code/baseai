package com.clinflash.baseai.infrastructure.event.listener;

import com.clinflash.baseai.domain.audit.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <h2>审计事件监听器</h2>
 *
 * <p>这个监听器就像是系统的"记录员"，它时刻关注着系统中发生的重要事件，
 * 并自动将这些事件转换为审计记录。想象一下安保人员在监控大楼的各个角落，
 * 一旦有重要事件发生，就会立即记录在案。</p>
 *
 * <p><b>监听范围：</b></p>
 * <p>我们监听多种类型的事件，包括：安全事件（登录、认证）、业务事件（数据操作）、
 * 系统事件（配置变更）等。通过事件驱动的方式，我们可以确保所有重要操作
 * 都被准确记录，而且不会遗漏。</p>
 *
 * <p><b>异步处理：</b></p>
 * <p>所有的审计记录都是异步处理的，这确保了审计操作不会影响主业务流程的性能。
 * 即使审计系统暂时不可用，也不会影响用户的正常操作。</p>
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    @Autowired
    private AuditService auditService;

    /**
     * 监听用户登录成功事件
     *
     * <p>用户登录是系统安全的第一道防线，每次成功登录都应该被记录。
     * 这些记录不仅用于安全审计，还可以用于用户行为分析。</p>
     */
    @EventListener
    @Async("auditTaskExecutor")
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        try {
            String username = event.getAuthentication().getName();
            String ipAddress = extractIpAddress();
            String userAgent = extractUserAgent();

            Map<String, Object> details = new HashMap<>();
            details.put("username", username);
            details.put("authenticationMethod", event.getAuthentication().getClass().getSimpleName());
            details.put("timestamp", System.currentTimeMillis());

            // 记录成功登录事件
            auditService.recordUserAction(
                    extractUserId(event.getAuthentication()),
                    "USER_LOGIN_SUCCESS",
                    "USER",
                    extractUserId(event.getAuthentication()),
                    "用户登录成功: " + username,
                    ipAddress,
                    userAgent,
                    details
            );

            log.debug("记录用户登录成功事件: username={}, ip={}", username, ipAddress);

        } catch (Exception e) {
            log.error("记录登录成功事件失败", e);
            // 注意：这里不抛出异常，避免影响登录流程
        }
    }

    /**
     * 监听用户登录失败事件
     *
     * <p>登录失败事件是安全监控的重要指标。连续的登录失败可能表示
     * 暴力破解攻击或者用户账号存在问题。我们需要特别关注这类事件。</p>
     */
    @EventListener
    @Async("auditTaskExecutor")
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        try {
            String username = event.getAuthentication().getName();
            String ipAddress = extractIpAddress();
            String userAgent = extractUserAgent();
            String failureReason = event.getException().getMessage();

            Map<String, Object> details = new HashMap<>();
            details.put("username", username);
            details.put("failureReason", failureReason);
            details.put("exceptionType", event.getException().getClass().getSimpleName());
            details.put("timestamp", System.currentTimeMillis());

            // 记录安全事件
            auditService.recordSecurityEvent(
                    "USER_LOGIN_FAILED",
                    null, // 登录失败时可能还没有用户ID
                    "用户登录失败: " + username + ", 原因: " + failureReason,
                    AuditService.RiskLevel.MEDIUM,
                    ipAddress,
                    List.of("USER:" + username)
            );

            log.warn("记录用户登录失败事件: username={}, ip={}, reason={}",
                    username, ipAddress, failureReason);

        } catch (Exception e) {
            log.error("记录登录失败事件失败", e);
        }
    }

    /**
     * 监听权限拒绝事件
     *
     * <p>权限拒绝事件可能表示用户尝试访问超出其权限范围的资源。
     * 虽然这在正常使用中可能会发生，但频繁的权限拒绝可能表示
     * 权限配置问题或潜在的安全威胁。</p>
     */
    @EventListener
    @Async("auditTaskExecutor")
    public void handleAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        try {
            String username = event.getAuthentication() != null ?
                    event.getAuthentication().get().getName() : "anonymous";
            String ipAddress = extractIpAddress();

            Map<String, Object> details = new HashMap<>();
            details.put("username", username);
            details.put("requestedResource", event.getAuthorizationDecision().toString());
            details.put("authorities", event.getAuthentication() != null ?
                    event.getAuthentication().get().getAuthorities().toString() : "none");

            auditService.recordSecurityEvent(
                    "AUTHORIZATION_DENIED",
                    extractUserId(event.getAuthentication().get()),
                    "访问权限被拒绝: " + username,
                    AuditService.RiskLevel.LOW,
                    ipAddress,
                    List.of("RESOURCE:" + event.getAuthorizationDecision().toString())
            );

            log.debug("记录权限拒绝事件: username={}, resource={}",
                    username, event.getAuthorizationDecision());

        } catch (Exception e) {
            log.error("记录权限拒绝事件失败", e);
        }
    }

    /**
     * 监听会话超时事件
     *
     * <p>会话超时是正常的安全机制，但我们仍然需要记录这些事件，
     * 特别是在安全要求严格的环境中。</p>
     */
    @EventListener
    @Async("auditTaskExecutor")
    public void handleSessionTimeout(SessionTimeoutEvent event) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("sessionId", event.getSessionId());
            details.put("timeout", event.getTimeout());
            details.put("lastAccessTime", event.getLastAccessTime());

            auditService.recordSystemEvent(
                    "SESSION_TIMEOUT",
                    "SecurityManager",
                    "用户会话超时: sessionId=" + event.getSessionId(),
                    AuditService.EventSeverity.LOW,
                    details
            );

            log.debug("记录会话超时事件: sessionId={}", event.getSessionId());

        } catch (Exception e) {
            log.error("记录会话超时事件失败", e);
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 从当前请求中提取IP地址
     *
     * <p>这个方法会尝试从多个HTTP头中获取真实的客户端IP地址，
     * 因为在使用代理或负载均衡器的环境中，直接获取的IP可能不准确。</p>
     */
    private String extractIpAddress() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                return "unknown";
            }

            HttpServletRequest request = attributes.getRequest();

            // 尝试从各种代理头中获取真实IP
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
                    // 取第一个IP地址（多个IP用逗号分隔）
                    return ip.split(",")[0].trim();
                }
            }

            // 如果都没有，使用远程地址
            return request.getRemoteAddr();

        } catch (Exception e) {
            log.debug("提取IP地址失败", e);
            return "unknown";
        }
    }

    /**
     * 从当前请求中提取用户代理信息
     */
    private String extractUserAgent() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                return "unknown";
            }

            HttpServletRequest request = attributes.getRequest();
            String userAgent = request.getHeader("User-Agent");

            return userAgent != null ? userAgent : "unknown";

        } catch (Exception e) {
            log.debug("提取User-Agent失败", e);
            return "unknown";
        }
    }

    /**
     * 从认证对象中提取用户ID
     *
     * <p>这个方法需要根据具体的认证实现来调整。不同的认证方式
     * 可能会以不同的格式存储用户信息。</p>
     */
    private Long extractUserId(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        try {
            // 这里需要根据实际的用户主体对象来调整
            Object principal = authentication.getPrincipal();

            // 如果是UserDetails实现
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                // 这里应该通过用户服务查询用户ID
                return parseUserIdFromUsername(username);
            }

            // 如果是自定义的用户主体对象
            if (principal.toString().matches("\\d+")) {
                return Long.parseLong(principal.toString());
            }

            return null;

        } catch (Exception e) {
            log.debug("提取用户ID失败", e);
            return null;
        }
    }

    /**
     * 从用户名解析用户ID
     *
     * <p>这是一个简化的实现。在实际项目中，应该通过用户服务
     * 查询用户名对应的用户ID。</p>
     */
    private Long parseUserIdFromUsername(String username) {
        // 简化实现，实际应该查询数据库
        return 1L;
    }

    // =================== 自定义事件类 ===================

    /**
     * <h3>会话超时事件</h3>
     *
     * <p>这是一个自定义的事件类，用于表示用户会话超时。
     * 在实际项目中，可能需要定义更多的自定义事件。</p>
     */
    public static class SessionTimeoutEvent {
        private final String sessionId;
        private final long timeout;
        private final long lastAccessTime;

        public SessionTimeoutEvent(String sessionId, long timeout, long lastAccessTime) {
            this.sessionId = sessionId;
            this.timeout = timeout;
            this.lastAccessTime = lastAccessTime;
        }

        public String getSessionId() {
            return sessionId;
        }

        public long getTimeout() {
            return timeout;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }
    }
}