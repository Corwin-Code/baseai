package com.clinflash.baseai.infrastructure.persistence.audit.entity;

import com.clinflash.baseai.domain.audit.model.SysAuditLog;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * 系统审计操作日志JPA实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "sys_audit_logs")
public class SysAuditLogEntity {

    /**
     * 日志 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 操作用户ID
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 所属租户ID
     */
    @Column(name = "tenant_id")
    private Long tenantId;

    /**
     * 操作类型（LOGIN / UPDATE / DELETE …）
     */
    @Column(length = 64, nullable = false)
    private String action;

    /**
     * 目标对象类型（User / Document …）
     */
    @Column(name = "target_type", length = 64)
    private String targetType;

    /**
     * 目标对象主键 ID
     */
    @Column(name = "target_id")
    private Long targetId;

    /**
     * 操作 IP 地址
     */
    @Column(name = "ip_address")
    private String ipAddress;

    /**
     * 客户端 User-Agent
     */
    @Column(name = "user_agent", length = 256)
    private String userAgent;

    /**
     * 详细内容（JSON 格式存储）
     */
    @Column(name = "detail", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String detail;

    /**
     * 操作结果（success / fail / partial）
     */
    @Column(name = "result_status", length = 16)
    private String resultStatus;

    /**
     * 日志等级（info / warn / error）
     */
    @Column(name = "log_level", length = 16)
    private String logLevel;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    /**
     * 从领域对象创建实体
     */
    public static SysAuditLogEntity fromDomain(SysAuditLog domain) {
        if (domain == null) return null;

        SysAuditLogEntity entity = new SysAuditLogEntity();
        entity.setId(domain.id());
        entity.setTenantId(domain.tenantId());
        entity.setUserId(domain.userId());
        entity.setAction(domain.action());
        entity.setTargetType(domain.targetType());
        entity.setTargetId(domain.targetId());
        entity.setIpAddress(domain.ipAddress());
        entity.setUserAgent(domain.userAgent());
        entity.setDetail(domain.detail());
        entity.setResultStatus(domain.resultStatus());
        entity.setLogLevel(domain.logLevel());
        entity.setCreatedAt(domain.createdAt());

        return entity;
    }

    /**
     * 转换为领域对象
     */
    public SysAuditLog toDomain() {
        return new SysAuditLog(
                this.id,
                this.tenantId,
                this.userId,
                this.action,
                this.targetType,
                this.targetId,
                this.ipAddress,
                this.userAgent,
                this.detail,
                this.resultStatus,
                this.logLevel,
                this.createdAt

        );
    }

    /**
     * 从领域对象更新实体
     */
    public void updateFromDomain(SysAuditLog domain) {
        if (domain == null) return;

        this.setTenantId(domain.tenantId());
        this.setUserId(domain.userId());
        this.setAction(domain.action());
        this.setTargetType(domain.targetType());
        this.setTargetId(domain.targetId());
        this.setIpAddress(domain.ipAddress());
        this.setUserAgent(domain.userAgent());
        this.setDetail(domain.detail());
        this.setResultStatus(domain.resultStatus());
        this.setLogLevel(domain.logLevel());
        this.setCreatedAt(domain.createdAt());
    }
}