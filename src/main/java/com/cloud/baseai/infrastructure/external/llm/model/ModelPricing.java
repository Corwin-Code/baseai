package com.cloud.baseai.infrastructure.external.llm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型定价信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModelPricing {
    /**
     * 每百万输入token的价格
     */
    private double inputPricePerMTokens;
    /**
     * 每百万输出token的价格
     */
    private double outputPricePerMTokens;
}