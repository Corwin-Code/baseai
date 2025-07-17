package com.cloud.baseai.domain.system.model;

import com.cloud.baseai.domain.system.model.enums.SettingValueType;

import java.time.OffsetDateTime;

/**
 * <h2>系统设置领域模型</h2>
 *
 * <p>系统设置定义了系统的行为方式、默认值和配置策略。</p>
 *
 * <p><b>设计哲学：</b></p>
 * <p>好的配置系统应该做到"约定优于配置"，大部分情况下使用合理的默认值，
 * 只在必要时才需要用户修改。同时，配置的变更应该是可追溯的，
 * 确保系统的稳定性和可维护性。</p>
 */
public record SystemSetting(
        String key,
        String value,
        SettingValueType valueType,
        String remark,
        Long updatedBy,
        OffsetDateTime updatedAt
) {

    /**
     * 创建新的系统设置
     */
    public static SystemSetting create(String key, String value, SettingValueType valueType,
                                       String remark, Long createdBy) {
        return new SystemSetting(
                key,
                value,
                valueType,
                remark,
                createdBy,
                OffsetDateTime.now()
        );
    }

    /**
     * 更新设置值
     *
     * <p>设置的更新需要验证新值的格式和合法性。比如数值类型要检查范围，
     * 布尔类型要检查格式，JSON类型要检查语法等。</p>
     */
    public SystemSetting updateValue(String newValue, Long updatedBy) {
        // 验证新值的格式
        if (!isValidValue(newValue)) {
            throw new IllegalArgumentException("设置值格式无效: " + newValue);
        }

        return new SystemSetting(
                this.key,
                newValue,
                this.valueType,
                this.remark,
                updatedBy,
                OffsetDateTime.now()
        );
    }

    /**
     * 验证值的格式是否符合类型要求
     */
    private boolean isValidValue(String value) {
        if (value == null) return false;

        return switch (this.valueType) {
            case STRING -> true; // 字符串类型无特殊限制
            case INTEGER -> {
                try {
                    Integer.parseInt(value);
                    yield true;
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            case BOOLEAN -> "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
            case JSON -> isValidJson(value);
        };
    }

    /**
     * 简单的JSON格式验证
     */
    private boolean isValidJson(String value) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.readTree(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取类型化的值
     */
    public Object getTypedValue() {
        return switch (this.valueType) {
            case STRING -> this.value;
            case INTEGER -> Integer.parseInt(this.value);
            case BOOLEAN -> Boolean.parseBoolean(this.value);
            case JSON -> this.value; // JSON保持字符串形式，由调用者解析
        };
    }

    /**
     * 检查是否为敏感配置
     *
     * <p>敏感配置（如密码、密钥等）在日志和API响应中应该被掩码处理，
     * 确保系统的安全性。</p>
     */
    public boolean isSensitive() {
        String lowerKey = this.key.toLowerCase();
        return lowerKey.contains("password") ||
                lowerKey.contains("secret") ||
                lowerKey.contains("key") ||
                lowerKey.contains("token");
    }

    /**
     * 获取掩码后的值（用于日志和展示）
     */
    public String getMaskedValue() {
        if (isSensitive()) {
            return "***";
        }
        return this.value;
    }
}