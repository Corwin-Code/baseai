package com.clinflash.baseai.infrastructure.repository.user;

import com.clinflash.baseai.domain.user.model.User;
import com.clinflash.baseai.domain.user.repository.UserRepository;
import com.clinflash.baseai.infrastructure.persistence.user.entity.SysUserEntity;
import com.clinflash.baseai.infrastructure.persistence.user.mapper.UserMapper;
import com.clinflash.baseai.infrastructure.repository.user.spring.SpringSysUserRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>用户仓储实现</h2>
 *
 * <p>这个类是用户仓储接口的具体实现，它将领域层的抽象操作转换为
 * 具体的数据访问操作。作为领域层和基础设施层之间的适配器，
 * 它确保了领域层的纯净性。</p>
 */
@Repository
public class SysUserJpaRepository implements UserRepository {

    private final SpringSysUserRepo springRepo;
    private final UserMapper mapper;

    public SysUserJpaRepository(SpringSysUserRepo springRepo, UserMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        SysUserEntity entity;

        if (user.id() == null) {
            // 新建用户
            entity = mapper.toEntity(user);
        } else {
            // 更新现有用户
            entity = springRepo.findById(user.id())
                    .orElse(mapper.toEntity(user));
            entity.updateFromDomain(user);
        }

        SysUserEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return springRepo.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springRepo.findByUsernameAndDeletedAtIsNull(username)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springRepo.findByEmailAndDeletedAtIsNull(email)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return springRepo.existsByUsernameAndDeletedAtIsNull(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springRepo.existsByEmailAndDeletedAtIsNull(email);
    }

    @Override
    public boolean existsById(Long id) {
        return springRepo.findById(id)
                .map(entity -> entity.getDeletedAt() == null)
                .orElse(false);
    }

    @Override
    public List<User> findByIds(List<Long> ids) {
        return springRepo.findByIdInAndDeletedAtIsNull(ids)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<User> searchByKeyword(String keyword, int page, int size, Long tenantId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SysUserEntity> entityPage = springRepo.searchByKeyword(keyword, tenantId, pageable);

        return entityPage.getContent()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByKeyword(String keyword, Long tenantId) {
        return springRepo.countByKeyword(keyword, tenantId);
    }

    @Override
    public long countAll() {
        return springRepo.countByDeletedAtIsNull();
    }

    @Override
    public long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end, Long tenantId) {
        return springRepo.countByCreatedAtBetween(start, end, tenantId);
    }

    @Override
    public long countByLastLoginAtBetween(OffsetDateTime start, OffsetDateTime end, Long tenantId) {
        return springRepo.countByLastLoginAtBetween(start, end, tenantId);
    }

    @Override
    public boolean softDelete(Long id, Long deletedBy) {
        int updated = springRepo.softDeleteById(id, OffsetDateTime.now());
        return updated > 0;
    }

    @Override
    public List<User> findExpiredUsers(OffsetDateTime before) {
        return springRepo.findExpiredUsers(before)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}