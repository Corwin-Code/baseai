package com.cloud.baseai.infrastructure.repository.flow.spring;

import com.cloud.baseai.infrastructure.persistence.flow.entity.FlowProjectEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * <h2>流程项目Spring Data JPA仓储</h2>
 */
@Repository
public interface SpringFlowProjectRepo extends JpaRepository<FlowProjectEntity, Long> {

    boolean existsByTenantIdAndNameAndDeletedAtIsNull(Long tenantId, String name);

    Page<FlowProjectEntity> findByTenantIdAndDeletedAtIsNull(Long tenantId, Pageable pageable);

    long countByTenantIdAndDeletedAtIsNull(Long tenantId);

    Page<FlowProjectEntity> findByTenantIdAndNameContainingIgnoreCaseAndDeletedAtIsNull(
            Long tenantId, String name, Pageable pageable);

    long countByTenantIdAndNameContainingIgnoreCaseAndDeletedAtIsNull(Long tenantId, String name);
}