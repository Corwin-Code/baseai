package com.cloud.baseai.infrastructure.persistence.system.entity;

import com.cloud.baseai.domain.system.model.SystemSetting;
import com.cloud.baseai.domain.system.model.enums.SettingValueType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * <h2>系统设置JPA实体</h2>
 *
 * <p>系统设置实体是配置管理的数据基础。每一条记录都代表着系统的一个配置项，
 * 这些配置项共同构成了系统的"性格"和"行为模式"。</p>
 */
@Setter
@Getter
@Entity
@Table(name = "sys_settings")
public class SysSettingEntity {

    @Id
    @Column(name = "key", length = 64)
    private String key;

    @Column(name = "value", nullable = false, columnDefinition = "TEXT")
    private String value;

    @Column(name = "value_type", nullable = false, length = 16)
    private String valueType;

    @Column(name = "remark", length = 255)
    private String remark;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // 构造函数
    public SysSettingEntity() {
    }

    // =================== 领域对象转换 ===================

    /**
     * 从领域对象创建实体
     */
    public static SysSettingEntity fromDomain(SystemSetting setting) {
        if (setting == null) {
            return null;
        }

        SysSettingEntity entity = new SysSettingEntity();
        entity.setKey(setting.key());
        entity.setValue(setting.value());
        entity.setValueType(setting.valueType().name());
        entity.setRemark(setting.remark());
        entity.setUpdatedBy(setting.updatedBy());
        entity.setUpdatedAt(setting.updatedAt());

        return entity;
    }

    /**
     * 转换为领域对象
     */
    public SystemSetting toDomain() {
        return new SystemSetting(
                key,
                value,
                SettingValueType.valueOf(valueType),
                remark,
                updatedBy,
                updatedAt
        );
    }

    /**
     * 从领域对象更新实体状态
     */
    public void updateFromDomain(SystemSetting setting) {
        if (setting == null) {
            return;
        }

        // ID和创建时间不可更改
        this.key = setting.key();
        this.value = setting.value();
        this.valueType = setting.valueType().getLabel();
        this.remark = setting.remark();
        this.updatedBy = setting.updatedBy();
        this.updatedAt = setting.updatedAt();
    }
}