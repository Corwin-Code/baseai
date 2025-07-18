package com.cloud.baseai.infrastructure.security;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * <h1>安全工具类</h1>
 *
 * <p>SecurityUtils提供了各种常用的安全相关功能，从密码处理到权限检查，
 * 从令牌生成到数据脱敏，应有尽有。无论是在业务代码中进行安全检查，
 * 还是在工具方法中处理敏感数据，这个类都能提供便捷、安全、可靠的支持。</p>
 *
 * <p><b>设计原则：</b></p>
 * <p>1. <strong>安全第一：</strong>所有方法都经过安全评估，避免常见的安全漏洞</p>
 * <p>2. <strong>易于使用：</strong>提供简洁的API，让开发者能够轻松集成安全功能</p>
 * <p>3. <strong>性能优化：</strong>对于频繁调用的方法进行了性能优化</p>
 * <p>4. <strong>错误容忍：</strong>优雅地处理异常情况，不会因为安全检查而影响业务流程</p>
 *
 * <p><b>常用场景：</b></p>
 * <p>- 在Service层检查当前用户权限</p>
 * <p>- 在数据处理时脱敏敏感信息</p>
 * <p>- 生成安全的随机令牌</p>
 * <p>- 验证密码强度</p>
 * <p>- 记录安全相关的操作日志</p>
 */
public final class SecurityUtils {

