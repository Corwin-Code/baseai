package com.clinflash.baseai.infrastructure.repository.user.spring;

import com.clinflash.baseai.infrastructure.persistence.user.entity.SysTenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringSysTenantRepo extends JpaRepository<SysTenantEntity, Long> {

    Optional<SysTenantEntity> findByOrgNameAndDeletedAtIsNull(String orgName);

    boolean existsByOrgNameAndDeletedAtIsNull(String orgName);

    List<SysTenantEntity> findByIdInAndDeletedAtIsNull(List<Long> ids);

    List<SysTenantEntity> findByDeletedAtIsNull();

    @Query("SELECT t FROM SysTenantEntity t WHERE t.deletedAt IS NULL " +
            "AND t.expireAt IS NOT NULL AND t.expireAt < :beforeDate")
    List<SysTenantEntity> findExpiringBefore(@Param("beforeDate") OffsetDateTime beforeDate);

    List<SysTenantEntity> findByPlanCodeAndDeletedAtIsNull(String planCode);

    long countByDeletedAtIsNull();

    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    @Modifying
    @Query("UPDATE SysTenantEntity t SET t.deletedAt = :deletedAt WHERE t.id = :id AND t.deletedAt IS NULL")
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") OffsetDateTime deletedAt);
}