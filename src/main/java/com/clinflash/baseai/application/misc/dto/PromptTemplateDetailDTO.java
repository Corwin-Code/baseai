package com.clinflash.baseai.application.misc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * <h2>提示词模板详情DTO</h2>
 *
 * <p>详情DTO通常包含比列表DTO更多的信息，这样可以避免在列表接口中
 * 传输过多不必要的数据，提高性能。</p>
 */
public record PromptTemplateDetailDTO(
        @JsonProperty("id")
        Long id,

        @JsonProperty("tenantId")
        Long tenantId,

        @JsonProperty("name")
        String name,

        @JsonProperty("content")
        String content,

        @JsonProperty("modelCode")
        String modelCode,

        @JsonProperty("version")
        Integer version,

        @JsonProperty("isSystem")
        Boolean isSystem,

        @JsonProperty("variables")
        List<String> variables, // 从模板内容中提取的变量列表

        @JsonProperty("estimatedTokens")
        Integer estimatedTokens, // 预估的token数量

        @JsonProperty("usageCount")
        Integer usageCount, // 使用次数统计

        @JsonProperty("creatorName")
        String creatorName,

        @JsonProperty("createdAt")
        OffsetDateTime createdAt
) {
}