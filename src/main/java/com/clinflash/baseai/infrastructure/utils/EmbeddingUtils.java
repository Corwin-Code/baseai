package com.clinflash.baseai.infrastructure.utils;

import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * <h2>嵌入服务工具类</h2>
 *
 * <p>这个工具类提供了通用的功能，就像是一个工具箱，里面装着各种常用的工具。
 * 无论是OpenAI还是千问的实现，都可以使用这些工具来处理常见的任务。</p>
 */
public class EmbeddingUtils {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingUtils.class);

    /**
     * 创建限流器
     *
     * <p>限流器就像是水龙头的阀门，控制水流的速度。在API调用中，
     * 它确保我们不会因为请求过快而被服务商拒绝。</p>
     */
    public static RateLimiter createRateLimiter(double permitsPerSecond) {
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

    /**
     * 估算文本的Token数量
     *
     * <p>这是一个简化的估算方法。在实际应用中，不同的模型有不同的分词方式，
     * 但这个估算能够帮助我们大致判断文本是否会超出API限制。</p>
     */
    public static int estimateTokenCount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }

        // 对于中文，大约每个字符等于1个token
        // 对于英文，大约每4个字符等于1个token
        int chineseChars = 0;
        int totalChars = text.length();

        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) {
                chineseChars++;
            }
        }

        int englishChars = totalChars - chineseChars;
        return chineseChars + (englishChars / 4);
    }

    /**
     * 验证和清理文本输入
     */
    public static String validateAndCleanText(String text) {
        if (text == null) {
            throw new IllegalArgumentException("文本内容不能为null");
        }

        String cleaned = text.trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("文本内容不能为空");
        }

        // 移除控制字符，但保留必要的空白字符
        cleaned = cleaned.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        return cleaned;
    }
}