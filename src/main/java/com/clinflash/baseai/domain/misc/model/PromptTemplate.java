package com.clinflash.baseai.domain.misc.model;

import com.clinflash.baseai.infrastructure.persistence.user.entity.SysTenantEntity;
import com.clinflash.baseai.infrastructure.persistence.user.entity.SysUserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * <h2>PromptTemplate — 提示词模板</h2>
 *
 * <p>支持：</p>
 * <ul>
 *   <li>多租户隔离（{@link SysTenantEntity}）</li>
 *   <li>多模型适配（<code>modelCode</code>）</li>
 *   <li>多版本迭代（<code>version</code>）</li>
 *   <li>系统级内置模板（<code>isSystem</code>=true，租户可见只读）</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "prompt_templates")
public class PromptTemplate {

    /**
     * 模板主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ---------- 多租户 ---------- */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private SysTenantEntity tenant;

    /* ---------- 内容与属性 ---------- */

    /**
     * 模板名称（租户内唯一性可在 Service 层保证）
     */
    @Column(length = 128, nullable = false)
    private String name;

    /**
     * 模板正文
     */
    @Lob
    @Column(nullable = false)
    private String content;

    /**
     * 适用模型编码（如 gpt-4o）
     */
    @Column(name = "model_code", length = 32, nullable = false)
    private String modelCode;

    /**
     * 版本号（1,2,3…；可用乐观锁实现覆盖）
     */
    @Column
    private Integer version = 1;

    /**
     * 是否系统内置（true 时仅超级管理员可修改）
     */
    @Column(name = "is_system")
    private Boolean system = Boolean.FALSE;

    /* ---------- 创建人 & 时间 ---------- */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private SysUserEntity creator;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at", columnDefinition = "timestamptz")
    private OffsetDateTime deletedAt;
}
