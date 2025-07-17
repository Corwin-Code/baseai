package com.cloud.baseai.infrastructure.utils;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * <h2>对话工具类</h2>
 *
 * <p>这个工具类就像一个瑞士军刀，包含了对话系统中经常需要用到的各种实用功能。
 * 虽然每个方法都不复杂，但它们解决的都是实际开发中会遇到的具体问题。</p>
 *
 * <p><b>工具类的设计原则：</b></p>
 * <p>1. <strong>单一职责：</strong>每个方法只做一件事，做好一件事</p>
 * <p>2. <strong>无状态：</strong>方法不依赖实例状态，便于测试和并发</p>
 * <p>3. <strong>性能优化：</strong>对常用操作进行缓存和优化</p>
 * <p>4. <strong>防御性编程：</strong>对输入参数进行验证和容错处理</p>
 */
@Component
public class ChatUtils {

    // 常用正则表达式缓存
    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    // 内容安全检查的敏感词模式
    private static final Pattern SENSITIVE_PATTERN = getPattern(
            "(?i)(password|token|secret|key|api_key|access_token|密码|密钥|令牌)"
    );

    // 邮箱地址模式
    private static final Pattern EMAIL_PATTERN = getPattern(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    );

    // 电话号码模式
    private static final Pattern PHONE_PATTERN = getPattern(
            "(\\+?\\d{1,3}[- ]?)?\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}"
    );

