package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.config.base.BaseAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.HashMap;
import java.util.Map;

/**
 * <h2>JPA数据访问配置类</h2>
 *
 * <p>该配置类负责设置JPA相关的核心配置，包括实体扫描、仓储扫描、
 * 事务管理等数据访问层的基础功能。</p>
 */
@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = {
        "com.cloud.baseai.infrastructure.persistence"
})
@EnableJpaRepositories(
        basePackages = {
                "com.cloud.baseai.infrastructure.repository"
        },
        enableDefaultTransactions = true
)
public class SpringJpaAutoConfiguration extends BaseAutoConfiguration {
    /**
     * 实体扫描包路径
     */
    private static final String[] ENTITY_PACKAGES = {
            "com.cloud.baseai.infrastructure.persistence"
    };

    /**
     * Repository扫描包路径
     */
    private static final String[] REPOSITORY_PACKAGES = {
            "com.cloud.baseai.infrastructure.repository"
    };

    public SpringJpaAutoConfiguration() {
//        // 统一初始化
//        initializeConfiguration();
    }

    @Bean
    public SmartLifecycle jpaConfigVerifierLifecycle() {
        return new SmartLifecycle() {
            private volatile boolean running = false;

            @Override
            public void start() {
                // 在容器刷新完成、单例创建后调用
                initializeConfiguration();
                running = true;
            }

            @Override
            public void stop() {
                running = false;
            }

            @Override
            public boolean isRunning() {
                return running;
            }

            @Override
            public int getPhase() {
                return Integer.MAX_VALUE - 10;
            } // 尽量靠后

            @Override
            public boolean isAutoStartup() {
                return true;
            }

            @Override
            public void stop(Runnable callback) {
                callback.run();
                stop();
            }
        };
    }

    @Override
    protected String getConfigurationName() {
        return "JPA数据访问层";
    }

    @Override
    protected String getModuleName() {
        return "JPA";
    }

    @Override
    protected void validateConfiguration() {
        logInfo("开始验证JPA配置...");

        // 验证包路径配置
        validatePackagePaths();

        // 检查类路径依赖
        validateClasspathDependencies();

        // 验证数据库相关配置
        validateDatabaseConfiguration();

        logSuccess("JPA配置验证通过");
    }

    /**
     * 验证包路径配置
     */
    private void validatePackagePaths() {
        logInfo("验证包路径配置...");

        // 验证实体包路径
        for (String packagePath : ENTITY_PACKAGES) {
            validateNotBlank(packagePath, "实体扫描包路径");
            logInfo("实体扫描包: %s", packagePath);
        }

        // 验证Repository包路径
        for (String packagePath : REPOSITORY_PACKAGES) {
            validateNotBlank(packagePath, "Repository扫描包路径");
            logInfo("Repository扫描包: %s", packagePath);
        }

        // 检查包路径重叠
        checkPackageOverlap();
    }

    /**
     * 检查包路径重叠
     */
    private void checkPackageOverlap() {
        for (String entityPkg : ENTITY_PACKAGES) {
            for (String repoPkg : REPOSITORY_PACKAGES) {
                if (entityPkg.equals(repoPkg)) {
                    logWarning("实体包和Repository包路径重叠: %s", entityPkg);
                }
            }
        }
    }

    /**
     * 验证类路径依赖
     */
    private void validateClasspathDependencies() {
        logInfo("检查必要的类路径依赖...");

        // 检查JPA相关类
        checkClassExists("jakarta.persistence.Entity", "JPA Entity注解");
        checkClassExists("org.springframework.data.jpa.repository.JpaRepository", "Spring Data JPA");
        checkClassExists("org.springframework.orm.jpa.JpaTransactionManager", "JPA事务管理器");

        // 检查数据库驱动
        checkDatabaseDrivers();
    }

    /**
     * 检查类是否存在
     */
    private void checkClassExists(String className, String description) {
        try {
            Class.forName(className);
            logInfo("✓ %s 可用", description);
        } catch (ClassNotFoundException e) {
            logWarning("⚠ %s 不可用: %s", description, className);
        }
    }

    /**
     * 检查数据库驱动
     */
    private void checkDatabaseDrivers() {
        String[] drivers = {
                "com.mysql.cj.jdbc.Driver",           // MySQL 8.x
                "com.mysql.jdbc.Driver",              // MySQL 5.x
                "org.postgresql.Driver",              // PostgreSQL
                "com.microsoft.sqlserver.jdbc.SQLServerDriver", // SQL Server
                "org.h2.Driver",                      // H2
                "org.hsqldb.jdbcDriver"              // HSQLDB
        };

        boolean foundDriver = false;
        for (String driver : drivers) {
            try {
                Class.forName(driver);
                logInfo("✓ 发现数据库驱动: %s", driver);
                foundDriver = true;
            } catch (ClassNotFoundException ignored) {
                // 忽略未找到的驱动
            }
        }

        if (!foundDriver) {
            logWarning("未发现任何数据库驱动，请确保添加了相应的数据库依赖");
        }
    }

    /**
     * 验证数据库相关配置
     */
    private void validateDatabaseConfiguration() {
        logInfo("验证数据库配置...");

        // 这里可以添加更多数据库相关的验证
        // 例如连接池配置、数据源配置等

        logInfo("数据库配置检查完成");
    }

    @Override
    protected Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();

        // 基础配置
        summary.put("事务管理", "已启用");
        summary.put("实体扫描包数量", ENTITY_PACKAGES.length);
        summary.put("Repository扫描包数量", REPOSITORY_PACKAGES.length);

        // 包路径信息
        summary.put("实体扫描包", String.join(", ", ENTITY_PACKAGES));
        summary.put("Repository扫描包", String.join(", ", REPOSITORY_PACKAGES));

        // JPA特性
        summary.put("默认事务", "已启用");
        summary.put("Repository实现后缀", "Impl");

        // 运行时信息
        summary.put("JVM可用内存(MB)", Runtime.getRuntime().maxMemory() / 1024 / 1024);

        return summary;
    }

    /**
     * 输出JPA使用建议
     */
    private void outputUsageRecommendations() {
        logInfo("📋 JPA使用建议:");
        logInfo("   1. 实体类应放在 %s 包下", String.join(" 或 ", ENTITY_PACKAGES));
        logInfo("   2. Repository接口应放在 %s 包下", String.join(" 或 ", REPOSITORY_PACKAGES));
        logInfo("   3. 使用 @Transactional 注解进行事务管理");
        logInfo("   4. 复杂查询建议使用 @Query 注解");
        logInfo("   5. 大批量操作建议使用 @Modifying 注解");
        logInfo("   6. 注意N+1查询问题，合理使用 @EntityGraph");
    }
}