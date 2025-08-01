package com.cloud.baseai.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * <h2>大语言模型服务配置属性类</h2>
 *
 * <p>该类管理与大语言模型相关的所有配置，支持多个服务提供商，
 * 包括OpenAI、Claude等，并提供负载均衡和故障转移功能。</p>
 */
@Data
@ConfigurationProperties(prefix = "baseai.llm")
public class LlmProperties {

    /**
     * 默认服务提供商
     */
    private String defaultProvider = "openai";

    /**
     * 是否启用故障转移
     */
    private Boolean failoverEnabled = true;

    /**
     * 负载均衡策略：round_robin、random、weighted
     */
    private String loadBalancing = "round_robin";

    /**
     * OpenAI配置
     */
    private OpenAiProperties openai = new OpenAiProperties();

    /**
     * Claude配置
     */
    private ClaudeProperties claude = new ClaudeProperties();

    /**
     * Qwen配置
     */
    private QwenProperties qwen = new QwenProperties();

    /**
     * 默认模型参数
     */
    private DefaultParametersProperties defaultParameters = new DefaultParametersProperties();

    /**
     * 模型特定参数
     */
    private Map<String, ModelParameters> modelSpecificParameters = Map.of(
            "gpt-4", new ModelParameters(0.5, 2000),
            "gpt-3.5-turbo", new ModelParameters(0.7, 1000),
            "claude-3-sonnet-20240229", new ModelParameters(0.6, 1500)
    );

    /**
     * 高级功能配置
     */
    private FeaturesProperties features = new FeaturesProperties();

    /**
     * OpenAI配置内部类
     */
    @Data
    public static class OpenAiProperties {
        /**
         * 是否启用OpenAI服务
         */
        private Boolean enabled = true;

        /**
         * API基础URL，支持代理服务
         */
        private String baseUrl = "https://api.openai-hk.com";

        /**
         * API密钥
         */
        private String apiKey = "hk-5y2tsx100002516783d25a5427af18502616fb030a23be51";

        /**
         * 组织ID（可选）
         */
        private String organization = "";

        /**
         * 请求超时时间
         */
        private Duration timeout = Duration.ofMinutes(2);

        /**
         * 最大重试次数
         */
        private Integer maxRetries = 3;

        /**
         * 支持的模型列表
         */
        private List<String> models = List.of(
                "gpt-4", "gpt-4-turbo", "gpt-3.5-turbo",
                "text-embedding-3-small", "text-embedding-3-large"
        );
    }

    /**
     * Claude配置内部类
     */
    @Data
    public static class ClaudeProperties {
        /**
         * 是否启用Claude服务
         */
        private Boolean enabled = false;

        /**
         * Claude API密钥
         */
        private String apiKey = "";

        /**
         * API基础URL
         */
        private String baseUrl = "https://api.anthropic.com";

        /**
         * 请求超时时间
         */
        private Duration timeout = Duration.ofMinutes(3);

        /**
         * 最大重试次数
         */
        private Integer maxRetries = 2;

        /**
         * 支持的模型列表
         */
        private List<String> models = List.of(
                "claude-3-sonnet-20240229", "claude-3-haiku-20240307"
        );
    }

    /**
     * Qwen配置内部类
     */
    @Data
    public static class QwenProperties {
        /**
         * 是否启用Qwen服务
         */
        private Boolean enabled = true;

        /**
         * API基础URL，通义千问的服务地址
         */
        private String baseUrl = "https://dashscope.aliyuncs.com";

        /**
         * API密钥
         */
        private String apiKey;

        /**
         * 请求超时时间
         */
        private Duration timeout = Duration.ofMinutes(2);

        /**
         * 最大重试次数
         */
        private Integer maxRetries = 3;

        /**
         * 支持的模型列表
         */
        private List<String> models = List.of(
                "qwen-plus", "qwen-turbo", "qwen-max",
                "qwen-max-longcontext", "text-embedding-v2"
        );
    }

    /**
     * 默认模型参数配置内部类
     */
    @Data
    public static class DefaultParametersProperties {
        /**
         * 默认使用的模型
         */
        private String model = "gpt-3.5-turbo";

        /**
         * 温度参数，控制回答的随机性
         */
        private Double temperature = 0.7;

        /**
         * 最大生成令牌数
         */
        private Integer maxTokens = 1000;

        /**
         * Top-p 采样参数
         */
        private Double topP = 1.0;

        /**
         * 频率惩罚参数
         */
        private Double frequencyPenalty = 0.0;

        /**
         * 存在惩罚参数
         */
        private Double presencePenalty = 0.0;
    }

    /**
     * 模型参数内部类
     */
    @Data
    public static class ModelParameters {
        private Double temperature;
        private Integer maxTokens;

        public ModelParameters() {
        }

        public ModelParameters(Double temperature, Integer maxTokens) {
            this.temperature = temperature;
            this.maxTokens = maxTokens;
        }
    }

    /**
     * 高级功能配置内部类
     */
    @Data
    public static class FeaturesProperties {
        /**
         * 是否启用流式响应
         */
        private Boolean enableStreaming = true;

        /**
         * 是否启用函数调用
         */
        private Boolean enableFunctionCalling = true;

        /**
         * 是否启用响应缓存
         */
        private Boolean enableResponseCache = true;

        /**
         * 响应缓存时间（秒）
         */
        private Integer responseCacheTtl = 3600;
    }
}