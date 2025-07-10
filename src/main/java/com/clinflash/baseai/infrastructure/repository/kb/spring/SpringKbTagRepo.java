package com.clinflash.baseai.infrastructure.repository.kb.spring;

import com.clinflash.baseai.infrastructure.persistence.kb.entity.KbTagEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <h2>标签Spring Data JPA仓储</h2>
 *
 * <p>标签系统为知识库提供了灵活的分类和组织能力。
 * 这个仓储接口支持标签的CRUD操作以及基于使用频率的热门标签查询。</p>
 */
@Repository
public interface SpringKbTagRepo extends JpaRepository<KbTagEntity, Long> {

    /**
     * 根据名称查找标签
     *
     * <p>标签名称在全局范围内是唯一的，这个方法用于检查标签是否已存在。</p>
     *
     * @param name 标签名称
     * @return 匹配的标签实体
     */
    Optional<KbTagEntity> findByNameAndDeletedAtIsNull(String name);

    /**
     * 查询所有可用标签
     *
     * @param pageable 分页参数
     * @return 标签列表，按名称排序
     */
    Page<KbTagEntity> findByDeletedAtIsNullOrderByName(Pageable pageable);

    /**
     * 根据名称模糊搜索标签
     *
     * <p>这个查询支持标签的模糊搜索，用户可以通过输入部分标签名称来找到相关标签。</p>
     *
     * @param namePattern 名称模式
     * @param pageable    分页参数
     * @return 匹配的标签列表
     */
    Page<KbTagEntity> findByNameContainingIgnoreCaseAndDeletedAtIsNullOrderByName(String namePattern, Pageable pageable);

    /**
     * 统计未删除标签总数
     *
     * @return 标签总数
     */
    long countByDeletedAtIsNull();

    /**
     * 获取热门标签
     *
     * <p>这个查询统计每个标签的使用次数，返回最受欢迎的标签。
     * 这种信息对于用户界面的标签推荐功能很有用。</p>
     * <p>
     * // @param limit 最大返回数量
     *
     * @return 热门标签列表，按使用次数倒序
     */
    @Query("""
            SELECT t as tag, COUNT(ct.tagId) as usageCount
            FROM KbTagEntity t
            LEFT JOIN KbChunkTagEntity ct ON t.id = ct.tagId
            WHERE t.deletedAt IS NULL
            GROUP BY t.id, t.name, t.remark, t.createdBy, t.createdAt, t.deletedAt
            ORDER BY COUNT(ct.tagId) DESC
            """)
    List<TagUsageProjection> findPopularTags(Pageable pageable);

    /**
     * 标签使用统计投影接口
     */
    interface TagUsageProjection {
        KbTagEntity getTag();

        Long getUsageCount();
    }
}