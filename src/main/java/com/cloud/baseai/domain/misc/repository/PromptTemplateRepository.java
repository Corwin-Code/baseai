package com.cloud.baseai.domain.misc.repository;

import com.cloud.baseai.domain.misc.model.PromptTemplate;

import java.util.List;
import java.util.Optional;

/**
 * <h2>提示词模板仓储接口</h2>
 *
 * <p>定义了提示词模板的持久化操作契约。作为领域层与基础设施层的边界，
 * 这个接口封装了所有与提示词模板相关的数据访问逻辑，让领域逻辑保持纯净。</p>
 */
public interface PromptTemplateRepository {

    /**
     * 保存提示词模板
     *
     * <p>支持新增和更新操作。在保存时会自动处理版本号的递增，
     * 确保模板的版本历史能够被正确追踪。</p>
     *
     * @param template 要保存的模板对象
     * @return 保存后的模板对象，包含生成的ID
     */
    PromptTemplate save(PromptTemplate template);

    /**
     * 根据ID查找模板
     *
     * @param id 模板ID
     * @return 模板对象的Optional包装
     */
    Optional<PromptTemplate> findById(Long id);

    /**
     * 查找租户下的所有可见模板
     *
     * <p>包含租户自己的模板和系统预置模板。结果按创建时间降序排列，
     * 让最新的模板优先显示。</p>
     *
     * @param tenantId 租户ID，可为null（查询所有系统模板）
     * @param page     页码（从0开始）
     * @param size     每页大小
     * @return 模板列表
     */
    List<PromptTemplate> findVisibleTemplates(Long tenantId, int page, int size);

    /**
     * 统计租户可见的模板数量
     *
     * @param tenantId 租户ID
     * @return 模板总数
     */
    long countVisibleTemplates(Long tenantId);

    /**
     * 根据名称查找模板
     *
     * <p>在租户范围内按名称查找模板。系统模板和租户模板分开处理，
     * 避免命名冲突。</p>
     *
     * @param tenantId 租户ID，null表示查找系统模板
     * @param name     模板名称
     * @return 模板对象的Optional包装
     */
    Optional<PromptTemplate> findByTenantIdAndName(Long tenantId, String name);

    /**
     * 搜索模板
     *
     * <p>根据关键词在模板名称和内容中进行模糊搜索。
     * 支持多租户隔离，只返回用户有权限看到的模板。</p>
     *
     * @param tenantId 租户ID
     * @param keyword  搜索关键词
     * @param page     页码
     * @param size     每页大小
     * @return 匹配的模板列表
     */
    List<PromptTemplate> searchTemplates(Long tenantId, String keyword, int page, int size);

    /**
     * 查找特定模型的模板
     *
     * <p>按AI模型筛选模板，帮助用户快速找到适用于特定模型的提示词。
     * 这对于模型迁移和优化场景特别有用。</p>
     *
     * @param tenantId  租户ID
     * @param modelCode 模型代码
     * @param page      页码
     * @param size      每页大小
     * @return 模板列表
     */
    List<PromptTemplate> findByModelCode(Long tenantId, String modelCode, int page, int size);

    /**
     * 获取模板的所有版本
     *
     * <p>返回同一模板的所有历史版本，按版本号降序排列。
     * 这对于版本比较和回滚操作很重要。</p>
     *
     * @param tenantId 租户ID
     * @param name     模板名称
     * @return 版本列表
     */
    List<PromptTemplate> findVersions(Long tenantId, String name);

    /**
     * 检查模板名称是否已存在
     *
     * @param tenantId 租户ID
     * @param name     模板名称
     * @return 是否存在
     */
    boolean existsByTenantIdAndName(Long tenantId, String name);

    /**
     * 软删除模板
     *
     * <p>将模板标记为已删除，但不从数据库中物理删除。
     * 这样可以保留审计记录和支持恢复操作。</p>
     *
     * @param id        模板ID
     * @param deletedBy 删除操作者ID
     */
    void softDelete(Long id, Long deletedBy);

    /**
     * 获取热门模板
     *
     * <p>根据使用频率返回最受欢迎的模板。统计维度可以包括
     * 被复制次数、引用次数等指标。</p>
     *
     * @param tenantId 租户ID
     * @param limit    返回数量限制
     * @return 热门模板列表
     */
    List<PromptTemplate> findPopularTemplates(Long tenantId, int limit);
}