    private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);

    /**
     * 密码强度验证的正则表达式
     * <p>要求密码至少包含大写字母、小写字母、数字，长度至少8位</p>
     */
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    /**
     * 弱密码模式（用于检测常见的弱密码）
     */
    private static final List<Pattern> WEAK_PASSWORD_PATTERNS = List.of(
            Pattern.compile("^(.)\\1+$"),                    // 全部相同字符：aaaaaaa
            Pattern.compile("^(\\d+)$"),                     // 全数字：12345678
            Pattern.compile("^([a-zA-Z]+)$"),                // 全字母：abcdefgh
            Pattern.compile("^(password|123456|qwerty|admin)$", Pattern.CASE_INSENSITIVE)  // 常见弱密码
    );

    /**
     * 安全随机数生成器
     * <p>使用SecureRandom确保生成的随机数具有密码学安全性</p>
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 默认的密码编码器
     * <p>使用BCrypt算法，它是目前最推荐的密码哈希算法之一</p>
     */
    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(12);

    /**
     * 私有构造函数，防止实例化
     * <p>这是一个工具类，所有方法都是静态的，不需要实例化</p>
     */
    private SecurityUtils() {
        throw new UnsupportedOperationException("这是一个工具类，不应该被实例化");
    }

    // =================== 用户身份相关方法 ===================

    /**
     * 获取当前登录用户的ID
     *
     * <p>这是最常用的方法之一。在业务代码中，我们经常需要知道"是谁在执行这个操作"。
     * 这个方法从Spring Security的上下文中提取当前用户信息。</p>
     *
     * <p><b>使用场景：</b></p>
     * <p>- 记录操作日志：谁在什么时候做了什么</p>
     * <p>- 数据权限控制：只能查看自己创建的数据</p>
     * <p>- 业务逻辑处理：根据用户身份执行不同逻辑</p>
     *
     * @return 当前用户ID，如果用户未登录则返回Optional.empty()
     */
    public static Optional<Long> getCurrentUserId() {
        try {
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            return userPrincipal != null ? Optional.of(userPrincipal.getId()) : Optional.empty();
        } catch (Exception e) {
            log.debug("获取当前用户ID失败", e);
            return Optional.empty();
        }
    }

    /**
     * 获取当前登录用户的用户名
     *
     * <p>用户名通常用于显示和日志记录。相比于用户ID，用户名更容易理解和识别。</p>
     */
    public static Optional<String> getCurrentUsername() {
        try {
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            return userPrincipal != null ? Optional.of(userPrincipal.getUsername()) : Optional.empty();
        } catch (Exception e) {
            log.debug("获取当前用户名失败", e);
            return Optional.empty();
        }
    }

    /**
     * 获取当前用户的邮箱
     */
    public static Optional<String> getCurrentUserEmail() {
        try {
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            return userPrincipal != null ? Optional.of(userPrincipal.getEmail()) : Optional.empty();
        } catch (Exception e) {
            log.debug("获取当前用户邮箱失败", e);
            return Optional.empty();
        }
    }

    /**
     * 获取当前用户所属的租户ID列表
     *
     * <p>在多租户系统中，这个方法非常重要。它帮助我们确定用户可以访问哪些租户的数据。</p>
     */
    public static List<Long> getCurrentUserTenantIds() {
        try {
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            return userPrincipal != null ? userPrincipal.getTenantIds() : List.of();
        } catch (Exception e) {
            log.debug("获取当前用户租户ID失败", e);
            return List.of();
        }
    }

    /**
     * 获取当前用户的角色列表
     */
    public static List<String> getCurrentUserRoles() {
        try {
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            return userPrincipal != null ? userPrincipal.getRoles() : List.of();
        } catch (Exception e) {
            log.debug("获取当前用户角色失败", e);
            return List.of();
        }
    }

    /**
     * 获取当前用户的UserPrincipal对象
     *
     * <p>这是获取用户信息的底层方法。其他获取用户信息的方法都基于这个方法。</p>
     */
    public static UserPrincipal getCurrentUserPrincipal() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() &&
                    authentication.getPrincipal() instanceof UserPrincipal) {
                return (UserPrincipal) authentication.getPrincipal();
            }
            return null;
        } catch (Exception e) {
            log.debug("获取当前用户主体失败", e);
            return null;
        }
    }

    // =================== 权限检查方法 ===================

    /**
     * 检查当前用户是否拥有指定角色
     *
     * <p>这个方法提供了编程式的角色检查能力。在某些复杂的业务逻辑中，
     * 我们可能需要根据用户角色执行不同的处理流程。</p>
     *
     * <p><b>使用示例：</b></p>
     * <pre>
     * if (SecurityUtils.hasRole("ADMIN")) {
     *     // 执行管理员特有的逻辑
     *     processAdminOperation();
     * } else {
     *     // 执行普通用户逻辑
     *     processUserOperation();
     * }
     * </pre>
     *
     * @param role 要检查的角色名称（不包含ROLE_前缀）
     * @return 如果用户拥有该角色返回true，否则返回false
     */
    public static boolean hasRole(String role) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return false;
            }

            String roleWithPrefix = "ROLE_" + role;
            return authentication.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(authority -> authority.equals(roleWithPrefix));
        } catch (Exception e) {
            log.debug("角色检查失败: role={}", role, e);
            return false;
        }
    }

    /**
     * 检查当前用户是否拥有任一指定角色
     *
     * <p>这个方法用于检查用户是否拥有多个角色中的任意一个。在权限设计中，
     * 我们经常遇到"只要有其中一个角色就可以执行操作"的场景。</p>
     */
    public static boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查当前用户是否拥有所有指定角色
     *
     * <p>与hasAnyRole相反，这个方法要求用户必须同时拥有所有指定的角色。</p>
     */
    public static boolean hasAllRoles(String... roles) {
        for (String role : roles) {
            if (!hasRole(role)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查当前用户是否属于指定租户
     *
     * <p>在多租户系统中，这是一个基础的权限检查方法。用户只能访问
     * 所属租户的数据，这个方法帮助我们进行这种检查。</p>
     */
    public static boolean belongsToTenant(Long tenantId) {
        try {
            UserPrincipal userPrincipal = getCurrentUserPrincipal();
            return userPrincipal != null && userPrincipal.belongsToTenant(tenantId);
        } catch (Exception e) {
            log.debug("租户归属检查失败: tenantId={}", tenantId, e);
            return false;
        }
    }

    /**
     * 检查当前用户是否为超级管理员
     */
    public static boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN");
    }

    /**
     * 检查当前用户是否为系统管理员
     */
    public static boolean isSystemAdmin() {
        return hasRole("SYSTEM_ADMIN") || isSuperAdmin();
    }

    // =================== 密码相关方法 ===================

    /**
     * 加密密码
     *
     * <p>这个方法使用BCrypt算法对密码进行加密。BCrypt是目前最推荐的
     * 密码哈希算法，它内置了盐值生成和多轮哈希处理。</p>
     *
     * <p><b>为什么不能直接存储明文密码？</b></p>
     * <p>即使是系统管理员也不应该知道用户的实际密码。加密存储确保了
     * 即使数据库泄露，攻击者也无法直接获取用户密码。</p>
     *
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public static String encodePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return PASSWORD_ENCODER.encode(rawPassword);
    }

    /**
     * 验证密码
     *
     * <p>这个方法验证用户输入的密码是否与存储的加密密码匹配。
     * BCrypt算法会自动处理盐值和哈希比较。</p>
     *
     * @param rawPassword     用户输入的原始密码
     * @param encodedPassword 存储的加密密码
     * @return 如果密码匹配返回true，否则返回false
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        try {
            return PASSWORD_ENCODER.matches(rawPassword, encodedPassword);
        } catch (Exception e) {
            log.error("密码验证失败", e);
            return false;
        }
    }

    /**
     * 检查密码强度
     *
     * <p>强密码是账户安全的第一道防线。这个方法检查密码是否符合安全要求。</p>
     *
     * <p><b>密码强度要求：</b></p>
     * <p>- 至少8个字符</p>
     * <p>- 包含大写字母</p>
     * <p>- 包含小写字母</p>
     * <p>- 包含数字</p>
     * <p>- 包含特殊字符</p>
     *
     * @param password 要检查的密码
     * @return 密码强度检查结果
     */
    public static PasswordStrengthResult checkPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return new PasswordStrengthResult(false, "密码不能为空", 0);
        }

        // 检查是否为弱密码
        for (Pattern weakPattern : WEAK_PASSWORD_PATTERNS) {
            if (weakPattern.matcher(password).matches()) {
                return new PasswordStrengthResult(false, "密码过于简单，请使用更复杂的密码", 1);
            }
        }

        // 计算密码强度分数
        int score = calculatePasswordScore(password);

        if (STRONG_PASSWORD_PATTERN.matcher(password).matches() && score >= 80) {
            return new PasswordStrengthResult(true, "密码强度良好", score);
        } else if (score >= 60) {
            return new PasswordStrengthResult(false, "密码强度中等，建议增加复杂度", score);
        } else {
            return new PasswordStrengthResult(false, "密码强度较弱，请使用更安全的密码", score);
        }
    }

    /**
     * 生成安全的随机密码
     *
     * <p>这个方法生成符合安全要求的随机密码。在某些场景下，
     * 如管理员重置用户密码，需要生成临时的安全密码。</p>
     */
    public static String generateSecurePassword(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("密码长度不能小于8");
        }

        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String specialChars = "@$!%*?&";
        String allChars = upperCase + lowerCase + digits + specialChars;

        StringBuilder password = new StringBuilder();

        // 确保至少包含每种类型的字符
        password.append(upperCase.charAt(SECURE_RANDOM.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(SECURE_RANDOM.nextInt(lowerCase.length())));
        password.append(digits.charAt(SECURE_RANDOM.nextInt(digits.length())));
        password.append(specialChars.charAt(SECURE_RANDOM.nextInt(specialChars.length())));

        // 填充剩余长度
        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(SECURE_RANDOM.nextInt(allChars.length())));
        }

        // 打乱字符顺序
        return shuffleString(password.toString());
    }

    // =================== 令牌生成方法 ===================

    /**
     * 生成安全的随机令牌
     *
     * <p>这个方法生成密码学安全的随机令牌，可用于各种安全目的：</p>
     * <p>- 邮箱验证码</p>
     * <p>- 密码重置令牌</p>
     * <p>- API密钥</p>
     * <p>- 会话标识符</p>
     */
    public static String generateSecureToken(int length) {
        byte[] randomBytes = new byte[length];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * 生成数字验证码
     *
     * <p>生成指定长度的数字验证码，常用于短信验证和邮箱验证。</p>
     */
    public static String generateNumericCode(int length) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(SECURE_RANDOM.nextInt(10));
        }
        return code.toString();
    }

    // =================== 数据脱敏方法 ===================

    /**
     * 脱敏邮箱地址
     *
     * <p>在日志记录和界面显示时，我们需要对敏感信息进行脱敏处理。
     * 这个方法将邮箱地址的用户名部分进行部分隐藏。</p>
     *
     * <p><b>脱敏示例：</b></p>
     * <p>john.doe@example.com → j***e@example.com</p>
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }

        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return "*".repeat(username.length()) + "@" + domain;
        }

        String masked = username.charAt(0) + "*".repeat(username.length() - 2) + username.charAt(username.length() - 1);
        return masked + "@" + domain;
    }

    /**
     * 脱敏手机号码
     *
     * <p>手机号码脱敏，保留前3位和后4位，中间用星号替换。</p>
     * <p>示例：13812345678 → 138****5678</p>
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }

        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        } else {
            int visibleStart = Math.min(3, phone.length() / 3);
            int visibleEnd = Math.min(4, phone.length() / 3);
            int start = visibleStart;
            int end = phone.length() - visibleEnd;

            return phone.substring(0, start) + "*".repeat(end - start) + phone.substring(end);
        }
    }

    /**
     * 脱敏身份证号
     *
     * <p>身份证号脱敏，保留前6位和后4位。</p>
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return "***";
        }

        return idCard.substring(0, 6) + "*".repeat(idCard.length() - 10) + idCard.substring(idCard.length() - 4);
    }

    // =================== 私有辅助方法 ===================

    /**
     * 计算密码强度分数
     */
    private static int calculatePasswordScore(String password) {
        int score = 0;

        // 长度评分
        if (password.length() >= 8) score += 25;
        if (password.length() >= 12) score += 25;

        // 字符类型评分
        if (password.matches(".*[a-z].*")) score += 10;  // 包含小写字母
        if (password.matches(".*[A-Z].*")) score += 10;  // 包含大写字母
        if (password.matches(".*\\d.*")) score += 10;    // 包含数字
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) score += 20; // 包含特殊字符

        return Math.min(100, score);
    }

    /**
     * 打乱字符串顺序
     */
    private static String shuffleString(String input) {
        char[] array = input.toCharArray();
        for (int i = array.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
        return new String(array);
    }

    // =================== 数据传输对象 ===================

    /**
     * 密码强度检查结果
     */
    @Getter
    public static class PasswordStrengthResult {
        private final boolean strong;
        private final String message;
        private final int score;

        public PasswordStrengthResult(boolean strong, String message, int score) {
            this.strong = strong;
            this.message = message;
            this.score = score;
        }

        @Override
        public String toString() {
            return String.format("PasswordStrength{strong=%s, score=%d, message='%s'}",
                    strong, score, message);
        }
    }
}