package com.clinflash.baseai.domain.system.repository;

import com.clinflash.baseai.domain.system.model.SystemSetting;

import java.util.List;
import java.util.Optional;

/**
 * <h2>系统设置仓储接口</h2>
 *
 * <p>系统设置的管理需要特别谨慎，因为配置的错误可能导致整个系统的异常。
 * 这个仓储接口不仅提供基本的CRUD操作，还包含了配置验证、缓存管理等高级功能。</p>
 */
public interface SystemSettingRepository {

    /**
     * 保存或更新设置
     */
    SystemSetting save(SystemSetting setting);

    /**
     * 根据键查找设置
     */
    Optional<SystemSetting> findByKey(String key);

    /**
     * 查找所有设置
     */
    List<SystemSetting> findAll();

    /**
     * 根据键前缀查找设置
     *
     * <p>这个方法对于查找某个模块的所有配置很有用，
     * 比如查找所有以"email."开头的邮件相关配置。</p>
     */
    List<SystemSetting> findByKeyPrefix(String prefix);

    /**
     * 批量更新设置
     */
    void batchUpdate(List<SystemSetting> settings);

    /**
     * 删除设置
     */
    void deleteByKey(String key);

    /**
     * 检查键是否存在
     */
    boolean existsByKey(String key);
}