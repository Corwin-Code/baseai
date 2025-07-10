package com.clinflash.baseai.infrastructure.repository.user;

import com.clinflash.baseai.domain.user.model.Role;
import com.clinflash.baseai.domain.user.repository.RoleRepository;
import com.clinflash.baseai.infrastructure.persistence.user.mapper.UserMapper;
import com.clinflash.baseai.infrastructure.repository.user.spring.SpringSysRoleRepo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>角色仓储实现</h2>
 */
@Repository
public class SysRoleJpaRepository implements RoleRepository {

    private final SpringSysRoleRepo springRepo;
    private final UserMapper mapper;

    public SysRoleJpaRepository(SpringSysRoleRepo springRepo, UserMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public Role save(Role role) {
        var entity = mapper.toEntity(role);
        var saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Role> findById(Long id) {
        return springRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return springRepo.findByName(name).map(mapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return springRepo.existsByName(name);
    }

    @Override
    public List<Role> findByIds(List<Long> ids) {
        return springRepo.findByIdIn(ids)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Role> findAll() {
        return springRepo.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Role> findSystemRoles() {
        return springRepo.findSystemRoles()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Role> findTenantRoles() {
        return springRepo.findTenantRoles()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Role> findAdminRoles() {
        return springRepo.findAdminRoles()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Role> findAllOrderByWeight() {
        return springRepo.findAllOrderByWeight()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public long countAll() {
        return springRepo.count();
    }
}