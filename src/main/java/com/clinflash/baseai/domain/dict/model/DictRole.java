package com.clinflash.baseai.domain.dict.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

/**
 * 对话角色类型字典（USER / ASSISTANT / SYSTEM 等）
 *
 * <p>对应表：<b>dict_roles</b></p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "code")
@Entity
@Table(name = "dict_roles")
public class DictRole {

    /**
     * 角色代码（主键）
     */
    @Id
    @Column(length = 16, nullable = false)
    private String code;

    /**
     * 角色显示名称
     */
    @Column(length = 32, nullable = false)
    private String label;
}
