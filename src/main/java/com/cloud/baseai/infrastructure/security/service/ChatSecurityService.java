package com.cloud.baseai.infrastructure.security.service;

import com.cloud.baseai.infrastructure.utils.ChatUtils;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;
import java.util.List;
import java.util.Set;

/**
 * <h2>对话安全服务</h2>
 *
 * <p>在AI对话系统中，安全性是至关重要的。这个服务提供了多层次的安全保护，
 * 包括内容过滤、敏感信息检测、恶意输入防护等功能。</p>
 *
 * <p><b>安全威胁与防护：</b></p>
 * <p>AI系统面临的安全威胁包括但不限于：</p>
 * <p>1. <strong>Prompt注入攻击：</strong>恶意用户试图操控AI行为</p>
 * <p>2. <strong>敏感信息泄露：</strong>AI可能无意中暴露敏感数据</p>
 * <p>3. <strong>有害内容生成：</strong>AI可能生成不当或有害内容</p>
 * <p>4. <strong>系统滥用：</strong>恶意用户大量消耗系统资源</p>
 */
@Service
public class ChatSecurityService {

    private static final Logger log = LoggerFactory.getLogger(ChatSecurityService.class);

    // 危险指令模式
    private static final Set<Pattern> DANGEROUS_PATTERNS = Set.of(
            Pattern.compile("(?i)(ignore|forget|disregard).*(previous|above|system|instruction)", Pattern.MULTILINE),
            Pattern.compile("(?i)(you are|act as|pretend to be|role.?play)", Pattern.MULTILINE),
            Pattern.compile("(?i)(jailbreak|prison|escape|break free)", Pattern.MULTILINE),
            Pattern.compile("(?i)(generate|create|write).*(malware|virus|exploit)", Pattern.MULTILINE)
    );

    // 敏感信息模式
    private static final Set<Pattern> SENSITIVE_PATTERNS = Set.of(
            Pattern.compile("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b"), // 信用卡号
            Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"), // 社会安全号码
            Pattern.compile("(?i)(password|pwd|secret|token|key)\\s*[:=]\\s*\\S+"), // 密码模式
            Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b") // 邮箱
    );

    /**
     * 综合安全检查
     *
     * <p>这是安全检查的入口方法，它会执行全面的安全扫描，
     * 包括内容安全、敏感信息检测、恶意模式识别等。</p>
     */
    public SecurityCheckResult performSecurityCheck(String content, SecurityContext context) {
        if (content == null || content.trim().isEmpty()) {
            return SecurityCheckResult.safe();
        }

        SecurityCheckResult.Builder resultBuilder = SecurityCheckResult.builder();

        // 1. 检查危险指令
        DangerousPatternResult dangerousResult = checkDangerousPatterns(content);
        resultBuilder.withDangerousPatterns(dangerousResult);

        // 2. 检查敏感信息
        SensitiveInfoResult sensitiveResult = checkSensitiveInformation(content);
        resultBuilder.withSensitiveInfo(sensitiveResult);

        // 3. 检查内容长度和复杂度
        ContentAnalysisResult analysisResult = analyzeContent(content);
        resultBuilder.withContentAnalysis(analysisResult);

        // 4. 检查用户行为模式
        if (context != null) {
            BehaviorAnalysisResult behaviorResult = analyzeBehavior(context);
            resultBuilder.withBehaviorAnalysis(behaviorResult);
        }

        return resultBuilder.build();
    }

    /**
     * 内容清理和脱敏
     *
     * <p>对于检测到问题的内容，这个方法会尝试进行清理和脱敏处理，
     * 移除或替换敏感信息，确保内容的安全性。</p>
     */
    public String sanitizeContent(String content) {
        if (content == null) {
            return null;
        }

        String sanitized = content;

        // 脱敏敏感信息
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("***");
        }

        // 清理HTML标签和潜在的脚本
        sanitized = sanitized.replaceAll("<[^>]*>", "");

        // 限制长度
        if (sanitized.length() > 10000) {
            sanitized = sanitized.substring(0, 10000) + "...";
        }

