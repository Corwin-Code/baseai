package com.cloud.baseai.infrastructure.security.service;

import com.cloud.baseai.infrastructure.config.properties.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <h1>密码历史管理服务</h1>
 *
 * <p>这个服务确保用户不会反复使用相同的密码，
 * 从而提高账户安全性。即使一个旧密码被泄露，攻击者也无法用它来重新设置密码。</p>
 *
 * <p><b>核心功能：</b></p>
 * <ul>
 *   <li>记录用户的密码历史</li>
 *   <li>防止重复使用最近的N个密码</li>
 *   <li>检测密码是否在泄露数据库中</li>
 *   <li>评估密码强度</li>
 * </ul>
 */
@Service
public class PasswordHistoryService {

    private static final Logger log = LoggerFactory.getLogger(PasswordHistoryService.class);

    private static final String PASSWORD_HISTORY_KEY = "password:history:";
    private static final String PASSWORD_CHANGE_COUNT_KEY = "password:change:count:";
    private static final String WEAK_PASSWORD_KEY = "password:weak:list";

    private final RedisTemplate<String, Object> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;

    public PasswordHistoryService(RedisTemplate<String, Object> redisTemplate,
                                  PasswordEncoder passwordEncoder,
                                  SecurityProperties securityProperties) {
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
        this.securityProperties = securityProperties;

        // 初始化弱密码列表
        initializeWeakPasswordList();
    }

    /**
     * 检查密码是否被重复使用
     *
     * <p>这个方法检查新密码是否在用户的密码历史中出现过。
     * 就像您不能用同一把钥匙开两次锁一样，同一个密码不应该被重复使用。</p>
     *
     * @param userId      用户ID
     * @param newPassword 新密码（明文）
     * @return 如果密码被重复使用返回true
     */
    public boolean isPasswordReused(Long userId, String newPassword) {
        if (userId == null || newPassword == null) {
            return false;
        }

        int historyCount = securityProperties.getPassword().getHistoryCount();
        if (historyCount <= 0) {
            // 如果配置为0，表示不检查密码历史
            return false;
        }

        try {
            String key = PASSWORD_HISTORY_KEY + userId;
            List<Object> history = redisTemplate.opsForList().range(key, 0, historyCount - 1);

            if (history == null || history.isEmpty()) {
                return false;
            }

            // 检查新密码是否与历史密码匹配
            for (Object obj : history) {
                PasswordHistoryEntry entry = (PasswordHistoryEntry) obj;
                if (passwordEncoder.matches(newPassword, entry.hashedPassword())) {
                    log.warn("用户尝试重用历史密码: userId={}, 重用第{}个历史密码",
                            userId, history.indexOf(obj) + 1);
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            log.error("检查密码历史失败: userId={}", userId, e);
            // 出错时默认允许（但记录日志）
            return false;
        }
    }

    /**
     * 添加密码到历史记录
     *
     * @param userId         用户ID
     * @param hashedPassword 加密后的密码
     */
    public void addPasswordToHistory(Long userId, String hashedPassword) {
        if (userId == null || hashedPassword == null) {
            return;
        }

        try {
            String key = PASSWORD_HISTORY_KEY + userId;
            PasswordHistoryEntry entry = new PasswordHistoryEntry(hashedPassword, Instant.now());

            // 添加到列表头部（最新的在前）
            redisTemplate.opsForList().leftPush(key, entry);

            // 保留指定数量的历史记录
            int historyCount = securityProperties.getPassword().getHistoryCount();
            if (historyCount > 0) {
                redisTemplate.opsForList().trim(key, 0, historyCount - 1);
            }

            // 设置过期时间（防止永久占用存储）
            redisTemplate.expire(key, 365, TimeUnit.DAYS);

            // 更新密码修改计数
            incrementPasswordChangeCount(userId);

            log.info("密码历史已更新: userId={}", userId);

        } catch (Exception e) {
            log.error("添加密码历史失败: userId={}", userId, e);
        }
    }

    /**
     * 检查密码强度
     *
     * <p>就像检查一把锁是否足够坚固，这个方法评估密码的强度。</p>
     *
     * @param password 要检查的密码
     * @return 密码强度评估结果
     */
    public PasswordStrengthResult checkPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return new PasswordStrengthResult(false, "密码不能为空", 0, PasswordStrength.VERY_WEAK);
        }

        SecurityProperties.PasswordProperties config = securityProperties.getPassword();
        List<String> issues = new ArrayList<>();
        int score = 0;

        // 长度检查
        if (password.length() < config.getMinLength()) {
            issues.add(String.format("密码长度至少需要%d个字符", config.getMinLength()));
        } else if (password.length() > config.getMaxLength()) {
            issues.add(String.format("密码长度不能超过%d个字符", config.getMaxLength()));
        } else {
            score += calculateLengthScore(password.length());
        }

        // 复杂度检查
        if (config.isEnableComplexityCheck()) {
            if (config.isRequireUppercase() && !password.matches(".*[A-Z].*")) {
                issues.add("需要包含大写字母");
            } else {
                score += 15;
            }

            if (config.isRequireLowercase() && !password.matches(".*[a-z].*")) {
                issues.add("需要包含小写字母");
            } else {
                score += 15;
            }

            if (config.isRequireNumbers() && !password.matches(".*\\d.*")) {
                issues.add("需要包含数字");
            } else {
                score += 15;
            }

            if (config.isRequireSpecialChars() &&
                    !password.matches(".*[" + config.getSpecialChars() + "].*")) {
                issues.add("需要包含特殊字符");
            } else {
                score += 20;
            }
        }

        // 弱密码检查
        if (config.isEnableWeakPasswordCheck() && isWeakPassword(password)) {
            issues.add("密码过于简单，请使用更复杂的密码");
            score = Math.min(score, 30); // 弱密码最高30分
        }

        // 密码泄露检查（如果启用）
        if (config.isEnableBreachCheck() && isPasswordBreached(password)) {
            issues.add("该密码已在数据泄露中出现，请使用其他密码");
            return new PasswordStrengthResult(false, String.join("; ", issues), 0, PasswordStrength.COMPROMISED);
        }

        // 计算最终强度
        PasswordStrength strength = calculateStrength(score);
        boolean isStrong = issues.isEmpty() && strength.ordinal() >= PasswordStrength.GOOD.ordinal();

        String message = issues.isEmpty() ?
                String.format("密码强度：%s", strength.getDescription()) :
                String.join("; ", issues);

        return new PasswordStrengthResult(isStrong, message, score, strength);
    }

