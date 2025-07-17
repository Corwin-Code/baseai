package com.cloud.baseai.domain.misc.repository;

import com.cloud.baseai.domain.misc.model.FileObject;
import com.cloud.baseai.domain.misc.model.StorageStatistics;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>文件对象仓储接口</h2>
 *
 * <p>管理文件对象元数据的持久化操作。这个接口为对象存储系统提供了
 * 完整的元数据管理能力，包括文件的生命周期、去重、清理等关键功能。</p>
 *
 * <p><b>核心职责：</b></p>
 * <p>除了基本的CRUD操作外，还要支持文件去重、空间统计、清理策略等
 * 高级功能。这些功能对于大规模文件存储系统的稳定运行至关重要。</p>
 */
public interface FileObjectRepository {

    /**
     * 保存文件对象元数据
     *
     * <p>在保存前会检查SHA256哈希的唯一性，避免重复存储相同内容的文件。
     * 如果发现重复文件，可以选择返回已存在的记录或抛出异常。</p>
     *
     * @param fileObject 文件对象
     * @return 保存后的文件对象，包含生成的ID
     */
    FileObject save(FileObject fileObject);

    /**
     * 根据ID查找文件对象
     *
     * @param id 文件对象ID
     * @return 文件对象的Optional包装
     */
    Optional<FileObject> findById(Long id);

    /**
     * 根据对象键查找文件
     *
     * <p>对象键在桶内是唯一的，这个方法通常用于文件的直接访问。
     * 支持跨桶查找，但需要指定完整的桶名。</p>
     *
     * @param bucket    存储桶名称
     * @param objectKey 对象键
     * @return 文件对象的Optional包装
     */
    Optional<FileObject> findByBucketAndObjectKey(String bucket, String objectKey);

    /**
     * 根据SHA256哈希查找文件
     *
     * <p>这是去重功能的核心方法。通过内容哈希快速定位是否已存在
     * 相同内容的文件，避免重复存储。</p>
     *
     * @param sha256 文件内容的SHA256哈希值
     * @return 文件对象的Optional包装
     */
    Optional<FileObject> findBySha256(String sha256);

    /**
     * 查找桶中的所有文件
     *
     * <p>分页查询指定存储桶中的所有文件，支持按创建时间排序。
     * 这对于桶的管理和统计分析很有用。</p>
     *
     * @param bucket 存储桶名称
     * @param page   页码
     * @param size   每页大小
     * @return 文件对象列表
     */
    List<FileObject> findByBucket(String bucket, int page, int size);

    /**
     * 统计桶中的文件数量
     *
     * @param bucket 存储桶名称
     * @return 文件总数
     */
    long countByBucket(String bucket);

    /**
     * 计算桶的总存储大小
     *
     * <p>汇总桶中所有文件的大小，用于存储容量规划和计费。
     * 只统计未删除的文件。</p>
     *
     * @param bucket 存储桶名称
     * @return 总大小（字节）
     */
    long getTotalSizeByBucket(String bucket);

    /**
     * 查找大文件
     *
     * <p>返回超过指定大小阈值的文件列表，用于存储优化和成本管理。
     * 大文件通常需要特殊的处理策略。</p>
     *
     * @param minSizeBytes 最小文件大小（字节）
     * @param page         页码
     * @param size         每页大小
     * @return 大文件列表
     */
    List<FileObject> findLargeFiles(long minSizeBytes, int page, int size);

    /**
     * 查找待清理的文件
     *
     * <p>返回在指定时间之前被标记为删除的文件，用于定期清理任务。
     * 物理删除通常延迟一段时间，以支持误删恢复。</p>
     *
     * @param deletedBefore 删除时间阈值
     * @param limit         返回数量限制
     * @return 待清理文件列表
     */
    List<FileObject> findFilesForCleanup(OffsetDateTime deletedBefore, int limit);

    /**
     * 软删除文件对象
     *
     * <p>标记文件为已删除状态，但保留元数据记录。
     * 实际的存储空间清理由后台任务处理。</p>
     *
     * @param id 文件对象ID
     */
    void softDelete(Long id);

    /**
     * 物理删除文件对象记录
     *
     * <p>彻底删除文件的元数据记录，通常在存储空间也被清理后调用。
     * 这个操作不可逆，需要谨慎使用。</p>
     *
     * @param id 文件对象ID
     */
    void physicalDelete(Long id);

    /**
     * 批量删除文件对象
     *
     * <p>批量处理删除操作，提高清理效率。适用于定期清理任务
     * 和大批量文件的删除场景。</p>
     *
     * @param ids 文件对象ID列表
     */
    void batchDelete(List<Long> ids);

    /**
     * 获取存储统计信息
     *
     * <p>返回各个存储桶的文件数量和存储空间使用情况，
     * 用于监控和容量规划。</p>
     *
     * @return 存储统计信息
     */
    List<StorageStatistics> getStorageStatistics();
}