package com.cloud.baseai.infrastructure.repository.user;

import com.cloud.baseai.domain.user.model.UserRole;
import com.cloud.baseai.domain.user.repository.UserRoleRepository;
import com.cloud.baseai.infrastructure.persistence.user.mapper.UserMapper;
import com.cloud.baseai.infrastructure.repository.user.spring.SpringSysUserRoleRepo;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>用户-角色关联仓储实现</h2>
 */
@Repository
public class SysUserRoleJpaRepository implements UserRoleRepository {

    private final SpringSysUserRoleRepo springRepo;
    private final UserMapper mapper;

    public SysUserRoleJpaRepository(SpringSysUserRoleRepo springRepo, UserMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public UserRole save(UserRole userRole) {
        var entity = mapper.toEntity(userRole);
        var saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<UserRole> saveAll(List<UserRole> userRoles) {
        var entities = mapper.toUserRoleEntityList(userRoles);
        var saved = springRepo.saveAll(entities);
        return mapper.toUserRoleDomainList(saved);
    }

    @Override
    public List<UserRole> findByUserId(Long userId) {
        return springRepo.findByUserId(userId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<UserRole> findByRoleId(Long roleId) {
        return springRepo.findByRoleId(roleId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByUserIdAndRoleId(Long userId, Long roleId) {
        return springRepo.existsByUserIdAndRoleId(userId, roleId);
    }

    @Override
    public void deleteByUserIdAndRoleId(Long userId, Long roleId) {
        springRepo.deleteByUserIdAndRoleId(userId, roleId);
    }

    @Override
    public void deleteByUserId(Long userId) {
        springRepo.deleteByUserId(userId);
    }

    @Override
    public void deleteByRoleId(Long roleId) {
        springRepo.deleteByRoleId(roleId);
    }

    @Override
    public long countByRoleId(Long roleId) {
        return springRepo.countByRoleId(roleId);
    }
}