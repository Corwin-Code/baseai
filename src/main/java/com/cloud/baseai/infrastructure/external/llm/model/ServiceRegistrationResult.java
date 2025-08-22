package com.cloud.baseai.infrastructure.external.llm.model;

/**
 * 服务注册结果记录
 *
 * @param success 是否成功
 * @param status  状态描述
 */
public record ServiceRegistrationResult(boolean success, String status) {

    public static ServiceRegistrationResult success(String message) {
        return new ServiceRegistrationResult(true, "✓ " + message);
    }

    public static ServiceRegistrationResult failed(String reason) {
        return new ServiceRegistrationResult(false, "✗ " + reason);
    }

    public static ServiceRegistrationResult disabled(String reason) {
        return new ServiceRegistrationResult(false, "- " + reason);
    }
}