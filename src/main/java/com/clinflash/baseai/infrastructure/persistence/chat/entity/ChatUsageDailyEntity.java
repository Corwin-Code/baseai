package com.clinflash.baseai.infrastructure.persistence.chat.entity;

import com.clinflash.baseai.domain.chat.model.ChatUsageDaily;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <h2>每日使用量JPA实体</h2>
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "chat_usage_daily")
@IdClass(ChatUsageDailyEntityId.class)
public class ChatUsageDailyEntity {

    @Id
    @Column(name = "stat_date")
    private LocalDate statDate;

    @Id
    @Column(name = "tenant_id")
    private Long tenantId;

    @Id
    @Column(name = "model_code", length = 32)
    private String modelCode;

    @Column(name = "prompt_tokens")
    private Long promptTokens;

    @Column(name = "completion_tokens")
    private Long completionTokens;

    @Column(name = "cost_usd", precision = 10, scale = 4)
    private BigDecimal costUsd;

    /**
     * 从领域对象创建实体
     */
    public static ChatUsageDailyEntity fromDomain(ChatUsageDaily domain) {
        if (domain == null) return null;

        ChatUsageDailyEntity entity = new ChatUsageDailyEntity();
        entity.setStatDate(domain.statDate());
        entity.setTenantId(domain.tenantId());
        entity.setModelCode(domain.modelCode());
        entity.setPromptTokens(domain.promptTokens());
        entity.setCompletionTokens(domain.completionTokens());
        entity.setCostUsd(domain.costUsd());
        return entity;
    }

    /**
     * 转换为领域对象
     */
    public ChatUsageDaily toDomain() {
        return new ChatUsageDaily(
                this.statDate,
                this.tenantId,
                this.modelCode,
                this.promptTokens,
                this.completionTokens,
                this.costUsd
        );
    }
}