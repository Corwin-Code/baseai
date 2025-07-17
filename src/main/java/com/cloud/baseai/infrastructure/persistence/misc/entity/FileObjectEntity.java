package com.cloud.baseai.infrastructure.persistence.misc.entity;

import com.cloud.baseai.domain.misc.model.FileObject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * <h2>文件对象JPA实体</h2>
 */
@Setter
@Getter
@Entity
@Table(name = "file_objects")
public class FileObjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "bucket", nullable = false, length = 64)
    private String bucket;

    @Column(name = "object_key", nullable = false, length = 256)
    private String objectKey;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "sha256", length = 64, unique = true)
    private String sha256;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // 构造函数
    public FileObjectEntity() {
    }

    // =================== 领域对象转换 ===================

    /**
     * 从领域对象创建实体
     */
    public static FileObjectEntity fromDomain(FileObject object) {
        if (object == null) {
            return null;
        }

        FileObjectEntity entity = new FileObjectEntity();
        entity.id = object.id();
        entity.bucket = object.bucket();
        entity.objectKey = object.objectKey();
        entity.sizeBytes = object.sizeBytes();
        entity.sha256 = object.sha256();
        entity.createdAt = object.createdAt();
        entity.deletedAt = object.deletedAt();

        return entity;
    }

    /**
     * 转换为领域对象
     */
    public FileObject toDomain() {
        return new FileObject(
                id,
                bucket,
                objectKey,
                sizeBytes,
                sha256,
                createdAt,
                deletedAt
        );
    }

    /**
     * 从领域对象更新实体状态
     */
    public void updateFromDomain(FileObject object) {
        if (object == null) {
            return;
        }

        // ID和创建时间不可更改
        this.bucket = object.bucket();
        this.objectKey = object.objectKey();
        this.sizeBytes = object.sizeBytes();
        this.sha256 = object.sha256();
        this.deletedAt = object.deletedAt();
    }
}