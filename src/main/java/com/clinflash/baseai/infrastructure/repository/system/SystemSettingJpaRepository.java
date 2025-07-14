package com.clinflash.baseai.infrastructure.repository.system;

import com.clinflash.baseai.domain.system.model.SystemSetting;
import com.clinflash.baseai.domain.system.repository.SystemSettingRepository;
import com.clinflash.baseai.infrastructure.persistence.system.entity.SysSettingEntity;
import com.clinflash.baseai.infrastructure.persistence.system.mapper.SystemMapper;
import com.clinflash.baseai.infrastructure.repository.system.spring.SpringSysSettingRepo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <h2>系统设置仓储实现</h2>
 */
@Repository
public class SystemSettingJpaRepository implements SystemSettingRepository {

    private final SpringSysSettingRepo springRepo;
    private final SystemMapper mapper;

    public SystemSettingJpaRepository(SpringSysSettingRepo springRepo, SystemMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public SystemSetting save(SystemSetting setting) {
        SysSettingEntity entity = mapper.toEntity(setting);
        SysSettingEntity savedEntity = springRepo.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<SystemSetting> findByKey(String key) {
        return springRepo.findById(key)
                .map(mapper::toDomain);
    }

    @Override
    public List<SystemSetting> findAll() {
        return springRepo.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<SystemSetting> findByKeyPrefix(String prefix) {
        return springRepo.findByKeyPrefix(prefix)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void batchUpdate(List<SystemSetting> settings) {
        List<SysSettingEntity> entities = settings.stream()
                .map(mapper::toEntity)
                .collect(Collectors.toList());
        springRepo.saveAll(entities);
    }

    @Override
    public void deleteByKey(String key) {
        springRepo.deleteById(key);
    }

    @Override
    public boolean existsByKey(String key) {
        return springRepo.existsByKey(key);
    }
}