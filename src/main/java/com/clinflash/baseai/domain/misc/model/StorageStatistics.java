package com.clinflash.baseai.domain.misc.model;

/**
 * 存储统计信息DTO
 */
public record StorageStatistics(
        String bucket,
        long fileCount,
        long totalSizeBytes,
        long deletedFileCount,
        long deletedSizeBytes
) {
}