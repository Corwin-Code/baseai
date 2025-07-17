package com.cloud.baseai.domain.flow.repository;

import com.cloud.baseai.domain.flow.model.FlowSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * <h2>流程快照仓储接口</h2>
 */
public interface FlowSnapshotRepository {

    /**
     * 保存快照
     *
     * @param snapshot 要保存的快照
     * @return 保存后的快照（包含生成的ID等信息）
     */
    FlowSnapshot save(FlowSnapshot snapshot);

    /**
     * 根据ID查找快照
     *
     * @param id 快照ID
     * @return 查找到的快照，如果不存在则返回Optional.empty()
     */
    Optional<FlowSnapshot> findById(Long id);

    /**
     * 根据流程定义ID和版本查找快照
     *
     * @param definitionId 流程定义ID
     * @param version      版本号
     * @return 匹配的快照，如果不存在则返回Optional.empty()
     */
    Optional<FlowSnapshot> findByDefinitionIdAndVersion(Long definitionId, Integer version);

    /**
     * 查找流程定义的最新版本快照
     *
     * <p>在大多数情况下，我们都希望使用流程的最新版本。这个方法提供了
     * 一个便捷的方式来获取最新快照，而不需要先查询最大版本号。</p>
     *
     * <p><b>实现提示：</b></p>
     * <p>这个方法通常通过ORDER BY version DESC LIMIT 1来实现，
     * 即按版本号降序排列，取第一个记录。</p>
     *
     * @param definitionId 流程定义ID
     * @return 最新版本的快照，如果不存在则返回Optional.empty()
     */
    Optional<FlowSnapshot> findLatestByDefinitionId(Long definitionId);

    /**
     * 查找流程定义的所有快照
     *
     * <p>这个方法返回某个流程定义的所有历史版本快照。在流程管理界面中，
     * 用户可能需要查看一个流程的版本历史，或者比较不同版本之间的差异。</p>
     *
     * @param definitionId 流程定义ID
     * @return 该流程定义的所有快照列表，按版本号降序排列
     */
    List<FlowSnapshot> findByDefinitionId(Long definitionId);

    /**
     * 分页查询所有可用快照
     *
     * <p>当快照数量很多时，分页查询可以避免一次加载过多数据导致的性能问题。
     * 这个方法只返回未删除的快照，过滤掉已软删除的记录。</p>
     *
     * <p><b>分页参数：</b></p>
     * <p>Pageable参数包含了页码、页大小、排序等信息。Spring Data JPA
     * 会自动处理这些参数，生成相应的SQL查询。</p>
     *
     * @param pageable 分页参数
     * @return 分页结果，包含快照列表和分页信息
     */
    Page<FlowSnapshot> findAllAvailable(Pageable pageable);

    /**
     * 根据创建者分页查询快照
     *
     * <p>这个方法允许用户查看自己创建的快照。在多用户环境中，
     * 这种按创建者过滤的功能很常见。</p>
     *
     * @param createdBy 创建者ID
     * @param pageable  分页参数
     * @return 该用户创建的快照分页结果
     */
    Page<FlowSnapshot> findByCreatedBy(Long createdBy, Pageable pageable);

    /**
     * 检查快照是否存在
     *
     * <p>这是一个便利方法，用于快速检查某个快照是否存在，
     * 而不需要真正加载快照数据。这在某些验证场景中很有用。</p>
     *
     * @param id 快照ID
     * @return 如果存在则返回true
     */
    boolean existsById(Long id);

    /**
     * 检查特定版本的快照是否存在
     *
     * <p>在创建新快照时，我们通常需要检查该版本是否已经存在，
     * 避免版本冲突。这个方法提供了这种检查能力。</p>
     *
     * @param definitionId 流程定义ID
     * @param version      版本号
     * @return 如果存在则返回true
     */
    boolean existsByDefinitionIdAndVersion(Long definitionId, Integer version);

    /**
     * 统计流程定义的快照数量
     *
     * <p>这个方法可以用于统计报表或者进行容量规划。
     * 了解每个流程定义有多少个版本，有助于管理存储空间。</p>
     *
     * @param definitionId 流程定义ID
     * @return 快照总数
     */
    long countByDefinitionId(Long definitionId);

    /**
     * 删除快照（软删除）
     *
     * @param id 要删除的快照ID
     */
    void deleteById(Long id);

    /**
     * 批量删除快照
     *
     * @param ids 要删除的快照ID列表
     */
    void deleteByIds(List<Long> ids);
}