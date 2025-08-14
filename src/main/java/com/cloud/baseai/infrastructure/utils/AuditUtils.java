package com.cloud.baseai.infrastructure.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * <h2>审计与脱敏工具类</h2>
 *
 * <p>这个工具类就像是审计系统的"工具箱"，它提供了各种实用的方法
 * 来处理审计相关的常见任务，如数据脱敏、格式转换、验证等。</p>
 *
 * <p><b>特性：</b></p>
 * <ul>
 * <li><b>键名识别：</b>支持中文与英文的常见敏感字段（如：密钥/令牌/口令/凭据/URL/端点等）。</li>
 * <li><b>值级兜底：</b>即便键名不敏感，只要值中出现密钥 Token/Authorization/Bearer、URL 查询敏感参数等，也会被替换。</li>
 * <li><b>URL 脱敏：</b>仅保留 <code>scheme://host/***</code>，隐藏 path/query/fragment。</li>
 * <li><b>日志友好：</b>提供 sanitize/sanitizeArgs 用于日志格式化参数统一脱敏。</li>
 * </ul>
 *
 * <p>注意：该类为无状态静态工具，线程安全，可在任意位置直接调用。</p>
 */
public final class AuditUtils {

    private static final Logger log = LoggerFactory.getLogger(AuditUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private AuditUtils() {
    }

    // 敏感信息匹配模式
    private static final Pattern[] SENSITIVE_PATTERNS = {
            Pattern.compile("password", Pattern.CASE_INSENSITIVE),
            Pattern.compile("pwd", Pattern.CASE_INSENSITIVE),
            Pattern.compile("secret", Pattern.CASE_INSENSITIVE),
            Pattern.compile("token", Pattern.CASE_INSENSITIVE),
            Pattern.compile("key", Pattern.CASE_INSENSITIVE),
            Pattern.compile("credit.*card", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ssn", Pattern.CASE_INSENSITIVE)
    };

    /* -----------------------------
     * 键名正则（中文 + 英文常见别名）
     * ----------------------------- */
    private static final Pattern KEYNAME_SECRET = Pattern.compile(
            "(?i)(key|secret|password|token|authorization|credential|client_?secret|access_?key|refresh_?token|passwd|pwd|"
                    + "密钥|口令|凭据|访问密钥|刷新令牌)"
    );

    private static final Pattern KEYNAME_URL = Pattern.compile(
            "(?i)(url|endpoint|address|host|base_?url|base-?url|server|addr|"
                    + "服务地址|端点|主机|域名|接口地址)"
    );

    /* -----------------------------
     * 值级兜底正则（无论键名如何）
     * ----------------------------- */
    /**
     * OpenAI/代理类密钥 sk-/hk-（仅保留前缀和后4位）
     */
    private static final Pattern KEY_STYLE = Pattern.compile("\\b(?:sk|hk)-[A-Za-z0-9-_]{8,}\\b");
    /**
     * Bearer Token
     */
    private static final Pattern BEARER = Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9-_.]{8,}");
    /**
     * Authorization: xxxxx
     */
    private static final Pattern AUTH_HEADER = Pattern.compile("(?i)(Authorization\\s*:\\s*)([^\\s,;]{8,})");
    /**
     * URL 查询中的敏感参数值（token/key/secret/sig 等）
     */
    private static final Pattern URL_SECRET_QUERY = Pattern.compile(
            "([?&](?:key|api[_-]?key|token|access[_-]?token|signature|sig|secret)\\s*=)\\s*([^&\\s#]+)",
            Pattern.CASE_INSENSITIVE
    );

    /* ============================================================
     * API：用于配置摘要（有键名）或任意需要键值对层级脱敏的场景
     * ============================================================ */

    /**
     * 对键值对进行综合脱敏：
     * <ol>
     *   <li>若键名匹配敏感字段：对值做对应强脱敏（密钥仅留后4位；URL 仅留域名）。</li>
     *   <li>否则：对值启用模式兜底（sk-/hk-/Bearer/Authorization/URL查询敏感项）。</li>
     * </ol>
     *
     * @param key   字段名（可为中文）
     * @param value 字段值
     * @return 已脱敏字符串
     */
    public static String sanitize(String key, Object value) {
        final String original = Objects.toString(value, "null");
        String v = original;

        // 1) 键名强制脱敏
        if (key != null) {
            String k = key.toLowerCase();
            if (KEYNAME_SECRET.matcher(k).find()) {
                return maskSecretKeepingPrefix(v);
            }
            if (KEYNAME_URL.matcher(k).find()) {
                return maskUrl(v);
            }
        }

        // 2) 值级兜底
        v = KEY_STYLE.matcher(v).replaceAll(m -> maskSecretKeepingPrefix(m.group()));
        v = BEARER.matcher(v).replaceAll("Bearer ****");
        v = AUTH_HEADER.matcher(v).replaceAll("$1****");
        v = URL_SECRET_QUERY.matcher(v).replaceAll("$1****");

        if (looksLikeUrl(v)) {
            v = maskUrl(v);
        }
        return v;
    }

    /* =========================================
     * API：用于日志格式化参数（无键名上下文）
     * ========================================= */

    /**
     * 对日志入参进行统一脱敏（无键名上下文），可直接用于 String.format 的 args。
     *
     * @param args 原始参数
     * @return 脱敏后的参数数组
     */
    public static Object[] sanitizeArgs(Object... args) {
        if (args == null || args.length == 0) return args;
        Object[] out = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            String v = Objects.toString(args[i], "null");
            v = KEY_STYLE.matcher(v).replaceAll(m -> maskSecretKeepingPrefix(m.group()));
            v = BEARER.matcher(v).replaceAll("Bearer ****");
            v = AUTH_HEADER.matcher(v).replaceAll("$1****");
            v = URL_SECRET_QUERY.matcher(v).replaceAll("$1****");
            if (looksLikeUrl(v)) v = maskUrl(v);
            out[i] = v;
        }
        return out;
    }

