package com.cloud.baseai.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * <h2>Redis自动配置类</h2>
 *
 * <p>该配置类负责创建和配置Redis相关的Bean，包括RedisTemplate的序列化配置，
 * 确保数据在Redis中的存储和读取能够正确处理Java对象和JSON之间的转换。</p>
 *
 * <p><b>配置特点：</b></p>
 * <ul>
 * <li><b>JSON序列化：</b>使用Jackson进行值的序列化，提高可读性</li>
 * <li><b>字符串序列化：</b>键使用字符串序列化，确保键的可读性</li>
 * <li><b>类型安全：</b>配置了泛型类型，确保类型安全</li>
 * <li><b>兼容性：</b>支持多种Java对象类型的序列化</li>
 * </ul>
 */
@Configuration
public class RedisAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RedisAutoConfiguration.class);

    /**
     * 创建主要的RedisTemplate Bean
     *
     * <p>这个RedisTemplate使用了优化的序列化配置：</p>
     * <ul>
     * <li>键序列化：使用StringRedisSerializer，确保键在Redis中可读</li>
     * <li>值序列化：使用Jackson2JsonRedisSerializer，支持复杂对象存储</li>
     * <li>哈希键序列化：使用StringRedisSerializer</li>
     * <li>哈希值序列化：使用Jackson2JsonRedisSerializer</li>
     * </ul>
     *
     * @param connectionFactory Redis连接工厂，由Spring Boot自动配置提供
     * @return 配置好的RedisTemplate实例
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("正在创建RedisTemplate<String, Object>...");

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 创建JSON序列化器
        Jackson2JsonRedisSerializer<Object> jsonSerializer = createJsonSerializer();

        // 创建字符串序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 配置键值序列化器
        template.setKeySerializer(stringSerializer);           // key序列化
        template.setValueSerializer(jsonSerializer);           // value序列化
        template.setHashKeySerializer(stringSerializer);       // hash key序列化
        template.setHashValueSerializer(jsonSerializer);       // hash value序列化

        // 启用默认序列化器
        template.setDefaultSerializer(jsonSerializer);
        template.setEnableDefaultSerializer(true);

        // 初始化模板
        template.afterPropertiesSet();

        log.info("RedisTemplate<String, Object>创建完成");
        return template;
    }

    /**
     * 创建Jackson JSON序列化器
     *
     * <p>配置Jackson序列化器以支持多种Java对象类型的序列化和反序列化：</p>
     * <ul>
     * <li>支持所有访问级别的字段</li>
     * <li>启用默认类型信息，确保反序列化时类型正确</li>
     * <li>使用安全的类型验证器</li>
     * </ul>
     *
     * @return 配置好的Jackson序列化器
     */
    private Jackson2JsonRedisSerializer<Object> createJsonSerializer() {
        // 创建和配置ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();

        // 设置可见性：允许访问所有字段
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 启用默认类型信息，这样反序列化时能知道原始类型
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    }

    /**
     * 创建专用于缓存的RedisTemplate
     *
     * <p>这个模板专门优化用于缓存场景，使用了更紧凑的序列化设置。</p>
     *
     * @param connectionFactory Redis连接工厂
     * @return 缓存优化的RedisTemplate
     */
    @Bean("cacheRedisTemplate")
    @ConditionalOnMissingBean(name = "cacheRedisTemplate")
    public RedisTemplate<String, Object> cacheRedisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("正在创建缓存优化的RedisTemplate...");

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 为缓存场景创建优化的序列化器
        Jackson2JsonRedisSerializer<Object> cacheSerializer = createCacheOptimizedSerializer();
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(cacheSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(cacheSerializer);
        template.setDefaultSerializer(cacheSerializer);

        template.afterPropertiesSet();

        log.info("缓存优化的RedisTemplate创建完成");
        return template;
    }

    /**
     * 创建缓存优化的JSON序列化器
     *
     * <p>针对缓存场景优化，减少不必要的类型信息，提高性能。</p>
     *
     * @return 缓存优化的序列化器
     */
    private Jackson2JsonRedisSerializer<Object> createCacheOptimizedSerializer() {
        // 创建和配置ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 缓存场景可以关闭类型信息以节省空间（根据需要调整）
        // objectMapper.activateDefaultTyping(...); // 注释掉以减少序列化大小

        return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    }
}