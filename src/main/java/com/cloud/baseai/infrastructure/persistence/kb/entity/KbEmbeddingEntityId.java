package com.cloud.baseai.infrastructure.persistence.kb.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * <h2>向量嵌入复合主键</h2>
 *
 * <p>向量嵌入表使用复合主键（chunkId, modelCode, vectorVersion），
 * 需要专门的ID类来支持JPA的复合主键。</p>
 */
@Setter
@Getter
public class KbEmbeddingEntityId {

    private Long chunkId;
    private String modelCode;
    private Integer vectorVersion;

    public KbEmbeddingEntityId() {
    }

    public KbEmbeddingEntityId(Long chunkId, String modelCode, Integer vectorVersion) {
        this.chunkId = chunkId;
        this.modelCode = modelCode;
        this.vectorVersion = vectorVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KbEmbeddingEntityId that = (KbEmbeddingEntityId) o;
        return Objects.equals(chunkId, that.chunkId) &&
                Objects.equals(modelCode, that.modelCode) &&
                Objects.equals(vectorVersion, that.vectorVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chunkId, modelCode, vectorVersion);
    }
}