    /* =========================
     * 具体脱敏策略
     * ========================= */

    /**
     * 密钥仅保留 sk-/hk- 前缀与后4位。
     *
     * @param s 原密钥
     * @return 脱敏密钥
     */
    public static String maskSecretKeepingPrefix(String s) {
        if (s == null || s.isEmpty()) return "****";
        String prefix = "";
        if (s.startsWith("sk-") || s.startsWith("hk-")) {
            prefix = s.substring(0, 3);
            s = s.substring(3);
        }
        int n = s.length();
        if (n <= 4) return prefix + "****";
        return prefix + "****" + s.substring(n - 4);
    }

    /**
     * URL 仅保留 scheme://host/***，隐藏 path/query/fragment。
     *
     * @param url 原 URL
     * @return 脱敏后的 URL 描述
     */
    public static String maskUrl(String url) {
        if (url == null || url.isEmpty()) return "***";
        try {
            URI u = URI.create(url);
            String scheme = u.getScheme() != null ? u.getScheme() : "http";
            String host = u.getHost() != null ? u.getHost() : "";
            if (!host.isEmpty()) return scheme + "://" + host + "/***";
        } catch (Exception ignored) {
        }
        return "***";
    }

    private static boolean looksLikeUrl(String s) {
        return s.startsWith("http://") || s.startsWith("https://");
    }