    /**
     * 检查密码是否过期
     *
     * @param userId                 用户ID
     * @param lastPasswordChangeTime 上次密码修改时间
     * @return 是否过期
     */
    public boolean isPasswordExpired(Long userId, Instant lastPasswordChangeTime) {
        if (lastPasswordChangeTime == null) {
            return true; // 如果没有记录，认为已过期
        }

        int maxAgeDays = securityProperties.getPassword().getMaxAgeDays();
        if (maxAgeDays <= 0) {
            return false; // 如果配置为0或负数，表示密码永不过期
        }

        Instant expirationTime = lastPasswordChangeTime.plusSeconds(maxAgeDays * 24L * 60 * 60);
        boolean expired = Instant.now().isAfter(expirationTime);

        if (expired) {
            log.info("用户密码已过期: userId={}, 上次修改时间={}, 过期天数={}",
                    userId, lastPasswordChangeTime, maxAgeDays);
        }

        return expired;
    }

    /**
     * 获取密码即将过期的提醒天数
     *
     * @param lastPasswordChangeTime 上次密码修改时间
     * @return 剩余天数，如果已过期返回负数
     */
    public int getDaysUntilPasswordExpiry(Instant lastPasswordChangeTime) {
        if (lastPasswordChangeTime == null) {
            return 0;
        }

        int maxAgeDays = securityProperties.getPassword().getMaxAgeDays();
        if (maxAgeDays <= 0) {
            return Integer.MAX_VALUE; // 永不过期
        }

        Instant expirationTime = lastPasswordChangeTime.plusSeconds(maxAgeDays * 24L * 60 * 60);
        long secondsUntilExpiry = expirationTime.getEpochSecond() - Instant.now().getEpochSecond();

        return (int) (secondsUntilExpiry / (24 * 60 * 60));
    }

    // =================== 私有辅助方法 ===================

    /**
     * 初始化弱密码列表
     */
    private void initializeWeakPasswordList() {
        try {
            // 常见的弱密码
            List<String> commonWeakPasswords = List.of(
                    "password", "123456", "12345678", "qwerty", "abc123",
                    "monkey", "1234567", "letmein", "trustno1", "dragon",
                    "baseball", "111111", "iloveyou", "master", "sunshine",
                    "ashley", "bailey", "passw0rd", "shadow", "123123",
                    "654321", "superman", "qazwsx", "michael", "football",
                    "password1", "password123", "admin", "welcome", "login"
            );

            String key = WEAK_PASSWORD_KEY;
            for (String weakPassword : commonWeakPasswords) {
                redisTemplate.opsForSet().add(key, weakPassword.toLowerCase());
            }

            // 设置过期时间（定期更新）
            redisTemplate.expire(key, 30, TimeUnit.DAYS);

            log.info("弱密码列表初始化完成，包含 {} 个常见弱密码", commonWeakPasswords.size());

        } catch (Exception e) {
            log.error("初始化弱密码列表失败", e);
        }
    }

