package com.clinflash.baseai.infrastructure.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>AI 客户端统一配置。</p>
 * <p>如同时接入 Azure/OpenAI/自托管模型，可在此进行适配。</p>
 */
@Configuration
public class AiConfig {

    /**
     * 将具体实现别名为通用接口，方便后续替换。
     */
    @Bean
    public EmbeddingModel embeddingClient(OpenAiEmbeddingModel model) {
        return model;
    }
}
