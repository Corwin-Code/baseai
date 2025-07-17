package com.cloud.baseai.infrastructure.persistence.kb.mapper;

import com.cloud.baseai.domain.kb.model.Chunk;
import com.cloud.baseai.domain.kb.model.Document;
import com.cloud.baseai.domain.kb.model.Embedding;
import com.cloud.baseai.domain.kb.model.Tag;
import com.cloud.baseai.infrastructure.persistence.kb.entity.KbChunkEntity;
import com.cloud.baseai.infrastructure.persistence.kb.entity.KbDocumentEntity;
import com.cloud.baseai.infrastructure.persistence.kb.entity.KbEmbeddingEntity;
import com.cloud.baseai.infrastructure.persistence.kb.entity.KbTagEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <h2>知识库映射器</h2>
 *
 * <p>负责领域对象与JPA实体之间的转换映射。
 * 这个类集中处理所有的对象转换逻辑，确保转换的一致性和可维护性。</p>
 *
 * <p><b>设计原则：</b></p>
 * <ul>
 * <li><b>单一职责：</b>只负责对象映射，不包含业务逻辑</li>
 * <li><b>双向映射：</b>支持领域对象到实体和实体到领域对象的转换</li>
 * <li><b>空安全：</b>所有转换方法都处理null值情况</li>
 * <li><b>批量转换：</b>提供集合类型的批量转换方法</li>
 * </ul>
 */
@Component
public class KbMapper {

    // =================== Document 映射 ===================

    /**
     * 将文档领域对象转换为JPA实体
     *
     * @param domain 文档领域对象
     * @return JPA实体，如果输入为null则返回null
     */
    public KbDocumentEntity toEntity(Document domain) {
        if (domain == null) {
            return null;
        }
        return KbDocumentEntity.fromDomain(domain);
    }

    /**
     * 将JPA实体转换为文档领域对象
     *
     * @param entity JPA实体
     * @return 文档领域对象，如果输入为null则返回null
     */
    public Document toDomain(KbDocumentEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.toDomain();
    }

    /**
     * 批量转换文档领域对象列表为JPA实体列表
     *
     * @param domains 文档领域对象列表
     * @return JPA实体列表
     */
    public List<KbDocumentEntity> toEntityList(List<Document> domains) {
        if (domains == null) {
            return null;
        }
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * 批量转换JPA实体列表为文档领域对象列表
     *
     * @param entities JPA实体列表
     * @return 文档领域对象列表
     */
    public List<Document> toDomainList(List<KbDocumentEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== Chunk 映射 ===================

    /**
     * 将知识块领域对象转换为JPA实体
     *
     * @param domain 知识块领域对象
     * @return JPA实体
     */
    public KbChunkEntity toEntity(Chunk domain) {
        if (domain == null) {
            return null;
        }
        return KbChunkEntity.fromDomain(domain);
    }

    /**
     * 将JPA实体转换为知识块领域对象
     *
     * @param entity JPA实体
     * @return 知识块领域对象
     */
    public Chunk toDomain(KbChunkEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.toDomain();
    }

    /**
     * 批量转换知识块领域对象列表
     *
     * @param domains 知识块领域对象列表
     * @return JPA实体列表
     */
    public List<KbChunkEntity> toChunkEntityList(List<Chunk> domains) {
        if (domains == null) {
            return null;
        }
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * 批量转换知识块JPA实体列表
     *
     * @param entities JPA实体列表
     * @return 知识块领域对象列表
     */
    public List<Chunk> toChunkDomainList(List<KbChunkEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== Embedding 映射 ===================

    /**
     * 将向量嵌入领域对象转换为JPA实体
     *
     * @param domain 向量嵌入领域对象
     * @return JPA实体
     */
    public KbEmbeddingEntity toEntity(Embedding domain) {
        if (domain == null) {
            return null;
        }
        return KbEmbeddingEntity.fromDomain(domain);
    }

    /**
     * 将JPA实体转换为向量嵌入领域对象
     *
     * @param entity JPA实体
     * @return 向量嵌入领域对象
     */
    public Embedding toDomain(KbEmbeddingEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.toDomain();
    }

    /**
     * 批量转换向量嵌入领域对象列表
     *
     * @param domains 向量嵌入领域对象列表
     * @return JPA实体列表
     */
    public List<KbEmbeddingEntity> toEmbeddingEntityList(List<Embedding> domains) {
        if (domains == null) {
            return null;
        }
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * 批量转换向量嵌入JPA实体列表
     *
     * @param entities JPA实体列表
     * @return 向量嵌入领域对象列表
     */
    public List<Embedding> toEmbeddingDomainList(List<KbEmbeddingEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== Tag 映射 ===================

    /**
     * 将标签领域对象转换为JPA实体
     *
     * @param domain 标签领域对象
     * @return JPA实体
     */
    public KbTagEntity toEntity(Tag domain) {
        if (domain == null) {
            return null;
        }
        return KbTagEntity.fromDomain(domain);
    }

    /**
     * 将JPA实体转换为标签领域对象
     *
     * @param entity JPA实体
     * @return 标签领域对象
     */
    public Tag toDomain(KbTagEntity entity) {
        if (entity == null) {
            return null;
        }
        return entity.toDomain();
    }

    /**
     * 批量转换标签领域对象列表
     *
     * @param domains 标签领域对象列表
     * @return JPA实体列表
     */
    public List<KbTagEntity> toTagEntityList(List<Tag> domains) {
        if (domains == null) {
            return null;
        }
        return domains.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * 批量转换标签JPA实体列表
     *
     * @param entities JPA实体列表
     * @return 标签领域对象列表
     */
    public List<Tag> toTagDomainList(List<KbTagEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // =================== 辅助方法 ===================

    /**
     * 安全地复制数组
     *
     * <p>用于向量数据的深度复制，避免引用共享导致的数据污染。</p>
     *
     * @param source 源数组
     * @return 复制的数组，如果源数组为null则返回null
     */
    public float[] copyArray(float[] source) {
        if (source == null) {
            return null;
        }
        return source.clone();
    }

    /**
     * 验证向量维度
     *
     * <p>确保向量数据符合预期的维度要求。</p>
     *
     * @param vector            向量数组
     * @param expectedDimension 期望的维度
     * @throws IllegalArgumentException 如果维度不匹配
     */
    public void validateVectorDimension(float[] vector, int expectedDimension) {
        if (vector == null) {
            throw new IllegalArgumentException("向量不能为null");
        }
        if (vector.length != expectedDimension) {
            throw new IllegalArgumentException(
                    String.format("向量维度不匹配，期望%d维，实际%d维", expectedDimension, vector.length));
        }
    }

    /**
     * 检查字符串是否为空或null
     *
     * @param str 要检查的字符串
     * @return true如果字符串为null或空
     */
    public boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 安全地截断字符串
     *
     * <p>确保字符串不超过指定长度，用于数据库字段长度限制。</p>
     *
     * @param str       原始字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
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
     * 清理和标准化文本
     *
     * <p>移除多余的空白字符，标准化换行符。</p>
     *
     * @param text 原始文本
     * @return 清理后的文本
     */
    public String cleanText(String text) {
        if (text == null) {
            return null;
        }

        // 移除首尾空白
        text = text.trim();

        // 标准化换行符
        text = text.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");

        // 移除多余的空行
        text = text.replaceAll("\\n{3,}", "\n\n");

        return text;
    }
}