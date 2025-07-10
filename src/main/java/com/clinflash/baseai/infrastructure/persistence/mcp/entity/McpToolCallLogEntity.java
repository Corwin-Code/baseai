package com.clinflash.baseai.infrastructure.persistence.mcp.entity;

import com.clinflash.baseai.domain.mcp.model.ToolCallLog;
import com.clinflash.baseai.infrastructure.persistence.mcp.entity.enums.ToolCallStatus;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * <h2>工具调用日志JPA实体类</h2>
 */
@Setter
@Getter
@Entity
@Table(name = "mcp_tool_call_logs")
public class McpToolCallLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tool_id", nullable = false)
    private Long toolId;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "thread_id")
    private Long threadId;

    @Column(name = "flow_run_id")
    private Long flowRunId;

    @Type(JsonType.class)
    @Column(name = "params", columnDefinition = "jsonb")
    private Map<String, Object> params;

    @Type(JsonType.class)
    @Column(name = "result", columnDefinition = "jsonb")
    private Map<String, Object> result;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ToolCallStatus status = ToolCallStatus.STARTED;

    @Column(name = "error_msg", columnDefinition = "TEXT")
    private String errorMsg;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    // =================== 构造函数 ===================

    public McpToolCallLogEntity() {
    }

    // =================== 领域对象转换 ===================

    public static McpToolCallLogEntity fromDomain(ToolCallLog log) {
        if (log == null) {
            return null;
        }

        McpToolCallLogEntity entity = new McpToolCallLogEntity();
        entity.id = log.id();
        entity.toolId = log.toolId();
        entity.tenantId = log.tenantId();
        entity.userId = log.userId();
        entity.threadId = log.threadId();
        entity.flowRunId = log.flowRunId();
        entity.params = log.params();
        entity.result = log.result();
        entity.status = ToolCallStatus.fromString(log.status());
        entity.errorMsg = log.errorMsg();
        entity.latencyMs = log.latencyMs();
        entity.createdAt = log.createdAt();

        return entity;
    }

    public ToolCallLog toDomain() {
        return new ToolCallLog(
                id,
                toolId,
                tenantId,
                userId,
                threadId,
                flowRunId,
                params,
                result,
                status != null ? status.name() : null,
                errorMsg,
                latencyMs,
                createdAt
        );
    }

    public void updateFromDomain(ToolCallLog log) {
        if (log == null) {
            return;
        }

        this.result = log.result();
        this.status = ToolCallStatus.fromString(log.status());
        this.errorMsg = log.errorMsg();
        this.latencyMs = log.latencyMs();
    }
}