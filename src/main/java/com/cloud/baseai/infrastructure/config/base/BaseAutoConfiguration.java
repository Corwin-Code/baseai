package com.cloud.baseai.infrastructure.config.base;

import com.cloud.baseai.infrastructure.utils.AuditUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * <h2>自动配置基类</h2>
 *
 * <p>为所有AutoConfiguration类提供统一的验证和日志功能，
 * 确保配置的一致性和可维护性。</p>
 */
@Slf4j
public abstract class BaseAutoConfiguration {

    /**
     * 配置名称，子类必须实现
     */
    protected abstract String getConfigurationName();

    /**
     * 配置模块标识，用于日志前缀
     */
    protected abstract String getModuleName();

    /**
     * 验证配置的有效性，子类可以重写
     */
    protected void validateConfiguration() {
        // 默认实现为空，子类按需重写
    }

    /**
     * 获取配置摘要信息，用于日志输出
     */
    protected abstract Map<String, Object> getConfigurationSummary();

    /**
     * 初始化配置，统一的入口方法
     */
    protected void initializeConfiguration() {
        String moduleName = getModuleName();
        String configName = getConfigurationName();

        log.info("🚀 正在初始化 {} 配置...", configName);

        try {
            // 执行配置验证
            validateConfiguration();
            log.info("✅ {} 配置验证通过", configName);

            // 输出配置摘要
            outputConfigurationSummary();

            log.info("🎉 {} 配置初始化完成", configName);

        } catch (Exception e) {
            log.error("❌ {} 配置初始化失败: {}", configName, e.getMessage());
            throw new IllegalStateException(String.format("%s 配置初始化失败", configName), e);
        }
    }

    /**
     * 输出配置摘要信息
     */
    private void outputConfigurationSummary() {
        Map<String, Object> summary = getConfigurationSummary();
        if (summary != null && !summary.isEmpty()) {
            log.info("📋 {} 配置摘要:", getConfigurationName());
            summary.forEach((key, value) -> {
                if (value != null) {
                    // 脱敏处理
                    String displayValue = AuditUtils.sanitize(key, value);
                    log.info("   {} = {}", key, displayValue);
                }
            });
        }
    }

    // =================== 通用验证方法 ===================

    /**
     * 验证字符串不为空
     */
    protected void validateNotBlank(String value, String fieldName) {
        Assert.hasText(value, String.format("%s 不能为空", fieldName));
    }

    /**
     * 验证集合不为空
     */
    protected void validateNotEmpty(Collection<?> collection, String fieldName) {
        Assert.notEmpty(collection, String.format("%s 不能为空", fieldName));
    }

    /**
     * 验证对象不为null
     */
    protected void validateNotNull(Object value, String fieldName) {
        Assert.notNull(value, String.format("%s 不能为null", fieldName));
    }

    /**
     * 验证数值范围
     */
    protected void validateRange(Integer value, int min, int max, String fieldName) {
        validateNotNull(value, fieldName);
        Assert.isTrue(value >= min && value <= max,
                String.format("%s 必须在 %d 到 %d 之间，当前值: %d", fieldName, min, max, value));
    }

    /**
     * 验证正数
     */
    protected void validatePositive(Number value, String fieldName) {
        validateNotNull(value, fieldName);
        Assert.isTrue(value.doubleValue() > 0,
                String.format("%s 必须为正数，当前值: %s", fieldName, value));
    }

    /**
     * 验证非负数
     */
    protected void validateNonNegative(Number value, String fieldName) {
        validateNotNull(value, fieldName);
        Assert.isTrue(value.doubleValue() >= 0,
                String.format("%s 不能为负数，当前值: %s", fieldName, value));
    }

    /**
     * 验证URL格式
     */
    protected void validateUrl(String url, String fieldName) {
        validateNotBlank(url, fieldName);
        Assert.isTrue(url.startsWith("http://") || url.startsWith("https://"),
                String.format("%s 必须是有效的URL地址，当前值: %s", fieldName, url));
    }

