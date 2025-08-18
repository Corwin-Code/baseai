package com.cloud.baseai.infrastructure.security.service;

import com.cloud.baseai.infrastructure.config.properties.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <h1>登录保护服务 - 防止暴力破解</h1>
 *
 * <p>这个服务防止攻击者通过不断尝试来破解密码。</p>
 *
 * <p><b>核心功能：</b></p>
 * <ul>
 *   <li>记录失败登录尝试</li>
 *   <li>自动锁定频繁失败的账户或IP</li>
 *   <li>实现渐进式延迟（失败越多，等待越久）</li>
 *   <li>检测异常登录模式</li>
 * </ul>
 */
@Service
public class LoginProtectionService {

    private static final Logger log = LoggerFactory.getLogger(LoginProtectionService.class);

    // Redis键前缀
    private static final String FAILED_ATTEMPT_KEY = "login:failed:";
    private static final String IP_BLOCK_KEY = "login:blocked:ip:";
    private static final String USER_BLOCK_KEY = "login:blocked:user:";
    private static final String LOGIN_HISTORY_KEY = "login:history:";
    private static final String CAPTCHA_REQUIRED_KEY = "login:captcha:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityProperties securityProperties;

    public LoginProtectionService(RedisTemplate<String, Object> redisTemplate,
                                  SecurityProperties securityProperties) {
        this.redisTemplate = redisTemplate;
        this.securityProperties = securityProperties;
    }

    /**
     * 记录失败的登录尝试
     *
     * @param identifier IP地址或用户名
     */
    public void recordFailedAttempt(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return;
        }

