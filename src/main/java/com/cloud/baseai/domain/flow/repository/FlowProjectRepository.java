package com.cloud.baseai.domain.flow.repository;

import com.cloud.baseai.domain.flow.model.FlowProject;

import java.util.List;
import java.util.Optional;

/**
 * <h2>流程项目仓储接口</h2>
 *
 * <p>定义流程项目的数据访问抽象，遵循DDD仓储模式。
 * 这个接口专注于业务语义，而不涉及具体的数据存储技术。</p>
 */
public interface FlowProjectRepository {

    /**
     * 保存流程项目
     */
    FlowProject save(FlowProject project);

    /**
     * 根据ID查找项目
     */
    Optional<FlowProject> findById(Long id);

    /**
     * 检查租户下是否存在同名项目
     */
    boolean existsByTenantIdAndName(Long tenantId, String name);

    /**
     * 分页查询租户下的项目
     */
    List<FlowProject> findByTenantId(Long tenantId, int page, int size);

    /**
     * 统计租户下的项目总数
     */
    long countByTenantId(Long tenantId);

    /**
     * 按名称搜索项目
     */
    List<FlowProject> searchByName(Long tenantId, String name, int page, int size);

    /**
     * 统计名称包含关键词的项目数量
     */
    long countByTenantIdAndNameContaining(Long tenantId, String name);

    /**
     * 软删除项目
     */
    boolean softDelete(Long id, Long deletedBy);
}