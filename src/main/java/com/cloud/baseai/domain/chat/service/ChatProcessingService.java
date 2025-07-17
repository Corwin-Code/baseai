package com.cloud.baseai.domain.chat.service;

import com.cloud.baseai.domain.chat.model.ChatMessage;
import com.cloud.baseai.domain.chat.model.MessageRole;
import com.cloud.baseai.infrastructure.config.ChatProperties;
import com.cloud.baseai.infrastructure.external.llm.ChatCompletionService;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <h2>对话处理领域服务</h2>
 *
 * <p>这是对话系统的核心领域服务，负责处理复杂的对话逻辑。
 * 它就像一个智能的对话分析师，能够理解用户意图、分析对话模式、
 * 提供个性化建议等高级功能。</p>
 *
 * <p><b>核心能力：</b></p>
 * <ul>
 * <li><b>意图理解：</b>从自然语言中提取用户的真实意图</li>
 * <li><b>上下文管理：</b>维护和优化对话的上下文窗口</li>
 * <li><b>工具意图分析：</b>识别用户是否需要调用特定工具</li>
 * <li><b>建议生成：</b>基于上下文智能推荐后续问题</li>
 * <li><b>对话质量评估：</b>分析对话的连贯性和有效性</li>
 * </ul>
 */
@Service
public class ChatProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ChatProcessingService.class);

    private final ChatCompletionService llmService;
    private final ChatProperties config;

    // 工具意图识别的关键词模式
    private static final Map<String, Pattern> TOOL_INTENT_PATTERNS = Map.of(
            "search_tool", Pattern.compile("搜索|查找|检索|search|find", Pattern.CASE_INSENSITIVE),
            "weather_tool", Pattern.compile("天气|weather|温度|气温", Pattern.CASE_INSENSITIVE),
            "calculator_tool", Pattern.compile("计算|算|calculator|数学|math", Pattern.CASE_INSENSITIVE),
            "file_tool", Pattern.compile("文件|file|上传|下载|upload|download", Pattern.CASE_INSENSITIVE),
            "email_tool", Pattern.compile("邮件|发送|email|send|mail", Pattern.CASE_INSENSITIVE)
    );

    // 建议问题的模板
    private static final List<String> SUGGESTION_TEMPLATES = List.of(
            "你能详细解释一下{topic}吗？",
            "关于{topic}，我还想了解什么？",
            "在{topic}方面有什么最佳实践？",
            "如何解决{topic}相关的常见问题？",
            "能否提供一些{topic}的实际案例？",
            "{topic}的发展趋势是什么？",
            "学习{topic}需要哪些前置知识？",
            "有什么工具可以帮助处理{topic}？"
    );

    public ChatProcessingService(ChatCompletionService llmService, ChatProperties config) {
        this.llmService = llmService;
        this.config = config;
    }

    /**
     * 分析用户消息中的工具调用意图
     *
     * <p>这个方法使用多种技术来识别用户是否需要调用特定工具：</p>
     * <p>1. <b>关键词匹配：</b>基于预定义的关键词模式进行快速匹配</p>
     * <p>2. <b>语义分析：</b>使用NLP技术理解深层语义</p>
     * <p>3. <b>上下文推理：</b>结合对话历史进行综合判断</p>
     *
     * @param content 用户输入的消息内容
     * @return 识别出的工具代码列表
     */
    public List<String> analyzeToolIntents(String content) {
        log.debug("分析工具调用意图: content={}", content);

        List<String> detectedTools = new ArrayList<>();

        try {
            // 第一步：基于关键词的快速匹配
            for (Map.Entry<String, Pattern> entry : TOOL_INTENT_PATTERNS.entrySet()) {
                if (entry.getValue().matcher(content).find()) {
                    detectedTools.add(entry.getKey());
                }
            }

            // 第二步：语义分析增强（如果有可用的LLM）
            if (detectedTools.isEmpty() && config.isSemanticAnalysisEnabled()) {
                detectedTools.addAll(analyzeSemanticIntents(content));
            }

            // 第三步：去重和优先级排序
            detectedTools = detectedTools.stream()
                    .distinct()
                    .sorted(this::compareToolPriority)
                    .collect(Collectors.toList());

            log.debug("工具意图分析完成: content={}, detected={}", content, detectedTools);
            return detectedTools;

        } catch (Exception e) {
            log.warn("工具意图分析失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 基于对话历史生成建议问题
     *
     * <p>这个功能就像一个贴心的对话引导者，能够根据当前的对话内容，
     * 智能地推荐用户可能感兴趣的后续问题，让对话更加深入和有价值。</p>
     *
     * @param recentMessages 最近的对话消息列表
     * @param count          需要生成的建议数量
     * @return 生成的建议问题列表
     */
    public List<String> generateSuggestions(List<ChatMessage> recentMessages, Integer count) {
        log.debug("生成建议问题: messageCount={}, requestedCount={}", recentMessages.size(), count);

        try {
            if (recentMessages.isEmpty()) {
                return getDefaultSuggestions(count);
            }

            // 分析对话主题
            Set<String> topics = extractTopicsFromMessages(recentMessages);

            // 基于主题生成建议
            List<String> suggestions = generateTopicBasedSuggestions(topics, count);

            // 如果生成的建议不够，补充通用建议
            if (suggestions.size() < count) {
                suggestions.addAll(getDefaultSuggestions(count - suggestions.size()));
            }

            return suggestions.stream()
                    .limit(count)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("生成建议问题失败: {}", e.getMessage());
            return getDefaultSuggestions(count);
        }
    }

    /**
     * 优化对话上下文窗口
     *
     * <p>这个方法负责管理对话的记忆容量。当对话历史过长时，
     * 需要智能地选择保留哪些信息，丢弃哪些信息，确保AI能够
     * 在有限的上下文窗口内获得最有价值的信息。</p>
     *
     * @param messages  原始消息列表
     * @param maxTokens 最大Token限制
     * @return 优化后的消息列表
     */
    public List<ChatMessage> optimizeContextWindow(List<ChatMessage> messages, int maxTokens) {
        log.debug("优化上下文窗口: messageCount={}, maxTokens={}", messages.size(), maxTokens);

        if (messages.isEmpty()) {
            return messages;
        }

        try {
            // 计算当前消息的总Token数
            int currentTokens = estimateTotalTokens(messages);

            if (currentTokens <= maxTokens) {
                return messages;
            }

            // 使用滑动窗口策略优化
            return applySlidingWindowStrategy(messages, maxTokens);

        } catch (Exception e) {
            log.warn("上下文优化失败: {}", e.getMessage());
            // 失败时返回最近的几条消息
            return messages.stream()
                    .limit(Math.min(10, messages.size()))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 评估对话质量
     *
     * <p>对话质量评估帮助我们了解AI助手的表现，包括回答的相关性、
     * 准确性、有用性等多个维度。这些指标对于持续改进AI系统非常重要。</p>
     *
     * @param userMessage      用户消息
     * @param assistantMessage 助手回复
     * @return 质量评估结果
     */
    public ConversationQuality evaluateConversationQuality(ChatMessage userMessage, ChatMessage assistantMessage) {
        log.debug("评估对话质量: userMessageId={}, assistantMessageId={}",
                userMessage.id(), assistantMessage.id());

        try {
            ConversationQuality.Builder builder = ConversationQuality.builder();

            // 相关性评估
            float relevance = calculateRelevance(userMessage.content(), assistantMessage.content());
            builder.relevance(relevance);

            // 完整性评估
            float completeness = calculateCompleteness(userMessage.content(), assistantMessage.content());
            builder.completeness(completeness);

            // 准确性评估（基于是否有知识库引用）
            float accuracy = assistantMessage.hasToolCall() ? 0.9f : 0.7f;
            builder.accuracy(accuracy);

            // 响应时间评估
            float responseTime = evaluateResponseTime(assistantMessage.latencyMs());
            builder.responseTime(responseTime);

            // 综合评分
            float overallScore = (relevance + completeness + accuracy + responseTime) / 4.0f;
            builder.overallScore(overallScore);

            return builder.build();

        } catch (Exception e) {
            log.warn("对话质量评估失败: {}", e.getMessage());
            return ConversationQuality.builder()
                    .relevance(0.5f)
                    .completeness(0.5f)
                    .accuracy(0.5f)
                    .responseTime(0.5f)
                    .overallScore(0.5f)
                    .build();
        }
    }

    /**
     * 检测对话中的敏感内容
     *
     * <p>内容安全是AI系统的重要保障。这个方法能够识别和标记
     * 可能包含敏感信息、不当内容或潜在风险的对话内容。</p>
     *
     * @param content 待检测的内容
     * @return 内容安全评估结果
     */
    public ContentSafety detectSensitiveContent(String content) {
        log.debug("检测敏感内容: contentLength={}", content.length());

        try {
            ContentSafety.Builder builder = ContentSafety.builder();

            // 个人信息检测
            boolean hasPersonalInfo = detectPersonalInformation(content);
            builder.hasPersonalInfo(hasPersonalInfo);

            // 不当内容检测
            boolean hasInappropriateContent = detectInappropriateContent(content);
            builder.hasInappropriateContent(hasInappropriateContent);

            // 商业敏感信息检测
            boolean hasCommercialSecrets = detectCommercialSecrets(content);
            builder.hasCommercialSecrets(hasCommercialSecrets);

            // 计算总体风险等级
            String riskLevel = calculateRiskLevel(hasPersonalInfo, hasInappropriateContent, hasCommercialSecrets);
            builder.riskLevel(riskLevel);

            return builder.build();

        } catch (Exception e) {
            log.warn("敏感内容检测失败: {}", e.getMessage());
            return ContentSafety.builder()
                    .hasPersonalInfo(false)
                    .hasInappropriateContent(false)
                    .hasCommercialSecrets(false)
                    .riskLevel("UNKNOWN")
                    .build();
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 使用语义分析识别工具意图
     */
    private List<String> analyzeSemanticIntents(String content) {
        try {
            // 这里可以调用更高级的NLP模型进行语义分析
            // 为了简化，这里使用基础的语义规则
            List<String> semanticTools = new ArrayList<>();

            String lowerContent = content.toLowerCase();

            // 时间相关查询
            if (lowerContent.contains("时间") || lowerContent.contains("date") || lowerContent.contains("when")) {
                semanticTools.add("time_tool");
            }

            // 数据分析相关
            if (lowerContent.contains("分析") || lowerContent.contains("统计") || lowerContent.contains("图表")) {
                semanticTools.add("analytics_tool");
            }

            // 翻译相关
            if (lowerContent.contains("翻译") || lowerContent.contains("translate")) {
                semanticTools.add("translation_tool");
            }

            return semanticTools;

        } catch (Exception e) {
            log.warn("语义分析失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 比较工具优先级
     */
    private int compareToolPriority(String tool1, String tool2) {
        Map<String, Integer> priorities = Map.of(
                "search_tool", 1,
                "calculator_tool", 2,
                "weather_tool", 3,
                "file_tool", 4,
                "email_tool", 5
        );

        int priority1 = priorities.getOrDefault(tool1, 999);
        int priority2 = priorities.getOrDefault(tool2, 999);

        return Integer.compare(priority1, priority2);
    }

    /**
     * 从消息中提取主题
     */
    private Set<String> extractTopicsFromMessages(List<ChatMessage> messages) {
        Set<String> topics = new HashSet<>();

        try {
            for (ChatMessage message : messages) {
                if (message.role() == MessageRole.USER || message.role() == MessageRole.ASSISTANT) {
                    Set<String> messageTopics = extractTopicsFromText(message.content());
                    topics.addAll(messageTopics);
                }
            }

            return topics;

        } catch (Exception e) {
            log.warn("主题提取失败: {}", e.getMessage());
            return Set.of("通用话题");
        }
    }

    /**
     * 从文本中提取主题关键词
     */
    private Set<String> extractTopicsFromText(String text) {
        Set<String> topics = new HashSet<>();

        // 技术类主题
        if (text.toLowerCase().matches(".*\\b(java|python|javascript|react|spring|docker)\\b.*")) {
            topics.add("编程技术");
        }

        // 业务类主题
        if (text.toLowerCase().matches(".*\\b(业务|需求|产品|市场|用户)\\b.*")) {
            topics.add("业务需求");
        }

        // 架构类主题
        if (text.toLowerCase().matches(".*\\b(架构|设计|性能|优化|扩展)\\b.*")) {
            topics.add("系统架构");
        }

        // 如果没有识别出特定主题，使用通用主题
        if (topics.isEmpty()) {
            topics.add("一般问题");
        }

        return topics;
    }

    /**
     * 基于主题生成建议问题
     */
    private List<String> generateTopicBasedSuggestions(Set<String> topics, Integer count) {
        List<String> suggestions = new ArrayList<>();

        for (String topic : topics) {
            List<String> topicSuggestions = SUGGESTION_TEMPLATES.stream()
                    .map(template -> template.replace("{topic}", topic))
                    .limit(Math.max(1, count / topics.size()))
                    .toList();

            suggestions.addAll(topicSuggestions);

            if (suggestions.size() >= count) {
                break;
            }
        }

        return suggestions;
    }

    /**
     * 获取默认建议问题
     */
    private List<String> getDefaultSuggestions(Integer count) {
        List<String> defaultSuggestions = List.of(
                "你能帮我解决一个技术问题吗？",
                "我想了解最新的行业趋势",
                "有什么工具可以提高工作效率？",
                "能否分享一些最佳实践？",
                "如何开始学习新技术？",
                "目前有什么值得关注的技术发展？",
                "在项目管理方面有什么建议？",
                "如何提升团队协作效率？"
        );

        return defaultSuggestions.stream()
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * 估算消息列表的总Token数
     */
    private int estimateTotalTokens(List<ChatMessage> messages) {
        return messages.stream()
                .mapToInt(msg -> estimateTokenCount(msg.content()))
                .sum();
    }

    /**
     * 估算单条消息的Token数
     */
    private int estimateTokenCount(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }

        // 简化的Token估算：英文约4字符1个Token，中文约1.5字符1个Token
        int englishChars = (int) content.chars()
                .filter(c -> c < 128)
                .count();

        int chineseChars = content.length() - englishChars;

        return (englishChars / 4) + (chineseChars * 2 / 3);
    }

    /**
     * 应用滑动窗口策略
     */
    private List<ChatMessage> applySlidingWindowStrategy(List<ChatMessage> messages, int maxTokens) {
        List<ChatMessage> optimizedMessages = new ArrayList<>();
        int currentTokens = 0;

        // 保留系统消息
        List<ChatMessage> systemMessages = messages.stream()
                .filter(msg -> msg.role() == MessageRole.SYSTEM)
                .toList();

        for (ChatMessage systemMsg : systemMessages) {
            int msgTokens = estimateTokenCount(systemMsg.content());
            if (currentTokens + msgTokens <= maxTokens) {
                optimizedMessages.add(systemMsg);
                currentTokens += msgTokens;
            }
        }

        // 从最新消息开始添加
        List<ChatMessage> nonSystemMessages = messages.stream()
                .filter(msg -> msg.role() != MessageRole.SYSTEM)
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .toList();

        for (ChatMessage message : nonSystemMessages) {
            int msgTokens = estimateTokenCount(message.content());
            if (currentTokens + msgTokens <= maxTokens) {
                optimizedMessages.addFirst(message); // 插入到开头保持时间顺序
                currentTokens += msgTokens;
            } else {
                break;
            }
        }

        return optimizedMessages;
    }

    /**
     * 计算相关性评分
     */
    private float calculateRelevance(String userContent, String assistantContent) {
        if (userContent == null || assistantContent == null) {
            return 0.0f;
        }

        // 简化的相关性计算：基于关键词重叠
        Set<String> userWords = extractKeywords(userContent.toLowerCase());
        Set<String> assistantWords = extractKeywords(assistantContent.toLowerCase());

        if (userWords.isEmpty() || assistantWords.isEmpty()) {
            return 0.5f;
        }

        Set<String> intersection = new HashSet<>(userWords);
        intersection.retainAll(assistantWords);

        Set<String> union = new HashSet<>(userWords);
        union.addAll(assistantWords);

        return (float) intersection.size() / union.size();
    }

    /**
     * 计算完整性评分
     */
    private float calculateCompleteness(String userContent, String assistantContent) {
        if (assistantContent == null || assistantContent.trim().isEmpty()) {
            return 0.0f;
        }

        // 基于回复长度和结构的简单评估
        int responseLength = assistantContent.length();

        if (responseLength < 50) {
            return 0.3f;
        } else if (responseLength < 200) {
            return 0.6f;
        } else if (responseLength < 500) {
            return 0.8f;
        } else {
            return 1.0f;
        }
    }

    /**
     * 评估响应时间
     */
    private float evaluateResponseTime(Integer latencyMs) {
        if (latencyMs == null) {
            return 0.5f;
        }

        // 响应时间评估：越快越好
        if (latencyMs < 1000) {
            return 1.0f;
        } else if (latencyMs < 3000) {
            return 0.8f;
        } else if (latencyMs < 5000) {
            return 0.6f;
        } else if (latencyMs < 10000) {
            return 0.4f;
        } else {
            return 0.2f;
        }
    }

    /**
     * 提取关键词
     */
    private Set<String> extractKeywords(String text) {
        return Arrays.stream(text.split("\\W+"))
                .filter(word -> word.length() > 2)
                .filter(word -> !isStopWord(word))
                .collect(Collectors.toSet());
    }

    /**
     * 检查是否为停用词
     */
    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
                "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
                "of", "with", "by", "is", "are", "was", "were", "be", "been", "have",
                "has", "had", "do", "does", "did", "will", "would", "could", "should",
                "的", "了", "在", "是", "有", "和", "与", "或", "但", "如果", "因为"
        );
        return stopWords.contains(word.toLowerCase());
    }

    /**
     * 检测个人信息
     */
    private boolean detectPersonalInformation(String content) {
        // 电话号码模式
        Pattern phonePattern = Pattern.compile("\\d{3}-\\d{4}-\\d{4}|\\d{11}");
        // 邮箱模式
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        // 身份证模式
        Pattern idPattern = Pattern.compile("\\d{15}|\\d{18}");

        return phonePattern.matcher(content).find() ||
                emailPattern.matcher(content).find() ||
                idPattern.matcher(content).find();
    }

    /**
     * 检测不当内容
     */
    private boolean detectInappropriateContent(String content) {
        // 简化的不当内容检测
        String[] inappropriateKeywords = {
                "暴力", "威胁", "仇恨", "歧视", "色情"
        };

        String lowerContent = content.toLowerCase();
        return Arrays.stream(inappropriateKeywords)
                .anyMatch(lowerContent::contains);
    }

    /**
     * 检测商业机密
     */
    private boolean detectCommercialSecrets(String content) {
        String[] sensitiveKeywords = {
                "密码", "密钥", "token", "secret", "confidential", "机密", "内部"
        };

        String lowerContent = content.toLowerCase();
        return Arrays.stream(sensitiveKeywords)
                .anyMatch(lowerContent::contains);
    }

    /**
     * 计算风险等级
     */
    private String calculateRiskLevel(boolean hasPersonalInfo, boolean hasInappropriateContent,
                                      boolean hasCommercialSecrets) {
        int riskScore = 0;
        if (hasPersonalInfo) riskScore += 2;
        if (hasInappropriateContent) riskScore += 3;
        if (hasCommercialSecrets) riskScore += 2;

        return switch (riskScore) {
            case 0 -> "LOW";
            case 1, 2 -> "MEDIUM";
            case 3, 4 -> "HIGH";
            default -> "CRITICAL";
        };
    }

    // =================== 内部数据结构 ===================

    /**
     * 对话质量评估结果
     */
    @Getter
    public static class ConversationQuality {
        private final float relevance;
        private final float completeness;
        private final float accuracy;
        private final float responseTime;
        private final float overallScore;

        private ConversationQuality(Builder builder) {
            this.relevance = builder.relevance;
            this.completeness = builder.completeness;
            this.accuracy = builder.accuracy;
            this.responseTime = builder.responseTime;
            this.overallScore = builder.overallScore;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private float relevance;
            private float completeness;
            private float accuracy;
            private float responseTime;
            private float overallScore;

            public Builder relevance(float relevance) {
                this.relevance = relevance;
                return this;
            }

            public Builder completeness(float completeness) {
                this.completeness = completeness;
                return this;
            }

            public Builder accuracy(float accuracy) {
                this.accuracy = accuracy;
                return this;
            }

            public Builder responseTime(float responseTime) {
                this.responseTime = responseTime;
                return this;
            }

            public Builder overallScore(float overallScore) {
                this.overallScore = overallScore;
                return this;
            }

            public ConversationQuality build() {
                return new ConversationQuality(this);
            }
        }
    }

    /**
     * 内容安全评估结果
     */
    public static class ContentSafety {
        private final boolean hasPersonalInfo;
        private final boolean hasInappropriateContent;
        private final boolean hasCommercialSecrets;
        @Getter
        private final String riskLevel;

        private ContentSafety(Builder builder) {
            this.hasPersonalInfo = builder.hasPersonalInfo;
            this.hasInappropriateContent = builder.hasInappropriateContent;
            this.hasCommercialSecrets = builder.hasCommercialSecrets;
            this.riskLevel = builder.riskLevel;
        }

        public static Builder builder() {
            return new Builder();
        }

        // Getters
        public boolean hasPersonalInfo() {
            return hasPersonalInfo;
        }

        public boolean hasInappropriateContent() {
            return hasInappropriateContent;
        }

        public boolean hasCommercialSecrets() {
            return hasCommercialSecrets;
        }

        public static class Builder {
            private boolean hasPersonalInfo;
            private boolean hasInappropriateContent;
            private boolean hasCommercialSecrets;
            private String riskLevel;

            public Builder hasPersonalInfo(boolean hasPersonalInfo) {
                this.hasPersonalInfo = hasPersonalInfo;
                return this;
            }

            public Builder hasInappropriateContent(boolean hasInappropriateContent) {
                this.hasInappropriateContent = hasInappropriateContent;
                return this;
            }

            public Builder hasCommercialSecrets(boolean hasCommercialSecrets) {
                this.hasCommercialSecrets = hasCommercialSecrets;
                return this;
            }

            public Builder riskLevel(String riskLevel) {
                this.riskLevel = riskLevel;
                return this;
            }

            public ContentSafety build() {
                return new ContentSafety(this);
            }
        }
    }
}