        try {
            String key = FAILED_ATTEMPT_KEY + identifier;

            // 增加失败计数
            Long attempts = redisTemplate.opsForValue().increment(key);

            // 设置过期时间（失败记录保留时间）
            redisTemplate.expire(key, 1, TimeUnit.HOURS);

            if (attempts == null) {
                attempts = 1L;
            }

            log.info("记录登录失败: identifier={}, 失败次数={}", identifier, attempts);

            // 检查是否需要锁定
            int lockoutThreshold = securityProperties.getPassword().getLockoutThreshold();
            if (attempts >= lockoutThreshold) {
                blockIdentifier(identifier);
            }

            // 检查是否需要验证码
            int captchaThreshold = securityProperties.getAuth().getCaptchaThreshold();
            if (attempts >= captchaThreshold) {
                requireCaptcha(identifier);
            }

        } catch (Exception e) {
            log.error("记录登录失败异常: identifier={}", identifier, e);
        }
    }

    /**
     * 记录成功的登录
     *
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @param deviceFingerprint 设备指纹
     */
    public void recordSuccessfulLogin(Long userId, String ipAddress, String deviceFingerprint) {
        try {
            // 重置失败计数
            resetFailedAttempts(ipAddress);
            if (userId != null) {
                resetFailedAttempts("user:" + userId);
            }

            // 记录登录历史（用于异常检测）
            if (securityProperties.getAuth().isEnableLoginHistory()) {
                String historyKey = LOGIN_HISTORY_KEY + userId;
                LoginHistoryEntry entry = new LoginHistoryEntry(
                        ipAddress,
                        deviceFingerprint,
                        Instant.now(),
                        true
                );

                redisTemplate.opsForList().leftPush(historyKey, entry);
                redisTemplate.opsForList().trim(historyKey, 0, 99); // 保留最近100条

                int retentionDays = securityProperties.getAuth().getLoginHistoryRetentionDays();
                redisTemplate.expire(historyKey, retentionDays, TimeUnit.DAYS);
            }

            // 异常检测
            if (securityProperties.getAuth().isEnableAnomalyDetection()) {
                detectAnomalousLogin(userId, ipAddress, deviceFingerprint);
            }

            log.info("登录成功记录: userId={}, ip={}", userId, ipAddress);

        } catch (Exception e) {
            log.error("记录成功登录异常: userId={}", userId, e);
        }
    }

    /**
     * 检查IP是否被封禁
     *
     * @param ipAddress IP地址
     * @return 是否被封禁
     */
    public boolean isIpBlocked(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return false;
        }

        String key = IP_BLOCK_KEY + ipAddress;
        Boolean blocked = redisTemplate.hasKey(key);

        if (Boolean.TRUE.equals(blocked)) {
            log.warn("IP地址被封禁: {}", ipAddress);
            return true;
        }

        return false;
    }

    /**
     * 检查用户是否被锁定
     *
     * @param userId 用户ID
     * @return 是否被锁定
     */
    public boolean isUserLocked(Long userId) {
        if (userId == null) {
            return false;
        }

        String key = USER_BLOCK_KEY + userId;
        Boolean locked = redisTemplate.hasKey(key);

        if (Boolean.TRUE.equals(locked)) {
            log.warn("用户账户被锁定: userId={}", userId);
            return true;
        }

        return false;
    }

    /**
     * 检查是否需要验证码
     *
     * @param identifier IP或用户标识
     * @return 是否需要验证码
     */
    public boolean isCaptchaRequired(String identifier) {
        if (!securityProperties.getAuth().isEnableCaptcha()) {
            return false;
        }

        if (identifier == null || identifier.trim().isEmpty()) {
            return false;
        }

        // 检查失败次数
        String attemptKey = FAILED_ATTEMPT_KEY + identifier;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptKey);

        if (attempts != null && attempts >= securityProperties.getAuth().getCaptchaThreshold()) {
            return true;
        }

        // 检查是否被标记需要验证码
        String captchaKey = CAPTCHA_REQUIRED_KEY + identifier;
        return Boolean.TRUE.equals(redisTemplate.hasKey(captchaKey));
    }

    /**
     * 验证验证码
     *
     * @param identifier 标识符
     * @param captchaCode 用户输入的验证码
     * @param expectedCode 期望的验证码
     * @return 是否验证成功
     */
    public boolean verifyCaptcha(String identifier, String captchaCode, String expectedCode) {
        if (captchaCode == null || expectedCode == null) {
            return false;
        }

        boolean valid = captchaCode.equalsIgnoreCase(expectedCode);

        if (valid) {
            // 验证成功，移除验证码要求
            String captchaKey = CAPTCHA_REQUIRED_KEY + identifier;
            redisTemplate.delete(captchaKey);
            log.info("验证码验证成功: identifier={}", identifier);
        } else {
            log.warn("验证码验证失败: identifier={}", identifier);
        }

        return valid;
    }

    /**
     * 重置失败尝试计数
     *
     * @param identifier 标识符
     */
    public void resetFailedAttempts(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return;
        }

        try {
            String attemptKey = FAILED_ATTEMPT_KEY + identifier;
            String captchaKey = CAPTCHA_REQUIRED_KEY + identifier;

            redisTemplate.delete(attemptKey);
            redisTemplate.delete(captchaKey);

            log.debug("重置失败计数: identifier={}", identifier);

        } catch (Exception e) {
            log.error("重置失败计数异常: identifier={}", identifier, e);
        }
    }

    /**
     * 解锁用户账户
     *
     * @param userId 用户ID
     */
    public void unlockUser(Long userId) {
        if (userId == null) {
            return;
        }

        String key = USER_BLOCK_KEY + userId;
        redisTemplate.delete(key);

        // 同时重置失败计数
        resetFailedAttempts("user:" + userId);

        log.info("用户账户已解锁: userId={}", userId);
    }

    /**
     * 解封IP地址
     *
     * @param ipAddress IP地址
     */
    public void unblockIp(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return;
        }

        String key = IP_BLOCK_KEY + ipAddress;
        redisTemplate.delete(key);

        // 同时重置失败计数
        resetFailedAttempts(ipAddress);

        log.info("IP地址已解封: {}", ipAddress);
    }

    /**
     * 获取剩余锁定时间（秒）
     *
     * @param identifier 标识符
     * @return 剩余秒数，如果未锁定返回0
     */
    public long getRemainingLockTime(String identifier) {
        if (identifier == null) {
            return 0;
        }

        String ipKey = IP_BLOCK_KEY + identifier;
        Long ipTtl = redisTemplate.getExpire(ipKey, TimeUnit.SECONDS);

        String userKey = USER_BLOCK_KEY + identifier;
        Long userTtl = redisTemplate.getExpire(userKey, TimeUnit.SECONDS);

        long maxTtl = 0;
        if (ipTtl != null && ipTtl > 0) {
            maxTtl = ipTtl;
        }
        if (userTtl != null && userTtl > maxTtl) {
            maxTtl = userTtl;
        }

        return maxTtl;
    }

    // =================== 私有辅助方法 ===================

    /**
     * 封禁标识符（IP或用户）
     */
    private void blockIdentifier(String identifier) {
        int lockoutMinutes = securityProperties.getPassword().getLockoutDurationMinutes();

        if (isIpAddress(identifier)) {
            // 封禁IP
            String key = IP_BLOCK_KEY + identifier;
            redisTemplate.opsForValue().set(key, true, lockoutMinutes, TimeUnit.MINUTES);
            log.warn("IP地址已封禁: ip={}, 持续{}分钟", identifier, lockoutMinutes);
        } else if (identifier.startsWith("user:")) {
            // 锁定用户
            String userId = identifier.substring(5);
            String key = USER_BLOCK_KEY + userId;
            redisTemplate.opsForValue().set(key, true, lockoutMinutes, TimeUnit.MINUTES);
            log.warn("用户账户已锁定: userId={}, 持续{}分钟", userId, lockoutMinutes);
        }
    }

    /**
     * 标记需要验证码
     */
    private void requireCaptcha(String identifier) {
        if (!securityProperties.getAuth().isEnableCaptcha()) {
            return;
        }

        String key = CAPTCHA_REQUIRED_KEY + identifier;
        redisTemplate.opsForValue().set(key, true, 24, TimeUnit.HOURS);
        log.info("标记需要验证码: identifier={}", identifier);
    }

    /**
     * 检测异常登录
     */
    private void detectAnomalousLogin(Long userId, String ipAddress, String deviceFingerprint) {
        try {
            String historyKey = LOGIN_HISTORY_KEY + userId;
            List<Object> history = redisTemplate.opsForList().range(historyKey, 0, 9);

            if (history == null || history.isEmpty()) {
                return;
            }

            // 检查是否为新IP或新设备
            boolean newIp = true;
            boolean newDevice = true;

            for (Object obj : history) {
                LoginHistoryEntry entry = (LoginHistoryEntry) obj;
                if (ipAddress.equals(entry.ipAddress())) {
                    newIp = false;
                }
                if (deviceFingerprint != null &&
                        deviceFingerprint.equals(entry.deviceFingerprint())) {
                    newDevice = false;
                }
            }

            if (newIp || newDevice) {
                log.info("检测到异常登录: userId={}, 新IP={}, 新设备={}",
                        userId, newIp, newDevice);

                // 这里可以发送告警通知
                // notificationService.sendAnomalousLoginAlert(userId, ipAddress);
            }

        } catch (Exception e) {
            log.error("异常登录检测失败: userId={}", userId, e);
        }
    }

    /**
     * 判断是否为IP地址
     */
    private boolean isIpAddress(String str) {
        if (str == null) return false;

        // 简单的IP格式判断
        return str.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") ||
                str.contains(":"); // IPv6
    }

    /**
     * 计算渐进式延迟时间
     *
     * <p>失败次数越多，等待时间越长</p>
     */
    public int calculateProgressiveDelay(int failedAttempts) {
        if (failedAttempts <= 0) return 0;

        // 延迟策略：1次=0秒，2次=2秒，3次=5秒，4次=10秒，5次+=30秒
        if (failedAttempts == 1) return 0;
        if (failedAttempts == 2) return 2;
        if (failedAttempts == 3) return 5;
        if (failedAttempts == 4) return 10;
        return 30;
    }

    // =================== 内部类 ===================

    /**
     * 登录历史记录
     */
    private record LoginHistoryEntry(
            String ipAddress,
            String deviceFingerprint,
            Instant timestamp,
            boolean successful
    ) {}
}