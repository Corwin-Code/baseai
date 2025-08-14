package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.config.base.BaseAutoConfiguration;
import com.cloud.baseai.infrastructure.config.properties.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
        StorageProperties.class,
        MessageSourceProperties.class
})
public class BaseAiPropertiesAutoConfiguration extends BaseAutoConfiguration {

    private final ConfigurableEnvironment environment;

    // 注入所有配置属性类以进行验证
    @Autowired(required = false)
    private BaseAiProperties baseAiProperties;

    @Autowired(required = false)
    private SecurityProperties securityProperties;

    @Autowired(required = false)
    private LlmProperties llmProperties;

    @Autowired(required = false)
    private KnowledgeBaseProperties knowledgeBaseProperties;

    @Autowired(required = false)
    private AsyncProperties asyncProperties;

    /**
     * 配置属性类列表
     */
    private static final Class<?>[] PROPERTY_CLASSES = {
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
    };

    public BaseAiPropertiesAutoConfiguration(ConfigurableEnvironment environment) {
        this.environment = environment;

        // 统一初始化
        initializeConfiguration();
    }

    @Override
    protected String getConfigurationName() {
        return "BaseAI配置属性管理";
    }

    @Override
    protected String getModuleName() {
        return "PROPERTIES";
    }

    @Override
    protected void validateConfiguration() {
        logInfo("开始验证BaseAI配置属性...");

        // 验证配置属性类的加载状态
        validatePropertyClassesLoading();

        // 验证环境配置
        validateEnvironmentConfiguration();

        // 验证关键配置项
        validateCriticalConfigurations();

        // 验证配置一致性
        validateConfigurationConsistency();

        // 提供环境特定建议
        provideEnvironmentSpecificAdvice();

        logSuccess("BaseAI配置属性验证通过");
    }

    /**
     * 验证配置属性类的加载状态
     */
    private void validatePropertyClassesLoading() {
        logInfo("检查配置属性类加载状态...");

        int loadedCount = 0;
        int totalCount = PROPERTY_CLASSES.length;

        for (Class<?> propertyClass : PROPERTY_CLASSES) {
            String className = propertyClass.getSimpleName();

            try {
                // 检查是否有@ConfigurationProperties注解
                ConfigurationProperties annotation = propertyClass.getAnnotation(ConfigurationProperties.class);
                if (annotation != null) {
                    String prefix = annotation.prefix();

                    // 检查是否有相关配置
                    boolean hasConfig = hasConfigurationWithPrefix(prefix);

                    if (hasConfig) {
                        logInfo("✓ %s - 前缀: %s (已配置)", className, prefix);
                        loadedCount++;
                    } else {
                        logInfo("○ %s - 前缀: %s (使用默认值)", className, prefix);
                        loadedCount++;
                    }
                } else {
                    logWarning("✗ %s - 缺少@ConfigurationProperties注解", className);
                }
            } catch (Exception e) {
                logWarning("✗ %s - 检查时出错: %s", className, e.getMessage());
            }
        }

        logInfo("配置属性类加载统计: %d/%d 个类已启用", loadedCount, totalCount);

        if (loadedCount < totalCount) {
            logWarning("部分配置属性类未正确加载，请检查类路径和注解配置");
        }
    }

    /**
     * 检查是否存在指定前缀的配置
     */
    private boolean hasConfigurationWithPrefix(String prefix) {
        // 检查environment中是否有以该前缀开头的属性
        if (environment instanceof ConfigurableEnvironment configurableEnv) {
            return configurableEnv.getPropertySources().stream()
                    .filter(ps -> ps instanceof EnumerablePropertySource<?>)
                    .map(ps -> (EnumerablePropertySource<?>) ps)
                    .flatMap(ps -> Arrays.stream(ps.getPropertyNames()))
                    .anyMatch(name -> name.startsWith(prefix));
        }
        return false;
    }

