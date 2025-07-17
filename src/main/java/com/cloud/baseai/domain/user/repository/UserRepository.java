package com.cloud.baseai.domain.user.repository;

import com.cloud.baseai.domain.user.model.User;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <h2>用户仓储接口</h2>
 *
 * <p>仓储接口定义了用户实体的持久化操作契约。在DDD中，仓储为领域层
 * 提供了一个类似集合的接口，隐藏了底层的数据访问细节。</p>
 *
 * <p><b>设计原则：</b></p>
 * <ul>
 * <li><b>领域导向：</b>方法命名体现业务意图，而非技术实现</li>
 * <li><b>接口隔离：</b>只包含领域层真正需要的操作</li>
 * <li><b>抽象性：</b>不依赖具体的持久化技术</li>
 * </ul>
 */
public interface UserRepository {

    /**
     * 保存用户
     *
     * <p>这是一个通用的保存方法，既可以创建新用户，也可以更新现有用户。
     * 实现层会根据用户ID是否为空来判断是创建还是更新操作。</p>
     */
    User save(User user);

    /**
     * 根据ID查找用户
     */
    Optional<User> findById(Long id);

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查用户是否存在
     */
    boolean existsById(Long id);

    /**
     * 批量查找用户
     */
    List<User> findByIds(List<Long> ids);

    /**
     * 搜索用户
     *
     * <p>支持按用户名、邮箱等字段进行模糊搜索。
     * 这是一个业务友好的搜索接口，隐藏了复杂的查询逻辑。</p>
     *
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @param tenantId 租户ID过滤（可选）
     * @return 匹配的用户列表
     */
    List<User> searchByKeyword(String keyword, int page, int size, Long tenantId);

    /**
     * 统计搜索结果数量
     */
    long countByKeyword(String keyword, Long tenantId);

    /**
     * 统计用户总数
     */
    long countAll();

    /**
     * 按创建时间范围统计用户
     */
    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end, Long tenantId);

    /**
     * 按最后登录时间范围统计用户
     */
    long countByLastLoginAtBetween(OffsetDateTime start, OffsetDateTime end, Long tenantId);

    /**
     * 软删除用户
     */
    boolean softDelete(Long id, Long deletedBy);

    /**
     * 查找需要清理的过期数据
     *
     * <p>这个方法用于定期清理任务，查找长期未激活或已删除的用户数据。</p>
     */
    List<User> findExpiredUsers(OffsetDateTime before);
}