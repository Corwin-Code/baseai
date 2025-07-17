package com.cloud.baseai.infrastructure.utils;

import java.security.SecureRandom;
import java.util.regex.Pattern;

/**
 * <h2>用户工具类</h2>
 *
 * <p>这个工具类提供了用户管理相关的各种辅助方法。就像工匠的工具箱一样，
 * 它包含了处理用户数据时经常需要的小工具和验证方法。</p>
 *
 * <p><b>设计理念：</b></p>
 * <p>工具类应该是纯函数式的，无状态的，可复用的。每个方法都应该有清晰的职责，
 * 输入和输出都是可预测的。这样的设计让代码更容易测试和维护。</p>
 */
public class UserUtils {

    // 正则表达式模式
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,32}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"
    );

    // 随机数生成器
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // 字符集定义
    private static final String DIGITS = "0123456789";
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String SPECIAL_CHARS = "!@#$%^&*";
    private static final String TOKEN_CHARS = DIGITS + LETTERS + "-_";

    /**
     * 验证用户名格式
     *
     * <p>用户名是用户的重要标识，需要满足一定的格式要求：
     * 只能包含字母、数字、下划线和连字符，长度在3-32位之间。
     * 这样的规则既保证了唯一性，又具有良好的可读性。</p>
     *
     * @param username 用户名
     * @return true如果格式正确
     */
    public static boolean isValidUsername(String username) {
        if (username == null) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * 验证邮箱格式
     *
     * <p>邮箱验证是用户注册的重要环节。我们使用相对宽松的正则表达式，
     * 因为邮箱格式的RFC标准非常复杂，过于严格的验证可能会误判有效邮箱。</p>
     */
    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * 验证密码强度
     *
     * <p>强密码是账户安全的第一道防线。我们要求密码至少包含：
     * 大写字母、小写字母、数字、特殊字符各至少一个，总长度至少8位。</p>
     */
    public static boolean isStrongPassword(String password) {
        if (password == null) {
            return false;
        }
        return STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * 生成随机验证码
     *
     * <p>验证码用于邮箱验证、短信验证等场景。我们使用安全的随机数生成器
     * 来确保验证码的不可预测性。</p>
     *
     * @param length 验证码长度
     * @return 随机验证码
     */
    public static String generateRandomCode(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("验证码长度必须大于0");
        }

        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(DIGITS.charAt(SECURE_RANDOM.nextInt(DIGITS.length())));
        }
        return code.toString();
    }

    /**
     * 生成安全令牌
     *
     * <p>安全令牌用于邀请链接、密码重置等敏感操作。我们生成一个
     * 足够长且随机的字符串来确保安全性。</p>
     */
    public static String generateSecureToken() {
        return generateSecureToken(32);
    }

    /**
     * 生成指定长度的安全令牌
     */
    public static String generateSecureToken(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("令牌长度必须大于0");
        }

        StringBuilder token = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            token.append(TOKEN_CHARS.charAt(SECURE_RANDOM.nextInt(TOKEN_CHARS.length())));
        }
        return token.toString();
    }

    /**
     * 脱敏邮箱地址
     *
     * <p>在日志记录或显示时，我们需要对邮箱地址进行脱敏处理，
     * 既保护用户隐私，又保留必要的信息用于识别。</p>
     *
     * @param email 原始邮箱
     * @return 脱敏后的邮箱，如 "u***@example.com"
     */
    public static String maskEmail(String email) {
        if (email == null || !isValidEmail(email)) {
            return "***";
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            return "***";
        }

        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 1) {
            return username + "***@" + domain;
        } else if (username.length() <= 3) {
            return username.charAt(0) + "**@" + domain;
        } else {
            return username.charAt(0) + "***" + username.charAt(username.length() - 1) + "@" + domain;
        }
    }

    /**
     * 脱敏手机号码
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return "***";
        }

        if (phoneNumber.length() == 11) {
            // 中国手机号格式：138****1234
            return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(7);
        } else {
            // 其他格式：保留前3位和后4位
            int keepStart = Math.min(3, phoneNumber.length() / 3);
            int keepEnd = Math.min(4, phoneNumber.length() / 3);

            if (keepStart + keepEnd >= phoneNumber.length()) {
                return "***";
            }

            return phoneNumber.substring(0, keepStart) +
                    "*".repeat(phoneNumber.length() - keepStart - keepEnd) +
                    phoneNumber.substring(phoneNumber.length() - keepEnd);
        }
    }

    /**
     * 生成用户显示名
     *
     * <p>当用户没有设置昵称时，我们根据用户名或邮箱生成一个友好的显示名。</p>
     */
    public static String generateDisplayName(String username, String email) {
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }

        if (email != null && isValidEmail(email)) {
            String[] parts = email.split("@");
            return parts[0];
        }

        return "用户" + generateRandomCode(6);
    }

    /**
     * 验证邀请码格式
     */
    public static boolean isValidInviteCode(String inviteCode) {
        if (inviteCode == null) {
            return false;
        }

        String trimmed = inviteCode.trim();
        // 邀请码格式：字母数字组合，6-32位
        return trimmed.length() >= 6 &&
                trimmed.length() <= 32 &&
                trimmed.matches("^[A-Za-z0-9-_]+$");
    }

    /**
     * 计算密码强度分数
     *
     * <p>这个方法返回密码强度的数值评分，可以用于向用户展示密码强度。
     * 分数范围是0-100，分数越高表示密码越强。</p>
     */
    public static int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;

        // 长度加分
        if (password.length() >= 8) score += 25;
        if (password.length() >= 12) score += 15;
        if (password.length() >= 16) score += 10;

        // 字符类型加分
        if (password.matches(".*[a-z].*")) score += 10; // 小写字母
        if (password.matches(".*[A-Z].*")) score += 10; // 大写字母
        if (password.matches(".*\\d.*")) score += 10;   // 数字
        if (password.matches(".*[@$!%*?&].*")) score += 15; // 特殊字符

        // 复杂度加分
        if (password.matches(".*[a-z].*") && password.matches(".*[A-Z].*")) score += 5;
        if (password.matches(".*\\d.*") && password.matches(".*[@$!%*?&].*")) score += 10;

        return Math.min(score, 100);
    }

    /**
     * 获取密码强度描述
     */
    public static String getPasswordStrengthDescription(String password) {
        int strength = calculatePasswordStrength(password);

        if (strength < 30) return "弱";
        if (strength < 60) return "中等";
        if (strength < 80) return "强";
        return "非常强";
    }

    /**
     * 清理和规范化用户输入
     *
     * <p>用户输入的数据可能包含多余的空格、特殊字符等，需要进行清理。</p>
     */
    public static String cleanUserInput(String input) {
        if (input == null) {
            return null;
        }

        // 去除首尾空格
        String cleaned = input.trim();

        // 如果清理后为空，返回null
        if (cleaned.isEmpty()) {
            return null;
        }

        // 移除可能的XSS攻击字符
        cleaned = cleaned.replaceAll("[<>\"'&]", "");

        return cleaned;
    }

    /**
     * 验证用户年龄
     */
    public static boolean isValidAge(Integer age) {
        return age != null && age >= 13 && age <= 120;
    }

    /**
     * 生成头像URL的默认值
     */
    public static String generateDefaultAvatarUrl(String username) {
        if (username == null || username.isEmpty()) {
            username = "user";
        }

        // 使用Gravatar或其他头像服务
        int hash = Math.abs(username.hashCode());
        return String.format("https://api.dicebear.com/7.x/avataaars/svg?seed=%s", hash);
    }

    // 私有构造函数，防止实例化
    private UserUtils() {
        throw new UnsupportedOperationException("工具类不能被实例化");
    }
}