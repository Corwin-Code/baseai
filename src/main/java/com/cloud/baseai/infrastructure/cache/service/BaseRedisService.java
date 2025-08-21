package com.cloud.baseai.infrastructure.cache.service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * <h2>BaseAI统一Redis服务接口</h2>
 *
 * <p>提供完整的Redis操作封装，支持所有常用数据类型和高级功能，
 * 包括分布式锁、发布订阅、缓存管理等企业级功能。</p>
 *
 * <p><b>设计特点：</b></p>
 * <ul>
 * <li><b>类型安全：</b>泛型支持，避免类型转换异常</li>
 * <li><b>配置驱动：</b>基于配置文件的TTL、压缩等策略</li>
 * <li><b>异常安全：</b>优雅处理Redis连接异常</li>
 * <li><b>性能优化：</b>批量操作、管道操作支持</li>
 * <li><b>监控友好：</b>操作指标和日志记录</li>
 * </ul>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * // 基本操作
 * redisService.set("user:123", userInfo, Duration.ofHours(1));
 * UserInfo user = redisService.get("user:123", UserInfo.class);
 *
 * // 缓存模式
 * UserInfo user = redisService.getOrCompute("user:123",
 *     () -> userRepository.findById(123), Duration.ofMinutes(30));
 *
 * // 分布式锁
 * redisService.executeWithLock("lock:user:123", Duration.ofSeconds(10), () -> {
 *     // 临界区代码
 *     return updateUser(123);
 * });
 * }</pre>
 */
public interface BaseRedisService {

    // =================== 基础键值操作 ===================

    /**
     * 设置键值，使用默认TTL
     *
     * @param key   键
     * @param value 值
     * @param <T>   值类型
     * @return 是否成功
     */
    <T> boolean set(String key, T value);

    /**
     * 设置键值，指定TTL
     *
     * @param key      键
     * @param value    值
     * @param duration 过期时间
     * @param <T>      值类型
     * @return 是否成功
     */
    <T> boolean set(String key, T value, Duration duration);

    /**
     * 设置键值，指定TTL（时间单位）
     *
     * @param key      键
     * @param value    值
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @param <T>      值类型
     * @return 是否成功
     */
    <T> boolean set(String key, T value, long timeout, TimeUnit timeUnit);

    /**
     * 仅当键不存在时设置值
     *
     * @param key      键
     * @param value    值
     * @param duration 过期时间
     * @param <T>      值类型
     * @return 是否成功设置（false表示键已存在）
     */
    <T> boolean setIfAbsent(String key, T value, Duration duration);

    /**
     * 获取值
     *
     * @param key   键
     * @param clazz 值类型
     * @param <T>   值类型
     * @return 值，不存在返回null
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 获取值，如果不存在则计算并缓存
     *
     * @param key      键
     * @param supplier 值计算函数
     * @param duration 缓存时间
     * @param <T>      值类型
     * @return 值
     */
    <T> T getOrCompute(String key, Supplier<T> supplier, Duration duration);

    /**
     * 获取值，如果不存在则返回默认值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @param <T>          值类型
     * @return 值或默认值
     */
    <T> T getOrDefault(String key, T defaultValue);

    /**
     * 删除键
     *
     * @param key 键
     * @return 是否成功删除
     */
    boolean delete(String key);

    /**
     * 批量删除键
     *
     * @param keys 键集合
     * @return 删除的键数量
     */
    long delete(Collection<String> keys);

    /**
     * 检查键是否存在
     *
     * @param key 键
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 设置键的过期时间
     *
     * @param key      键
     * @param duration 过期时间
     * @return 是否成功
     */
    boolean expire(String key, Duration duration);

    /**
     * 获取键的剩余TTL
     *
     * @param key 键
     * @return 剩余时间，-1表示永不过期，-2表示键不存在
     */
    Duration getTtl(String key);

    // =================== 批量操作 ===================

    /**
     * 批量设置键值
     *
     * @param keyValueMap 键值映射
     * @param duration    过期时间
     * @param <T>         值类型
     * @return 成功设置的数量
     */
    <T> long multiSet(Map<String, T> keyValueMap, Duration duration);

    /**
     * 批量获取值
     *
     * @param keys  键集合
     * @param clazz 值类型
     * @param <T>   值类型
     * @return 键值映射，不存在的键对应null值
     */
    <T> Map<String, T> multiGet(Collection<String> keys, Class<T> clazz);

