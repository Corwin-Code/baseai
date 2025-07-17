package com.cloud.baseai.infrastructure.repository.user;

import com.cloud.baseai.domain.user.model.TenantMemberStatus;
import com.cloud.baseai.domain.user.model.UserTenant;
import com.cloud.baseai.domain.user.repository.UserTenantRepository;
import com.cloud.baseai.infrastructure.persistence.user.mapper.UserMapper;
import com.cloud.baseai.infrastructure.repository.user.spring.SpringSysUserTenantRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <h2>用户-租户关联仓储实现</h2>
 */
@Repository
public class SysUserTenantJpaRepository implements UserTenantRepository {

    private final SpringSysUserTenantRepo springRepo;
    private final UserMapper mapper;

    public SysUserTenantJpaRepository(SpringSysUserTenantRepo springRepo, UserMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public UserTenant save(UserTenant userTenant) {
        var entity = mapper.toEntity(userTenant);
        var saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<UserTenant> findByUserIdAndTenantId(Long userId, Long tenantId) {
        return springRepo.findByUserIdAndTenantId(userId, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByUserIdAndTenantId(Long userId, Long tenantId) {
        return springRepo.existsByUserIdAndTenantId(userId, tenantId);
    }

    @Override
    public List<UserTenant> findByUserId(Long userId) {
        return springRepo.findByUserId(userId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<UserTenant> findByTenantId(Long tenantId) {
        return springRepo.findByTenantId(tenantId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<UserTenant> findByTenantIdWithFilters(Long tenantId, String status, Long roleId,
                                                      int page, int size) {
        Integer statusCode = null;
        if (status != null) {
            try {
                TenantMemberStatus memberStatus = TenantMemberStatus.valueOf(status.toUpperCase());
                statusCode = memberStatus.getCode();
            } catch (IllegalArgumentException e) {
                // 忽略无效状态，返回空结果
                return List.of();
            }
        }

        Pageable pageable = PageRequest.of(page, size);
        return springRepo.findByTenantIdWithFilters(tenantId, statusCode, roleId, pageable)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countByTenantId(Long tenantId) {
        return springRepo.countByTenantId(tenantId);
    }

    @Override
    public long countByTenantIdWithFilters(Long tenantId, String status, Long roleId) {
        Integer statusCode = null;
        if (status != null) {
            try {
                TenantMemberStatus memberStatus = TenantMemberStatus.valueOf(status.toUpperCase());
                statusCode = memberStatus.getCode();
            } catch (IllegalArgumentException e) {
                return 0;
            }
        }

        return springRepo.countByTenantIdWithFilters(tenantId, statusCode, roleId);
    }

    @Override
    public Map<String, Long> countByTenantIdGroupByStatus(Long tenantId) {
        List<Object[]> results = springRepo.countByTenantIdGroupByStatus(tenantId);

        return results.stream()
                .collect(Collectors.toMap(
                        result -> TenantMemberStatus.fromCode((Integer) result[0]).name(),
                        result -> (Long) result[1]
                ));
    }

    @Override
    public long countByTenantIdAndRoleIds(Long tenantId, Set<Long> roleIds) {
        return springRepo.countByTenantIdAndRoleIds(tenantId, roleIds);
    }

    @Override
    public void delete(UserTenant userTenant) {
        var entity = mapper.toEntity(userTenant);
        springRepo.delete(entity);
    }

    @Override
    public void deleteByUserId(Long userId) {
        springRepo.deleteByUserId(userId);
    }

    @Override
    public void deleteByTenantId(Long tenantId) {
        springRepo.deleteByTenantId(tenantId);
    }
}