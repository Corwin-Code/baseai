package com.cloud.baseai.infrastructure.repository.misc;

import com.cloud.baseai.domain.misc.model.FileObject;
import com.cloud.baseai.domain.misc.model.StorageStatistics;
import com.cloud.baseai.domain.misc.repository.FileObjectRepository;
import com.cloud.baseai.infrastructure.persistence.misc.entity.FileObjectEntity;
import com.cloud.baseai.infrastructure.persistence.misc.mapper.MiscMapper;
import com.cloud.baseai.infrastructure.repository.misc.spring.SpringFileObjectRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <h2>文件对象仓储实现</h2>
 */
@Repository
public class FileObjectJpaRepository implements FileObjectRepository {

    private final SpringFileObjectRepo springRepo;
    private final MiscMapper mapper;

    public FileObjectJpaRepository(SpringFileObjectRepo springRepo, MiscMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public FileObject save(FileObject object) {
        FileObjectEntity entity = mapper.toEntity(object);
        FileObjectEntity savedEntity = springRepo.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<FileObject> findById(Long id) {
        return springRepo.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<FileObject> findByBucketAndObjectKey(String bucket, String objectKey) {
        return springRepo.findByBucketAndObjectKeyAndDeletedAtIsNull(bucket, objectKey)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<FileObject> findBySha256(String sha256) {
        return springRepo.findBySha256AndDeletedAtIsNull(sha256)
                .map(mapper::toDomain);
    }

    @Override
    public List<FileObject> findByBucket(String bucket, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return springRepo.findByBucket(bucket, pageable)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByBucket(String bucket) {
        return springRepo.countByBucketAndDeletedAtIsNull(bucket);
    }

    @Override
    public long getTotalSizeByBucket(String bucket) {
        return springRepo.getTotalSizeByBucket(bucket);
    }

    @Override
    public List<FileObject> findLargeFiles(long minSizeBytes, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return springRepo.findLargeFiles(minSizeBytes, pageable)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<FileObject> findFilesForCleanup(OffsetDateTime deletedBefore, int limit) {
        Pageable pageable = PageRequest.ofSize(limit);
        return springRepo.findFilesForCleanup(deletedBefore, pageable)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void softDelete(Long id) {
        springRepo.findById(id).ifPresent(entity -> {
            entity.setDeletedAt(OffsetDateTime.now());
            springRepo.save(entity);
        });
    }

    @Override
    public void physicalDelete(Long id) {
        springRepo.deleteById(id);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        springRepo.findAllById(ids)
                .stream()
                .findAny()
                .ifPresent(entity -> softDelete(entity.getId()));
    }

    @Override
    public List<StorageStatistics> getStorageStatistics() {
        return springRepo.getStorageStatisticsRaw();
    }
}