    /**
     * 验证环境配置
     */
    private void validateEnvironmentConfiguration() {
        logInfo("验证运行环境配置...");

        // 检查活动配置文件
        String[] activeProfiles = environment.getActiveProfiles();
        String[] defaultProfiles = environment.getDefaultProfiles();

        if (activeProfiles.length > 0) {
            logInfo("活动配置文件: %s", String.join(", ", activeProfiles));
        } else if (defaultProfiles.length > 0) {
            logInfo("默认配置文件: %s", String.join(", ", defaultProfiles));
        } else {
            logWarning("未检测到任何配置文件，使用内建默认配置");
        }

        // 检查重要的环境变量
        checkEnvironmentVariable("JAVA_HOME", "Java安装路径");
        checkEnvironmentVariable("SPRING_PROFILES_ACTIVE", "Spring活动配置");
        checkEnvironmentVariable("SERVER_PORT", "服务端口");

        // 检查系统属性
        logInfo("JVM配置:");
        logInfo("  Java版本: %s", System.getProperty("java.version"));
        logInfo("  操作系统: %s %s",
                System.getProperty("os.name"), System.getProperty("os.version"));
        logInfo("  文件编码: %s", Charset.defaultCharset().displayName());
        logInfo("  时区: %s", System.getProperty("user.timezone"));
    }

    /**
     * 检查环境变量
     */
    private void checkEnvironmentVariable(String name, String description) {
        String value = System.getenv(name);
        if (value != null) {
            logInfo("环境变量 %s (%s): %s", name, description,
                    name.toLowerCase().contains("key") || name.toLowerCase().contains("secret") ?
                            "****" : value);
        } else {
            logInfo("环境变量 %s (%s): 未设置", name, description);
        }
    }

    /**
     * 验证关键配置项
     */
    private void validateCriticalConfigurations() {
        logInfo("验证关键配置项...");

        // 检查数据库配置
        validateDatabaseConfiguration();

        // 检查安全配置
        validateSecurityConfiguration();

        // 检查LLM配置
        validateLlmConfiguration();

        // 检查存储配置
        validateStorageConfiguration();
    }

    /**
     * 验证数据库配置
     */
    private void validateDatabaseConfiguration() {
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        String datasourceDriver = environment.getProperty("spring.datasource.driver-class-name");

        if (datasourceUrl != null) {
            logInfo("✓ 数据库配置已设置");
            if (datasourceUrl.contains("h2")) {
                logWarning("使用H2内存数据库，仅适用于开发和测试环境");
            } else if (datasourceUrl.contains("mysql")) {
                logInfo("使用MySQL数据库");
            } else if (datasourceUrl.contains("postgresql")) {
                logInfo("使用PostgreSQL数据库");
            }
        } else {
            logWarning("未检测到数据库配置，请确认spring.datasource.url已设置");
        }
    }

    /**
     * 验证安全配置
     */
    private void validateSecurityConfiguration() {
        if (securityProperties != null) {
            logInfo("✓ 安全配置已加载");

            // 检查JWT配置
            if (securityProperties.getJwt().isUseRsa()) {
                boolean hasPrivateKey = securityProperties.getJwt().getRsaPrivateKey() != null;
                boolean hasPublicKey = securityProperties.getJwt().getRsaPublicKey() != null;

                if (hasPrivateKey && hasPublicKey) {
                    logInfo("✓ JWT RSA密钥配置完整");
                } else {
                    logWarning("JWT RSA密钥配置不完整，请检查配置文件");
                }
            } else {
                boolean hasSecret = securityProperties.getJwt().getSecret() != null &&
                        securityProperties.getJwt().getSecret().length() >= 32;
                if (hasSecret) {
                    logInfo("✓ JWT HMAC密钥配置正确");
                } else {
                    logWarning("JWT HMAC密钥配置不正确，密钥长度应至少32字符");
                }
            }
        } else {
            logWarning("安全配置未加载，请检查SecurityProperties配置");
        }
    }

    /**
     * 验证LLM配置
     */
    private void validateLlmConfiguration() {
        if (llmProperties != null) {
            logInfo("✓ LLM配置已加载");

            // 检查各个LLM提供商的配置
            if (llmProperties.getOpenai().getEnabled()) {
                boolean hasApiKey = llmProperties.getOpenai().getApiKey() != null &&
                        !llmProperties.getOpenai().getApiKey().isEmpty();
                logInfo("OpenAI: %s", hasApiKey ? "已配置" : "缺少API密钥");
            }

            if (llmProperties.getAnthropic().getEnabled()) {
                boolean hasApiKey = llmProperties.getAnthropic().getApiKey() != null &&
                        !llmProperties.getAnthropic().getApiKey().isEmpty();
                logInfo("Anthropic: %s", hasApiKey ? "已配置" : "缺少API密钥");
            }

            if (llmProperties.getQwen().getEnabled()) {
                boolean hasApiKey = llmProperties.getQwen().getApiKey() != null &&
                        !llmProperties.getQwen().getApiKey().isEmpty();
                logInfo("通义千问: %s", hasApiKey ? "已配置" : "缺少API密钥");
            }
        } else {
            logWarning("LLM配置未加载，请检查LlmProperties配置");
        }
    }

