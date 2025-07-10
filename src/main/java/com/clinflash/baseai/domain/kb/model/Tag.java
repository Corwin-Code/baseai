package com.clinflash.baseai.domain.kb.model;

import java.time.OffsetDateTime;

/**
 * <h2>标签实体</h2>
 *
 * <p>用于知识块分类和组织的标签系统。
 * 支持多对多关联，一个知识块可以有多个标签。</p>
 *
 * @param id        标签唯一标识
 * @param name      标签名称（全局唯一）
 * @param remark    标签说明
 * @param createdBy 创建人ID
 * @param createdAt 创建时间
 * @param deletedAt 软删除时间
 */
public record Tag(
        Long id,
        String name,
        String remark,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {

    /**
     * 创建新标签的工厂方法
     *
     * @param name      标签名称
     * @param remark    标签说明
     * @param createdBy 创建人ID
     * @return 新标签实例
     */
    public static Tag create(String name, String remark, Long createdBy) {
        return new Tag(
                null, name, remark, createdBy,
                OffsetDateTime.now(), null
        );
    }

    /**
     * 更新标签信息
     *
     * @param newRemark 新的说明
     * @return 更新后的标签实例
     */
    public Tag updateRemark(String newRemark) {
        return new Tag(
                this.id, this.name, newRemark, this.createdBy,
                this.createdAt, this.deletedAt
        );
    }

    /**
     * 检查是否已删除
     *
     * @return true如果已被软删除
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}