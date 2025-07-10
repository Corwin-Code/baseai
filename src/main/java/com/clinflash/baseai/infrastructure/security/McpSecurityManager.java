package com.clinflash.baseai.infrastructure.security;

import com.clinflash.baseai.infrastructure.config.McpConfig;
import com.clinflash.baseai.infrastructure.exception.McpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h2>MCP安全管理器</h2>
 *
 * <p>负责MCP系统的安全控制，包括权限验证、配额检查、IP限制等功能。
 * 这个组件是MCP系统安全防护的第一道防线。</p>
 *
 * <p><b>安全检查流程：</b></p>
 * <ol>
 * <li>IP地址白名单验证</li>
 * <li>API密钥有效性检查</li>
 * <li>租户权限验证</li>
 * <li>配额使用情况检查</li>
 * <li>请求签名验证（可选）</li>
 * </ol>
 */
@Component
public class McpSecurityManager {

    private static final Logger log = LoggerFactory.getLogger(McpSecurityManager.class);

    private final McpConfig.SecurityConfig securityConfig;
    private final Map<String, RateLimitInfo> rateLimitCache;

    public McpSecurityManager(McpConfig.SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
        this.rateLimitCache = new ConcurrentHashMap<>();
    }

    /**
     * 验证工具访问权限
     *
     * @param tenantId 租户ID
     * @param toolCode 工具代码
     * @param clientIp 客户端IP
     * @param apiKey   API密钥
     * @return 验证通过返回true
     * @throws McpException 验证失败时抛出异常
     */
    public boolean validateAccess(Long tenantId, String toolCode, String clientIp, String apiKey) {
        log.debug("验证工具访问权限: tenantId={}, toolCode={}, clientIp={}", tenantId, toolCode, clientIp);

        try {
            // 1. IP地址检查
            if (securityConfig.isEnableIpRestriction()) {
                validateIpAddress(clientIp);
            }

            // 2. API密钥检查
            validateApiKey(apiKey);

            // 3. 速率限制检查
            validateRateLimit(tenantId, toolCode);

            log.debug("工具访问权限验证通过: tenantId={}, toolCode={}", tenantId, toolCode);
            return true;

        } catch (Exception e) {
            log.warn("工具访问权限验证失败: tenantId={}, toolCode={}, error={}",
                    tenantId, toolCode, e.getMessage());
            throw e;
        }
    }

    /**
     * 验证配额使用情况
     */
    public boolean validateQuota(Long tenantId, String toolCode, Integer quotaUsed, Integer quotaLimit) {
        if (!securityConfig.isEnableQuotaCheck()) {
            return true; // 配额检查已禁用
        }

        if (quotaLimit == null) {
            return true; // 无配额限制
        }

        int used = quotaUsed != null ? quotaUsed : 0;
        if (used >= quotaLimit) {
            throw new McpException("QUOTA_EXCEEDED",
                    String.format("租户 %d 的工具 %s 配额已用完: %d/%d", tenantId, toolCode, used, quotaLimit));
        }

        // 检查是否接近配额限制
        double usageRate = (double) used / quotaLimit;
        if (usageRate > 0.9) {
            log.warn("租户配额使用率过高: tenantId={}, toolCode={}, usage={}/{}, rate={}%",
                    tenantId, toolCode, used, quotaLimit, String.format("%.1f", usageRate * 100));
        }

        return true;
    }

    /**
     * 验证工具参数安全性
     */
    public boolean validateParameters(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return true;
        }

        // 检查危险参数
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 检查SQL注入风险
            if (value instanceof String stringValue) {
                if (containsSqlInjectionPattern(stringValue)) {
                    throw new McpException("DANGEROUS_PARAMETER",
                            "参数包含潜在的SQL注入模式: " + key);
                }

                // 检查脚本注入风险
                if (containsScriptInjectionPattern(stringValue)) {
                    throw new McpException("DANGEROUS_PARAMETER",
                            "参数包含潜在的脚本注入模式: " + key);
                }
            }
        }

        return true;
    }

    // =================== 私有验证方法 ===================

    /**
     * 验证IP地址
     */
    private void validateIpAddress(String clientIp) {
        if (clientIp == null || clientIp.trim().isEmpty()) {
            throw new McpException("INVALID_IP", "客户端IP地址为空");
        }

        if (securityConfig.getAllowedIps().length > 0) {
            boolean allowed = Arrays.asList(securityConfig.getAllowedIps()).contains(clientIp);
            if (!allowed) {
                throw new McpException("IP_NOT_ALLOWED", "IP地址不在白名单中: " + clientIp);
            }
        }
    }

    /**
     * 验证API密钥
     */
    private void validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new McpException("MISSING_API_KEY", "API密钥不能为空");
        }

        // 这里可以实现更复杂的API密钥验证逻辑
        // 比如检查密钥格式、过期时间、签名等
        if (apiKey.length() < 16) {
            throw new McpException("INVALID_API_KEY", "API密钥格式无效");
        }
    }

    /**
     * 验证速率限制
     */
    private void validateRateLimit(Long tenantId, String toolCode) {
        String key = tenantId + ":" + toolCode;
        RateLimitInfo info = rateLimitCache.computeIfAbsent(key, k -> new RateLimitInfo());

        long now = System.currentTimeMillis();
        long windowStart = now - 60000; // 1分钟窗口

        // 清理过期的请求记录
        info.requests.removeIf(timestamp -> timestamp < windowStart);

        // 检查当前窗口内的请求数
        if (info.requests.size() >= 100) { // 每分钟最多100次请求
            throw new McpException("RATE_LIMIT_EXCEEDED",
                    String.format("租户 %d 的工具 %s 请求频率过高", tenantId, toolCode));
        }

        // 记录当前请求
        info.requests.add(now);
    }

    /**
     * 检查SQL注入模式
     */
    private boolean containsSqlInjectionPattern(String value) {
        String lowerValue = value.toLowerCase();
        String[] sqlPatterns = {
                "union select", "drop table", "delete from", "insert into",
                "update set", "'; ", "\"; ", "-- ", "/*", "*/"
        };

        for (String pattern : sqlPatterns) {
            if (lowerValue.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查脚本注入模式
     */
    private boolean containsScriptInjectionPattern(String value) {
        String lowerValue = value.toLowerCase();
        String[] scriptPatterns = {
                "<script", "javascript:", "onload=", "onerror=", "eval(",
                "document.cookie", "window.location", "alert("
        };

        for (String pattern : scriptPatterns) {
            if (lowerValue.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 速率限制信息
     */
    private static class RateLimitInfo {
        private final java.util.List<Long> requests = new java.util.ArrayList<>();
    }
}