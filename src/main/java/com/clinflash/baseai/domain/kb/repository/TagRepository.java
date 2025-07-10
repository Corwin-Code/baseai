package com.clinflash.baseai.domain.kb.repository;

import com.clinflash.baseai.domain.kb.model.Tag;

import java.util.List;
import java.util.Optional;

/**
 * <h2>标签仓储接口</h2>
 *
 * <p>管理知识块的分类标签，支持层级化组织和快速检索。</p>
 */
public interface TagRepository {

    /**
     * 保存标签
     *
     * @param tag 标签实体
     * @return 保存后的标签实体
     */
    Tag save(Tag tag);

    /**
     * 根据ID查找标签
     *
     * @param id 标签ID
     * @return 标签实体
     */
    Optional<Tag> findById(Long id);

    /**
     * 根据名称查找标签
     *
     * @param name 标签名称
     * @return 标签实体
     */
    Optional<Tag> findByName(String name);

    /**
     * 批量查询标签
     *
     * @param ids 标签ID列表
     * @return 标签列表
     */
    List<Tag> findByIds(List<Long> ids);

    /**
     * 查询所有可用标签
     *
     * @return 所有标签列表，按名称排序
     */
    List<Tag> findAll();

    /**
     * 分页查询标签
     *
     * @param page 页码
     * @param size 每页大小
     * @return 标签列表
     */
    List<Tag> findAll(int page, int size);

    /**
     * 根据名称模糊搜索标签
     *
     * @param namePattern 名称模式（支持通配符）
     * @param limit       最大返回数量
     * @return 匹配的标签列表
     */
    List<Tag> searchByName(String namePattern, int limit);

    /**
     * 统计标签总数
     *
     * @return 标签总数
     */
    long count();

    /**
     * 获取热门标签
     *
     * <p>根据标签的使用频率返回最受欢迎的标签。</p>
     *
     * @param limit 最大返回数量
     * @return 热门标签列表，按使用次数倒序
     */
    List<TagUsageInfo> findPopularTags(int limit);

    /**
     * 标签使用信息
     *
     * @param tag   标签实体
     * @param count 使用次数
     */
    record TagUsageInfo(Tag tag, Long count) {
    }
}