    /**
     * 验证存储配置
     */
    private void validateStorageConfiguration() {
        String tempDir = System.getProperty("java.io.tmpdir");
        logInfo("系统临时目录: %s", tempDir);

        // 检查存储目录权限
        java.io.File temp = new java.io.File(tempDir);
        if (temp.canRead() && temp.canWrite()) {
            logInfo("✓ 临时目录可读写");
        } else {
            logWarning("临时目录权限不足，可能影响文件操作");
        }
    }

    /**
     * 验证配置一致性
     */
    private void validateConfigurationConsistency() {
        logInfo("检查配置一致性...");

        // 检查异步配置和调度配置的一致性
        if (asyncProperties != null) {
            int asyncCoreSize = asyncProperties.getCorePoolSize();
            int cpuCores = Runtime.getRuntime().availableProcessors();

            if (asyncCoreSize > cpuCores * 4) {
                logWarning("异步线程池核心大小 (%d) 远超CPU核心数 (%d)，可能影响性能",
                        asyncCoreSize, cpuCores);
            }
        }

        // 检查缓存和数据库配置一致性
        String redisHost = environment.getProperty("spring.redis.host");
        String cacheType = environment.getProperty("spring.cache.type");

        if ("redis".equals(cacheType) && redisHost == null) {
            logWarning("缓存类型设置为Redis但未配置Redis连接信息");
        }
    }

    /**
     * 提供环境特定建议
     */
    private void provideEnvironmentSpecificAdvice() {
        String[] activeProfiles = environment.getActiveProfiles();

        if (activeProfiles.length == 0 || Arrays.asList(activeProfiles).contains("default")) {
            logWarning("使用默认配置文件，生产环境建议明确指定配置文件");
        }

        for (String profile : activeProfiles) {
            switch (profile.toLowerCase()) {
                case "dev", "development" -> {
                    logInfo("💡 开发环境建议:");
                    logInfo("   - 启用热重载功能");
                    logInfo("   - 使用详细的日志级别");
                    logInfo("   - 考虑使用H2内存数据库");
                }
                case "test", "testing" -> {
                    logInfo("💡 测试环境建议:");
                    logInfo("   - 使用独立的测试数据库");
                    logInfo("   - 启用测试专用配置");
                    logInfo("   - 考虑Mock外部服务");
                }
                case "prod", "production" -> {
                    logInfo("💡 生产环境建议:");
                    logInfo("   - 确保所有密钥通过环境变量配置");
                    logInfo("   - 启用健康检查和监控");
                    logInfo("   - 配置适当的日志级别");
                    logInfo("   - 启用安全头和HTTPS");
                }
                case "staging" -> {
                    logInfo("💡 预发布环境建议:");
                    logInfo("   - 使用与生产环境相似的配置");
                    logInfo("   - 启用详细监控");
                    logInfo("   - 进行性能测试");
                }
            }
        }
    }

    @Override
    protected Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();

        // 基础信息
        summary.put("配置属性类数量", PROPERTY_CLASSES.length);
        summary.put("活动配置文件", String.join(", ", environment.getActiveProfiles()));
        summary.put("Java版本", System.getProperty("java.version"));
        summary.put("操作系统", System.getProperty("os.name"));

        // 配置状态
        summary.put("安全配置", securityProperties != null ? "已加载" : "未加载");
        summary.put("LLM配置", llmProperties != null ? "已加载" : "未加载");
        summary.put("知识库配置", knowledgeBaseProperties != null ? "已加载" : "未加载");
        summary.put("异步配置", asyncProperties != null ? "已加载" : "未加载");

        // 环境信息
        summary.put("数据库配置",
                environment.getProperty("spring.datasource.url") != null ? "已配置" : "未配置");
        summary.put("Redis配置",
                environment.getProperty("spring.redis.host") != null ? "已配置" : "未配置");

        // 系统资源
        Runtime runtime = Runtime.getRuntime();
        summary.put("CPU核心数", Runtime.getRuntime().availableProcessors());
        summary.put("JVM最大内存(MB)", runtime.maxMemory() / 1024 / 1024);
        summary.put("JVM当前内存(MB)", runtime.totalMemory() / 1024 / 1024);

        return summary;
    }
}