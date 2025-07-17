package com.cloud.baseai.domain.kb.repository;

import com.cloud.baseai.domain.kb.model.Document;
import com.cloud.baseai.domain.kb.model.ParsingStatus;

import java.util.List;
import java.util.Optional;

/**
 * <h2>文档仓储接口</h2>
 *
 * <p>定义文档聚合根的持久化操作契约。
 * 遵循DDD仓储模式，屏蔽底层数据访问细节。</p>
 *
 * <p><b>设计原则：</b></p>
 * <ul>
 * <li>面向领域设计，方法命名体现业务语义</li>
 * <li>返回领域对象而非数据传输对象</li>
 * <li>支持复杂查询条件但不暴露底层实现</li>
 * <li>实现数据租户隔离</li>
 * </ul>
 */
public interface DocumentRepository {

    /**
     * 保存文档
     *
     * <p>创建新文档或更新现有文档。仓储实现需要处理ID的生成。</p>
     *
     * @param document 要保存的文档实体
     * @return 保存后的文档实体（包含生成的ID）
     */
    Document save(Document document);

    /**
     * 根据ID查找文档
     *
     * @param id 文档ID
     * @return 文档实体，如果不存在则返回空
     */
    Optional<Document> findById(Long id);

    /**
     * 根据SHA256哈希查找文档
     *
     * <p>用于防重复上传，检查是否已存在相同内容的文档。</p>
     *
     * @param sha256 文档内容的SHA256哈希值
     * @return 匹配的文档实体，如果不存在则返回空
     */
    Optional<Document> findBySha256(String sha256);

    /**
     * 检查租户下是否存在同名文档
     *
     * <p>确保租户内文档标题的唯一性。</p>
     *
     * @param tenantId 租户ID
     * @param title    文档标题
     * @return true如果存在同名文档
     */
    boolean existsByTenantIdAndTitle(Long tenantId, String title);

    /**
     * 分页查询租户下的文档列表
     *
     * <p>支持软删除过滤，只返回未删除的文档。</p>
     *
     * @param tenantId 租户ID
     * @param page     页码（从0开始）
     * @param size     每页大小
     * @return 文档列表，按更新时间倒序
     */
    List<Document> findByTenantId(Long tenantId, int page, int size);

    /**
     * 统计租户下的文档总数
     *
     * @param tenantId 租户ID
     * @return 文档总数（不包括已删除的）
     */
    long countByTenantId(Long tenantId);

    /**
     * 根据解析状态查询文档
     *
     * <p>用于批处理任务，如重新解析失败的文档。</p>
     *
     * @param status 解析状态
     * @param limit  最大返回数量
     * @return 匹配状态的文档列表
     */
    List<Document> findByParsingStatus(ParsingStatus status, int limit);

    /**
     * 批量查询文档
     *
     * @param ids 文档ID列表
     * @return 匹配的文档列表
     */
    List<Document> findByIds(List<Long> ids);

    /**
     * 根据来源类型查询文档
     *
     * @param tenantId   租户ID
     * @param sourceType 来源类型
     * @return 匹配的文档列表
     */
    List<Document> findByTenantIdAndSourceType(Long tenantId, String sourceType);

    /**
     * 软删除文档
     *
     * <p>将文档标记为已删除，但不物理删除数据。</p>
     *
     * @param id        文档ID
     * @param deletedBy 删除人ID
     * @return true如果删除成功
     */
    boolean softDelete(Long id, Long deletedBy);
}