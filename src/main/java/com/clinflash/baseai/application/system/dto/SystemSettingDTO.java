package com.clinflash.baseai.application.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * <h2>系统设置DTO</h2>
 *
 * <p>系统设置的DTO需要特别处理敏感信息的展示。我们不希望在API响应中
 * 暴露密码、密钥等敏感配置，因此需要智能的掩码处理。</p>
 */
public record SystemSettingDTO(
        @JsonProperty("key")
        String key,

        @JsonProperty("value")
        String value,

        @JsonProperty("valueType")
        String valueType,

        @JsonProperty("remark")
        String remark,

        @JsonProperty("updatedBy")
        Long updatedBy,

        @JsonProperty("updatedAt")
        OffsetDateTime updatedAt,

        @JsonProperty("isSensitive")
        Boolean isSensitive
) {
    /**
     * 创建带掩码的DTO
     */
    public static SystemSettingDTO createMasked(String key, String maskedValue, String valueType,
                                                String remark, Long updatedBy, OffsetDateTime updatedAt,
                                                Boolean isSensitive) {
        return new SystemSettingDTO(key, maskedValue, valueType, remark, updatedBy, updatedAt, isSensitive);
    }
}