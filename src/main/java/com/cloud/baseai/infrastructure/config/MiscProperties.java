package com.cloud.baseai.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * <h1>基础设施模块配置类</h1>
 *
 * <p>这个配置类定义了模块运行所需的各种参数和组件。
 * 通过统一的配置管理，我们可以轻松地调整系统行为，而不需要修改业务代码。</p>
 *
 * <p><b>设计原则：</b></p>
 * <p>配置类应该做到"可配置、可监控、可调优"。每个配置项都有合理的默认值，
 * 同时支持通过外部配置文件进行调整，这样可以适应不同的部署环境。</p>
 */
@Setter
@Getter
@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "baseai.misc")
public class MiscProperties {

    /**
     * 文件上传配置
     */
    private FileUpload fileUpload = new FileUpload();

    /**
     * 模板处理配置
     */
    private Template template = new Template();

    /**
     * 存储配置
     */
    private Storage storage = new Storage();

    /**
     * 文件处理异步执行器
     *
     * <p>文件处理通常是IO密集型操作，使用专门的线程池可以避免阻塞主线程，
     * 提高系统的响应性能。线程池的大小需要根据实际的硬件资源和负载情况调整。</p>
     */
    @Bean("fileProcessingExecutor")
    public Executor fileProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("FileProcessing-");
        executor.setKeepAliveSeconds(60);

        // 优雅关闭：等待当前任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }

    /**
     * 模板处理异步执行器
     *
     * <p>模板的解析和变量提取是CPU密集型操作，使用单独的线程池
     * 可以确保这些操作不会影响其他业务流程的响应速度。</p>
     */
    @Bean("templateProcessingExecutor")
    public Executor templateProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("TemplateProcessing-");
        executor.setKeepAliveSeconds(30);

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }

    /**
     * 文件上传配置内部类
     */
    @Setter
    @Getter
    public static class FileUpload {
        /**
         * 最大文件大小（字节）
         */
        private long maxFileSize = 100L * 1024 * 1024 * 1024; // 100GB

        /**
         * 允许的文件类型
         */
        private String[] allowedMimeTypes = {
                "image/*", "application/pdf", "text/*",
                "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        };

        /**
         * 临时文件清理间隔（小时）
         */
        private int cleanupIntervalHours = 24;

    }

    /**
     * 模板配置内部类
     */
    @Setter
    @Getter
    public static class Template {
        /**
         * 最大模板内容长度
         */
        private int maxContentLength = 10000;

        /**
         * 变量提取正则表达式
         */
        private String variablePattern = "\\{\\{\\s*(\\w+)\\s*\\}\\}";

        /**
         * 是否启用模板缓存
         */
        private boolean cacheEnabled = true;

        /**
         * 缓存过期时间（分钟）
         */
        private int cacheExpirationMinutes = 30;

    }

    /**
     * 存储配置内部类
     */
    @Setter
    @Getter
    public static class Storage {
        /**
         * 默认存储桶
         */
        private String defaultBucket = "default";

        /**
         * 存储统计更新间隔（小时）
         */
        private int statisticsUpdateIntervalHours = 6;

        /**
         * 是否启用文件去重
         */
        private boolean deduplicationEnabled = true;

        /**
         * 已删除文件保留天数
         */
        private int deletedFileRetentionDays = 30;

    }
}