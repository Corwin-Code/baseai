package com.clinflash.baseai.infrastructure.persistence.kb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * <h2>知识块标签关联JPA实体</h2>
 *
 * <p>对应数据库表 kb_chunk_tags 的JPA映射。
 * 这是多对多关联的中间表。</p>
 */
@Setter
@Getter
@Entity
@Table(name = "kb_chunk_tags")
@IdClass(KbChunkTagEntityId.class)
public class KbChunkTagEntity {

    @Id
    @Column(name = "chunk_id")
    private Long chunkId;

    @Id
    @Column(name = "tag_id")
    private Long tagId;

    protected KbChunkTagEntity() {
    }

    public KbChunkTagEntity(Long chunkId, Long tagId) {
        this.chunkId = chunkId;
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KbChunkTagEntity that = (KbChunkTagEntity) o;
        return Objects.equals(chunkId, that.chunkId) &&
                Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chunkId, tagId);
    }
}