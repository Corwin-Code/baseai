package com.clinflash.baseai.domain.mcp.repository;

import com.clinflash.baseai.domain.mcp.model.Tool;

import java.util.List;
import java.util.Optional;

/**
 * <h2>工具仓储接口</h2>
 *
 * <p>定义了工具数据访问的领域接口。这个接口体现了仓储模式的精髓：
 * 为领域层提供了类似集合的数据访问抽象，隐藏了具体的持久化技术细节。</p>
 *
 * <p><b>设计原则：</b></p>
 * <p>接口方法的命名和签名都紧密贴合业务语言，让领域专家也能
 * 理解这些操作的含义。这是领域驱动设计的重要体现。</p>
 */
public interface ToolRepository {

    /**
     * 保存工具（新增或更新）
     *
     * @param tool 要保存的工具对象
     * @return 保存后的工具对象（包含生成的ID）
     */
    Tool save(Tool tool);

    /**
     * 根据ID查找工具
     *
     * @param id 工具ID
     * @return 工具对象，如果不存在则返回空
     */
    Optional<Tool> findById(Long id);

    /**
     * 根据代码查找工具
     *
     * @param code 工具代码
     * @return 工具对象，如果不存在则返回空
     */
    Optional<Tool> findByCode(String code);

    /**
     * 检查工具代码是否已存在
     *
     * @param code 工具代码
     * @return 如果存在返回true，否则返回false
     */
    boolean existsByCode(String code);

    /**
     * 根据多个ID批量查询工具
     *
     * @param ids 工具ID列表
     * @return 工具对象列表
     */
    List<Tool> findByIds(List<Long> ids);

    /**
     * 分页查询所有工具
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 工具列表
     */
    List<Tool> findAll(int page, int size);

    /**
     * 统计工具总数
     *
     * @return 工具总数
     */
    long count();

    /**
     * 根据类型和启用状态查询工具
     *
     * @param type    工具类型，null表示不限制
     * @param enabled 启用状态，null表示不限制
     * @param page    页码
     * @param size    每页大小
     * @return 工具列表
     */
    List<Tool> findByTypeAndEnabled(String type, Boolean enabled, int page, int size);

    /**
     * 根据类型和启用状态统计工具数量
     *
     * @param type    工具类型，null表示不限制
     * @param enabled 启用状态，null表示不限制
     * @return 工具数量
     */
    long countByTypeAndEnabled(String type, Boolean enabled);

    /**
     * 根据启用状态统计工具数量
     *
     * @param enabled 启用状态
     * @return 工具数量
     */
    int countByEnabled(boolean enabled);

    /**
     * 根据拥有者查询工具
     *
     * @param ownerId 拥有者ID
     * @param page    页码
     * @param size    每页大小
     * @return 工具列表
     */
    List<Tool> findByOwner(Long ownerId, int page, int size);

    /**
     * 软删除工具
     *
     * @param id        工具ID
     * @param deletedBy 删除操作人ID
     * @return 删除成功返回true
     */
    boolean softDelete(Long id, Long deletedBy);
}