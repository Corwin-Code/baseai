package com.clinflash.baseai.infrastructure.persistence.mcp.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * <h2>工具租户授权复合主键</h2>
 */
@Setter
@Getter
public class McpToolTenantAuthEntityId implements Serializable {

    private Long toolId;
    private Long tenantId;

    public McpToolTenantAuthEntityId() {
    }

    public McpToolTenantAuthEntityId(Long toolId, Long tenantId) {
        this.toolId = toolId;
        this.tenantId = tenantId;
    }

    // =================== equals和hashCode ===================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        McpToolTenantAuthEntityId that = (McpToolTenantAuthEntityId) o;
        return Objects.equals(toolId, that.toolId) && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toolId, tenantId);
    }
}