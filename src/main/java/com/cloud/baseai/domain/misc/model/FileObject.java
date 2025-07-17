package com.cloud.baseai.domain.misc.model;

import java.time.OffsetDateTime;

/**
 * <h2>文件对象元数据领域模型</h2>
 *
 * <p>FileObject是对象存储系统的元数据抽象。
 * 它记录了文件在存储系统中的位置、大小、校验和等关键信息，
 * 为文件的管理、检索和完整性验证提供支持。</p>
 *
 * <p><b>设计考虑：</b></p>
 * <p>通过SHA256哈希实现去重，避免存储相同内容的文件。
 * 支持多种存储桶的组织方式，为不同类型的文件提供逻辑分区。</p>
 *
 * @param id        文件对象唯一标识
 * @param bucket    存储桶名称，用于逻辑分组
 * @param objectKey 对象唯一键，在桶内唯一
 * @param sizeBytes 文件大小（字节）
 * @param sha256    文件内容的SHA256哈希值
 * @param createdAt 文件上传时间
 * @param deletedAt 软删除时间
 */
public record FileObject(
        Long id,
        String bucket,
        String objectKey,
        Long sizeBytes,
        String sha256,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {

    /**
     * 创建新的文件对象记录
     *
     * <p>创建时会自动计算文件的哈希值，用于后续的去重和完整性验证。
     * 对象键的生成策略通常基于时间戳、用户ID等信息，确保全局唯一性。</p>
     */
    public static FileObject create(String bucket, String objectKey,
                                    Long sizeBytes, String sha256) {
        return new FileObject(
                null, // ID由数据库生成
                bucket,
                objectKey,
                sizeBytes,
                sha256,
                OffsetDateTime.now(),
                null
        );
    }

    /**
     * 软删除文件对象
     *
     * <p>软删除只是标记删除时间，实际的存储清理通常由后台任务异步处理。
     * 这样可以支持文件的恢复操作，也便于审计和问题排查。</p>
     */
    public FileObject markAsDeleted() {
        return new FileObject(
                this.id,
                this.bucket,
                this.objectKey,
                this.sizeBytes,
                this.sha256,
                this.createdAt,
                OffsetDateTime.now()
        );
    }

    /**
     * 检查文件是否存在（未被删除）
     */
    public boolean exists() {
        return this.deletedAt == null;
    }

    /**
     * 获取文件的存储路径
     *
     * <p>组合桶名和对象键，生成完整的存储路径。
     * 这个路径可以直接用于存储系统的访问。</p>
     */
    public String getStoragePath() {
        return this.bucket + "/" + this.objectKey;
    }

    /**
     * 验证文件大小是否合理
     */
    public boolean hasValidSize() {
        return this.sizeBytes != null &&
                this.sizeBytes > 0 &&
                this.sizeBytes <= 100L * 1024 * 1024 * 1024; // 假设最大100GB
    }

    /**
     * 检查是否为大文件（超过指定阈值）
     */
    public boolean isLargeFile(long thresholdBytes) {
        return this.sizeBytes != null && this.sizeBytes > thresholdBytes;
    }

    /**
     * 格式化文件大小为人类可读格式
     */
    public String getFormattedSize() {
        if (this.sizeBytes == null) return "未知";

        if (this.sizeBytes < 1024) {
            return this.sizeBytes + " B";
        } else if (this.sizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", this.sizeBytes / 1024.0);
        } else if (this.sizeBytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", this.sizeBytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", this.sizeBytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 验证SHA256哈希格式
     */
    public boolean hasValidSha256() {
        return this.sha256 != null &&
                this.sha256.matches("^[a-fA-F0-9]{64}$");
    }
}