    /**
     * 验证端口号
     */
    protected void validatePort(Integer port, String fieldName) {
        validateRange(port, 1, 65535, fieldName);
    }

    /**
     * 验证超时时间
     */
    protected void validateTimeout(Duration timeout, String fieldName) {
        validateNotNull(timeout, fieldName);
        Assert.isTrue(!timeout.isNegative() && !timeout.isZero(),
                String.format("%s 必须为正数，当前值: %s", fieldName, timeout));
    }

    /**
     * 验证API密钥格式
     */
    protected void validateApiKey(String apiKey, String fieldName, String expectedPrefix) {
        validateNotBlank(apiKey, fieldName);
        if (StringUtils.hasText(expectedPrefix)) {
            Assert.isTrue(apiKey.startsWith(expectedPrefix),
                    String.format("%s 格式不正确，应以 '%s' 开头", fieldName, expectedPrefix));
        }
        Assert.isTrue(apiKey.length() >= 20,
                String.format("%s 长度不足，至少需要20个字符", fieldName));
    }

    /**
     * 验证文件路径
     */
    protected void validatePath(String path, String fieldName) {
        validateNotBlank(path, fieldName);
        // 可以添加更多路径验证逻辑
    }

    /**
     * 验证枚举值
     */
    protected void validateEnum(String value, List<String> allowedValues, String fieldName) {
        validateNotBlank(value, fieldName);
        Assert.isTrue(allowedValues.contains(value),
                String.format("%s 的值必须是以下之一: %s，当前值: %s", fieldName, allowedValues, value));
    }

    /**
     * 条件验证 - 只有当条件为真时才执行验证
     */
    protected void validateIf(boolean condition, Supplier<RuntimeException> validator) {
        if (condition) {
            try {
                validator.get();
            } catch (RuntimeException e) {
                throw e;
            }
        }
    }

    /**
     * 验证配置组合的有效性
     */
    protected void validateCombination(boolean condition, String message) {
        Assert.isTrue(condition, message);
    }

    // =================== 日志辅助方法 ===================

    /**
     * 对日志参数进行脱敏并进行 String.format 格式化
     */
    private String fmt(String message, Object... args) {
        Object[] safe = AuditUtils.sanitizeArgs(args);
        try {
            return (safe == null || safe.length == 0) ? message : String.format(message, safe);
        } catch (Exception e) {
            // 格式化意外时降级为简单拼接（仍为脱敏后的值）
            StringBuilder sb = new StringBuilder(message);
            sb.append(" | args=");
            for (int i = 0; i < safe.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(safe[i]);
            }
            return sb.toString();
        }
    }

    /**
     * 记录配置警告
     */
    protected void logWarning(String message, Object... args) {
        log.warn("⚠️  [{}] {}", getModuleName(), fmt(message, args));
    }

    /**
     * 记录配置信息
     */
    protected void logInfo(String message, Object... args) {
        log.info("ℹ️  [{}] {}", getModuleName(), fmt(message, args));
    }

    /**
     * 记录配置成功
     */
    protected void logSuccess(String message, Object... args) {
        log.info("✅ [{}] {}", getModuleName(), fmt(message, args));
    }

    /**
     * 记录Bean创建信息
     */
    protected void logBeanCreation(String beanName, String description) {
        log.debug("🔧 [{}] 创建Bean: {} - {}", getModuleName(), fmt("%s", beanName), fmt("%s", description));
    }

    /**
     * 记录Bean创建成功
     */
    protected void logBeanSuccess(String beanName) {
        log.info("✅ [{}] Bean创建成功: {}", getModuleName(), fmt("%s", beanName));
    }

    /**
     * 记录配置跳过信息
     */
    protected void logSkipped(String reason) {
        log.info("⏭️ [{}] 配置跳过: {}", getModuleName(), fmt("%s", reason));
    }
}