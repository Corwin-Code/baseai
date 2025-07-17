package com.cloud.baseai.domain.dict.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

/**
 * 大语言模型类型字典，包含最大 Token 与计费信息
 *
 * <p>对应表：<b>dict_models</b></p>
 *
 * <p>注意：</p>
 * <ul>
 *   <li>{@code code} 在整个系统中被大量外键引用，务必保持唯一且稳定；</li>
 *   <li>价格字段精度为 (10,4)，使用 {@link BigDecimal} 避免精度损失。</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "code")
@Entity
@Table(name = "dict_models")
public class DictModel {

    /**
     * 模型编码（主键，如 gpt-4o、glm-4）
     */
    @Id
    @Column(length = 32, nullable = false)
    private String code;

    /**
     * 模型显示名称
     */
    @Column(length = 64, nullable = false)
    private String label;

    /**
     * 最大 Token 数
     */
    @Column(name = "max_tokens")
    private Integer maxTokens;

    /**
     * 输入价格（USD/Token）
     */
    @Column(name = "price_in", precision = 10, scale = 4)
    private BigDecimal priceIn;

    /**
     * 输出价格（USD/Token）
     */
    @Column(name = "price_out", precision = 10, scale = 4)
    private BigDecimal priceOut;
}