    /**
     * 脱敏敏感数据
     *
     * <p>这个方法会检查数据中是否包含敏感信息，如果包含，
     * 会用星号替换敏感部分。这是保护用户隐私的重要措施。</p>
     *
     * @param data 原始数据
     * @return 脱敏后的数据
     */
    public static Map<String, Object> maskSensitiveData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        Map<String, Object> maskedData = new HashMap<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (isSensitiveField(key)) {
                maskedData.put(key, maskValue(value));
            } else {
                maskedData.put(key, value);
            }
        }

        return maskedData;
    }

    /**
     * 判断字段是否为敏感字段
     */
    private static boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }

        for (Pattern pattern : SENSITIVE_PATTERNS) {
            if (pattern.matcher(fieldName).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 脱敏值
     */
    private static Object maskValue(Object value) {
        if (value == null) {
            return null;
        }

        String str = value.toString();
        if (str.length() <= 4) {
            return "****";
        }

        // 保留前2位和后2位，中间用星号替换
        return str.substring(0, 2) + "****" + str.substring(str.length() - 2);
    }

    /* ============================================================
     * 其余辅助能力（JSON 安全序列化/反序列化等）
     * ============================================================ */

    /**
     * 安全地序列化对象为JSON
     *
     * <p>这个方法会安全地将对象转换为JSON字符串，即使转换失败
     * 也不会抛出异常，而是返回一个错误描述。</p>
     */
    public static String safeSerializeToJson(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("JSON序列化失败: {}", e.getMessage());
            return "{\"error\":\"序列化失败\",\"type\":\"" + obj.getClass().getSimpleName() + "\"}";
        }
    }

    /**
     * 安全地从JSON反序列化对象
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> safeDeserializeFromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("JSON反序列化失败: {}", e.getMessage());
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "反序列化失败");
            errorMap.put("originalJson", json.length() > 100 ? json.substring(0, 100) + "..." : json);
            return errorMap;
        }
    }

    /**
     * 生成操作摘要
     *
     * <p>这个方法生成简洁的操作摘要，适合在界面上显示。</p>
     */
    public static String generateOperationSummary(String action, String targetType,
                                                  Long targetId, String username) {
        StringBuilder summary = new StringBuilder();

        if (username != null) {
            summary.append(username);
        } else {
            summary.append("系统");
        }

        summary.append(" ");

        // 将操作码转换为人类可读的描述
        String actionDesc = translateAction(action);
        summary.append(actionDesc);

        if (targetType != null) {
            summary.append(" ").append(translateTargetType(targetType));
            if (targetId != null) {
                summary.append("(#").append(targetId).append(")");
            }
        }

        return summary.toString();
    }

    /**
     * 翻译操作码为中文描述
     */
    private static String translateAction(String action) {
        if (action == null) {
            return "执行了操作";
        }

        return switch (action) {
            case "USER_LOGIN" -> "登录了系统";
            case "USER_LOGOUT" -> "退出了系统";
            case "DATA_CREATE" -> "创建了";
            case "DATA_UPDATE" -> "更新了";
            case "DATA_DELETE" -> "删除了";
            case "DATA_QUERY" -> "查询了";
            case "PERMISSION_DENIED" -> "尝试访问";
            default -> "执行了 " + action;
        };
    }

    /**
     * 翻译目标类型为中文描述
     */
    private static String translateTargetType(String targetType) {
        if (targetType == null) {
            return "";
        }

        return switch (targetType) {
            case "USER" -> "用户";
            case "DOCUMENT" -> "文档";
            case "TENANT" -> "租户";
            case "SYSTEM" -> "系统";
            default -> targetType;
        };
    }

    /**
     * 验证IP地址格式
     */
    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }

        // IPv4地址验证
        if (ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
            String[] parts = ip.split("\\.");
            for (String part : parts) {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    return false;
                }
            }
            return true;
        }

        // IPv6地址验证（简化版）
        if (ip.matches("^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$")) {
            return true;
        }

        // 特殊地址
        return "localhost".equals(ip) || "127.0.0.1".equals(ip) || "::1".equals(ip);
    }

    /**
     * 计算风险评分
     *
     * <p>这个方法基于多个因素计算操作的风险评分，用于安全分析。</p>
     */
    public static int calculateRiskScore(String action, String resultStatus,
                                         String ipAddress, String userAgent) {
        int riskScore = 0;

        // 基于操作类型的风险
        if (action != null) {
            switch (action) {
                case "USER_LOGIN_FAILED", "PERMISSION_DENIED" -> riskScore += 30;
                case "DATA_DELETE", "SYSTEM_CONFIG" -> riskScore += 20;
                case "DATA_UPDATE", "USER_PRIVILEGE_CHANGE" -> riskScore += 10;
                default -> riskScore += 5;
            }
        }

        // 基于操作结果的风险
        if ("FAILED".equals(resultStatus)) {
            riskScore += 20;
        }

        // 基于IP地址的风险（简化实现）
        if (ipAddress != null && !ipAddress.startsWith("192.168.") &&
                !ipAddress.startsWith("10.") && !ipAddress.equals("127.0.0.1")) {
            riskScore += 15; // 外部IP增加风险
        }

        // 基于用户代理的风险（检测自动化工具）
        if (userAgent != null && (userAgent.contains("bot") || userAgent.contains("crawler"))) {
            riskScore += 25;
        }

        return Math.min(riskScore, 100); // 最高100分
    }

    /**
     * 格式化时间差
     *
     * <p>将毫秒时间差转换为人类可读的格式，如"2小时3分钟前"。</p>
     */
    public static String formatTimeDifference(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "天前";
        } else if (hours > 0) {
            return hours + "小时" + (minutes % 60 > 0 ? (minutes % 60) + "分钟" : "") + "前";
        } else if (minutes > 0) {
            return minutes + "分钟前";
        } else {
            return "刚刚";
        }
    }
}