package com.clinflash.baseai.infrastructure.persistence.mcp.entity;

import com.clinflash.baseai.domain.mcp.model.ToolAuth;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * <h2>工具租户授权JPA实体类</h2>
 */
@Setter
@Getter
@Entity
@Table(name = "mcp_tool_tenant_auth")
@IdClass(McpToolTenantAuthEntityId.class)
public class McpToolTenantAuthEntity {

    @Id
    @Column(name = "tool_id")
    private Long toolId;

    @Id
    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "api_key", length = 128)
    private String apiKey;

    @Column(name = "quota_limit")
    private Integer quotaLimit;

    @Column(name = "quota_used")
    private Integer quotaUsed = 0;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "granted_at")
    private OffsetDateTime grantedAt;

    // =================== 构造函数 ===================

    public McpToolTenantAuthEntity() {
    }

    // =================== 领域对象转换 ===================

    public static McpToolTenantAuthEntity fromDomain(ToolAuth auth) {
        if (auth == null) {
            return null;
        }

        McpToolTenantAuthEntity entity = new McpToolTenantAuthEntity();
        entity.toolId = auth.toolId();
        entity.tenantId = auth.tenantId();
        entity.apiKey = auth.apiKey();
        entity.quotaLimit = auth.quotaLimit();
        entity.quotaUsed = auth.quotaUsed();
        entity.enabled = auth.enabled();
        entity.grantedAt = auth.grantedAt();

        return entity;
    }

    public ToolAuth toDomain() {
        return new ToolAuth(
                toolId,
                tenantId,
                apiKey,
                quotaLimit,
                quotaUsed,
                enabled,
                grantedAt
        );
    }

    public void updateFromDomain(ToolAuth auth) {
        if (auth == null) {
            return;
        }

        this.apiKey = auth.apiKey();
        this.quotaLimit = auth.quotaLimit();
        this.quotaUsed = auth.quotaUsed();
        this.enabled = auth.enabled();
    }
}