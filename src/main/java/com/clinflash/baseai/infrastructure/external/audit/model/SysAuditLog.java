package com.clinflash.baseai.infrastructure.external.audit.model;

import com.clinflash.baseai.infrastructure.persistence.user.entity.SysTenantEntity;
import com.clinflash.baseai.infrastructure.persistence.user.entity.SysUserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * 系统审计操作日志表，对应 <b>sys_audit_logs</b>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "sys_audit_logs")
public class SysAuditLog {

    /**
     * 日志 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 操作用户（可空，使用惰性加载）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private SysUserEntity user;

    /**
     * 所属租户（可空）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private SysTenantEntity tenant;

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
    @Lob
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
}
