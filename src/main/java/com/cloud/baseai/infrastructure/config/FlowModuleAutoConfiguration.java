package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.flow.service.NodeTypeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <h2>流程模块自动配置</h2>
 *
 * <p>自动配置流程编排模块的核心Bean，确保所有组件正确初始化和装配。</p>
 */
@Configuration
public class FlowModuleAutoConfiguration {

    /**
     * 配置ObjectMapper
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper flowObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    /**
     * 配置流程配置
     */
    @Bean
    @ConditionalOnMissingBean
    public FlowProperties flowConfig() {
        return new FlowProperties();
    }

    /**
     * 配置节点类型服务
     */
    @Bean
    @ConditionalOnMissingBean
    public NodeTypeService nodeTypeService(ObjectMapper objectMapper) {
        return new NodeTypeService(objectMapper);
    }
}