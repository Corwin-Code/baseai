package com.cloud.baseai.domain.chat.event;

import lombok.Getter;

import java.util.Map;

/**
 * AI响应生成事件
 */
public class AIResponseGeneratedEvent extends ChatDomainEvent {
    @Getter
    private final Long messageId;
    @Getter
    private final Long threadId;
    @Getter
    private final String modelCode;
    @Getter
    private final Integer inputTokens;
    @Getter
    private final Integer outputTokens;
    @Getter
    private final Integer latencyMs;
    @Getter
    private final Double cost;
    private final boolean hasKnowledgeRetrieval;
    private final boolean hasToolCalling;

    public AIResponseGeneratedEvent(Long tenantId, Long userId, Long messageId, Long threadId,
                                    String modelCode, Integer inputTokens, Integer outputTokens,
                                    Integer latencyMs, Double cost, boolean hasKnowledgeRetrieval,
                                    boolean hasToolCalling) {
        super("AI_RESPONSE_GENERATED", tenantId, userId, Map.of(
                "messageId", messageId,
                "threadId", threadId,
                "modelCode", modelCode,
                "inputTokens", inputTokens != null ? inputTokens : 0,
                "outputTokens", outputTokens != null ? outputTokens : 0,
                "latencyMs", latencyMs != null ? latencyMs : 0,
                "cost", cost != null ? cost : 0.0,
                "hasKnowledgeRetrieval", hasKnowledgeRetrieval,
                "hasToolCalling", hasToolCalling
        ));
        this.messageId = messageId;
        this.threadId = threadId;
        this.modelCode = modelCode;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.latencyMs = latencyMs;
        this.cost = cost;
        this.hasKnowledgeRetrieval = hasKnowledgeRetrieval;
        this.hasToolCalling = hasToolCalling;
    }

    public boolean hasKnowledgeRetrieval() {
        return hasKnowledgeRetrieval;
    }

    public boolean hasToolCalling() {
        return hasToolCalling;
    }
}