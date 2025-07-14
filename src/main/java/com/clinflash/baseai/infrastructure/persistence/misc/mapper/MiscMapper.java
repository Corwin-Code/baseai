package com.clinflash.baseai.infrastructure.persistence.misc.mapper;

import com.clinflash.baseai.domain.misc.model.FileObject;
import com.clinflash.baseai.domain.misc.model.PromptTemplate;
import com.clinflash.baseai.infrastructure.persistence.misc.entity.FileObjectEntity;
import com.clinflash.baseai.infrastructure.persistence.misc.entity.PromptTemplateEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <h2>杂项模块映射器</h2>
 *
 * <p>Mapper的职责是在领域模型和持久化实体之间进行转换。
 * 它确保领域层不依赖于持久化层的具体实现。</p>
 */
@Component
public class MiscMapper {

    // =================== PromptTemplate 映射 ===================

    /**
     * 将提示词模板实体转换为领域模型
     */
    public PromptTemplate toDomain(PromptTemplateEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    /**
     * 将提示词模板领域模型转换为实体
     */
    public PromptTemplateEntity toEntity(PromptTemplate domain) {
        return PromptTemplateEntity.fromDomain(domain);
    }

    /**
     * 领域对象列表转换为实体列表
     */
    public List<PromptTemplateEntity> toPromptTemplateEntityList(List<PromptTemplate> domains) {
        if (domains == null) return null;
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * 实体列表转换为领域对象列表
     */
    public List<PromptTemplate> toPromptTemplateDomainList(List<PromptTemplateEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== FileObject 映射 ===================

    /**
     * 将文件对象实体转换为领域模型
     */
    public FileObject toDomain(FileObjectEntity entity) {
        return entity != null ? entity.toDomain() : null;
    }

    /**
     * 将文件对象领域模型转换为实体
     */
    public FileObjectEntity toEntity(FileObject domain) {
        return FileObjectEntity.fromDomain(domain);
    }

    /**
     * 领域对象列表转换为实体列表
     */
    public List<FileObjectEntity> toFileObjectEntityList(List<FileObject> domains) {
        if (domains == null) return null;
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * 实体列表转换为领域对象列表
     */
    public List<FileObject> toFileObjectDomainList(List<FileObjectEntity> entities) {
        if (entities == null) return null;
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
}