package com.clinflash.baseai.infrastructure.persistence.chat.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * <h2>每日使用量复合主键</h2>
 */
@Data
@NoArgsConstructor
public class ChatUsageDailyEntityId implements Serializable {
    private LocalDate statDate;
    private Long tenantId;
    private String modelCode;

    public ChatUsageDailyEntityId(LocalDate statDate, Long tenantId, String modelCode) {
        this.statDate = statDate;
        this.tenantId = tenantId;
        this.modelCode = modelCode;
    }
}