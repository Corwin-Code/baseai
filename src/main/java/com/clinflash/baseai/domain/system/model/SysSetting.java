package com.clinflash.baseai.domain.system.model;

import com.clinflash.baseai.domain.system.model.enums.SettingValueType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * 平台全局配置参数表
 *
 * <p>对应表：<b>sys_settings</b></p>
 *
 * <p>⚠ 说明：</p>
 * <ul>
 *   <li>{@code key} 为业务语义化主键，示例：<code>chat.max_conversation_length</code></li>
 *   <li>{@code valueType} 决定如何解析 {@code value}；详见 {@link SettingValueType}</li>
 *   <li>{@code updatedBy} 建议映射为 {@code SysUser}；此处仅用 Long 占位，待 user 模块实体补全后
 *       可改为 <code>@ManyToOne</code> 关联。</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "key")
@Entity
@Table(name = "sys_settings")
public class SysSetting {

    /**
     * 参数唯一 Key（主键）
     */
    @Id
    @Column(length = 64, nullable = false)
    private String key;

    /**
     * 参数内容（统一存文本，按 valueType 解析）
     */
    @Lob
    @Column(nullable = false)
    private String value;

    /**
     * 参数值类型（string/int/bool/json …）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", length = 16, nullable = false)
    private SettingValueType valueType;

    /**
     * 参数说明
     */
    @Column(length = 255)
    private String remark;

    /**
     * 最近修改人 ID（预留外键 sys_users.id）
     */
    @Column(name = "updated_by")
    private Long updatedBy;

    /**
     * 最近修改时间（由 Hibernate 自动更新）
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
