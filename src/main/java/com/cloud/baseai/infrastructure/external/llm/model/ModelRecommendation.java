package com.cloud.baseai.infrastructure.external.llm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>模型服务推荐结果</p>
 *
 * <p>封装一套完整的推荐信息，包括服务提供商、具体的服务、推荐的模型以及推荐的理由。
 * 通常作为服务推荐引擎或决策逻辑的返回对象。<p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelRecommendation {

    /**
     * 服务提供商的名称
     */
    private String provider;

    /**
     * 推荐的模型的具体名称或标识符
     */
    private String model;

    /**
     * 做出此推荐的理由和说明
     */
    private String reason;
}