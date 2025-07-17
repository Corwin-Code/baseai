package com.cloud.baseai.domain.dict.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

/**
 * 文档来源类型字典（如 PDF、URL、Markdown 等）
 *
 * <p>对应表：<b>dict_source_types</b></p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "code")
@Entity
@Table(name = "dict_source_types")
public class DictSourceType {

    /**
     * 来源类型编码（主键，如 PDF、URL）
     */
    @Id
    @Column(length = 32, nullable = false)
    private String code;

    /**
     * 来源类型显示名
     */
    @Column(length = 64, nullable = false)
    private String label;
}
