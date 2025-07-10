package com.clinflash.baseai.infrastructure.repository.kb;

import com.clinflash.baseai.domain.kb.model.Tag;
import com.clinflash.baseai.domain.kb.repository.TagRepository;
import com.clinflash.baseai.infrastructure.persistence.kb.entity.KbTagEntity;
import com.clinflash.baseai.infrastructure.persistence.kb.mapper.KbMapper;
import com.clinflash.baseai.infrastructure.repository.kb.spring.SpringKbTagRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <h2>标签仓储实现</h2>
 *
 * <p>标签仓储的JPA实现，提供标签的基本CRUD操作和使用统计功能。</p>
 */
@Repository
public class KbTagJpaRepository implements TagRepository {

    private final SpringKbTagRepo springRepo;
    private final KbMapper mapper;

    public KbTagJpaRepository(SpringKbTagRepo springRepo, KbMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public Tag save(Tag tag) {
        KbTagEntity entity;

        if (tag.id() == null) {
            entity = mapper.toEntity(tag);
        } else {
            entity = springRepo.findById(tag.id())
                    .orElse(mapper.toEntity(tag));
            entity.setRemark(tag.remark());
        }

        KbTagEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Tag> findById(Long id) {
        return springRepo.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Tag> findByName(String name) {
        return springRepo.findByNameAndDeletedAtIsNull(name)
                .map(mapper::toDomain);
    }

    @Override
    public List<Tag> findByIds(List<Long> ids) {
        return springRepo.findAllById(ids)
                .stream()
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Tag> findAll() {
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("name"));
        return springRepo.findByDeletedAtIsNullOrderByName(pageable)
                .getContent()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Tag> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name"));
        return springRepo.findByDeletedAtIsNullOrderByName(pageable)
                .getContent()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Tag> searchByName(String namePattern, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return springRepo.findByNameContainingIgnoreCaseAndDeletedAtIsNullOrderByName(namePattern, pageable)
                .getContent()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return springRepo.countByDeletedAtIsNull();
    }

    @Override
    public List<TagUsageInfo> findPopularTags(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return springRepo.findPopularTags(pageable)
                .stream()
                .map(projection -> new TagUsageInfo(
                        projection.getTag().toDomain(),
                        projection.getUsageCount()
                ))
                .collect(Collectors.toList());
    }
}