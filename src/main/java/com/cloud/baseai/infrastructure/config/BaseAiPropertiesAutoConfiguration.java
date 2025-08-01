package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.config.properties.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>BaseAI配置类启用器</h2>
 *
 * <p>该配置类用于启用所有的配置属性类，确保Spring Boot能够正确加载
 * 和管理各个模块的配置参数。采用集中式的配置启用方式，便于管理。</p>
 * <p>
 * 使用@EnableConfigurationProperties注解批量启用所有配置类，
 * 这样可以避免在每个配置类上重复添加@Component注解。
 */
@Configuration
@EnableConfigurationProperties({
        BaseAiProperties.class,
        SecurityProperties.class,
        RateLimitProperties.class,
        KnowledgeBaseProperties.class,
        ChatProperties.class,
        LlmProperties.class,
        EmailProperties.class,
        SmsProperties.class,
        AuditProperties.class,
        CacheProperties.class,
        AsyncProperties.class,
        SchedulingProperties.class,
        StorageProperties.class
})
public class BaseAiPropertiesAutoConfiguration {

    /*
      该类不需要包含任何方法，仅用于启用配置属性类
      Spring Boot会自动扫描并创建相应的配置Bean
     */
}