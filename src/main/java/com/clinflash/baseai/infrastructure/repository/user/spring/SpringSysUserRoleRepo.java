package com.clinflash.baseai.infrastructure.repository.user.spring;

import com.clinflash.baseai.infrastructure.persistence.user.entity.SysUserRoleEntity;
import com.clinflash.baseai.infrastructure.persistence.user.entity.SysUserRoleEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringSysUserRoleRepo extends JpaRepository<SysUserRoleEntity, SysUserRoleEntityId> {

    List<SysUserRoleEntity> findByUserId(Long userId);

    List<SysUserRoleEntity> findByRoleId(Long roleId);

    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);

    void deleteByUserId(Long userId);

    void deleteByRoleId(Long roleId);

    long countByRoleId(Long roleId);
}