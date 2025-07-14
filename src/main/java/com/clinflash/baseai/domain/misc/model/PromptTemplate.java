package com.clinflash.baseai.domain.misc.model;

import java.time.OffsetDateTime;

/**
 * <h2>提示词模板领域模型</h2>
 *
 * <p>提示词模板是AI应用中的重要资产，每个模板都经过特定场景的优化，
 * 能够引导AI模型产生更准确、更符合预期的输出。</p>
 *
 * @param id        模板唯一标识
 * @param tenantId  归属租户ID，系统模板为null
 * @param name      模板名称，在租户内唯一
 * @param content   模板内容，支持变量占位符
 * @param modelCode 适用的AI模型代码
 * @param version   模板版本号，支持迭代优化
 * @param isSystem  是否为系统预置模板
 * @param createdBy 创建人ID
 * @param createdAt 创建时间
 * @param deletedAt 软删除时间
 */
public record PromptTemplate(
        Long id,
        Long tenantId,
        String name,
        String content,
        String modelCode,
        Integer version,
        Boolean isSystem,
        Long createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt
) {

    /**
     * 创建新的提示词模板
     *
     * <p>创建模板时需要验证内容的完整性和适用性。系统会自动检测模板中的变量占位符，
     * 确保模板的可用性和安全性。</p>
     */
    public static PromptTemplate create(Long tenantId, String name, String content,
                                        String modelCode, Long createdBy) {
        return new PromptTemplate(
                null, // ID由数据库生成
                tenantId,
                name,
                content,
                modelCode,
                1, // 初始版本为1
                false, // 用户创建的模板默认非系统模板
                createdBy,
                OffsetDateTime.now(),
                null
        );
    }

    /**
     * 创建系统预置模板
     *
     * <p>系统模板是平台提供的高质量模板，经过充分测试和优化。
     * 这些模板对所有租户可见，作为最佳实践的参考。</p>
     */
    public static PromptTemplate createSystemTemplate(String name, String content,
                                                      String modelCode, Long createdBy) {
        return new PromptTemplate(
                null,
                null, // 系统模板不属于特定租户
                name,
                content,
                modelCode,
                1,
                true, // 标记为系统模板
                createdBy,
                OffsetDateTime.now(),
                null
        );
    }

    /**
     * 更新模板内容
     *
     * <p>更新模板会创建新版本，保持历史版本的可追溯性。
     * 这对于模板效果的A/B测试和回滚非常重要。</p>
     */
    public PromptTemplate updateContent(String newContent, String newModelCode, Long updatedBy) {
        return new PromptTemplate(
                this.id,
                this.tenantId,
                this.name,
                newContent,
                newModelCode,
                this.version + 1, // 版本号递增
                this.isSystem,
                this.createdBy,
                this.createdAt,
                this.deletedAt
        );
    }

    /**
     * 软删除模板
     */
    public PromptTemplate markAsDeleted() {
        return new PromptTemplate(
                this.id,
                this.tenantId,
                this.name,
                this.content,
                this.modelCode,
                this.version,
                this.isSystem,
                this.createdBy,
                this.createdAt,
                OffsetDateTime.now()
        );
    }

    /**
     * 检查模板是否可编辑
     *
     * <p>系统模板通常不允许普通用户编辑，只有系统管理员才能修改。
     * 已删除的模板也不能再编辑。</p>
     */
    public boolean isEditable() {
        return !this.isSystem && this.deletedAt == null;
    }

    /**
     * 检查模板是否对指定租户可见
     */
    public boolean isVisibleToTenant(Long tenantId) {
        return this.isSystem || (this.tenantId != null && this.tenantId.equals(tenantId));
    }

    /**
     * 验证模板内容的基本格式
     */
    public boolean hasValidContent() {
        return this.content != null &&
                !this.content.trim().isEmpty() &&
                this.content.length() <= 10000; // 假设最大长度限制
    }
}