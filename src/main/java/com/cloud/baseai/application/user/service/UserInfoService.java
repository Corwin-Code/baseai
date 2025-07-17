package com.cloud.baseai.application.user.service;

import com.cloud.baseai.domain.user.model.User;
import com.cloud.baseai.domain.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <h2>用户信息服务</h2>
 *
 * <p>负责提供用户的基本信息。</p>
 */
@Service
public class UserInfoService {

    private final UserRepository userRepo;

    /**
     * 用户信息缓存，避免频繁查询数据库
     */
    private final Map<Long, User> userCache = new ConcurrentHashMap<>();

    /**
     * 构造函数 - 初始化一些模拟用户数据
     *
     * <p>在实际项目中，这个构造函数应该注入用户仓储或其他用户服务，
     * 这里为了演示目的，我们初始化一些模拟数据。</p>
     */
    public UserInfoService(UserRepository userRepository) {
        this.userRepo = userRepository;

        log.info("用户信息服务初始化完成");
    }

    private static final Logger log = LoggerFactory.getLogger(UserInfoService.class);

    /**
     * 根据用户ID获取用户显示名称
     *
     * @param userId 用户的唯一标识符
     * @return 用户的显示名称，如果用户不存在则返回null或默认值
     */
    public String getUserName(Long userId) {
        if (userId == null) {
            return "系统";
        }

        try {
            log.debug("获取用户显示名称: userId={}", userId);

            // 先从缓存查找
            User cachedInfo = userCache.get(userId);
            if (cachedInfo != null) {
                return cachedInfo.username();
            }

            // 从数据源获取用户信息
            User userInfo = userRepo.findById(userId).orElse(null);
            if (userInfo != null) {
                // 更新缓存
                userCache.put(userId, userInfo);
                return userInfo.username();
            }

            // 如果找不到用户，返回默认格式
            String defaultName = "用户" + userId;
            log.debug("用户不存在，返回默认名称: {}", defaultName);
            return defaultName;

        } catch (Exception e) {
            log.warn("获取用户名称失败: userId={}", userId, e);
            return "用户" + userId;
        }
    }

    /**
     * 获取用户完整信息
     *
     * <p>这个方法返回用户的完整信息对象，包含名称、邮箱、部门等</p>
     *
     * @param userId 用户ID
     * @return 用户信息对象，如果用户不存在则返回null
     */
    public User getUserInfo(Long userId) {
        if (userId == null) {
            return null;
        }

        try {
            log.debug("获取用户完整信息: userId={}", userId);

            // 先从缓存查找
            User cachedInfo = userCache.get(userId);
            if (cachedInfo != null) {
                return cachedInfo;
            }

            // 从数据源获取
            User userInfo = userRepo.findById(userId).orElse(null);

            if (userInfo != null) {
                userCache.put(userId, userInfo);
            }

            return userInfo;

        } catch (Exception e) {
            log.warn("获取用户信息失败: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 批量获取用户显示名称
     *
     * @param userIds 用户ID列表
     * @return 用户ID到用户名的映射表
     */
    public Map<Long, String> getUserNames(Iterable<Long> userIds) {
        Map<Long, String> userNames = new HashMap<>();

        if (userIds == null) {
            return userNames;
        }

        try {
            log.debug("批量获取用户名称");

            for (Long userId : userIds) {
                if (userId != null) {
                    String userName = getUserName(userId);
                    userNames.put(userId, userName);
                }
            }

            log.debug("批量获取用户名称完成: 共{}个用户", userNames.size());
            return userNames;

        } catch (Exception e) {
            log.warn("批量获取用户名称失败", e);
            return userNames;
        }
    }

    /**
     * 检查用户是否存在
     *
     * @param userId 用户ID
     * @return true 如果用户存在
     */
    public boolean userExists(Long userId) {
        if (userId == null) {
            return false;
        }

        try {
            // 先检查缓存
            if (userCache.containsKey(userId)) {
                return true;
            }

            // 检查数据源
            User userInfo = userRepo.findById(userId).orElse(null);
            return userInfo != null;

        } catch (Exception e) {
            log.debug("检查用户存在性失败: userId={}", userId, e);
            return false;
        }
    }

    /**
     * 刷新用户缓存
     *
     * <p>当用户信息发生变更时，可以调用这个方法来刷新缓存</p>
     *
     * @param userId 要刷新的用户ID，如果为null则刷新所有缓存
     */
    public void refreshUserCache(Long userId) {
        try {
            if (userId == null) {
                log.info("刷新所有用户缓存");
                userCache.clear();
            } else {
                log.debug("刷新用户缓存: userId={}", userId);
                userCache.remove(userId);
                // 重新加载用户信息到缓存
                getUserInfo(userId);
            }

        } catch (Exception e) {
            log.warn("刷新用户缓存失败: userId={}", userId, e);
        }
    }

    /**
     * 获取缓存统计信息
     *
     * <p>这个方法提供缓存的使用情况统计，有助于监控和优化缓存性能。</p>
     *
     * @return 缓存统计信息
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", userCache.size());
        stats.put("hitRatio", calculateCacheHitRatio());
        stats.put("lastUpdateTime", System.currentTimeMillis());
        return stats;
    }

    // =================== 私有辅助方法 ===================


    /**
     * 计算缓存命中率（简化实现）
     */
    private double calculateCacheHitRatio() {
        // 在实际项目中，应该使用专业的缓存监控工具来计算命中率
        // 这里返回一个模拟值
        return 0.85; // 假设85%的命中率
    }
}