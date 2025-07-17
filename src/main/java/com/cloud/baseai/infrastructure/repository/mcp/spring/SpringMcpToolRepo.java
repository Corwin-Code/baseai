package com.cloud.baseai.infrastructure.repository.mcp.spring;

import com.cloud.baseai.domain.mcp.model.ToolType;
import com.cloud.baseai.infrastructure.persistence.mcp.entity.McpToolEntity;
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
 * <h2>工具Spring Data JPA仓储</h2>
 *
 * <p>提供工具数据的基础CRUD操作和特定查询方法。
 * Spring Data JPA会自动实现这些方法，大大减少了样板代码。</p>
 */
@Repository
public interface SpringMcpToolRepo extends JpaRepository<McpToolEntity, Long> {

    /**
     * 根据代码查找工具（未删除）
     */
    Optional<McpToolEntity> findByCodeAndDeletedAtIsNull(String code);

    /**
     * 检查代码是否存在（未删除）
     */
    boolean existsByCodeAndDeletedAtIsNull(String code);

    /**
     * 根据拥有者查找工具
     */
    Page<McpToolEntity> findByOwnerIdAndDeletedAtIsNull(Long ownerId, Pageable pageable);

    /**
     * 统计启用状态的工具数量
     */
    long countByEnabledAndDeletedAtIsNull(boolean enabled);

    /**
     * 根据类型和启用状态查询工具
     */
    @Query("SELECT t FROM McpToolEntity t WHERE " +
            "(:type IS NULL OR t.type = :type) AND " +
            "(:enabled IS NULL OR t.enabled = :enabled) AND " +
            "t.deletedAt IS NULL")
    Page<McpToolEntity> findByTypeAndEnabled(@Param("type") ToolType type,
                                             @Param("enabled") Boolean enabled,
                                             Pageable pageable);

    /**
     * 统计符合条件的工具数量
     */
    @Query("SELECT COUNT(t) FROM McpToolEntity t WHERE " +
            "(:type IS NULL OR t.type = :type) AND " +
            "(:enabled IS NULL OR t.enabled = :enabled) AND " +
            "t.deletedAt IS NULL")
    long countByTypeAndEnabled(@Param("type") ToolType type,
                               @Param("enabled") Boolean enabled);

    /**
     * 查找所有未删除的工具
     */
    Page<McpToolEntity> findByDeletedAtIsNull(Pageable pageable);

    /**
     * 统计未删除的工具总数
     */
    long countByDeletedAtIsNull();

    /**
     * 根据ID列表批量查询
     */
    @Query("SELECT t FROM McpToolEntity t WHERE t.id IN :ids AND t.deletedAt IS NULL")
    List<McpToolEntity> findByIdInAndDeletedAtIsNull(@Param("ids") List<Long> ids);

    /**
     * 软删除工具
     */
    @Modifying
    @Query("UPDATE McpToolEntity t SET t.deletedAt = :deletedAt, t.createdBy = :deletedBy " +
            "WHERE t.id = :id AND t.deletedAt IS NULL")
    int softDeleteById(@Param("id") Long id,
                       @Param("deletedAt") OffsetDateTime deletedAt,
                       @Param("deletedBy") Long deletedBy);

    /**
     * 根据名称模糊查询工具
     */
    Page<McpToolEntity> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name, Pageable pageable);
}