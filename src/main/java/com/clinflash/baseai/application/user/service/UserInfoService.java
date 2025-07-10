package com.clinflash.baseai.application.user.service;

import java.util.List;
import java.util.Map;

/**
 * <h2>用户信息查询服务接口</h2>
 */
public interface UserInfoService {

    /**
     * 根据用户ID获取用户显示名称
     *
     * @param userId 用户的唯一标识符
     * @return 用户的显示名称，如果用户不存在则返回null或默认值
     */
    String getUserName(Long userId);

    /**
     * 批量获取用户显示名称
     *
     * @param userIds 用户ID列表
     * @return 用户ID到用户名的映射表
     */
    Map<Long, String> getUserNames(List<Long> userIds);

    /**
     * 检查用户是否存在
     *
     * @param userId 用户ID
     * @return true 如果用户存在
     */
    boolean userExists(Long userId);

    /**
     * 获取用户的基本信息
     *
     * @param userId 用户ID
     * @return 用户基本信息对象，包含ID、姓名、邮箱等
     */
    Object getUserInfo(Long userId);
}