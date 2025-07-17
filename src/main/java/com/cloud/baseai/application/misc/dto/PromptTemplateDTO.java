package com.cloud.baseai.application.misc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * <h2>提示词模板数据传输对象</h2>
 *
 * <p>DTO的设计原则是"数据的最佳表现形式"。我们不是简单地复制领域模型的结构，
 * 而是根据前端和API消费者的需求，精心设计每个字段的名称、类型和组织方式。</p>
 */
public record PromptTemplateDTO(
        @JsonProperty("id")
        Long id,

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

        @JsonProperty("creatorName")
        String creatorName,

        @JsonProperty("createdAt")
        OffsetDateTime createdAt
) {
}