    /**
     * 检查是否为弱密码
     */
    private boolean isWeakPassword(String password) {
        if (password == null) return true;

        String lowerPassword = password.toLowerCase();

        // 检查是否在弱密码列表中
        Boolean isWeak = redisTemplate.opsForSet().isMember(WEAK_PASSWORD_KEY, lowerPassword);
        if (Boolean.TRUE.equals(isWeak)) {
            return true;
        }

        // 检查常见模式
        // 1. 全部相同字符
        if (password.matches("^(.)\\1+$")) {
            return true;
        }

        // 2. 连续数字或字母
        if (password.matches("^(012345678|123456789|abcdefgh|qwertyui).*$")) {
            return true;
        }

        // 3. 键盘模式
        if (lowerPassword.contains("qwerty") || lowerPassword.contains("asdfgh")) {
            return true;
        }

        return false;
    }

    /**
     * 检查密码是否已泄露（模拟实现）
     *
     * <p>实际项目中，这里应该调用 Have I Been Pwned API 或类似的服务</p>
     */
    private boolean isPasswordBreached(String password) {
        // 这里只是示例，实际应该：
        // 1. 计算密码的SHA-1哈希
        // 2. 取前5个字符调用 HIBP API
        // 3. 在返回的结果中查找完整哈希

        // 模拟一些已知的泄露密码
        List<String> breachedPasswords = List.of(
                "password", "123456", "qwerty", "admin123"
        );

        return breachedPasswords.contains(password.toLowerCase());
    }

    /**
     * 计算长度分数
     */
    private int calculateLengthScore(int length) {
        if (length < 8) return 0;
        if (length < 12) return 20;
        if (length < 16) return 30;
        return 35;
    }

    /**
     * 计算密码强度级别
     */
    private PasswordStrength calculateStrength(int score) {
        if (score < 20) return PasswordStrength.VERY_WEAK;
        if (score < 40) return PasswordStrength.WEAK;
        if (score < 60) return PasswordStrength.FAIR;
        if (score < 80) return PasswordStrength.GOOD;
        return PasswordStrength.STRONG;
    }

    /**
     * 增加密码修改计数
     */
    private void incrementPasswordChangeCount(Long userId) {
        try {
            String key = PASSWORD_CHANGE_COUNT_KEY + userId;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 365, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("更新密码修改计数失败: userId={}", userId, e);
        }
    }

    // =================== 内部类 ===================

    /**
     * 密码历史记录条目
     */
    private record PasswordHistoryEntry(String hashedPassword, Instant changedAt) {
    }

    /**
     * 密码强度级别
     */
    public enum PasswordStrength {
        VERY_WEAK("非常弱"),
        WEAK("弱"),
        FAIR("一般"),
        GOOD("良好"),
        STRONG("强"),
        COMPROMISED("已泄露");

        private final String description;

        PasswordStrength(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 密码强度检查结果
     */
    public record PasswordStrengthResult(
            boolean isStrong,
            String message,
            int score,
            PasswordStrength strength
    ) {
        /**
         * 是否满足最低要求
         */
        public boolean meetsRequirements() {
            return isStrong && strength.ordinal() >= PasswordStrength.FAIR.ordinal();
        }

        /**
         * 获取改进建议
         */
        public List<String> getSuggestions() {
            List<String> suggestions = new ArrayList<>();

            if (strength == PasswordStrength.VERY_WEAK || strength == PasswordStrength.WEAK) {
                suggestions.add("使用至少12个字符的密码");
                suggestions.add("混合使用大小写字母、数字和特殊字符");
                suggestions.add("避免使用常见词汇或个人信息");
                suggestions.add("考虑使用密码短语（多个词组合）");
            } else if (strength == PasswordStrength.FAIR) {
                suggestions.add("增加密码长度可以显著提高安全性");
                suggestions.add("添加更多特殊字符");
                suggestions.add("避免可预测的模式");
            }

            if (strength == PasswordStrength.COMPROMISED) {
                suggestions.add("立即更换密码");
                suggestions.add("检查其他账户是否使用相同密码");
                suggestions.add("启用双因素认证");
            }

            return suggestions;
        }
    }
}