        return ChatUtils.sanitizeUserInput(sanitized, 10000);
    }

    /**
     * 生成安全的错误响应
     *
     * <p>当检测到安全问题时，需要给用户一个友好而安全的错误提示，
     * 既不暴露系统细节，又能引导用户正确使用。</p>
     */
    public String generateSecurityErrorResponse(SecurityCheckResult result) {
        if (result.isDangerous()) {
            return "抱歉，您的输入包含了一些不适当的内容。请重新组织您的问题，我会很乐意为您提供帮助。";
        }

        if (result.hasSensitiveInfo()) {
            return "为了保护您的隐私安全，请不要在对话中包含敏感信息如密码、信用卡号等。您可以重新描述您的问题。";
        }

        return "您的输入存在一些问题，请检查后重新发送。";
    }

    // =================== 私有检查方法 ===================

    private DangerousPatternResult checkDangerousPatterns(String content) {
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(content).find()) {
                return new DangerousPatternResult(true, "检测到潜在的危险指令模式");
            }
        }
        return new DangerousPatternResult(false, null);
    }

    private SensitiveInfoResult checkSensitiveInformation(String content) {
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            if (pattern.matcher(content).find()) {
                return new SensitiveInfoResult(true, "检测到敏感信息");
            }
        }
        return new SensitiveInfoResult(false, null);
    }

    private ContentAnalysisResult analyzeContent(String content) {
        boolean isSuspicious = false;
        String reason = null;

        // 检查异常长度
        if (content.length() > 50000) {
            isSuspicious = true;
            reason = "内容长度异常";
        }

        // 检查重复模式（可能是攻击）
        if (content.matches(".*(.)\\1{50,}.*")) {
            isSuspicious = true;
            reason = "检测到异常重复模式";
        }

        // 检查特殊字符密度
        long specialCharCount = content.chars().filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c)).count();
        if (specialCharCount > content.length() * 0.3) {
            isSuspicious = true;
            reason = "特殊字符密度过高";
        }

        return new ContentAnalysisResult(isSuspicious, reason);
    }

    private BehaviorAnalysisResult analyzeBehavior(SecurityContext context) {
        // 这里可以实现更复杂的行为分析
        // 例如：检查请求频率、异常模式等
        return new BehaviorAnalysisResult(false, null);
    }

    // =================== 数据传输对象 ===================

    public record SecurityContext(
            Long userId,
            String userAgent,
            String ipAddress,
            int recentRequestCount,
            List<String> recentMessages
    ) {}

    public record DangerousPatternResult(boolean detected, String reason) {}
    public record SensitiveInfoResult(boolean detected, String reason) {}
    public record ContentAnalysisResult(boolean suspicious, String reason) {}
    public record BehaviorAnalysisResult(boolean suspicious, String reason) {}

    /**
     * 安全检查结果
     */
    public static class SecurityCheckResult {
        @Getter
        private final boolean safe;
        private final DangerousPatternResult dangerousPatterns;
        private final SensitiveInfoResult sensitiveInfo;
        private final ContentAnalysisResult contentAnalysis;
        private final BehaviorAnalysisResult behaviorAnalysis;

        private SecurityCheckResult(Builder builder) {
            this.dangerousPatterns = builder.dangerousPatterns;
            this.sensitiveInfo = builder.sensitiveInfo;
            this.contentAnalysis = builder.contentAnalysis;
            this.behaviorAnalysis = builder.behaviorAnalysis;

            this.safe = !isDangerous() && !hasSensitiveInfo() && !isSuspicious();
        }

        public static SecurityCheckResult safe() {
            return new Builder()
                    .withDangerousPatterns(new DangerousPatternResult(false, null))
                    .withSensitiveInfo(new SensitiveInfoResult(false, null))
                    .withContentAnalysis(new ContentAnalysisResult(false, null))
                    .build();
        }

        public static Builder builder() {
            return new Builder();
        }

        public boolean isDangerous() { return dangerousPatterns != null && dangerousPatterns.detected(); }
        public boolean hasSensitiveInfo() { return sensitiveInfo != null && sensitiveInfo.detected(); }
        public boolean isSuspicious() {
            return (contentAnalysis != null && contentAnalysis.suspicious()) ||
                    (behaviorAnalysis != null && behaviorAnalysis.suspicious());
        }

        public static class Builder {
            private DangerousPatternResult dangerousPatterns;
            private SensitiveInfoResult sensitiveInfo;
            private ContentAnalysisResult contentAnalysis;
            private BehaviorAnalysisResult behaviorAnalysis;

            public Builder withDangerousPatterns(DangerousPatternResult result) {
                this.dangerousPatterns = result;
                return this;
            }

            public Builder withSensitiveInfo(SensitiveInfoResult result) {
                this.sensitiveInfo = result;
                return this;
            }

            public Builder withContentAnalysis(ContentAnalysisResult result) {
                this.contentAnalysis = result;
                return this;
            }

            public Builder withBehaviorAnalysis(BehaviorAnalysisResult result) {
                this.behaviorAnalysis = result;
                return this;
            }

            public SecurityCheckResult build() {
                return new SecurityCheckResult(this);
            }
        }
    }
}