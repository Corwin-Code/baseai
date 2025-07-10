package com.clinflash.baseai.domain.misc.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * <h2>FileObject — 对象存储元数据</h2>
 *
 * <p>记录外部对象存储（S3 / MinIO / OSS …）中的文件索引，支持内容去重。</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "file_objects")
public class FileObject {

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 存储桶名称
     */
    @Column(length = 64, nullable = false)
    private String bucket;

    /**
     * 对象唯一 Key（完整路径）
     */
    @Column(name = "object_key", length = 256, nullable = false)
    private String objectKey;

    /**
     * 文件大小（字节）
     */
    @Column(name = "size_bytes")
    private Long sizeBytes;

    /**
     * 内容 SHA-256，用于去重
     */
    @Column(length = 64, unique = true)
    private String sha256;

    /* ---------- 时间戳 ---------- */

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at", columnDefinition = "timestamptz")
    private OffsetDateTime deletedAt;
}
