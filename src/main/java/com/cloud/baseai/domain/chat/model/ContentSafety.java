package com.cloud.baseai.domain.chat.model;

import lombok.Getter;

/**
 * 内容安全评估结果
 */
public class ContentSafety {
    private final boolean hasPersonalInfo;
    private final boolean hasInappropriateContent;
    private final boolean hasCommercialSecrets;
    @Getter
    private final String riskLevel;

    private ContentSafety(Builder builder) {
        this.hasPersonalInfo = builder.hasPersonalInfo;
        this.hasInappropriateContent = builder.hasInappropriateContent;
        this.hasCommercialSecrets = builder.hasCommercialSecrets;
        this.riskLevel = builder.riskLevel;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean hasPersonalInfo() {
        return hasPersonalInfo;
    }

    public boolean hasInappropriateContent() {
        return hasInappropriateContent;
    }

    public boolean hasCommercialSecrets() {
        return hasCommercialSecrets;
    }

    public static class Builder {
        private boolean hasPersonalInfo;
        private boolean hasInappropriateContent;
        private boolean hasCommercialSecrets;
        private String riskLevel;

        public Builder hasPersonalInfo(boolean hasPersonalInfo) {
            this.hasPersonalInfo = hasPersonalInfo;
            return this;
        }

        public Builder hasInappropriateContent(boolean hasInappropriateContent) {
            this.hasInappropriateContent = hasInappropriateContent;
            return this;
        }

        public Builder hasCommercialSecrets(boolean hasCommercialSecrets) {
            this.hasCommercialSecrets = hasCommercialSecrets;
            return this;
        }

        public Builder riskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public ContentSafety build() {
            return new ContentSafety(this);
        }
    }
}