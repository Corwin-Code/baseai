package com.cloud.baseai.infrastructure.repository.flow.spring;

import com.cloud.baseai.domain.flow.model.FlowStatus;
import com.cloud.baseai.infrastructure.persistence.flow.entity.FlowDefinitionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <h2>流程定义Spring Data JPA仓储</h2>
 */
@Repository
public interface SpringFlowDefinitionRepo extends JpaRepository<FlowDefinitionEntity, Long> {

    boolean existsByProjectIdAndNameAndDeletedAtIsNull(Long projectId, String name);

    Page<FlowDefinitionEntity> findByProjectIdAndDeletedAtIsNull(Long projectId, Pageable pageable);

    long countByProjectIdAndDeletedAtIsNull(Long projectId);

    Page<FlowDefinitionEntity> findByProjectIdAndStatusAndDeletedAtIsNull(
            Long projectId, FlowStatus status, Pageable pageable);

    long countByProjectIdAndStatusAndDeletedAtIsNull(Long projectId, FlowStatus status);

    List<FlowDefinitionEntity> findByProjectIdAndNameAndDeletedAtIsNullOrderByVersionDesc(
            Long projectId, String name);

    @Query("SELECT COUNT(d) FROM FlowDefinitionEntity d " +
            "JOIN FlowProjectEntity p ON d.projectId = p.id " +
            "WHERE p.tenantId = :tenantId AND d.deletedAt IS NULL")
    int countByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(d) FROM FlowDefinitionEntity d " +
            "JOIN FlowProjectEntity p ON d.projectId = p.id " +
            "WHERE p.tenantId = :tenantId AND d.status = :status AND d.deletedAt IS NULL")
    int countByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") Integer status);
}