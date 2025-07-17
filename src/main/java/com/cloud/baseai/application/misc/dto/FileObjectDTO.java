package com.cloud.baseai.application.misc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * <h2>文件对象DTO</h2>
 */
public record FileObjectDTO(
        @JsonProperty("id")
        Long id,

        @JsonProperty("bucket")
        String bucket,

        @JsonProperty("objectKey")
        String objectKey,

        @JsonProperty("originalName")
        String originalName,

        @JsonProperty("sizeBytes")
        Long sizeBytes,

        @JsonProperty("formattedSize")
        String formattedSize,

        @JsonProperty("contentType")
        String contentType,

        @JsonProperty("downloadUrl")
        String downloadUrl,

        @JsonProperty("createdAt")
        OffsetDateTime createdAt
) {
}