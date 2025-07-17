package com.cloud.baseai.infrastructure.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * <h2>JPA配置</h2>
 *
 * <p>配置实体扫描和仓储扫描路径。</p>
 */
@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = {
        "com.cloud.baseai.infrastructure.persistence"
})
@EnableJpaRepositories(basePackages = {
        "com.cloud.baseai.infrastructure.repository"
})
public class JpaConfig {
}