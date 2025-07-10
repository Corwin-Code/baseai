package com.clinflash.baseai.infrastructure.persistence.kb.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * <h2>知识块标签关联复合主键</h2>
 */
@Setter
@Getter
public class KbChunkTagEntityId {
    private Long chunkId;
    private Long tagId;

    public KbChunkTagEntityId() {
    }

    public KbChunkTagEntityId(Long chunkId, Long tagId) {
        this.chunkId = chunkId;
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KbChunkTagEntityId that = (KbChunkTagEntityId) o;
        return Objects.equals(chunkId, that.chunkId) &&
                Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chunkId, tagId);
    }
}