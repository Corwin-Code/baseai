package com.cloud.baseai.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>BaseAI应用主配置类</h2>
 *
 * <p>该类作为配置的入口点，通过嵌套属性的方式组织各个功能模块的配置。
 * 采用模块化设计，每个子模块都有独立的配置类，便于维护和扩展。</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "baseai")
public class BaseAiProperties {

    /**
     * 安全相关配置
     * 包括JWT、CORS、密码策略等安全设置
     */
    @NestedConfigurationProperty
    private SecurityProperties security = new SecurityProperties();

    /**
     * 速率限制配置
     * 用于控制API调用频率，防止系统过载
     */
    @NestedConfigurationProperty
    private RateLimitProperties rateLimit = new RateLimitProperties();

    /**
     * 知识库模块配置
     * 包括文档处理、向量嵌入、搜索等功能设置
     */
    @NestedConfigurationProperty
    private KnowledgeBaseProperties knowledgeBase = new KnowledgeBaseProperties();

    /**
     * 对话系统配置
     * 包括消息处理、知识检索、AI功能等设置
     */
    @NestedConfigurationProperty
    private ChatProperties chat = new ChatProperties();

    /**
     * 大语言模型服务配置
     * 支持多个LLM提供商的配置和管理
     */
    @NestedConfigurationProperty
    private LlmProperties llm = new LlmProperties();

    /**
     * 邮件服务配置
     * 包括发送设置、模板管理、投递策略等
     */
    @NestedConfigurationProperty
    private EmailProperties email = new EmailProperties();

    /**
     * 短信服务配置
     * 支持多个短信服务提供商的配置
     */
    @NestedConfigurationProperty
    private SmsProperties sms = new SmsProperties();

    /**
     * 审计系统配置
     * 用于系统操作日志记录和合规性管理
     */
    @NestedConfigurationProperty
    private AuditProperties audit = new AuditProperties();

    /**
     * 缓存配置
     * 包括Redis缓存和应用级缓存设置
     */
    @NestedConfigurationProperty
    private CacheProperties cache = new CacheProperties();

    /**
     * 异步处理配置
     * 线程池和异步任务相关设置
     */
    @NestedConfigurationProperty
    private AsyncProperties async = new AsyncProperties();

    /**
     * 定时任务配置
     * 线程池任务调度器的配置参数
     */
    @NestedConfigurationProperty
    private SchedulingProperties scheduling = new SchedulingProperties();

    /**
     * 文件存储配置
     * 支持本地存储和云存储配置
     */
    @NestedConfigurationProperty
    private StorageProperties storage = new StorageProperties();
}