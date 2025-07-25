package com.cloud.baseai.domain.chat.model;

import lombok.Getter;

/**
 * 对话质量评估结果
 */
@Getter
public class ConversationQuality {
    private final float relevance;
    private final float completeness;
    private final float accuracy;
    private final float responseTime;
    private final float overallScore;

    private ConversationQuality(Builder builder) {
        this.relevance = builder.relevance;
        this.completeness = builder.completeness;
        this.accuracy = builder.accuracy;
        this.responseTime = builder.responseTime;
        this.overallScore = builder.overallScore;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private float relevance;
        private float completeness;
        private float accuracy;
        private float responseTime;
        private float overallScore;

        public Builder relevance(float relevance) {
            this.relevance = relevance;
            return this;
        }

        public Builder completeness(float completeness) {
            this.completeness = completeness;
            return this;
        }

        public Builder accuracy(float accuracy) {
            this.accuracy = accuracy;
            return this;
        }

        public Builder responseTime(float responseTime) {
            this.responseTime = responseTime;
            return this;
        }

        public Builder overallScore(float overallScore) {
            this.overallScore = overallScore;
            return this;
        }

        public ConversationQuality build() {
            return new ConversationQuality(this);
        }
    }
}