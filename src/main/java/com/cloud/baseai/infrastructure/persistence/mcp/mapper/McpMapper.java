package com.cloud.baseai.infrastructure.persistence.mcp.mapper;

import com.cloud.baseai.domain.mcp.model.Tool;
import com.cloud.baseai.domain.mcp.model.ToolAuth;
import com.cloud.baseai.domain.mcp.model.ToolCallLog;
import com.cloud.baseai.infrastructure.persistence.mcp.entity.McpToolCallLogEntity;
import com.cloud.baseai.infrastructure.persistence.mcp.entity.McpToolEntity;
import com.cloud.baseai.infrastructure.persistence.mcp.entity.McpToolTenantAuthEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <h2>MCP模块映射器</h2>
 *
 * <p>负责MCP领域对象与JPA实体之间的转换映射。这个映射器采用了
 * "双向转换"的设计，确保领域层和持久化层之间的数据一致性。</p>
 *
 * <p><b>映射原则：</b></p>
 * <p>我们严格遵循了"防腐层"的概念，确保领域对象的纯净性不被
 * 持久化技术细节污染。所有的转换逻辑都封装在这个映射器中。</p>
 */
@Component
public class McpMapper {

    // =================== Tool 映射 ===================

    /**
     * 领域对象转换为实体
     */
    public McpToolEntity toEntity(Tool domain) {
        return McpToolEntity.fromDomain(domain);
    }

    /**
     * 实体转换为领域对象
     */
    public Tool toDomain(McpToolEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    /**
     * 领域对象列表转换为实体列表
     */
    public List<McpToolEntity> toToolEntityList(List<Tool> domains) {
        if (domains == null) return null;
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * 实体列表转换为领域对象列表
     */
    public List<Tool> toToolDomainList(List<McpToolEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== ToolAuth 映射 ===================

    /**
     * 工具授权领域对象转换为实体
     */
    public McpToolTenantAuthEntity toEntity(ToolAuth domain) {
        return McpToolTenantAuthEntity.fromDomain(domain);
    }

    /**
     * 工具授权实体转换为领域对象
     */
    public ToolAuth toDomain(McpToolTenantAuthEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    /**
     * 工具授权领域对象列表转换为实体列表
     */
    public List<McpToolTenantAuthEntity> toAuthEntityList(List<ToolAuth> domains) {
        if (domains == null) return null;
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * 工具授权实体列表转换为领域对象列表
     */
    public List<ToolAuth> toAuthDomainList(List<McpToolTenantAuthEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== ToolCallLog 映射 ===================

    /**
     * 调用日志领域对象转换为实体
     */
    public McpToolCallLogEntity toEntity(ToolCallLog domain) {
        return McpToolCallLogEntity.fromDomain(domain);
    }

    /**
     * 调用日志实体转换为领域对象
     */
    public ToolCallLog toDomain(McpToolCallLogEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    /**
     * 调用日志领域对象列表转换为实体列表
     */
    public List<McpToolCallLogEntity> toLogEntityList(List<ToolCallLog> domains) {
        if (domains == null) return null;
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * 调用日志实体列表转换为领域对象列表
     */
    public List<ToolCallLog> toLogDomainList(List<McpToolCallLogEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== 辅助方法 ===================

    /**
     * 安全地复制字符串，防止空指针
     */
    public String copyString(String source) {
        return source;
    }

    /**
     * 验证实体的完整性
     */
    public boolean validateEntity(Object entity) {
        return entity != null;
    }

    /**
     * 验证领域对象的完整性
     */
    public boolean validateDomain(Object domain) {
        return domain != null;
    }

    /**
     * 清理JSON字符串，确保格式正确
     */
    public String cleanJsonString(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return json.trim();
    }

    /**
     * 安全地截断字符串到指定长度
     */
    public String truncateString(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength);
    }
}