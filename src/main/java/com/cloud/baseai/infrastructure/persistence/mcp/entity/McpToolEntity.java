package com.cloud.baseai.infrastructure.persistence.mcp.entity;

import com.cloud.baseai.domain.mcp.model.Tool;
import com.cloud.baseai.domain.mcp.model.ToolType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * <h2>工具JPA实体类</h2>
 *
 * <p>工具领域对象的持久化表示。这个实体类负责将领域模型映射到
 * 关系数据库表，并提供与JPA框架的集成点。</p>
 *
 * <p><b>映射策略：</b></p>
 * <p>我们采用了"富实体"的设计，让实体类包含与领域对象的转换逻辑。
 * 这样既保持了层次的清晰，又避免了过多的映射代码。</p>
 */
@Setter
@Getter
@Entity
@Table(name = "mcp_tools")
public class McpToolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private ToolType type;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url", length = 256)
    private String iconUrl;

    @Column(name = "param_schema", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String paramSchema;

    @Column(name = "result_schema", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String resultSchema;

    @Column(name = "endpoint", length = 256)
    private String endpoint;

    @Column(name = "auth_type", length = 32)
    private String authType;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // =================== 构造函数 ===================

    public McpToolEntity() {
    }

    // =================== 领域对象转换 ===================

    /**
     * 从领域对象创建实体
     */
    public static McpToolEntity fromDomain(Tool tool) {
        if (tool == null) {
            return null;
        }

        McpToolEntity entity = new McpToolEntity();
        entity.id = tool.id();
        entity.code = tool.code();
        entity.name = tool.name();
        entity.type = tool.type();
        entity.description = tool.description();
        entity.iconUrl = tool.iconUrl();
        entity.paramSchema = tool.paramSchema();
        entity.resultSchema = tool.resultSchema();
        entity.endpoint = tool.endpoint();
        entity.authType = tool.authType();
        entity.enabled = tool.enabled();
        entity.ownerId = tool.ownerId();
        entity.createdBy = tool.createdBy();
        entity.createdAt = tool.createdAt();
        entity.deletedAt = tool.deletedAt();

        return entity;
    }

    /**
     * 转换为领域对象
     */
    public Tool toDomain() {
        return new Tool(
                id,
                code,
                name,
                type,
                description,
                iconUrl,
                paramSchema,
                resultSchema,
                endpoint,
                authType,
                enabled,
                ownerId,
                createdBy,
                createdAt,
                deletedAt
        );
    }

    /**
     * 从领域对象更新实体状态
     */
    public void updateFromDomain(Tool tool) {
        if (tool == null) {
            return;
        }

        // ID和创建时间不可更改
        this.code = tool.code();
        this.name = tool.name();
        this.type = tool.type();
        this.description = tool.description();
        this.iconUrl = tool.iconUrl();
        this.paramSchema = tool.paramSchema();
        this.resultSchema = tool.resultSchema();
        this.endpoint = tool.endpoint();
        this.authType = tool.authType();
        this.enabled = tool.enabled();
        this.ownerId = tool.ownerId();
        this.createdBy = tool.createdBy();
        this.deletedAt = tool.deletedAt();
    }
}