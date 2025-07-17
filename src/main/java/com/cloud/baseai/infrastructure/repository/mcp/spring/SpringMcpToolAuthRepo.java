package com.cloud.baseai.infrastructure.repository.mcp.spring;

import com.cloud.baseai.infrastructure.persistence.mcp.entity.McpToolTenantAuthEntity;
import com.cloud.baseai.infrastructure.persistence.mcp.entity.McpToolTenantAuthEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>工具租户授权Spring Data JPA仓储</h2>
 */
@Repository
public interface SpringMcpToolAuthRepo extends JpaRepository<McpToolTenantAuthEntity, McpToolTenantAuthEntityId> {

    /**
     * 根据工具ID和租户ID查找授权
     */
    Optional<McpToolTenantAuthEntity> findByToolIdAndTenantId(Long toolId, Long tenantId);

    /**
     * 根据租户ID查找所有授权
     */
    List<McpToolTenantAuthEntity> findByTenantId(Long tenantId);

    /**
     * 根据工具ID查找所有授权
     */
    List<McpToolTenantAuthEntity> findByToolId(Long toolId);

    /**
     * 统计工具的授权租户数量
     */
    long countByToolId(Long toolId);

    /**
     * 删除指定的授权
     */
    void deleteByToolIdAndTenantId(Long toolId, Long tenantId);

    /**
     * 批量删除工具的所有授权
     */
    @Modifying
    @Query("DELETE FROM McpToolTenantAuthEntity a WHERE a.toolId = :toolId")
    void deleteByToolId(@Param("toolId") Long toolId);

    /**
     * 批量删除租户的所有授权
     */
    @Modifying
    @Query("DELETE FROM McpToolTenantAuthEntity a WHERE a.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 根据租户ID查找启用的授权
     */
    List<McpToolTenantAuthEntity> findByTenantIdAndEnabledTrue(Long tenantId);

    /**
     * 根据工具ID查找启用的授权
     */
    List<McpToolTenantAuthEntity> findByToolIdAndEnabledTrue(Long toolId);
}