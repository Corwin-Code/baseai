package com.clinflash.baseai.domain.mcp.repository;

import com.clinflash.baseai.domain.mcp.model.ToolAuth;

import java.util.List;
import java.util.Optional;

/**
 * <h2>工具授权仓储接口</h2>
 *
 * <p>管理租户对工具的访问授权数据。这个仓储接口专注于授权关系的
 * 增删改查，为权限管理提供了数据层面的支持。</p>
 */
public interface ToolAuthRepository {

    /**
     * 保存工具授权
     */
    ToolAuth save(ToolAuth toolAuth);

    /**
     * 根据工具ID和租户ID查找授权
     */
    Optional<ToolAuth> findByToolIdAndTenantId(Long toolId, Long tenantId);

    /**
     * 根据租户ID查找所有授权
     */
    List<ToolAuth> findByTenantId(Long tenantId);

    /**
     * 根据工具ID查找所有授权
     */
    List<ToolAuth> findByToolId(Long toolId);

    /**
     * 统计工具的授权租户数量
     */
    int countByToolId(Long toolId);

    /**
     * 删除授权
     */
    void delete(Long toolId, Long tenantId);

    /**
     * 批量删除工具的所有授权
     */
    void deleteByToolId(Long toolId);

    /**
     * 批量删除租户的所有授权
     */
    void deleteByTenantId(Long tenantId);
}