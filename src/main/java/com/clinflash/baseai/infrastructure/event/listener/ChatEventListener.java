package com.clinflash.baseai.infrastructure.event.listener;

import com.clinflash.baseai.domain.chat.event.AIResponseGeneratedEvent;
import com.clinflash.baseai.domain.chat.event.ChatThreadCreatedEvent;
import com.clinflash.baseai.domain.chat.event.MessageSentEvent;
import com.clinflash.baseai.domain.chat.event.UserFeedbackSubmittedEvent;
import com.clinflash.baseai.infrastructure.monitoring.ChatMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * <h2>对话事件监听器</h2>
 *
 * <p>这个监听器展示了如何响应对话事件。在实际项目中，你可能会有多个监听器，
 * 分别处理不同的业务逻辑，如数据统计、用户通知、内容审核等。</p>
 */
@Component
public class ChatEventListener {

    private static final Logger log = LoggerFactory.getLogger(ChatEventListener.class);

    private final ChatMetricsCollector metricsCollector;

    public ChatEventListener(ChatMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }

    /**
     * 处理对话线程创建事件
     */
    @EventListener
    @Async("chatAsyncExecutor")
    public void handleThreadCreated(ChatThreadCreatedEvent event) {
        log.info("处理对话线程创建事件: threadId={}, tenantId={}",
                event.getThreadId(), event.getTenantId());

        // 记录指标
        metricsCollector.recordOperation("thread.create", 0, true);

        // 这里可以添加其他业务逻辑：
        // 1. 发送欢迎消息
        // 2. 初始化用户偏好设置
        // 3. 记录用户活动日志
    }

    /**
     * 处理消息发送事件
     */
    @EventListener
    @Async("chatAsyncExecutor")
    public void handleMessageSent(MessageSentEvent event) {
        log.debug("处理消息发送事件: messageId={}, role={}",
                event.getMessageId(), event.getRole());

        if ("USER".equals(event.getRole())) {
            // 用户消息的处理逻辑
            metricsCollector.recordMessageSent("user", event.getTokenCount() != null ? event.getTokenCount() : 0);

            // 可以添加的功能：
            // 1. 内容安全检查
            // 2. 用户行为分析
            // 3. 实时推荐
        }
    }

    /**
     * 处理AI响应生成事件
     */
    @EventListener
    @Async("chatAsyncExecutor")
    public void handleAIResponseGenerated(AIResponseGeneratedEvent event) {
        log.debug("处理AI响应事件: messageId={}, model={}, latency={}ms",
                event.getMessageId(), event.getModelCode(), event.getLatencyMs());

        // 记录详细指标
        metricsCollector.recordMessageReceived(
                event.getModelCode(),
                (event.getInputTokens() != null ? event.getInputTokens() : 0) +
                        (event.getOutputTokens() != null ? event.getOutputTokens() : 0),
                event.getLatencyMs() != null ? event.getLatencyMs() : 0
        );

        if (event.getCost() != null) {
            metricsCollector.recordCost(event.getModelCode(), event.getCost());
        }

        // 可以添加的功能：
        // 1. 质量评估
        // 2. 成本分析
        // 3. 性能优化建议
    }

    /**
     * 处理用户反馈事件
     */
    @EventListener
    @Async("chatAsyncExecutor")
    public void handleUserFeedback(UserFeedbackSubmittedEvent event) {
        log.info("处理用户反馈事件: messageId={}, rating={}",
                event.getMessageId(), event.getRating());

        metricsCollector.recordFeedback(event.getMessageId(), event.getRating(), event.getComment());

        // 可以添加的功能：
        // 1. 触发模型调优
        // 2. 内容质量分析
        // 3. 用户满意度统计
    }
}