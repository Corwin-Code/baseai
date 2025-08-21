package com.cloud.baseai.infrastructure.cache.service.impl;

import com.cloud.baseai.infrastructure.cache.service.BaseRedisService;
import com.cloud.baseai.infrastructure.config.properties.CacheProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * <h2>BaseAI统一Redis服务实现类</h2>
 *
 * <p>基于Spring Data Redis的完整Redis服务实现，提供企业级的缓存解决方案。
 * 集成了配置驱动的TTL管理、数据压缩、分布式锁、发布订阅等核心功能。</p>
 *
 * <p><b>核心特性：</b></p>
 * <ul>
 * <li><b>配置驱动：</b>基于CacheProperties的TTL、压缩、前缀等配置</li>
 * <li><b>智能压缩：</b>超过阈值的数据自动GZIP压缩</li>
 * <li><b>异常恢复：</b>Redis连接异常时的优雅降级</li>
 * <li><b>性能监控：</b>集成Micrometer指标收集</li>
 * <li><b>分布式锁：</b>基于Lua脚本的原子性锁操作</li>
 * <li><b>类型安全：</b>泛型支持和JSON序列化</li>
 * </ul>
 */
public class BaseRedisServiceImpl implements BaseRedisService {

    private static final Logger log = LoggerFactory.getLogger(BaseRedisServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheProperties cacheProperties;
    private final ObjectMapper objectMapper;
    private final RedisMessageListenerContainer listenerContainer;
    private final MeterRegistry meterRegistry;

    // 性能监控指标
    private final Counter hitCounter;
    private final Counter missCounter;
    private final Timer operationTimer;

    // 分布式锁Lua脚本
    private static final String LOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else " +
                    "return 0 " +
                    "end";

    private final DefaultRedisScript<Long> lockReleaseScript;

    public BaseRedisServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            CacheProperties cacheProperties,
            ObjectMapper objectMapper,
            RedisConnectionFactory connectionFactory,
            MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.cacheProperties = cacheProperties;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;

        // 初始化Redis消息监听器容器
        this.listenerContainer = new RedisMessageListenerContainer();
        this.listenerContainer.setConnectionFactory(connectionFactory);
        this.listenerContainer.afterPropertiesSet();
        this.listenerContainer.start();

        // 初始化分布式锁脚本
        this.lockReleaseScript = new DefaultRedisScript<>(LOCK_SCRIPT, Long.class);

        // 初始化监控指标
        this.hitCounter = Counter.builder("baseai.redis.cache.hit")
                .description("Redis缓存命中次数")
                .register(meterRegistry);
        this.missCounter = Counter.builder("baseai.redis.cache.miss")
                .description("Redis缓存未命中次数")
                .register(meterRegistry);
        this.operationTimer = Timer.builder("baseai.redis.operation.duration")
                .description("Redis操作耗时")
                .register(meterRegistry);

        log.info("BaseRedisService初始化完成 - 压缩阈值: {}字节, 默认TTL: {}秒",
                cacheProperties.getRedis().getCompressionThresholdBytes(),
                cacheProperties.getRedis().getDefaultTtlSeconds());
    }

    // =================== 基础键值操作 ===================

    @Override
    public <T> boolean set(String key, T value) {
        return set(key, value, getDefaultTtl());
    }

    @Override
    public <T> boolean set(String key, T value, Duration duration) {
        return executeWithTimer("set", () -> {
            try {
                String finalKey = buildKey(key);
                Object processedValue = processValue(value);

                if (duration != null && !duration.isNegative()) {
                    redisTemplate.opsForValue().set(finalKey, processedValue, duration);
                } else {
                    redisTemplate.opsForValue().set(finalKey, processedValue);
                }

                log.debug("Redis SET - key: {}, ttl: {}", finalKey, duration);
                return true;
            } catch (Exception e) {
                log.error("Redis SET操作失败 - key: {}, error: {}", key, e.getMessage());
                return false;
            }
        });
    }

    @Override
    public <T> boolean set(String key, T value, long timeout, TimeUnit timeUnit) {
        return set(key, value, Duration.of(timeout, timeUnit.toChronoUnit()));
    }

