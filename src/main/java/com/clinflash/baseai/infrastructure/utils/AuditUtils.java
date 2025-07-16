package com.clinflash.baseai.infrastructure.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <h2>审计工具类</h2>
 *
 * <p>这个工具类就像是审计系统的"工具箱"，它提供了各种实用的方法
 * 来处理审计相关的常见任务，如数据脱敏、格式转换、验证等。</p>
 *
 * <p><b>设计原则：</b></p>
 * <p>所有的方法都是静态的，无状态的，可以在任何地方安全调用。
 * 方法实现注重性能和健壮性，即使在异常情况下也不会影响主业务流程。</p>
 */
public class AuditUtils {

    private static final Logger log = LoggerFactory.getLogger(AuditUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

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