    // =================== Hash操作 ===================

    /**
     * 设置Hash字段
     *
     * @param key   Hash键
     * @param field 字段名
     * @param value 字段值
     * @param <T>   值类型
     * @return 是否成功
     */
    <T> boolean hSet(String key, String field, T value);

    /**
     * 获取Hash字段值
     *
     * @param key   Hash键
     * @param field 字段名
     * @param clazz 值类型
     * @param <T>   值类型
     * @return 字段值
     */
    <T> T hGet(String key, String field, Class<T> clazz);

    /**
     * 批量设置Hash字段
     *
     * @param key       Hash键
     * @param fieldMap  字段映射
     * @param <T>       值类型
     * @return 是否成功
     */
    <T> boolean hMultiSet(String key, Map<String, T> fieldMap);

    /**
     * 获取Hash所有字段
     *
     * @param key   Hash键
     * @param clazz 值类型
     * @param <T>   值类型
     * @return 字段映射
     */
    <T> Map<String, T> hGetAll(String key, Class<T> clazz);

    /**
     * 删除Hash字段
     *
     * @param key    Hash键
     * @param fields 字段名数组
     * @return 删除的字段数量
     */
    long hDelete(String key, String... fields);

    // =================== List操作 ===================

    /**
     * 从列表左端推入元素
     *
     * @param key    列表键
     * @param values 元素数组
     * @param <T>    元素类型
     * @return 推入后列表长度
     */
    <T> long lPush(String key, T... values);

    /**
     * 从列表右端推入元素
     *
     * @param key    列表键
     * @param values 元素数组
     * @param <T>    元素类型
     * @return 推入后列表长度
     */
    <T> long rPush(String key, T... values);

    /**
     * 从列表左端弹出元素
     *
     * @param key   列表键
     * @param clazz 元素类型
     * @param <T>   元素类型
     * @return 弹出的元素，列表为空返回null
     */
    <T> T lPop(String key, Class<T> clazz);

    /**
     * 从列表右端弹出元素
     *
     * @param key   列表键
     * @param clazz 元素类型
     * @param <T>   元素类型
     * @return 弹出的元素，列表为空返回null
     */
    <T> T rPop(String key, Class<T> clazz);

    /**
     * 获取列表指定范围的元素
     *
     * @param key   列表键
     * @param start 起始索引
     * @param end   结束索引
     * @param clazz 元素类型
     * @param <T>   元素类型
     * @return 元素列表
     */
    <T> List<T> lRange(String key, long start, long end, Class<T> clazz);

    /**
     * 获取列表长度
     *
     * @param key 列表键
     * @return 列表长度
     */
    long lSize(String key);

    // =================== Set操作 ===================

    /**
     * 向集合添加元素
     *
     * @param key     集合键
     * @param members 元素数组
     * @param <T>     元素类型
     * @return 添加的元素数量
     */
    <T> long sAdd(String key, T... members);

    /**
     * 获取集合所有元素
     *
     * @param key   集合键
     * @param clazz 元素类型
     * @param <T>   元素类型
     * @return 元素集合
     */
    <T> Set<T> sMembers(String key, Class<T> clazz);

    /**
     * 判断元素是否在集合中
     *
     * @param key    集合键
     * @param member 元素
     * @param <T>    元素类型
     * @return 是否存在
     */
    <T> boolean sIsMember(String key, T member);

    /**
     * 从集合移除元素
     *
     * @param key     集合键
     * @param members 元素数组
     * @param <T>     元素类型
     * @return 移除的元素数量
     */
    <T> long sRemove(String key, T... members);

    /**
     * 获取集合大小
     *
     * @param key 集合键
     * @return 集合大小
     */
    long sSize(String key);

    // =================== Sorted Set操作 ===================

    /**
     * 向有序集合添加元素
     *
     * @param key    有序集合键
     * @param value  元素值
     * @param score  分数
     * @param <T>    元素类型
     * @return 是否成功
     */
    <T> boolean zAdd(String key, T value, double score);

    /**
     * 批量向有序集合添加元素
     *
     * @param key        有序集合键
     * @param scoreMembers 分数-元素映射
     * @param <T>        元素类型
     * @return 添加的元素数量
     */
    <T> long zAdd(String key, Map<T, Double> scoreMembers);