    @Override
    public <T> boolean setIfAbsent(String key, T value, Duration duration) {
        return executeWithTimer("setIfAbsent", () -> {
            try {
                String finalKey = buildKey(key);
                Object processedValue = processValue(value);
                Boolean result = redisTemplate.opsForValue().setIfAbsent(finalKey, processedValue, duration);

                log.debug("Redis SETNX - key: {}, result: {}", finalKey, result);
                return Boolean.TRUE.equals(result);
            } catch (Exception e) {
                log.error("Redis SETNX操作失败 - key: {}, error: {}", key, e.getMessage());
                return false;
            }
        });
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        return executeWithTimer("get", () -> {
            try {
                String finalKey = buildKey(key);
                Object value = redisTemplate.opsForValue().get(finalKey);

                if (value != null) {
                    hitCounter.increment();
                    T result = convertValue(value, clazz);
                    log.debug("Redis GET命中 - key: {}", finalKey);
                    return result;
                } else {
                    missCounter.increment();
                    log.debug("Redis GET未命中 - key: {}", finalKey);
                    return null;
                }
            } catch (Exception e) {
                log.error("Redis GET操作失败 - key: {}, error: {}", key, e.getMessage());
                missCounter.increment();
                return null;
            }
        });
    }

    @Override
    public <T> T getOrCompute(String key, Supplier<T> supplier, Duration duration) {
        T value = get(key, (Class<T>) Object.class);
        if (value != null) {
            return value;
        }

        // 使用分布式锁防止缓存击穿
        String lockKey = buildKey("lock:" + key);
        String lockValue = UUID.randomUUID().toString();

        try {
            if (tryLock(lockKey, lockValue, Duration.ofSeconds(10))) {
                try {
                    // 双重检查
                    value = get(key, (Class<T>) Object.class);
                    if (value != null) {
                        return value;
                    }

                    // 计算值并缓存
                    value = supplier.get();
                    if (value != null) {
                        set(key, value, duration);
                    }
                    return value;
                } finally {
                    releaseLock(lockKey, lockValue);
                }
            } else {
                // 获取锁失败，直接计算值（不缓存）
                log.warn("获取缓存锁失败，直接计算值 - key: {}", key);
                return supplier.get();
            }
        } catch (Exception e) {
            log.error("getOrCompute操作失败 - key: {}, error: {}", key, e.getMessage());
            return supplier.get();
        }
    }

    @Override
    public <T> T getOrDefault(String key, T defaultValue) {
        try {
            T value = get(key, (Class<T>) defaultValue.getClass());
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            log.error("getOrDefault操作失败 - key: {}, error: {}", key, e.getMessage());
            return defaultValue;
        }
    }

    @Override
    public boolean delete(String key) {
        return executeWithTimer("delete", () -> {
            try {
                String finalKey = buildKey(key);
                Boolean result = redisTemplate.delete(finalKey);
                log.debug("Redis DELETE - key: {}, result: {}", finalKey, result);
                return Boolean.TRUE.equals(result);
            } catch (Exception e) {
                log.error("Redis DELETE操作失败 - key: {}, error: {}", key, e.getMessage());
                return false;
            }
        });
    }

