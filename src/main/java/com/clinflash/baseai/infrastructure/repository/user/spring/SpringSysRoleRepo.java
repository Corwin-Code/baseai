package com.clinflash.baseai.infrastructure.repository.user.spring;

import com.clinflash.baseai.infrastructure.persistence.user.entity.SysRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringSysRoleRepo extends JpaRepository<SysRoleEntity, Long> {

    Optional<SysRoleEntity> findByName(String name);

    boolean existsByName(String name);

    List<SysRoleEntity> findByIdIn(List<Long> ids);

    @Query("SELECT r FROM SysRoleEntity r WHERE r.name LIKE 'SYSTEM_%'")
    List<SysRoleEntity> findSystemRoles();

    @Query("SELECT r FROM SysRoleEntity r WHERE r.name LIKE 'TENANT_%'")
    List<SysRoleEntity> findTenantRoles();

    @Query("SELECT r FROM SysRoleEntity r WHERE r.name LIKE '%ADMIN%' OR r.name LIKE '%OWNER%'")
    List<SysRoleEntity> findAdminRoles();

    @Query("SELECT r FROM SysRoleEntity r ORDER BY " +
            "CASE WHEN r.name LIKE '%OWNER%' THEN 1 " +
            "     WHEN r.name LIKE '%ADMIN%' THEN 2 " +
            "     WHEN r.name LIKE '%MANAGER%' THEN 3 " +
            "     WHEN r.name LIKE '%MEMBER%' THEN 4 " +
            "     ELSE 5 END")
    List<SysRoleEntity> findAllOrderByWeight();
}