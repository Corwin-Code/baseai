package com.cloud.baseai.application.misc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * <h2>存储统计DTO</h2>
 */
public record StorageStatisticsDTO(
        @JsonProperty("totalFiles")
        long totalFiles,

        @JsonProperty("totalSizeBytes")
        long totalSizeBytes,

        @JsonProperty("formattedTotalSize")
        String formattedTotalSize,

        @JsonProperty("bucketStats")
        List<BucketStatistics> bucketStats
) {
    public record BucketStatistics(
            @JsonProperty("bucket")
            String bucket,

            @JsonProperty("fileCount")
            long fileCount,

            @JsonProperty("sizeBytes")
            long sizeBytes,

            @JsonProperty("formattedSize")
            String formattedSize
    ) {
    }
}