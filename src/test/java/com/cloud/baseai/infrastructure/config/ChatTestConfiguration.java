package com.cloud.baseai.infrastructure.config;

import com.cloud.baseai.infrastructure.external.llm.ChatCompletionService;
import com.cloud.baseai.infrastructure.external.llm.impl.MockChatCompletionService;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * <h2>Chat模块测试配置</h2>
 *
 * <p>测试是保证代码质量的重要手段。这个配置类为测试环境提供了
 * 专门的Mock实现，确保测试的可重复性和独立性。</p>
 */
@TestConfiguration
@Profile("test")
@Import(JacksonAutoConfiguration.class)
public class ChatTestConfiguration {

    /**
     * 测试环境专用的Mock Chat服务
     */
    @Bean
    @Primary
    public ChatCompletionService testChatCompletionService() {
        return new MockChatCompletionService();
    }
}