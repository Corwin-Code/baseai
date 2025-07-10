package com.clinflash.baseai.domain.dict.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

/**
 * 流程节点类型字典（RETRIEVER / LLM / TOOL …）
 *
 * <p>对应表：<b>dict_flow_node_types</b></p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "code")
@Entity
@Table(name = "dict_flow_node_types")
public class DictFlowNodeType {

    /**
     * 节点类型代码（主键）
     */
    @Id
    @Column(length = 32, nullable = false)
    private String code;

    /**
     * 节点类型显示名称
     */
    @Column(length = 64, nullable = false)
    private String label;
}
