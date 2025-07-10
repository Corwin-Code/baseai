package com.clinflash.baseai.infrastructure.config;

import com.clinflash.baseai.infrastructure.external.llm.EmbeddingService;
import com.clinflash.baseai.infrastructure.external.llm.impl.MockEmbeddingService;
import com.clinflash.baseai.infrastructure.external.llm.impl.OpenAIEmbeddingService;
import com.clinflash.baseai.infrastructure.external.llm.impl.QianWenEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>嵌入服务自动配置</h2>
 *
 * <p>这个配置类实现了Spring Boot的自动配置机制，让整个系统能够根据配置
 * 自动选择和初始化合适的嵌入服务实现。</p>
 *
 * <p>就像是一个智能的工厂管理系统，能够根据订单需求自动配置生产线。</p>
 */
@Configuration
@EnableConfigurationProperties(EmbeddingServiceConfig.class)
public class EmbeddingServiceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingServiceAutoConfiguration.class);

    /**
     * OpenAI嵌入服务Bean
     */
    @Bean
    @ConditionalOnProperty(name = "kb.embedding.provider", havingValue = "openai")
    public EmbeddingService openaiEmbeddingService(EmbeddingServiceConfig config) {
        validateOpenAIConfig(config);
        log.info("创建OpenAI嵌入服务");
        return new OpenAIEmbeddingService(config);
    }

    /**
     * 千问嵌入服务Bean
     */
    @Bean
    @ConditionalOnProperty(name = "kb.embedding.provider", havingValue = "qianwen")
    public EmbeddingService qianwenEmbeddingService(EmbeddingServiceConfig config) {
        validateQianWenConfig(config);
        log.info("创建千问嵌入服务");
        return new QianWenEmbeddingService(config);
    }

    /**
     * 模拟嵌入服务Bean（开发测试用）
     * <p>
     * 注意这里的条件注解顺序很重要：
     * 1. 首先检查配置属性，如果明确指定为mock则创建
     * 2. matchIfMissing=true表示如果没有配置这个属性，也会创建这个Bean
     * 3. @ConditionalOnMissingBean确保只有在没有其他EmbeddingService时才创建
     */
    @Bean
    @ConditionalOnProperty(name = "kb.embedding.provider", havingValue = "mock", matchIfMissing = true)
    @ConditionalOnMissingBean(EmbeddingService.class)
    public EmbeddingService mockEmbeddingService() {
        log.info("创建模拟嵌入服务（开发模式）");
        return new MockEmbeddingService();
    }

    /**
     * 验证OpenAI配置
     */
    private void validateOpenAIConfig(EmbeddingServiceConfig config) {
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            throw new IllegalArgumentException("OpenAI API密钥未配置，请设置 kb.embedding.api-key");
        }

        if (config.getOpenai().getBaseUrl() == null || config.getOpenai().getBaseUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("OpenAI API URL未配置");
        }

        log.info("OpenAI配置验证通过: baseUrl={}, model={}",
                config.getOpenai().getBaseUrl(),
                config.getOpenai().getDefaultModel());
    }

    /**
     * 验证千问配置
     */
    private void validateQianWenConfig(EmbeddingServiceConfig config) {
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            throw new IllegalArgumentException("千问API密钥未配置，请设置 kb.embedding.api-key");
        }

        if (config.getQianwen().getBaseUrl() == null || config.getQianwen().getBaseUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("千问API URL未配置");
        }

        log.info("千问配置验证通过: baseUrl={}, model={}",
                config.getQianwen().getBaseUrl(),
                config.getQianwen().getDefaultModel());
    }
}