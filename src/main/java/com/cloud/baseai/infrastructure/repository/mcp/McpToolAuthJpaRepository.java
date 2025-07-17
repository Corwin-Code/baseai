package com.cloud.baseai.infrastructure.repository.mcp;

import com.cloud.baseai.domain.mcp.model.ToolAuth;
import com.cloud.baseai.domain.mcp.repository.ToolAuthRepository;
import com.cloud.baseai.infrastructure.persistence.mcp.entity.McpToolTenantAuthEntity;
import com.cloud.baseai.infrastructure.persistence.mcp.mapper.McpMapper;
import com.cloud.baseai.infrastructure.repository.mcp.spring.SpringMcpToolAuthRepo;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * <h2>工具授权仓储JPA实现</h2>
 *
 * <p>工具授权仓储接口的JPA实现，专门负责租户工具授权关系的数据管理。
 * 这个实现确保了授权数据的一致性和完整性，是MCP权限控制的数据基础。</p>
 *
 * <p><b>事务管理：</b></p>
 * <p>对于涉及多表操作的方法，我们使用了事务注解来确保数据一致性。
 * 特别是在删除操作中，需要保证相关联的数据能够同步更新。</p>
 */
@Repository
public class McpToolAuthJpaRepository implements ToolAuthRepository {

    private final SpringMcpToolAuthRepo springRepo;
    private final McpMapper mapper;

    public McpToolAuthJpaRepository(SpringMcpToolAuthRepo springRepo, McpMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public ToolAuth save(ToolAuth toolAuth) {
        McpToolTenantAuthEntity entity;

        // 尝试查找现有的授权记录
        Optional<McpToolTenantAuthEntity> existingOpt = springRepo.findByToolIdAndTenantId(
                toolAuth.toolId(), toolAuth.tenantId());

        if (existingOpt.isPresent()) {
            // 更新现有授权
            entity = existingOpt.get();
            entity.updateFromDomain(toolAuth);
        } else {
            // 创建新授权
            entity = mapper.toEntity(toolAuth);
        }

        McpToolTenantAuthEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ToolAuth> findByToolIdAndTenantId(Long toolId, Long tenantId) {
        return springRepo.findByToolIdAndTenantId(toolId, tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ToolAuth> findByTenantId(Long tenantId) {
        List<McpToolTenantAuthEntity> entities = springRepo.findByTenantId(tenantId);
        return mapper.toAuthDomainList(entities);
    }

    @Override
    public List<ToolAuth> findByToolId(Long toolId) {
        List<McpToolTenantAuthEntity> entities = springRepo.findByToolId(toolId);
        return mapper.toAuthDomainList(entities);
    }

    @Override
    public int countByToolId(Long toolId) {
        return (int) springRepo.countByToolId(toolId);
    }

    @Override
    @Transactional
    public void delete(Long toolId, Long tenantId) {
        springRepo.deleteByToolIdAndTenantId(toolId, tenantId);
    }

    @Override
    @Transactional
    public void deleteByToolId(Long toolId) {
        springRepo.deleteByToolId(toolId);
    }

    @Override
    @Transactional
    public void deleteByTenantId(Long tenantId) {
        springRepo.deleteByTenantId(tenantId);
    }

    /**
     * 获取租户的启用授权列表
     *
     * <p>这个方法专门用于查询租户当前有效的工具授权，
     * 在工具执行前的权限检查中会用到。</p>
     */
    public List<ToolAuth> findEnabledByTenantId(Long tenantId) {
        List<McpToolTenantAuthEntity> entities = springRepo.findByTenantIdAndEnabledTrue(tenantId);
        return mapper.toAuthDomainList(entities);
    }

    /**
     * 获取工具的启用授权列表
     */
    public List<ToolAuth> findEnabledByToolId(Long toolId) {
        List<McpToolTenantAuthEntity> entities = springRepo.findByToolIdAndEnabledTrue(toolId);
        return mapper.toAuthDomainList(entities);
    }

    /**
     * 批量更新配额使用量
     *
     * <p>当需要批量重置或调整配额时使用。比如在月初重置所有租户的配额使用量。</p>
     */
    @Transactional
    public void batchResetQuota(Long toolId) {
        List<McpToolTenantAuthEntity> entities = springRepo.findByToolId(toolId);
        entities.forEach(entity -> {
            entity.setQuotaUsed(0);
        });
        springRepo.saveAll(entities);
    }
}