    /**
     * 获取有序集合指定范围的元素（按分数升序）
     *
     * @param key   有序集合键
     * @param start 起始索引
     * @param end   结束索引
     * @param clazz 元素类型
     * @param <T>   元素类型
     * @return 元素列表
     */
    <T> Set<T> zRange(String key, long start, long end, Class<T> clazz);

    /**
     * 获取有序集合指定分数范围的元素
     *
     * @param key      有序集合键
     * @param minScore 最小分数
     * @param maxScore 最大分数
     * @param clazz    元素类型
     * @param <T>      元素类型
     * @return 元素集合
     */
    <T> Set<T> zRangeByScore(String key, double minScore, double maxScore, Class<T> clazz);

    /**
     * 获取有序集合大小
     *
     * @param key 有序集合键
     * @return 集合大小
     */
    long zSize(String key);

    /**
     * 移除有序集合元素
     *
     * @param key     有序集合键
     * @param members 元素数组
     * @param <T>     元素类型
     * @return 移除的元素数量
     */
    <T> long zRemove(String key, T... members);

    // =================== 分布式锁 ===================

    /**
     * 尝试获取分布式锁
     *
     * @param lockKey   锁键
     * @param lockValue 锁值（通常使用UUID）
     * @param duration  锁持有时间
     * @return 是否成功获取锁
     */
    boolean tryLock(String lockKey, String lockValue, Duration duration);

    /**
     * 释放分布式锁
     *
     * @param lockKey   锁键
     * @param lockValue 锁值
     * @return 是否成功释放
     */
    boolean releaseLock(String lockKey, String lockValue);

    /**
     * 使用分布式锁执行操作
     *
     * @param lockKey  锁键
     * @param duration 锁持有时间
     * @param supplier 执行的操作
     * @param <T>      返回值类型
     * @return 操作结果
     * @throws IllegalStateException 获取锁失败时抛出
     */
    <T> T executeWithLock(String lockKey, Duration duration, Supplier<T> supplier);

    /**
     * 使用分布式锁执行操作（无返回值）
     *
     * @param lockKey  锁键
     * @param duration 锁持有时间
     * @param runnable 执行的操作
     * @throws IllegalStateException 获取锁失败时抛出
     */
    void executeWithLock(String lockKey, Duration duration, Runnable runnable);

    // =================== 发布订阅 ===================

    /**
     * 发布消息
     *
     * @param channel 频道
     * @param message 消息
     * @return 接收消息的订阅者数量
     */
    long publish(String channel, Object message);

    /**
     * 订阅频道
     *
     * @param messageListener 消息监听器
     * @param channels        频道数组
     */
    void subscribe(RedisMessageListener messageListener, String... channels);

    /**
     * 取消订阅
     *
     * @param channels 频道数组
     */
    void unsubscribe(String... channels);

    // =================== 缓存管理 ===================

    /**
     * 清空缓存（指定前缀）
     *
     * @param pattern 键模式，支持通配符
     * @return 清理的键数量
     */
    long clearCache(String pattern);

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计
     */
    CacheStats getCacheStats();

    /**
     * 缓存预热
     *
     * @param cacheLoader 缓存加载器
     */
    void warmUpCache(CacheLoader cacheLoader);

    // =================== 工具方法 ===================

    /**
     * 获取Redis信息
     *
     * @return Redis服务器信息
     */
    Map<String, String> getRedisInfo();

    /**
     * 测试Redis连接
     *
     * @return 连接是否正常
     */
    boolean ping();

    /**
     * 获取数据库大小
     *
     * @return 键的数量
     */
    long getDbSize();

    // =================== 内部接口 ===================

    /**
     * Redis消息监听器接口
     */
    @FunctionalInterface
    interface RedisMessageListener {
        /**
         * 处理接收到的消息
         *
         * @param channel 频道
         * @param message 消息
         */
        void onMessage(String channel, Object message);
    }

    /**
     * 缓存加载器接口
     */
    @FunctionalInterface
    interface CacheLoader {
        /**
         * 加载缓存数据
         *
         * @return 缓存数据映射
         */
        Map<String, Object> loadCache();
    }

    /**
     * 缓存统计信息
     */
    interface CacheStats {
        /**
         * 获取命中次数
         */
        long getHitCount();

        /**
         * 获取未命中次数
         */
        long getMissCount();

        /**
         * 获取命中率
         */
        double getHitRate();

        /**
         * 获取总键数量
         */
        long getTotalKeys();

        /**
         * 获取已使用内存
         */
        long getUsedMemory();
    }
}