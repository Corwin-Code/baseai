package com.cloud.baseai.domain.audit.model;

import lombok.Getter;

/**
 * 审计模式枚举
 *
 * <p>不同的审计模式适用于不同的业务场景和性能要求。</p>
 */
@Getter
public enum AuditMode {
    /**
     * 标准模式
     * <p>平衡性能和功能，适用于大多数场景。</p>
     */
    STANDARD("标准模式"),

    /**
     * 高性能模式
     * <p>优先考虑性能，减少详细信息记录。</p>
     */
    HIGH_PERFORMANCE("高性能模式"),

    /**
     * 高安全模式
     * <p>记录更详细的安全信息，适用于高安全要求的环境。</p>
     */
    HIGH_SECURITY("高安全模式"),

    /**
     * 合规模式
     * <p>严格按照法规要求记录信息，适用于受监管的行业。</p>
     */
    COMPLIANCE("合规模式");

    private final String description;

    AuditMode(String description) {
        this.description = description;
    }
}