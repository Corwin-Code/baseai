package com.cloud.baseai.infrastructure.repository.mcp;

import com.cloud.baseai.domain.mcp.model.Tool;
import com.cloud.baseai.domain.mcp.model.ToolType;
import com.cloud.baseai.domain.mcp.repository.ToolRepository;
import com.cloud.baseai.infrastructure.persistence.mcp.entity.McpToolEntity;
import com.cloud.baseai.infrastructure.persistence.mcp.mapper.McpMapper;
import com.cloud.baseai.infrastructure.repository.mcp.spring.SpringMcpToolRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>工具仓储JPA实现</h2>
 *
 * <p>工具仓储接口的JPA实现，负责工具数据的持久化操作。
 * 作为基础设施层的组件，它隐藏了JPA的技术细节，为领域层
 * 提供了简洁的数据访问接口。</p>
 *
 * <p><b>实现策略：</b></p>
 * <p>我们采用了"适配器模式"，将Spring Data JPA的接口适配为
 * 领域层期望的仓储接口。这种设计让领域层完全不依赖具体的持久化技术。</p>
 */
@Repository
public class McpToolJpaRepository implements ToolRepository {

    private final SpringMcpToolRepo springRepo;
    private final McpMapper mapper;

    public McpToolJpaRepository(SpringMcpToolRepo springRepo, McpMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public Tool save(Tool tool) {
        McpToolEntity entity;

        if (tool.id() == null) {
            // 新建工具
            entity = mapper.toEntity(tool);
        } else {
            // 更新现有工具
            entity = springRepo.findById(tool.id())
                    .orElse(mapper.toEntity(tool));
            entity.updateFromDomain(tool);
        }

        McpToolEntity saved = springRepo.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Tool> findById(Long id) {
        return springRepo.findById(id)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Tool> findByCode(String code) {
        return springRepo.findByCodeAndDeletedAtIsNull(code)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return springRepo.existsByCodeAndDeletedAtIsNull(code);
    }

    @Override
    public List<Tool> findByIds(List<Long> ids) {
        List<McpToolEntity> entities = springRepo.findByIdInAndDeletedAtIsNull(ids);
        return mapper.toToolDomainList(entities);
    }

    @Override
    public List<Tool> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<McpToolEntity> entityPage = springRepo.findByDeletedAtIsNull(pageable);
        return mapper.toToolDomainList(entityPage.getContent());
    }

    @Override
    public long count() {
        return springRepo.countByDeletedAtIsNull();
    }

    @Override
    public List<Tool> findByTypeAndEnabled(String type, Boolean enabled, int page, int size) {
        ToolType toolType = type != null ? ToolType.valueOf(type.toUpperCase()) : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<McpToolEntity> entityPage = springRepo.findByTypeAndEnabled(toolType, enabled, pageable);
        return mapper.toToolDomainList(entityPage.getContent());
    }

    @Override
    public long countByTypeAndEnabled(String type, Boolean enabled) {
        ToolType toolType = type != null ? ToolType.valueOf(type.toUpperCase()) : null;
        return springRepo.countByTypeAndEnabled(toolType, enabled);
    }

    @Override
    public int countByEnabled(boolean enabled) {
        return (int) springRepo.countByEnabledAndDeletedAtIsNull(enabled);
    }

    @Override
    public List<Tool> findByOwner(Long ownerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<McpToolEntity> entityPage = springRepo.findByOwnerIdAndDeletedAtIsNull(ownerId, pageable);
        return mapper.toToolDomainList(entityPage.getContent());
    }

    @Override
    public boolean softDelete(Long id, Long deletedBy) {
        int affected = springRepo.softDeleteById(id, OffsetDateTime.now(), deletedBy);
        return affected > 0;
    }
}