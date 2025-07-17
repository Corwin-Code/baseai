package com.cloud.baseai.infrastructure.event.listener;

import com.cloud.baseai.domain.audit.service.AuditService;
import com.cloud.baseai.domain.chat.event.AIResponseGeneratedEvent;
import com.cloud.baseai.domain.chat.event.ChatThreadCreatedEvent;
import com.cloud.baseai.domain.chat.event.MessageSentEvent;
import com.cloud.baseai.domain.chat.event.UserFeedbackSubmittedEvent;
import com.cloud.baseai.infrastructure.monitoring.ChatMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * <h2>对话事件监听器</h2>
 *
 * <p>这个监听器展示了如何响应对话事件。在实际项目中，你可能会有多个监听器，
 * 分别处理不同的业务逻辑，如数据统计、用户通知、内容审核等。</p>
 */
@Component
public class ChatEventListener {

    private static final Logger log = LoggerFactory.getLogger(ChatEventListener.class);

    private final AuditService auditService;
    private final ChatMetricsCollector metricsCollector;

    public ChatEventListener(AuditService auditService, ChatMetricsCollector metricsCollector) {
        this.auditService = auditService;
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

        try {
            Map<String, Object> context = new HashMap<>();
            context.put("threadId", event.getThreadId());
            context.put("threadTitle", event.getTitle());
            context.put("modelName", event.getModelCode());

//            auditService.recordUserAction(
//                    event.getUserId(),
//                    "CHAT_THREAD_CREATED",
//                    "CHAT_THREAD",
//                    event.getThreadId(),
//                    "创建了聊天会话: " + event.getTitle(),
//                    event.getIpAddress(),
//                    event.getUserAgent(),
//                    context
//            );

            log.debug("记录聊天会话创建事件: threadId={}, userId={}",
                    event.getThreadId(), event.getUserId());

        } catch (Exception e) {
            log.error("记录聊天会话创建事件失败", e);
        }
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

            try {
                Map<String, Object> context = new HashMap<>();
                context.put("messageId", event.getMessageId());
                context.put("threadId", event.getThreadId());
//                context.put("messageLength", event.getMessageLength());
//                context.put("hasAttachments", event.hasAttachments());
//
//                auditService.recordUserAction(
//                        event.getUserId(),
//                        "CHAT_MESSAGE_SENT",
//                        "CHAT_MESSAGE",
//                        event.getMessageId(),
//                        "发送了聊天消息",
//                        event.getIpAddress(),
//                        event.getUserAgent(),
//                        context
//                );

            } catch (Exception e) {
                log.error("记录消息发送事件失败", e);
            }
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