package com.cloud.baseai.infrastructure.persistence.flow.mapper;

import com.cloud.baseai.domain.flow.model.*;
import com.cloud.baseai.infrastructure.persistence.flow.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <h2>流程模块映射器</h2>
 *
 * <p>负责流程领域对象与JPA实体之间的转换映射。
 * 确保领域层和基础设施层之间的数据转换一致性和可维护性。</p>
 */
@Component
public class FlowMapper {

    // =================== FlowProject 映射 ===================

    public FlowProjectEntity toEntity(FlowProject domain) {
        return FlowProjectEntity.fromDomain(domain);
    }

    public FlowProject toDomain(FlowProjectEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    public List<FlowProjectEntity> toProjectEntityList(List<FlowProject> domains) {
        if (domains == null) return null;
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public List<FlowProject> toProjectDomainList(List<FlowProjectEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== FlowDefinition 映射 ===================

    public FlowDefinitionEntity toEntity(FlowDefinition domain) {
        return FlowDefinitionEntity.fromDomain(domain);
    }

    public FlowDefinition toDomain(FlowDefinitionEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    public List<FlowDefinitionEntity> toDefinitionEntityList(List<FlowDefinition> domains) {
        if (domains == null) return null;
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public List<FlowDefinition> toDefinitionDomainList(List<FlowDefinitionEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== FlowNode 映射 ===================

    public FlowNodeEntity toEntity(FlowNode domain) {
        return FlowNodeEntity.fromDomain(domain);
    }

    public FlowNode toDomain(FlowNodeEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    public List<FlowNodeEntity> toNodeEntityList(List<FlowNode> domains) {
        if (domains == null) return null;
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public List<FlowNode> toNodeDomainList(List<FlowNodeEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== FlowEdge 映射 ===================

    public FlowEdgeEntity toEntity(FlowEdge domain) {
        return FlowEdgeEntity.fromDomain(domain);
    }

    public FlowEdge toDomain(FlowEdgeEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    public List<FlowEdgeEntity> toEdgeEntityList(List<FlowEdge> domains) {
        if (domains == null) return null;
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public List<FlowEdge> toEdgeDomainList(List<FlowEdgeEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== FlowSnapshot 映射 ===================

    public FlowSnapshotEntity toEntity(FlowSnapshot domain) {
        return FlowSnapshotEntity.fromDomain(domain);
    }

    public FlowSnapshot toDomain(FlowSnapshotEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    public List<FlowSnapshotEntity> toSnapshotEntityList(List<FlowSnapshot> domains) {
        if (domains == null) return null;
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public List<FlowSnapshot> toSnapshotDomainList(List<FlowSnapshotEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== FlowRun 映射 ===================

    public FlowRunEntity toEntity(FlowRun domain) {
        return FlowRunEntity.fromDomain(domain);
    }

    public FlowRun toDomain(FlowRunEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    public List<FlowRunEntity> toRunEntityList(List<FlowRun> domains) {
        if (domains == null) return null;
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public List<FlowRun> toRunDomainList(List<FlowRunEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== FlowRunLog 映射 ===================

    public FlowRunLogEntity toEntity(FlowRunLog domain) {
        return FlowRunLogEntity.fromDomain(domain);
    }

    public FlowRunLog toDomain(FlowRunLogEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    public List<FlowRunLogEntity> toRunLogEntityList(List<FlowRunLog> domains) {
        if (domains == null) return null;
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    public List<FlowRunLog> toRunLogDomainList(List<FlowRunLogEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== 辅助方法 ===================

    /**
     * 安全地复制字符串
     */
    public String copyString(String source) {
        return source;
    }

    /**
     * 检查字符串是否为空或null
     */
    public boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 安全地截断字符串
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

    /**
     * 清理和标准化JSON文本
     */
    public String cleanJsonText(String json) {
        if (json == null) {
            return null;
        }

        // 移除首尾空白
        json = json.trim();

        // 基本JSON格式验证
        if (!json.isEmpty() && !json.startsWith("{") && !json.startsWith("[")) {
            return "{}"; // 返回空JSON对象
        }

        return json;
    }

    /**
     * 验证实体完整性
     */
    public boolean validateEntity(Object entity) {
        return entity != null;
    }

    /**
     * 验证领域对象完整性
     */
    public boolean validateDomain(Object domain) {
        return domain != null;
    }
}