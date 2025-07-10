package com.clinflash.baseai.infrastructure.config;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

/**
 * <h2>LLM服务配置属性类</h2>
 */
@Setter
@Getter
public class LLMServiceProperties {

    private String defaultProvider = "openai";
    private boolean failoverEnabled = true;
    private Duration defaultTimeout = Duration.ofMinutes(2);

    private OpenAIProperties openai = new OpenAIProperties();
    private ClaudeProperties claude = new ClaudeProperties();
    private MockProperties mock = new MockProperties();

    /**
     * OpenAI配置属性
     */
    @Setter
    @Getter
    public static class OpenAIProperties {
        private boolean enabled = true;
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String organization;
        private Duration timeout = Duration.ofMinutes(2);
    }

    /**
     * Claude配置属性
     */
    @Setter
    @Getter
    public static class ClaudeProperties {
        private boolean enabled = false;
        private String apiKey;
        private String baseUrl = "https://api.anthropic.com";
        private Duration timeout = Duration.ofMinutes(3);
    }

    /**
     * Mock配置属性
     */
    @Setter
    @Getter
    public static class MockProperties {
        private boolean enabled = false;
        private Duration simulatedDelay = Duration.ofMillis(500);
        private boolean simulateErrors = false;
        private double errorRate = 0.1;
    }
}