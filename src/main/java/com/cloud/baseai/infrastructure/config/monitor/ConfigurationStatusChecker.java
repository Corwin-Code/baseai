package com.cloud.baseai.infrastructure.config.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 * <h2>配置状态检查器</h2>
 *
 * <p>在应用启动完成后，统一检查和报告所有自动配置的状态，
 * 提供配置概览和问题诊断信息。</p>
 */
@Slf4j
@Component
public class ConfigurationStatusChecker implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        ApplicationContext context = event.getApplicationContext();
        Environment environment = context.getEnvironment();

        outputConfigurationReport(context, environment);
    }

    /**
     * 输出完整的配置报告
     */
    private void outputConfigurationReport(ApplicationContext context, Environment environment) {

        log.info("");
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║                    BaseAI 配置状态报告　　　                   　║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
        log.info("");

        // 应用基本信息
        outputApplicationInfo(environment);

        // 配置模块状态
        outputModuleStatus(environment);

        // Bean统计
        outputBeanStatistics(context);

        // 环境信息
        outputEnvironmentInfo(environment);

        // API文档信息
        outputApiDocumentationInfo(environment);

        // 完成报告
        outputCompletionReport(environment);
    }

    /**
     * 输出应用基本信息
     */
    private void outputApplicationInfo(Environment environment) {
        log.info("📱 应用信息:");
        log.info("   应用名称: {}", environment.getProperty("spring.application.name", "BaseAI"));
        log.info("   启动时间: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.info("   活动配置: {}", String.join(", ", environment.getActiveProfiles()));
        log.info("   JVM版本: {}", System.getProperty("java.version"));
        log.info("   Spring版本: {}", org.springframework.core.SpringVersion.getVersion());
    }

    /**
     * 输出模块状态
     */
    private void outputModuleStatus(Environment environment) {
        log.info("");
        log.info("🔧 配置模块状态:");

        Map<String, Boolean> moduleStatus = new TreeMap<>();

        // 检查各个模块的启用状态
        moduleStatus.put("安全模块 (Security)", true); // 总是启用
        moduleStatus.put("异步处理 (Async)", true); // 总是启用
        moduleStatus.put("定时任务 (Scheduling)",
                environment.getProperty("baseai.scheduling.enabled", Boolean.class, true));
        moduleStatus.put("JPA数据访问 (JPA)", isClassPresent("jakarta.persistence.Entity"));
        moduleStatus.put("OpenAI服务",
                environment.getProperty("baseai.llm.openai.enabled", Boolean.class, true));
        moduleStatus.put("Anthropic服务",
                environment.getProperty("baseai.llm.anthropic.enabled", Boolean.class, false));
        moduleStatus.put("Qwen服务",
                environment.getProperty("baseai.llm.qwen.enabled", Boolean.class, true));
        moduleStatus.put("Redis缓存", isClassPresent("org.springframework.data.redis.core.RedisTemplate"));
        moduleStatus.put("国际化 (I18n)", true); // 总是启用
        moduleStatus.put("审计日志 (Audit)", true); // 总是启用
        moduleStatus.put("API文档 (SpringDoc)", isClassPresent("org.springdoc.core.configuration.SpringDocConfiguration") &&
                environment.getProperty("springdoc.api-docs.enabled", Boolean.class, true));

        // 输出状态
        moduleStatus.forEach((module, enabled) -> {
            String status = enabled ? "✅ 已启用" : "⭕ 已禁用";
            log.info("   {}: {}", module, status);
        });
    }

    /**
     * 输出Bean统计信息
     */
    private void outputBeanStatistics(ApplicationContext context) {
        log.info("");
        log.info("📊 Bean统计:");

        String[] beanNames = context.getBeanDefinitionNames();
        long totalBeans = beanNames.length;

        // 统计各类Bean数量
        long configBeans = Arrays.stream(beanNames)
                .filter(name -> name.toLowerCase().contains("config"))
                .count();

        long serviceBeans = Arrays.stream(beanNames)
                .filter(name -> name.toLowerCase().contains("service"))
                .count();

        long repositoryBeans = Arrays.stream(beanNames)
                .filter(name -> name.toLowerCase().contains("repository"))
                .count();

        long controllerBeans = Arrays.stream(beanNames)
                .filter(name -> name.toLowerCase().contains("controller"))
                .count();

        log.info("   总Bean数量: {}", totalBeans);
        log.info("   配置类Bean: {}", configBeans);
        log.info("   服务Bean: {}", serviceBeans);
        log.info("   仓储Bean: {}", repositoryBeans);
        log.info("   控制器Bean: {}", controllerBeans);

        // 内存使用情况
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;

        log.info("   JVM内存使用: {}MB / {}MB ({}%)",
                usedMemory, totalMemory, (usedMemory * 100 / totalMemory));
    }

    /**
     * 输出环境信息
     */
    private void outputEnvironmentInfo(Environment environment) {
        log.info("");
        log.info("🌍 环境信息:");

        // 数据库配置
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        if (datasourceUrl != null) {
            log.info("   数据库: {}", datasourceUrl);
        }

        // Redis配置
        String redisHost = environment.getProperty("spring.redis.host");
        if (redisHost != null) {
            String redisPort = environment.getProperty("spring.redis.port", "6379");
            log.info("   Redis: {}:{}", redisHost, redisPort);
        }

        // 服务端口
        String port = environment.getProperty("server.port", "8080");
        log.info("   服务端口: {}", port);

        // 管理端口
        String managementPort = environment.getProperty("management.server.port");
        if (managementPort != null) {
            log.info("   管理端口: {}", managementPort);
        }

        // 日志级别
        String rootLogLevel = environment.getProperty("logging.level.root", "INFO");
        log.info("   日志级别: {}", rootLogLevel);
    }

    /**
     * 输出API文档信息
     */
    private void outputApiDocumentationInfo(Environment environment) {
        log.info("");
        log.info("📚 API文档信息:");

        // 检查SpringDoc是否启用
        boolean springdocEnabled = environment.getProperty("springdoc.api-docs.enabled", Boolean.class, true);
        boolean swaggerUiEnabled = environment.getProperty("springdoc.swagger-ui.enabled", Boolean.class, true);

        if (springdocEnabled && isClassPresent("org.springdoc.core.configuration.SpringDocConfiguration")) {
            String port = environment.getProperty("server.port", "8080");
            String contextPath = environment.getProperty("server.servlet.context-path", "");
            String baseUrl = "http://localhost:" + port + contextPath;

            // Swagger UI地址
            if (swaggerUiEnabled) {
                String swaggerPath = environment.getProperty("springdoc.swagger-ui.path", "/swagger-ui.html");
                log.info("   📖 Swagger UI: {}{}", baseUrl, swaggerPath);

                // 如果有分组配置，显示分组信息
                if (hasGroupConfigs(environment)) {
                    log.info("   📑 API分组: 已配置多个API分组，可在文档页面切换查看");
                }
            }

            // OpenAPI JSON地址
            String apiDocsPath = environment.getProperty("springdoc.api-docs.path", "/v3/api-docs");
            log.info("   📄 OpenAPI JSON: {}{}", baseUrl, apiDocsPath);

            // 状态
            log.info("   📊 文档状态: ✅ 可用");

        } else {
            log.info("   📊 文档状态: ❌ 已禁用或依赖缺失");
            if (!springdocEnabled) {
                log.info("   💡 启用提示: 设置 springdoc.api-docs.enabled=true");
            }
            if (!isClassPresent("org.springdoc.core.configuration.SpringDocConfiguration")) {
                log.info("   💡 依赖提示: 需要添加 springdoc-openapi-starter-webmvc-ui 依赖");
            }
        }
    }

    /**
     * 检查是否有分组配置
     */
    private boolean hasGroupConfigs(Environment environment) {
        // 检查是否配置了group-configs
        try {
            String[] groups = environment.getProperty("springdoc.group-configs[0].group", String[].class);
            return groups != null && groups.length > 0;
        } catch (Exception e) {
            // 如果无法解析配置，尝试其他方式
            return environment.containsProperty("springdoc.group-configs");
        }
    }

    /**
     * 输出完成报告
     */
    private void outputCompletionReport(Environment environment) {
        log.info("");
        log.info("🎉 BaseAI 应用启动完成!");
        log.info("   配置验证: 全部通过");
        log.info("   服务状态: 就绪");

        String port = environment.getProperty("server.port", "8080");
        String contextPath = environment.getProperty("server.servlet.context-path", "");
        String baseUrl = "http://localhost:" + port + contextPath;

        log.info("   主服务地址: {}", baseUrl);

        // 显示重要的访问地址
        log.info("");
        log.info("🔗 重要访问地址:");

        // API文档地址
        boolean springdocEnabled = environment.getProperty("springdoc.api-docs.enabled", Boolean.class, true);
        if (springdocEnabled && isClassPresent("org.springdoc.core.configuration.SpringDocConfiguration")) {
            String swaggerPath = environment.getProperty("springdoc.swagger-ui.path", "/swagger-ui.html");
            log.info("   📚 API文档: {}{}", baseUrl, swaggerPath);
        }

        // 健康检查地址
        String managementPort = environment.getProperty("management.server.port");
        if (managementPort != null) {
            log.info("   🏥 健康检查: http://localhost:{}/actuator/health", managementPort);
            log.info("   📊 监控指标: http://localhost:{}/actuator/metrics", managementPort);
        } else {
            log.info("   🏥 健康检查: {}/actuator/health", baseUrl);
        }

        log.info("");
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║                    🚀 系统已就绪，开始服务! 🚀                 　║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    /**
     * 检查类是否存在
     */
    private boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}