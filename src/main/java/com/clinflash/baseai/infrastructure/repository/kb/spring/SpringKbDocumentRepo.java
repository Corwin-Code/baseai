package com.clinflash.baseai.infrastructure.repository.kb.spring;

import com.clinflash.baseai.infrastructure.persistence.kb.entity.KbDocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>文档Spring Data JPA仓储</h2>
 *
 * <p>这是基础设施层的Spring Data JPA仓储接口，负责与数据库进行交互。
 * 它使用Spring Data JPA的强大功能来自动生成SQL查询，同时也提供了一些自定义查询来满足复杂的业务需求。</p>
 *
 * <p><b>设计理念：</b></p>
 * <p>Spring Data JPA仓储接口允许我们通过方法命名约定来自动生成查询，这大大减少了样板代码的编写。
 * 对于更复杂的查询需求，我们使用@Query注解来编写自定义JPQL或原生SQL查询。</p>
 */
@Repository
public interface SpringKbDocumentRepo extends JpaRepository<KbDocumentEntity, Long> {

    /**
     * 根据SHA256哈希查找文档
     *
     * <p>这个方法用于防止重复文档上传。当用户上传新文档时，
     * 系统会先计算文档内容的SHA256哈希值，然后通过这个方法检查是否已经存在相同内容的文档。</p>
     *
     * @param sha256 文档内容的SHA256哈希值
     * @return 匹配的文档实体，如果不存在则返回Optional.empty()
     */
    Optional<KbDocumentEntity> findBySha256(String sha256);

    /**
     * 检查租户下是否存在同名文档
     *
     * <p>确保在同一租户下文档标题的唯一性。这是一个业务规则，
     * 防止用户在同一租户下创建重名文档导致混淆。</p>
     *
     * @param tenantId 租户ID
     * @param title    文档标题
     *                 // * @param deletedAt 删除时间（null表示未删除）
     * @return true如果存在同名文档
     */
    boolean existsByTenantIdAndTitleAndDeletedAtIsNull(Long tenantId, String title);

    /**
     * 分页查询租户下的未删除文档
     *
     * <p>这个查询支持软删除机制，只返回未被删除的文档。
     * 使用分页参数来支持大量文档的高效查询。</p>
     *
     * @param tenantId 租户ID
     * @param pageable 分页参数（包含页码、页大小、排序）
     * @return 分页的文档结果
     */
    Page<KbDocumentEntity> findByTenantIdAndDeletedAtIsNull(Long tenantId, Pageable pageable);

    /**
     * 统计租户下的未删除文档总数
     *
     * @param tenantId 租户ID
     * @return 文档总数
     */
    long countByTenantIdAndDeletedAtIsNull(Long tenantId);

    /**
     * 根据解析状态查询文档
     *
     * <p>这个查询用于批处理任务，比如重新处理解析失败的文档，
     * 或者统计各种状态下的文档数量。</p>
     *
     * @param parsingStatus 解析状态代码
     * @param pageable      分页参数
     * @return 匹配状态的文档列表
     */
    Page<KbDocumentEntity> findByParsingStatusAndDeletedAtIsNull(Integer parsingStatus, Pageable pageable);

    /**
     * 根据来源类型查询文档
     *
     * <p>这个查询用于按文档来源进行分类统计或管理，
     * 例如查看所有PDF文档或所有从URL导入的文档。</p>
     *
     * @param tenantId   租户ID
     * @param sourceType 来源类型
     * @return 匹配的文档列表
     */
    List<KbDocumentEntity> findByTenantIdAndSourceTypeAndDeletedAtIsNull(Long tenantId, String sourceType);

    /**
     * 批量软删除文档
     *
     * <p>这是一个自定义的更新查询，用于批量软删除文档。
     * 软删除不会物理删除数据，而是设置deletedAt字段来标记删除状态。</p>
     *
     * @param documentIds 要删除的文档ID列表
     * @param deletedBy   删除操作的用户ID
     * @param deletedAt   删除时间
     * @return 受影响的行数
     */
    @Modifying
    @Query("UPDATE KbDocumentEntity d SET d.deletedAt = :deletedAt, d.updatedBy = :deletedBy " +
            "WHERE d.id IN :documentIds AND d.deletedAt IS NULL")
    int batchSoftDelete(@Param("documentIds") List<Long> documentIds,
                        @Param("deletedBy") Long deletedBy,
                        @Param("deletedAt") OffsetDateTime deletedAt);

    /**
     * 查询需要重新解析的文档
     *
     * <p>这个查询用于找出解析状态为待处理或失败的文档，
     * 以便批处理任务可以重新尝试解析这些文档。</p>
     *
     * @param statusCodes 状态代码列表（如待处理、失败）
     *                    // @param limit       最大返回数量
     * @return 需要重新解析的文档列表
     */
    @Query("SELECT d FROM KbDocumentEntity d WHERE d.parsingStatus IN :statusCodes " +
            "AND d.deletedAt IS NULL ORDER BY d.createdAt ASC")
    List<KbDocumentEntity> findDocumentsNeedingReprocessing(@Param("statusCodes") List<Integer> statusCodes,
                                                            Pageable pageable);
}