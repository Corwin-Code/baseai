package com.clinflash.baseai.infrastructure.repository.misc.spring;

import com.clinflash.baseai.domain.misc.model.StorageStatistics;
import com.clinflash.baseai.infrastructure.persistence.misc.entity.FileObjectEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>文件对象Spring Data JPA仓储</h2>
 */
@Repository
public interface SpringFileObjectRepo extends JpaRepository<FileObjectEntity, Long> {

    /**
     * 按桶和对象键查找
     */
    Optional<FileObjectEntity> findByBucketAndObjectKeyAndDeletedAtIsNull(String bucket, String objectKey);

    /**
     * 按SHA256查找
     */
    Optional<FileObjectEntity> findBySha256AndDeletedAtIsNull(String sha256);

    /**
     * 按桶查找文件
     */
    @Query("SELECT f FROM FileObjectEntity f WHERE f.bucket = :bucket AND f.deletedAt IS NULL " +
            "ORDER BY f.createdAt DESC")
    List<FileObjectEntity> findByBucket(@Param("bucket") String bucket, Pageable pageable);

    /**
     * 统计桶中文件数量
     */
    long countByBucketAndDeletedAtIsNull(String bucket);

    /**
     * 计算桶的总大小
     */
    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM FileObjectEntity f WHERE " +
            "f.bucket = :bucket AND f.deletedAt IS NULL")
    long getTotalSizeByBucket(@Param("bucket") String bucket);

    /**
     * 查找大文件
     */
    @Query("SELECT f FROM FileObjectEntity f WHERE f.sizeBytes > :minSize AND f.deletedAt IS NULL " +
            "ORDER BY f.sizeBytes DESC")
    List<FileObjectEntity> findLargeFiles(@Param("minSize") long minSizeBytes, Pageable pageable);

    /**
     * 查找待清理文件
     */
    @Query("SELECT f FROM FileObjectEntity f WHERE f.deletedAt IS NOT NULL " +
            "AND f.deletedAt < :deletedBefore ORDER BY f.deletedAt ASC")
    List<FileObjectEntity> findFilesForCleanup(@Param("deletedBefore") OffsetDateTime deletedBefore,
                                               Pageable pageable);

    /**
     * 获取存储统计
     */
    @Query("SELECT f.bucket as bucket, " +
            "COUNT(f) as fileCount, " +
            "COALESCE(SUM(f.sizeBytes), 0) as totalSizeBytes, " +
            "COUNT(CASE WHEN f.deletedAt IS NOT NULL THEN 1 END) as deletedFileCount, " +
            "COALESCE(SUM(CASE WHEN f.deletedAt IS NOT NULL THEN f.sizeBytes ELSE 0 END), 0) as deletedSizeBytes " +
            "FROM FileObjectEntity f GROUP BY f.bucket")
    List<StorageStatistics> getStorageStatisticsRaw();
}