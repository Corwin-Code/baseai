package com.cloud.baseai.infrastructure.repository.system.spring;

import com.cloud.baseai.infrastructure.persistence.system.entity.SysSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>系统设置Spring Data JPA仓储</h2>
 */
@Repository
public interface SpringSysSettingRepo extends JpaRepository<SysSettingEntity, String> {

    /**
     * 根据键前缀查找设置
     */
    @Query("SELECT s FROM SysSettingEntity s WHERE s.key LIKE CONCAT(:prefix, '%') ORDER BY s.key")
    List<SysSettingEntity> findByKeyPrefix(@Param("prefix") String prefix);

    /**
     * 检查键是否存在
     */
    boolean existsByKey(String key);
}