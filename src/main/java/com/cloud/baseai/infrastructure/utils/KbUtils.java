package com.cloud.baseai.infrastructure.utils;

import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * <h2>知识库工具类</h2>
 *
 * <p>提供知识库模块常用的工具方法，包括文本处理、哈希计算、向量操作等。</p>
 */
public class KbUtils {

    private static final Logger log = LoggerFactory.getLogger(KbUtils.class);

    // 常用的文本清理正则表达式
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\\n{3,}");
    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern NON_PRINTABLE = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");

    // 语言检测正则
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]");
    private static final Pattern JAPANESE_PATTERN = Pattern.compile("[\\u3040-\\u309f\\u30a0-\\u30ff]");
    private static final Pattern KOREAN_PATTERN = Pattern.compile("[\\uac00-\\ud7af]");

    private KbUtils() {
        // 工具类不允许实例化
    }

    // =================== 文本处理方法 ===================

    /**
     * 清理和标准化文本
     *
     * <p>移除HTML标签、不可打印字符，标准化空白字符。</p>
     *
     * @param text 原始文本
     * @return 清理后的文本
     */
    public static String cleanText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        // 移除HTML标签
        text = HTML_TAGS.matcher(text).replaceAll(" ");

        // 移除不可打印字符（保留换行、回车、制表符）
        text = NON_PRINTABLE.matcher(text).replaceAll("");

        // 标准化Unicode字符
        text = Normalizer.normalize(text, Normalizer.Form.NFC);

        // 标准化空白字符
        text = text.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
        text = MULTIPLE_SPACES.matcher(text).replaceAll(" ");
        text = MULTIPLE_NEWLINES.matcher(text).replaceAll("\n\n");

        // 移除首尾空白
        return text.trim();
    }

    /**
     * 自动检测文本语言
     *
     * @param text 输入文本
     * @return 语言代码（zh-CN, ja, ko, en等）
     */
    public static String detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "auto";
        }

        int totalChars = text.length();
        int chineseChars = countMatches(CHINESE_PATTERN, text);
        int japaneseChars = countMatches(JAPANESE_PATTERN, text);
        int koreanChars = countMatches(KOREAN_PATTERN, text);

        // 如果CJK字符占30%以上，则认为是对应语言
        double threshold = 0.3;

        if (chineseChars > totalChars * threshold) {
            return "zh-CN";
        } else if (japaneseChars > totalChars * threshold) {
            return "ja";
        } else if (koreanChars > totalChars * threshold) {
            return "ko";
        }

        // 默认返回英语
        return "en";
    }

    /**
     * 估算文本的Token数量
     *
     * <p>使用简化的算法估算token数量，实际生产环境建议使用专业的Token计算库。</p>
     *
     * @param text     文本内容
     * @param langCode 语言代码
     * @return 估算的Token数量
     */
    public static int estimateTokenCount(String text, String langCode) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // 根据语言使用不同的估算方法
        if (langCode != null && langCode.startsWith("zh")) {
            // 中文：平均1.5个字符对应1个token
            return (int) Math.ceil(text.length() / 1.5);
        } else if (langCode != null && (langCode.startsWith("ja") || langCode.startsWith("ko"))) {
            // 日语/韩语：平均2个字符对应1个token
            return (int) Math.ceil(text.length() / 2.0);
        } else {
            // 英语等：平均4个字符对应1个token
            return (int) Math.ceil(text.length() / 4.0);
        }
    }

    /**
     * 截断文本到指定长度
     *
     * @param text      原始文本
     * @param maxLength 最大长度
     * @param suffix    截断后缀（如"..."）
     * @return 截断后的文本
     */
    public static String truncateText(String text, int maxLength, String suffix) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }

        if (suffix == null) {
            suffix = "...";
        }

        int cutLength = maxLength - suffix.length();
        if (cutLength <= 0) {
            return suffix.substring(0, maxLength);
        }

        return text.substring(0, cutLength) + suffix;
    }

    // =================== 哈希计算方法 ===================

    /**
     * 计算文本的SHA256哈希值
     *
     * @param content 文本内容
     * @return 64字符的十六进制哈希值
     */
    public static String calculateSha256(String content) {
        if (content == null) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256算法不可用", e);
            throw new RuntimeException("无法计算SHA-256哈希", e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    // =================== 向量操作方法 ===================

    /**
     * 计算两个向量的余弦相似度
     *
     * @param vector1 第一个向量
     * @param vector2 第二个向量
     * @return 余弦相似度（-1到1之间）
     */
    public static double cosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1 == null || vector2 == null) {
            throw new IllegalArgumentException("向量不能为null");
        }

        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("向量维度不匹配");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 归一化向量
     *
     * @param vector 输入向量
     * @return 归一化后的向量
     */
    public static float[] normalizeVector(float[] vector) {
        if (vector == null || vector.length == 0) {
            return vector;
        }

        double norm = 0.0;
        for (float value : vector) {
            norm += value * value;
        }

        norm = Math.sqrt(norm);
        if (norm == 0.0) {
            return vector;
        }

        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }

        return normalized;
    }

    /**
     * 向量格式化为PostgreSQL格式
     *
     * @param vector 向量数组
     * @return PostgreSQL向量字符串
     */
    public static String formatVectorForPostgreSQL(float[] vector) {
        if (vector == null || vector.length == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector[i]);
        }
        sb.append("]");

        return sb.toString();
    }

    // =================== 分页和搜索方法 ===================

    /**
     * 生成搜索高亮片段
     *
     * @param text     原始文本
     * @param keywords 关键词列表
     * @param maxCount 最大片段数
     * @param length   每个片段的长度
     * @return 高亮片段列表
     */
    public static List<String> generateHighlights(String text, List<String> keywords,
                                                  int maxCount, int length) {
        if (text == null || text.isEmpty() || keywords == null || keywords.isEmpty()) {
            return List.of();
        }

        List<String> highlights = new java.util.ArrayList<>();
        String lowerText = text.toLowerCase();

        for (String keyword : keywords) {
            if (highlights.size() >= maxCount) {
                break;
            }

            String lowerKeyword = keyword.toLowerCase();
            int index = lowerText.indexOf(lowerKeyword);

            while (index >= 0 && highlights.size() < maxCount) {
                int start = Math.max(0, index - length / 2);
                int end = Math.min(text.length(), start + length);

                // 调整start确保不超过end
                if (end - start < length && start > 0) {
                    start = Math.max(0, end - length);
                }

                String highlight = text.substring(start, end);
                if (start > 0) {
                    highlight = "..." + highlight;
                }
                if (end < text.length()) {
                    highlight = highlight + "...";
                }

                highlights.add(highlight);

                // 查找下一个出现位置
                index = lowerText.indexOf(lowerKeyword, index + 1);
            }
        }

        return highlights;
    }

    /**
     * 验证分页参数
     *
     * @param page    页码
     * @param size    页大小
     * @param maxSize 最大页大小
     * @return 验证后的参数数组 [page, size]
     */
    public static int[] validatePagination(Integer page, Integer size, int maxSize) {
        int validPage = (page == null || page < 0) ? 0 : page;
        int validSize = (size == null || size <= 0) ? 20 : Math.min(size, maxSize);
        return new int[]{validPage, validSize};
    }

    // =================== 私有辅助方法 ===================

    /**
     * 统计正则表达式匹配次数
     */
    private static int countMatches(Pattern pattern, String text) {
        int count = 0;
        var matcher = pattern.matcher(text);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * 检查字符串是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 安全地获取字符串长度
     */
    public static int safeLength(String str) {
        return str == null ? 0 : str.length();
    }

    /**
     * 安全地比较两个字符串
     */
    public static boolean safeEquals(String str1, String str2) {
        if (str1 == null && str2 == null) {
            return true;
        }
        if (str1 == null || str2 == null) {
            return false;
        }
        return str1.equals(str2);
    }

    // =================== 限流器和带重试的执行器 ===================

    /**
     * 创建限流器
     *
     * @param requestsPerMinute 每分钟请求数
     * @return 限流器实例
     */
    public static RateLimiter createRateLimiter(int requestsPerMinute) {
        if (requestsPerMinute <= 0) {
            requestsPerMinute = 60; // 默认值
        }

        double permitsPerSecond = requestsPerMinute / 60.0;
        return RateLimiter.create(permitsPerSecond);
    }

    /**
     * 带重试的执行器
     *
     * <p>这就像是一个耐心的助手，当第一次尝试失败时，它会稍等片刻再试一次，
     * 而不是立即放弃。这种策略在网络不稳定的环境中特别有用。</p>
     */
    public static <T> T executeWithRetry(
            Supplier<T> operation,
            int maxAttempts,
            long initialDelayMs,
            double multiplier,
            long maxDelayMs,
            String operationName) {

        Exception lastException = null;
        long currentDelay = initialDelayMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;

                if (attempt == maxAttempts) {
                    log.error("操作失败，已达最大重试次数: {} ({}次尝试)", operationName, maxAttempts, e);
                    break;
                }

                if (!shouldRetry(e)) {
                    log.warn("遇到不可重试的异常: {}", operationName, e);
                    break;
                }

                log.warn("操作失败，将在{}ms后重试: {} (第{}次尝试)",
                        currentDelay, operationName, attempt, e);

                try {
                    Thread.sleep(currentDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试过程被中断", ie);
                }

                currentDelay = Math.min((long) (currentDelay * multiplier), maxDelayMs);
            }
        }

        throw new RuntimeException("操作失败: " + operationName, lastException);
    }

    /**
     * 判断异常是否应该重试
     */
    private static boolean shouldRetry(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        // 网络相关错误通常可以重试
        if (message.contains("timeout") ||
                message.contains("connection") ||
                message.contains("socket")) {
            return true;
        }

        // HTTP 5xx 错误可以重试
        if (message.contains("500") ||
                message.contains("502") ||
                message.contains("503") ||
                message.contains("504")) {
            return true;
        }

        // 限流错误可以重试
        if (message.contains("429") ||
                message.contains("rate limit") ||
                message.contains("too many requests")) {
            return true;
        }

        return false;
    }
}