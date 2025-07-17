package com.cloud.baseai.infrastructure.repository.user;

import com.cloud.baseai.domain.user.model.Tenant;
import com.cloud.baseai.domain.user.repository.TenantRepository;
import com.cloud.baseai.infrastructure.persistence.user.entity.SysTenantEntity;
import com.cloud.baseai.infrastructure.persistence.user.mapper.UserMapper;
import com.cloud.baseai.infrastructure.repository.user.spring.SpringSysTenantRepo;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>租户仓储实现</h2>
 */
@Repository
public class SysTenantJpaRepository implements TenantRepository {

    private final SpringSysTenantRepo springRepo;
    private final UserMapper mapper;

    public SysTenantJpaRepository(SpringSysTenantRepo springRepo, UserMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public Tenant save(Tenant tenant) {
        SysTenantEntity entity;

        if (tenant.id() == null) {
            entity = mapper.toEntity(tenant);
        } else {
            entity = springRepo.findById(tenant.id())
                    .orElse(mapper.toEntity(tenant));
            entity.updateFromDomain(tenant);
        }

        SysTenantEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Tenant> findById(Long id) {
        return springRepo.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Tenant> findByOrgName(String orgName) {
        return springRepo.findByOrgNameAndDeletedAtIsNull(orgName)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByOrgName(String orgName) {
        return springRepo.existsByOrgNameAndDeletedAtIsNull(orgName);
    }

    @Override
    public boolean existsById(Long id) {
        return springRepo.findById(id)
                .map(entity -> entity.getDeletedAt() == null)
                .orElse(false);
    }

    @Override
    public List<Tenant> findByIds(List<Long> ids) {
        return springRepo.findByIdInAndDeletedAtIsNull(ids)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Tenant> findAllActive() {
        return springRepo.findByDeletedAtIsNull()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Tenant> findExpiringBefore(OffsetDateTime beforeDate) {
        return springRepo.findExpiringBefore(beforeDate)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Tenant> findByPlanCode(String planCode) {
        return springRepo.findByPlanCodeAndDeletedAtIsNull(planCode)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countAll() {
        return springRepo.countByDeletedAtIsNull();
    }

    @Override
    public long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end) {
        return springRepo.countByCreatedAtBetween(start, end);
    }

    @Override
    public boolean softDelete(Long id, Long deletedBy) {
        int updated = springRepo.softDeleteById(id, OffsetDateTime.now());
        return updated > 0;
    }
}