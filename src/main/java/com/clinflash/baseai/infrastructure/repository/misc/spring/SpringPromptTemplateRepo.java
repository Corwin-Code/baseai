package com.clinflash.baseai.infrastructure.repository.misc.spring;

import com.clinflash.baseai.infrastructure.persistence.misc.entity.PromptTemplateEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>提示词模板Spring Data JPA仓储</h2>
 *
 * <p>这里的查询方法遵循Spring Data的命名约定，同时也包含一些自定义的JPQL查询。</p>
 */
@Repository
public interface SpringPromptTemplateRepo extends JpaRepository<PromptTemplateEntity, Long> {

    /**
     * 查找租户和系统可见的模板
     *
     * <p>这个查询体现了多租户架构的核心逻辑：每个租户可以看到自己的模板和系统模板。
     * 使用JPQL而不是原生SQL，保证了数据库无关性。</p>
     */
    @Query("SELECT t FROM PromptTemplateEntity t WHERE " +
            "(t.tenantId = :tenantId OR t.isSystem = true) AND t.deletedAt IS NULL " +
            "ORDER BY t.createdAt DESC")
    List<PromptTemplateEntity> findVisibleTemplates(@Param("tenantId") Long tenantId, Pageable pageable);

    /**
     * 统计可见模板数量
     */
    @Query("SELECT COUNT(t) FROM PromptTemplateEntity t WHERE " +
            "(t.tenantId = :tenantId OR t.isSystem = true) AND t.deletedAt IS NULL")
    long countVisibleTemplates(@Param("tenantId") Long tenantId);

    /**
     * 按租户和名称查找
     */
    Optional<PromptTemplateEntity> findByTenantIdAndNameAndDeletedAtIsNull(Long tenantId, String name);

    /**
     * 搜索模板
     */
    @Query("SELECT t FROM PromptTemplateEntity t WHERE " +
            "(t.tenantId = :tenantId OR t.isSystem = true) AND t.deletedAt IS NULL " +
            "AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(t.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY t.createdAt DESC")
    List<PromptTemplateEntity> searchTemplates(@Param("tenantId") Long tenantId,
                                               @Param("keyword") String keyword,
                                               Pageable pageable);

    /**
     * 按模型代码查找
     */
    @Query("SELECT t FROM PromptTemplateEntity t WHERE " +
            "(t.tenantId = :tenantId OR t.isSystem = true) AND t.deletedAt IS NULL " +
            "AND t.modelCode = :modelCode ORDER BY t.createdAt DESC")
    List<PromptTemplateEntity> findByModelCode(@Param("tenantId") Long tenantId,
                                               @Param("modelCode") String modelCode,
                                               Pageable pageable);

    /**
     * 查找模板的所有版本
     */
    @Query("SELECT t FROM PromptTemplateEntity t WHERE " +
            "t.tenantId = :tenantId AND t.name = :name AND t.deletedAt IS NULL " +
            "ORDER BY t.version DESC")
    List<PromptTemplateEntity> findVersions(@Param("tenantId") Long tenantId, @Param("name") String name);

    /**
     * 检查名称是否存在
     */
    boolean existsByTenantIdAndNameAndDeletedAtIsNull(Long tenantId, String name);
}