    @Override
    public long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }

        return executeWithTimer("multiDelete", () -> {
            try {
                Set<String> finalKeys = keys.stream()
                        .map(this::buildKey)
                        .collect(Collectors.toSet());
                Long result = redisTemplate.delete(finalKeys);
                log.debug("Redis批量DELETE - 删除数量: {}", result);
                return result != null ? result : 0L;
            } catch (Exception e) {
                log.error("Redis批量DELETE操作失败 - error: {}", e.getMessage());
                return 0L;
            }
        });
    }

    @Override
    public boolean exists(String key) {
        return executeWithTimer("exists", () -> {
            try {
                String finalKey = buildKey(key);
                Boolean result = redisTemplate.hasKey(finalKey);
                return Boolean.TRUE.equals(result);
            } catch (Exception e) {
                log.error("Redis EXISTS操作失败 - key: {}, error: {}", key, e.getMessage());
                return false;
            }
        });
    }

    @Override
    public boolean expire(String key, Duration duration) {
        return executeWithTimer("expire", () -> {
            try {
                String finalKey = buildKey(key);
                Boolean result = redisTemplate.expire(finalKey, duration);
                return Boolean.TRUE.equals(result);
            } catch (Exception e) {
                log.error("Redis EXPIRE操作失败 - key: {}, error: {}", key, e.getMessage());
                return false;
            }
        });
    }

    @Override
    public Duration getTtl(String key) {
        return executeWithTimer("getTtl", () -> {
            try {
                String finalKey = buildKey(key);
                Long ttl = redisTemplate.getExpire(finalKey, TimeUnit.SECONDS);
                return ttl != null ? Duration.ofSeconds(ttl) : Duration.ofSeconds(-2);
            } catch (Exception e) {
                log.error("Redis TTL操作失败 - key: {}, error: {}", key, e.getMessage());
                return Duration.ofSeconds(-2);
            }
        });
    }

    // =================== 批量操作 ===================

    @Override
    public <T> long multiSet(Map<String, T> keyValueMap, Duration duration) {
        if (keyValueMap == null || keyValueMap.isEmpty()) {
            return 0;
        }

        return executeWithTimer("multiSet", () -> {
            try {
                long successCount = 0;
                for (Map.Entry<String, T> entry : keyValueMap.entrySet()) {
                    if (set(entry.getKey(), entry.getValue(), duration)) {
                        successCount++;
                    }
                }
                log.debug("Redis批量SET - 成功数量: {}/{}", successCount, keyValueMap.size());
                return successCount;
            } catch (Exception e) {
                log.error("Redis批量SET操作失败 - error: {}", e.getMessage());
                return 0L;
            }
        });
    }

    @Override
    public <T> Map<String, T> multiGet(Collection<String> keys, Class<T> clazz) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        return executeWithTimer("multiGet", () -> {
            try {
                List<String> finalKeys = keys.stream()
                        .map(this::buildKey)
                        .collect(Collectors.toList());

                List<Object> values = redisTemplate.opsForValue().multiGet(finalKeys);
                Map<String, T> result = new HashMap<>();

                if (values != null) {
                    Iterator<String> keyIter = keys.iterator();
                    Iterator<Object> valueIter = values.iterator();

                    while (keyIter.hasNext() && valueIter.hasNext()) {
                        String originalKey = keyIter.next();
                        Object value = valueIter.next();

                        if (value != null) {
                            result.put(originalKey, convertValue(value, clazz));
                            hitCounter.increment();
                        } else {
                            result.put(originalKey, null);
                            missCounter.increment();
                        }
                    }
                }

                log.debug("Redis批量GET - 键数量: {}, 命中数量: {}",
                        keys.size(), result.values().stream().mapToLong(v -> v != null ? 1 : 0).sum());
                return result;
            } catch (Exception e) {
                log.error("Redis批量GET操作失败 - error: {}", e.getMessage());
                return Collections.emptyMap();
            }
        });
    }

    // =================== Hash操作 ===================

    @Override
    public <T> boolean hSet(String key, String field, T value) {
        return executeWithTimer("hSet", () -> {
            try {
                String finalKey = buildKey(key);
                Object processedValue = processValue(value);
                redisTemplate.opsForHash().put(finalKey, field, processedValue);
                log.debug("Redis HSET - key: {}, field: {}", finalKey, field);
                return true;
            } catch (Exception e) {
                log.error("Redis HSET操作失败 - key: {}, field: {}, error: {}", key, field, e.getMessage());
                return false;
            }
        });
    }

    @Override
    public <T> T hGet(String key, String field, Class<T> clazz) {
        return executeWithTimer("hGet", () -> {
            try {
                String finalKey = buildKey(key);
                Object value = redisTemplate.opsForHash().get(finalKey, field);

                if (value != null) {
                    hitCounter.increment();
                    return convertValue(value, clazz);
                } else {
                    missCounter.increment();
                    return null;
                }
            } catch (Exception e) {
                log.error("Redis HGET操作失败 - key: {}, field: {}, error: {}", key, field, e.getMessage());
                missCounter.increment();
                return null;
            }
        });
    }

    @Override
    public <T> boolean hMultiSet(String key, Map<String, T> fieldMap) {
        if (fieldMap == null || fieldMap.isEmpty()) {
            return true;
        }

        return executeWithTimer("hMultiSet", () -> {
            try {
                String finalKey = buildKey(key);
                Map<String, Object> processedMap = fieldMap.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> processValue(entry.getValue())
                        ));

                redisTemplate.opsForHash().putAll(finalKey, processedMap);
                log.debug("Redis HMSET - key: {}, field数量: {}", finalKey, fieldMap.size());
                return true;
            } catch (Exception e) {
                log.error("Redis HMSET操作失败 - key: {}, error: {}", key, e.getMessage());
                return false;
            }
        });
    }

    @Override
    public <T> Map<String, T> hGetAll(String key, Class<T> clazz) {
        return executeWithTimer("hGetAll", () -> {
            try {
                String finalKey = buildKey(key);
                Map<Object, Object> rawMap = redisTemplate.opsForHash().entries(finalKey);

                if (rawMap.isEmpty()) {
                    missCounter.increment();
                    return Collections.emptyMap();
                }

                hitCounter.increment();
                Map<String, T> result = new HashMap<>();
                for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {
                    String field = String.valueOf(entry.getKey());
                    T value = convertValue(entry.getValue(), clazz);
                    result.put(field, value);
                }

                log.debug("Redis HGETALL - key: {}, field数量: {}", finalKey, result.size());
                return result;
            } catch (Exception e) {
                log.error("Redis HGETALL操作失败 - key: {}, error: {}", key, e.getMessage());
                missCounter.increment();
                return Collections.emptyMap();
            }
        });
    }

    @Override
    public long hDelete(String key, String... fields) {
        if (fields == null || fields.length == 0) {
            return 0;
        }

        return executeWithTimer("hDelete", () -> {
            try {
                String finalKey = buildKey(key);
                Long result = redisTemplate.opsForHash().delete(finalKey, (Object[]) fields);
                log.debug("Redis HDEL - key: {}, 删除数量: {}", finalKey, result);
                return result != null ? result : 0L;
            } catch (Exception e) {
                log.error("Redis HDEL操作失败 - key: {}, error: {}", key, e.getMessage());
                return 0L;
            }
        });
    }

    // =================== List操作 ===================

    @Override
    public <T> long lPush(String key, T... values) {
        if (values == null || values.length == 0) {
            return 0;
        }

        return executeWithTimer("lPush", () -> {
            try {
                String finalKey = buildKey(key);
                Object[] processedValues = Arrays.stream(values)
                        .map(this::processValue)
                        .toArray();

                Long result = redisTemplate.opsForList().leftPushAll(finalKey, processedValues);
                log.debug("Redis LPUSH - key: {}, 推入数量: {}", finalKey, values.length);
                return result != null ? result : 0L;
            } catch (Exception e) {
                log.error("Redis LPUSH操作失败 - key: {}, error: {}", key, e.getMessage());
                return 0L;
            }
        });
    }

    @Override
    public <T> long rPush(String key, T... values) {
        if (values == null || values.length == 0) {
            return 0;
        }

        return executeWithTimer("rPush", () -> {
            try {
                String finalKey = buildKey(key);
                Object[] processedValues = Arrays.stream(values)
                        .map(this::processValue)
                        .toArray();

                Long result = redisTemplate.opsForList().rightPushAll(finalKey, processedValues);
                log.debug("Redis RPUSH - key: {}, 推入数量: {}", finalKey, values.length);
                return result != null ? result : 0L;
            } catch (Exception e) {
                log.error("Redis RPUSH操作失败 - key: {}, error: {}", key, e.getMessage());
                return 0L;
            }
        });
    }

    @Override
    public <T> T lPop(String key, Class<T> clazz) {
        return executeWithTimer("lPop", () -> {
            try {
                String finalKey = buildKey(key);
                Object value = redisTemplate.opsForList().leftPop(finalKey);
                return value != null ? convertValue(value, clazz) : null;
            } catch (Exception e) {
                log.error("Redis LPOP操作失败 - key: {}, error: {}", key, e.getMessage());
                return null;
            }
        });
    }

    @Override
    public <T> T rPop(String key, Class<T> clazz) {
        return executeWithTimer("rPop", () -> {
            try {
                String finalKey = buildKey(key);
                Object value = redisTemplate.opsForList().rightPop(finalKey);
                return value != null ? convertValue(value, clazz) : null;
            } catch (Exception e) {
                log.error("Redis RPOP操作失败 - key: {}, error: {}", key, e.getMessage());
                return null;
            }
        });
    }

    @Override
    public <T> List<T> lRange(String key, long start, long end, Class<T> clazz) {
        return executeWithTimer("lRange", () -> {
            try {
                String finalKey = buildKey(key);
                List<Object> values = redisTemplate.opsForList().range(finalKey, start, end);

                if (values == null || values.isEmpty()) {
                    return Collections.emptyList();
                }

                return values.stream()
                        .map(value -> convertValue(value, clazz))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("Redis LRANGE操作失败 - key: {}, error: {}", key, e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    @Override
    public long lSize(String key) {
        return executeWithTimer("lSize", () -> {
            try {
                String finalKey = buildKey(key);
                Long size = redisTemplate.opsForList().size(finalKey);
                return size != null ? size : 0L;
            } catch (Exception e) {
                log.error("Redis LLEN操作失败 - key: {}, error: {}", key, e.getMessage());
                return 0L;
            }
        });
    }

    // =================== Set操作 ===================

    @Override
    public <T> long sAdd(String key, T... members) {
        if (members == null || members.length == 0) {
            return 0;
        }

        return executeWithTimer("sAdd", () -> {
            try {
                String finalKey = buildKey(key);
                Object[] processedMembers = Arrays.stream(members)
                        .map(this::processValue)
                        .toArray();

                Long result = redisTemplate.opsForSet().add(finalKey, processedMembers);
                log.debug("Redis SADD - key: {}, 添加数量: {}", finalKey, result);
                return result != null ? result : 0L;
            } catch (Exception e) {
                log.error("Redis SADD操作失败 - key: {}, error: {}", key, e.getMessage());
                return 0L;
            }
        });
    }

    @Override
    public <T> Set<T> sMembers(String key, Class<T> clazz) {
        return executeWithTimer("sMembers", () -> {
            try {
                String finalKey = buildKey(key);
                Set<Object> members = redisTemplate.opsForSet().members(finalKey);

                if (members == null || members.isEmpty()) {
                    return Collections.emptySet();
                }

                return members.stream()
                        .map(member -> convertValue(member, clazz))
                        .collect(Collectors.toSet());
            } catch (Exception e) {
                log.error("Redis SMEMBERS操作失败 - key: {}, error: {}", key, e.getMessage());
                return Collections.emptySet();
            }
        });
    }

    @Override
    public <T> boolean sIsMember(String key, T member) {
        return executeWithTimer("sIsMember", () -> {
            try {
                String finalKey = buildKey(key);
                Object processedMember = processValue(member);
                Boolean result = redisTemplate.opsForSet().isMember(finalKey, processedMember);
                return Boolean.TRUE.equals(result);
            } catch (Exception e) {
                log.error("Redis SISMEMBER操作失败 - key: {}, error: {}", key, e.getMessage());
                return false;
            }
        });
    }

    @Override
    public <T> long sRemove(String key, T... members) {
        if (members == null || members.length == 0) {
            return 0;
        }

        return executeWithTimer("sRemove", () -> {
            try {
                String finalKey = buildKey(key);
                Object[] processedMembers = Arrays.stream(members)
                        .map(this::processValue)
                        .toArray();

                Long result = redisTemplate.opsForSet().remove(finalKey, processedMembers);
                return result != null ? result : 0L;
            } catch (Exception e) {
                log.error("Redis SREM操作失败 - key: {}, error: {}", key, e.getMessage());
                return 0L;
            }
        });
    }

    @Override
    public long sSize(String key) {
        return executeWithTimer("sSize", () -> {
            try {
                String finalKey = buildKey(key);
                Long size = redisTemplate.opsForSet().size(finalKey);
                return size != null ? size : 0L;
            } catch (Exception e) {
                log.error("Redis SCARD操作失败 - key: {}, error: {}", key, e.getMessage());
                return 0L;
            }
        });
    }

    // =================== Sorted Set操作 ===================

    @Override
    public <T> boolean zAdd(String key, T value, double score) {
        return executeWithTimer("zAdd", () -> {
            try {
                String finalKey = buildKey(key);
                Object processedValue = processValue(value);
                Boolean result = redisTemplate.opsForZSet().add(finalKey, processedValue, score);
                return Boolean.TRUE.equals(result);
            } catch (Exception e) {
                log.error("Redis ZADD操作失败 - key: {}, error: {}", key, e.getMessage());
                return false;
            }
        });
    }

    @Override
    public <T> long zAdd(String key, Map<T, Double> scoreMembers) {
        if (scoreMembers == null || scoreMembers.isEmpty()) {
            return 0;
        }

        return executeWithTimer("zAddMulti", () -> {
            try {
                String finalKey = buildKey(key);
                long count = 0;

                for (Map.Entry<T, Double> entry : scoreMembers.entrySet()) {
                    if (zAdd(key, entry.getKey(), entry.getValue())) {
                        count++;
                    }
                }

                return count;
            } catch (Exception e) {
                log.error("Redis批量ZADD操作失败 - key: {}, error: {}", key, e.getMessage());
                return 0L;
            }
        });
    }

    @Override
    public <T> Set<T> zRange(String key, long start, long end, Class<T> clazz) {
        return this.<Set<T>>executeWithTimer("zRange", () -> {
            try {
                String finalKey = buildKey(key);
                Set<?> values = redisTemplate.opsForZSet().range(finalKey, start, end);

                if (values == null || values.isEmpty()) {
                    return Collections.<T>emptySet();
                }

                return values.stream()
                        .map(value -> convertValue(value, clazz))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            } catch (Exception e) {
                log.error("Redis ZRANGE操作失败 - key: {}, error: {}", key, e.getMessage());
                return Collections.<T>emptySet();
            }
        });
    }

    @Override
    public <T> Set<T> zRangeByScore(String key, double minScore, double maxScore, Class<T> clazz) {
        return this.<Set<T>>executeWithTimer("zRangeByScore", () -> {
            try {
                String finalKey = buildKey(key);
                Set<?> values = redisTemplate.opsForZSet().rangeByScore(finalKey, minScore, maxScore);

                if (values == null || values.isEmpty()) {
                    return Collections.<T>emptySet();
                }

                return values.stream()
                        .map(value -> convertValue(value, clazz))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            } catch (Exception e) {
                log.error("Redis ZRANGEBYSCORE操作失败 - key: {}, error: {}", key, e.getMessage());
                return Collections.<T>emptySet();
            }
        });
    }

    @Override
    public long zSize(String key) {
        return executeWithTimer("zSize", () -> {
            try {
                String finalKey = buildKey(key);
                Long size = redisTemplate.opsForZSet().size(finalKey);
                return size != null ? size : 0L;
            } catch (Exception e) {
                log.error("Redis ZCARD操作失败 - key: {}, error: {}", key, e.getMessage());
                return 0L;
            }
        });
    }

    @Override
    public <T> long zRemove(String key, T... members) {
        if (members == null || members.length == 0) {
            return 0;
        }

        return executeWithTimer("zRemove", () -> {
            try {
                String finalKey = buildKey(key);
                Object[] processedMembers = Arrays.stream(members)
                        .map(this::processValue)
                        .toArray();

                Long result = redisTemplate.opsForZSet().remove(finalKey, processedMembers);
                return result != null ? result : 0L;
            } catch (Exception e) {
                log.error("Redis ZREM操作失败 - key: {}, error: {}", key, e.getMessage());
                return 0L;
            }
        });
    }

    // =================== 分布式锁 ===================

    @Override
    public boolean tryLock(String lockKey, String lockValue, Duration duration) {
        return executeWithTimer("tryLock", () -> {
            try {
                String finalKey = buildKey(lockKey);
                Boolean result = redisTemplate.opsForValue().setIfAbsent(finalKey, lockValue, duration);
                boolean success = Boolean.TRUE.equals(result);

                log.debug("分布式锁操作 - key: {}, value: {}, duration: {}, result: {}",
                        finalKey, lockValue, duration, success);
                return success;
            } catch (Exception e) {
                log.error("分布式锁获取失败 - key: {}, error: {}", lockKey, e.getMessage());
                return false;
            }
        });
    }

    @Override
    public boolean releaseLock(String lockKey, String lockValue) {
        return executeWithTimer("releaseLock", () -> {
            try {
                String finalKey = buildKey(lockKey);
                Long result = redisTemplate.execute(lockReleaseScript,
                        Collections.singletonList(finalKey), lockValue);
                boolean success = Long.valueOf(1).equals(result);

                log.debug("分布式锁释放 - key: {}, value: {}, result: {}",
                        finalKey, lockValue, success);
                return success;
            } catch (Exception e) {
                log.error("分布式锁释放失败 - key: {}, error: {}", lockKey, e.getMessage());
                return false;
            }
        });
    }

    @Override
    public <T> T executeWithLock(String lockKey, Duration duration, Supplier<T> supplier) {
        String lockValue = UUID.randomUUID().toString();

        if (!tryLock(lockKey, lockValue, duration)) {
            throw new IllegalStateException("获取分布式锁失败: " + lockKey);
        }

        try {
            return supplier.get();
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    @Override
    public void executeWithLock(String lockKey, Duration duration, Runnable runnable) {
        executeWithLock(lockKey, duration, () -> {
            runnable.run();
            return null;
        });
    }

    // =================== 发布订阅 ===================

    @Override
    public long publish(String channel, Object message) {
        return executeWithTimer("publish", () -> {
            try {
                Object processedMessage = processValue(message);
                Long result = redisTemplate.convertAndSend(channel, processedMessage);
                log.debug("Redis PUBLISH - channel: {}, subscribers: {}", channel, result);
                return result != null ? result : 0L;
            } catch (Exception e) {
                log.error("Redis PUBLISH操作失败 - channel: {}, error: {}", channel, e.getMessage());
                return 0L;
            }
        });
    }

    @Override
    public void subscribe(RedisMessageListener messageListener, String... channels) {
        try {
            // 实现消息监听器适配
            org.springframework.data.redis.connection.MessageListener adapter =
                    (message, pattern) -> {
                        try {
                            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
                            Object messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
                            messageListener.onMessage(channel, messageBody);
                        } catch (Exception e) {
                            log.error("处理Redis消息失败: {}", e.getMessage());
                        }
                    };

            // 订阅频道
            for (String channel : channels) {
                listenerContainer.addMessageListener(adapter,
                        org.springframework.data.redis.listener.ChannelTopic.of(channel));
            }

            log.info("Redis消息订阅成功 - channels: {}", Arrays.toString(channels));
        } catch (Exception e) {
            log.error("Redis消息订阅失败 - error: {}", e.getMessage());
        }
    }

    @Override
    public void unsubscribe(String... channels) {
        try {
            // Spring Data Redis的MessageListenerContainer没有直接的取消订阅方法
            // 这里需要重新配置监听器容器或者使用其他方式
            log.warn("Redis取消订阅功能需要完整实现 - channels: {}", Arrays.toString(channels));
        } catch (Exception e) {
            log.error("Redis取消订阅失败 - error: {}", e.getMessage());
        }
    }

    // =================== 缓存管理 ===================

    @Override
    public long clearCache(String pattern) {
        return executeWithTimer("clearCache", () -> {
            try {
                String finalPattern = buildKey(pattern);
                Set<String> keys = redisTemplate.keys(finalPattern);

                if (keys != null && !keys.isEmpty()) {
                    Long result = redisTemplate.delete(keys);
                    log.info("清理缓存完成 - pattern: {}, 清理数量: {}", finalPattern, result);
                    return result != null ? result : 0L;
                } else {
                    log.debug("未找到匹配的缓存键 - pattern: {}", finalPattern);
                    return 0L;
                }
            } catch (Exception e) {
                log.error("清理缓存失败 - pattern: {}, error: {}", pattern, e.getMessage());
                return 0L;
            }
        });
    }

    @Override
    public CacheStats getCacheStats() {
        return new CacheStatsImpl();
    }

    @Override
    public void warmUpCache(CacheLoader cacheLoader) {
        executeWithTimer("warmUpCache", () -> {
            try {
                Map<String, Object> cacheData = cacheLoader.loadCache();
                if (cacheData != null && !cacheData.isEmpty()) {
                    Duration warmUpTtl = getDefaultTtl();
                    long successCount = multiSet(cacheData, warmUpTtl);
                    log.info("缓存预热完成 - 加载数量: {}, 成功数量: {}", cacheData.size(), successCount);
                }
                return null;
            } catch (Exception e) {
                log.error("缓存预热失败: {}", e.getMessage());
                return null;
            }
        });
    }

    // =================== 工具方法 ===================

    @Override
    public Map<String, String> getRedisInfo() {
        return executeWithTimer("getRedisInfo", () -> {
            try {
                Properties info = redisTemplate.getConnectionFactory().getConnection().info();
                Map<String, String> result = new HashMap<>();
                for (Object key : info.keySet()) {
                    result.put(String.valueOf(key), info.getProperty(String.valueOf(key)));
                }
                return result;
            } catch (Exception e) {
                log.error("获取Redis信息失败: {}", e.getMessage());
                return Collections.emptyMap();
            }
        });
    }

    @Override
    public boolean ping() {
        return executeWithTimer("ping", () -> {
            try {
                String result = redisTemplate.getConnectionFactory().getConnection().ping();
                return "PONG".equals(result);
            } catch (Exception e) {
                log.error("Redis PING失败: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public long getDbSize() {
        return executeWithTimer("getDbSize", () -> {
            try {
                Long size = redisTemplate.getConnectionFactory().getConnection().dbSize();
                return size != null ? size : 0L;
            } catch (Exception e) {
                log.error("获取数据库大小失败: {}", e.getMessage());
                return 0L;
            }
        });
    }

    // =================== 私有工具方法 ===================

    /**
     * 构建最终的Redis键，添加配置的前缀
     */
    private String buildKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("Redis键不能为空");
        }

        String prefix = cacheProperties.getRedis().getKeyPrefix();
        if (StringUtils.hasText(prefix)) {
            return prefix + key;
        }
        return key;
    }

    /**
     * 处理值，包括压缩和序列化
     */
    private Object processValue(Object value) {
        if (value == null) {
            return null;
        }

        try {
            // 如果是基本类型，直接返回
            if (isSimpleType(value)) {
                return value;
            }

            // 序列化为JSON
            String jsonValue = objectMapper.writeValueAsString(value);

            // 检查是否需要压缩
            if (cacheProperties.getRedis().getEnableCompression() &&
                    jsonValue.length() > cacheProperties.getRedis().getCompressionThresholdBytes()) {

                return compressValue(jsonValue);
            }

            return jsonValue;
        } catch (JsonProcessingException e) {
            log.error("值序列化失败: {}", e.getMessage());
            return value.toString();
        }
    }

    /**
     * 转换值，包括解压缩和反序列化
     */
    private <T> T convertValue(Object value, Class<T> clazz) {
        if (value == null) {
            return null;
        }

        try {
            String stringValue;

            // 如果是压缩的数据，先解压
            if (value instanceof byte[]) {
                stringValue = decompressValue((byte[]) value);
            } else {
                stringValue = String.valueOf(value);
            }

            // 如果目标类型是字符串，直接返回
            if (clazz == String.class) {
                return clazz.cast(stringValue);
            }

            // 如果是基本类型，进行类型转换
            if (isSimpleType(clazz)) {
                return convertSimpleType(stringValue, clazz);
            }

            // JSON反序列化
            return objectMapper.readValue(stringValue, clazz);
        } catch (Exception e) {
            log.error("值转换失败 - value: {}, targetType: {}, error: {}",
                    value, clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * 压缩值
     */
    private byte[] compressValue(String value) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {

            gzipOut.write(value.getBytes(StandardCharsets.UTF_8));
            gzipOut.finish();

            byte[] compressed = baos.toByteArray();
            double ratio = (1.0 - (double) compressed.length / value.length()) * 100;
            log.debug("数据压缩 - 原始大小: {}, 压缩后: {}, 压缩率: {}%",
                    value.length(), compressed.length, String.format("%.2f", ratio));

            return compressed;
        } catch (IOException e) {
            log.error("数据压缩失败: {}", e.getMessage());
            return value.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * 解压缩值
     */
    private String decompressValue(byte[] compressedData) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipIn = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }

            String result = baos.toString(StandardCharsets.UTF_8);
            log.debug("数据解压缩 - 压缩大小: {}, 解压后: {}",
                    compressedData.length, result.length());

            return result;
        } catch (IOException e) {
            log.error("数据解压缩失败: {}", e.getMessage());
            return new String(compressedData, StandardCharsets.UTF_8);
        }
    }

    /**
     * 判断是否为简单类型
     */
    private boolean isSimpleType(Object value) {
        return isSimpleType(value.getClass());
    }

    /**
     * 判断是否为简单类型
     */
    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                Number.class.isAssignableFrom(clazz) ||
                String.class.isAssignableFrom(clazz) ||
                Boolean.class.isAssignableFrom(clazz) ||
                Character.class.isAssignableFrom(clazz);
    }

    /**
     * 转换简单类型
     */
    @SuppressWarnings("unchecked")
    private <T> T convertSimpleType(String value, Class<T> clazz) {
        if (clazz == Integer.class || clazz == int.class) {
            return (T) Integer.valueOf(value);
        } else if (clazz == Long.class || clazz == long.class) {
            return (T) Long.valueOf(value);
        } else if (clazz == Double.class || clazz == double.class) {
            return (T) Double.valueOf(value);
        } else if (clazz == Float.class || clazz == float.class) {
            return (T) Float.valueOf(value);
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            return (T) Boolean.valueOf(value);
        } else {
            return (T) value;
        }
    }

    /**
     * 获取默认TTL
     */
    private Duration getDefaultTtl() {
        int seconds = cacheProperties.getRedis().getDefaultTtlSeconds();
        return Duration.ofSeconds(seconds);
    }

    /**
     * 执行操作并记录性能指标
     */
    private <T> T executeWithTimer(String operation, Supplier<T> supplier) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return supplier.get();
        } finally {
            sample.stop(Timer.builder("baseai.redis.operation")
                    .tag("operation", operation)
                    .register(meterRegistry));
        }
    }

    /**
     * 缓存统计实现
     */
    private class CacheStatsImpl implements CacheStats {
        @Override
        public long getHitCount() {
            return (long) hitCounter.count();
        }

        @Override
        public long getMissCount() {
            return (long) missCounter.count();
        }

        @Override
        public double getHitRate() {
            long hits = getHitCount();
            long total = hits + getMissCount();
            return total > 0 ? (double) hits / total : 0.0;
        }

        @Override
        public long getTotalKeys() {
            return getDbSize();
        }

        @Override
        public long getUsedMemory() {
            try {
                Map<String, String> info = getRedisInfo();
                String usedMemory = info.get("used_memory");
                return usedMemory != null ? Long.parseLong(usedMemory) : 0L;
            } catch (Exception e) {
                return 0L;
            }
        }
    }
}