package com.clinflash.baseai.infrastructure.external.audit.model.dto;

import java.util.Map;

/**
 * 数据保留策略
 */
public record RetentionPolicy(String policyName, int retentionDays, boolean autoArchive, String archiveLocation,
                              Map<String, Object> policyParams) {

}