    /**
     * 计算文本的哈希值
     *
     * <p>在AI系统中，我们经常需要为用户输入生成唯一标识符，用于：</p>
     * <p>1. 缓存相同问题的回答，提高响应速度</p>
     * <p>2. 检测重复内容，避免重复处理</p>
     * <p>3. 生成消息的指纹，用于审计和追踪</p>
     */
    public static String calculateContentHash(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(content.trim().getBytes(StandardCharsets.UTF_8));

            return KbUtils.bytesToHex(hashBytes);

        } catch (Exception e) {
            throw new RuntimeException("计算内容哈希失败", e);
        }
    }

    /**
     * 智能估算文本的Token数量
     *
     * <p>Token计算是AI系统的基础，直接影响成本控制和性能优化。
     * 虽然不同模型的Token化规则不同，但我们可以提供一个通用的估算方法。</p>
     */
    public static int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // 移除多余的空格和换行
        String cleanText = text.trim().replaceAll("\\s+", " ");

        // 分别计算中英文字符
        long englishChars = cleanText.chars()
                .filter(c -> c < 256) // ASCII字符
                .count();

        long chineseChars = cleanText.length() - englishChars;

        // 英文约4个字符1个Token，中文约1.5个字符1个Token
        int englishTokens = (int) Math.ceil(englishChars / 4.0);
        int chineseTokens = (int) Math.ceil(chineseChars / 1.5);

        return englishTokens + chineseTokens;
    }

    /**
     * 检测内容中的敏感信息
     *
     * <p>在企业级应用中，防止敏感信息泄露是关键的安全要求。
     * 这个方法可以快速识别可能包含敏感信息的内容。</p>
     */
    public static boolean containsSensitiveInfo(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        String lowerContent = content.toLowerCase();

        // 检查敏感词
        if (SENSITIVE_PATTERN.matcher(lowerContent).find()) {
            return true;
        }

        // 检查邮箱地址
        if (EMAIL_PATTERN.matcher(content).find()) {
            return true;
        }

        // 检查电话号码
        if (PHONE_PATTERN.matcher(content).find()) {
            return true;
        }

        // 检查可能的API密钥格式
        if (looksLikeApiKey(content)) {
            return true;
        }

        return false;
    }

    /**
     * 清理和规范化用户输入
     *
     * <p>用户输入往往包含各种不规范的内容，需要进行清理和标准化：</p>
     * <p>1. 移除多余的空格和特殊字符</p>
     * <p>2. 转换全角字符为半角字符</p>
     * <p>3. 统一换行符格式</p>
     * <p>4. 限制内容长度</p>
     */
    public static String sanitizeUserInput(String input, int maxLength) {
        if (input == null) {
            return "";
        }

        // 1. 基础清理
        String cleaned = input.trim()
                .replaceAll("\\r\\n|\\r", "\n")  // 统一换行符
                .replaceAll("\\t", "    ")        // 替换制表符
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", ""); // 移除控制字符

        // 2. 转换全角字符
        cleaned = convertFullWidthToHalfWidth(cleaned);

        // 3. 合并多余的空格
        cleaned = cleaned.replaceAll(" +", " ");

        // 4. 限制长度
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength) + "...";
        }

        return cleaned;
    }

    /**
     * 提取文本关键词
     *
     * <p>关键词提取对于内容分析、相似度计算、搜索优化都很重要。
     * 这里实现了一个简单但有效的关键词提取算法。</p>
     */
    public static java.util.Set<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return java.util.Set.of();
        }

        // 分词并过滤
        return java.util.Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(word -> word.length() > 2)  // 过滤短词
                .filter(word -> !isStopWord(word))  // 过滤停用词
                .filter(word -> !word.matches("\\d+")) // 过滤纯数字
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 计算两个文本的相似度
     *
     * <p>文本相似度计算在对话系统中有很多应用场景：</p>
     * <p>1. 检测重复问题</p>
     * <p>2. 推荐相关内容</p>
     * <p>3. 评估回答质量</p>
     * <p>4. 聚类相似对话</p>
     */
    public static double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }

        java.util.Set<String> keywords1 = extractKeywords(text1);
        java.util.Set<String> keywords2 = extractKeywords(text2);

        if (keywords1.isEmpty() && keywords2.isEmpty()) {
            return 1.0;
        }

        if (keywords1.isEmpty() || keywords2.isEmpty()) {
            return 0.0;
        }

        // 计算Jaccard相似度
        java.util.Set<String> intersection = new java.util.HashSet<>(keywords1);
        intersection.retainAll(keywords2);

        java.util.Set<String> union = new java.util.HashSet<>(keywords1);
        union.addAll(keywords2);

        return (double) intersection.size() / union.size();
    }

    /**
     * 格式化Token使用量显示
     */
    public static String formatTokenUsage(int promptTokens, int completionTokens) {
        int totalTokens = promptTokens + completionTokens;
        return String.format("%,d tokens (输入: %,d, 输出: %,d)",
                totalTokens, promptTokens, completionTokens);
    }

    /**
     * 格式化费用显示
     */
    public static String formatCost(double costUsd) {
        if (costUsd < 0.01) {
            return String.format("$%.4f", costUsd);
        } else {
            return String.format("$%.2f", costUsd);
        }
    }

    /**
     * 格式化响应时间显示
     */
    public static String formatResponseTime(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        } else if (millis < 60000) {
            return String.format("%.1fs", millis / 1000.0);
        } else {
            long minutes = millis / 60000;
            long seconds = (millis % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 获取缓存的正则表达式
     */
    private static Pattern getPattern(String regex) {
        return PATTERN_CACHE.computeIfAbsent(regex, Pattern::compile);
    }

    /**
     * 检查是否像API密钥
     */
    private static boolean looksLikeApiKey(String text) {
        // 检查类似API密钥的模式：长度超过20的字母数字组合
        Pattern apiKeyPattern = getPattern("[a-zA-Z0-9]{20,}");
        return apiKeyPattern.matcher(text).find();
    }

    /**
     * 转换全角字符为半角字符
     */
    private static String convertFullWidthToHalfWidth(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= 0xFF01 && c <= 0xFF5E) {
                // 全角ASCII字符转半角
                sb.append((char) (c - 0xFEE0));
            } else if (c == 0x3000) {
                // 全角空格转半角空格
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 检查是否为停用词
     */
    private static boolean isStopWord(String word) {
        java.util.Set<String> stopWords = java.util.Set.of(
                // 英文停用词
                "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
                "of", "with", "by", "is", "are", "was", "were", "be", "been", "have",
                "has", "had", "do", "does", "did", "will", "would", "could", "should",
                "can", "may", "might", "must", "shall", "this", "that", "these", "those",
                // 中文停用词
                "的", "了", "在", "是", "有", "和", "与", "或", "但", "如果", "因为",
                "所以", "然后", "还是", "就是", "这个", "那个", "什么", "怎么", "为什么"
        );
        return stopWords.contains(word.toLowerCase());
    }
}