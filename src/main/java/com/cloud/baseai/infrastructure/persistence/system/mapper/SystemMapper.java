package com.cloud.baseai.infrastructure.persistence.system.mapper;

import com.cloud.baseai.domain.system.model.SystemSetting;
import com.cloud.baseai.domain.system.model.SystemTask;
import com.cloud.baseai.infrastructure.persistence.system.entity.SysSettingEntity;
import com.cloud.baseai.infrastructure.persistence.system.entity.SysTaskEntity;
import org.springframework.stereotype.Component;

/**
 * <h2>系统模块映射器</h2>
 */
@Component
public class SystemMapper {

    // =================== SystemSetting 映射 ===================

    /**
     * 系统设置实体转领域模型
     */
    public SystemSetting toDomain(SysSettingEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    /**
     * 系统设置领域模型转实体
     */
    public SysSettingEntity toEntity(SystemSetting domain) {
        return SysSettingEntity.fromDomain(domain);
    }

    // =================== SystemTask 映射 ===================

    /**
     * 系统任务实体转领域模型
     */
    public SystemTask toDomain(SysTaskEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    /**
     * 系统任务领域模型转实体
     */
    public SysTaskEntity toEntity(SystemTask domain) {
        return SysTaskEntity.fromDomain(domain);
    }
}