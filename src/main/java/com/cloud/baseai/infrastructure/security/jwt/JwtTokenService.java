package com.cloud.baseai.infrastructure.security.jwt;

import com.cloud.baseai.infrastructure.config.properties.SecurityProperties;
import com.cloud.baseai.infrastructure.security.UserPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <h1>生产级JWT管理服务</h1>
 *
 * <p>实现企业级JWT管理功能：</p>
 * <ul>
 * <li>支持RSA和HMAC两种加密算法</li>
 * <li>令牌黑名单机制</li>
 * <li>设备指纹验证</li>
 * <li>令牌刷新策略</li>
 * <li>配置化密钥管理</li>
 * </ul>
 */
@Component
public class JwtTokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

    private static final String TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String TOKEN_FINGERPRINT_PREFIX = "jwt:fingerprint:";
    private static final String REFRESH_TOKEN_PREFIX = "jwt:refresh:";

    private final SecurityProperties securityProps;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * RSA密钥对（用于高安全性场景）
     */
    private final RSAPrivateKey rsaPrivateKey;
    private final RSAPublicKey rsaPublicKey;

    /**
     * HMAC密钥（用于简单场景）
     */
    private final SecretKey hmacSecretKey;

    /**
     * 构造函数，初始化密钥对
     */
    public JwtTokenService(SecurityProperties securityProps,
                           RedisTemplate<String, Object> redisTemplate) {
        this.securityProps = securityProps;
        this.redisTemplate = redisTemplate;

        try {
            // 从配置加载RSA密钥对
            this.rsaPrivateKey = loadRSAPrivateKey(securityProps.getJwt().getRsaPrivateKey());
            this.rsaPublicKey = loadRSAPublicKey(securityProps.getJwt().getRsaPublicKey());

            // 初始化HMAC密钥
            String hmacSecret = securityProps.getJwt().getSecret();
            if (hmacSecret.length() < 32) {
                log.warn("HMAC密钥长度不足，自动填充到安全长度");
                hmacSecret = hmacSecret + "padding-to-make-key-longer-for-security";
            }
            this.hmacSecretKey = Keys.hmacShaKeyFor(hmacSecret.getBytes());

            log.info("JWT服务初始化完成，支持RSA和HMAC两种加密方式");

        } catch (Exception e) {
            log.error("JWT服务初始化失败", e);
            throw new RuntimeException("JWT服务初始化失败", e);
        }
    }

    // =================== 公共API方法 ===================

    /**
     * 生成访问令牌（从Authentication对象）
     *
     * <p>这是最常用的令牌生成方法，适用于用户登录场景。</p>
     */
    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateAccessToken(userPrincipal, null, null);
    }

    /**
     * 生成访问令牌（从UserPrincipal对象）
     *
     * <p>适用于令牌刷新等场景。</p>
     */
    public String generateAccessToken(UserPrincipal userPrincipal) {
        return generateAccessToken(userPrincipal, null, null);
    }

    /**
     * 生成完整的令牌对（访问令牌 + 刷新令牌）
     *
     * <p>推荐在登录成功后使用，提供完整的令牌管理能力。</p>
     */
    public TokenPair generateTokenPair(UserPrincipal userPrincipal,
                                       String deviceFingerprint,
                                       String ipAddress) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();

        try {
            // 生成访问令牌
            String accessToken = buildAccessToken(userPrincipal, jti, deviceFingerprint, ipAddress, now);

            // 生成刷新令牌
            String refreshToken = buildRefreshToken(userPrincipal, jti, deviceFingerprint, now);

            // 存储令牌指纹
            if (deviceFingerprint != null) {
                long accessExpiration = securityProps.getJwt().getAccessTokenExpiration();
                long refreshExpiration = securityProps.getJwt().getRefreshTokenExpiration();
                storeTokenFingerprint(jti, deviceFingerprint, accessExpiration);
                storeTokenFingerprint(jti + ":refresh", deviceFingerprint, refreshExpiration);
            }

            log.debug("令牌对生成成功: userId={}, jti={}", userPrincipal.getId(), jti);
            return new TokenPair(accessToken, refreshToken, jti);

        } catch (Exception e) {
            log.error("令牌对生成失败: userId={}", userPrincipal.getId(), e);
            throw new RuntimeException("令牌生成失败", e);
        }
    }

    /**
     * 验证令牌有效性
     *
     * <p>执行全面的令牌验证，包括签名、过期时间、黑名单等检查。</p>
     */
    public boolean validateToken(String token) {
        return validateToken(token, null);
    }

    /**
     * 验证令牌有效性（包含设备指纹验证）
     */
    public boolean validateToken(String token, String deviceFingerprint) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            // 解析令牌
            Claims claims = parseTokenClaims(token);
            if (claims == null) {
                return false;
            }

            // 检查黑名单
            String jti = claims.getId();
            if (isTokenBlacklisted(jti)) {
                log.debug("令牌在黑名单中: jti={}", jti);
                return false;
            }

            // 验证设备指纹（如果提供）
            if (deviceFingerprint != null) {
                String storedFingerprint = claims.get("fingerprint", String.class);
                if (storedFingerprint != null && !verifyFingerprint(deviceFingerprint, storedFingerprint)) {
                    log.warn("设备指纹不匹配: jti={}", jti);
                    return false;
                }
            }

            log.debug("令牌验证通过: jti={}", jti);
            return true;

        } catch (ExpiredJwtException e) {
            log.debug("令牌已过期: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("令牌签名无效: {}", e.getMessage());
        } catch (Exception e) {
            log.error("令牌验证失败: {}", e.getMessage());
        }

        return false;
    }

    /**
     * 刷新访问令牌
     */
    public String refreshAccessToken(String refreshToken, String deviceFingerprint) {
        try {
            Claims claims = parseTokenClaims(refreshToken);
            if (claims == null) {
                throw new IllegalArgumentException("无效的刷新令牌");
            }

            // 验证令牌类型
            String tokenType = claims.get("tokenType", String.class);
            if (!"refresh".equals(tokenType)) {
                throw new IllegalArgumentException("不是刷新令牌");
            }

            // 验证设备指纹
            if (deviceFingerprint != null) {
                String storedFingerprint = claims.get("fingerprint", String.class);
                if (!verifyFingerprint(deviceFingerprint, storedFingerprint)) {
                    throw new SecurityException("设备指纹不匹配");
                }
            }

            // 检查关联的访问令牌是否被撤销
            String accessJti = claims.get("accessJti", String.class);
            if (isTokenBlacklisted(accessJti)) {
                throw new SecurityException("关联的访问令牌已被撤销");
            }

            // 重新加载用户信息生成新令牌
            Long userId = Long.valueOf(claims.getSubject());
            // 这里需要通过UserDetailsService重新加载用户信息
            // UserPrincipal userPrincipal = userDetailsService.loadUserById(userId);

            // 简化实现：从令牌中提取基本信息
            List<String> roles = claims.get("roles", List.class);
            List<Long> tenantIds = claims.get("tenantIds", List.class);
            String username = claims.get("username", String.class);
            String email = claims.get("email", String.class);

            UserPrincipal userPrincipal = createUserPrincipalFromClaims(userId, username, email, roles, tenantIds);

            return generateAccessToken(userPrincipal, deviceFingerprint, null);

        } catch (Exception e) {
            log.error("刷新令牌失败: {}", e.getMessage(), e);
            throw new RuntimeException("令牌刷新失败", e);
        }
    }

    /**
     * 撤销令牌（加入黑名单）
     */
    public void revokeToken(String token) {
        try {
            Claims claims = parseTokenClaims(token);
            if (claims == null) {
                return;
            }

            String jti = claims.getId();
            Date expiration = claims.getExpiration();

            if (jti != null && expiration != null) {
                // 计算剩余有效时间
                long ttl = expiration.getTime() - System.currentTimeMillis();
                if (ttl > 0) {
                    String key = TOKEN_BLACKLIST_PREFIX + jti;
                    redisTemplate.opsForValue().set(key, true, ttl, TimeUnit.MILLISECONDS);
                    log.info("令牌已撤销: jti={}", jti);
                }
            }

        } catch (Exception e) {
            log.error("撤销令牌失败", e);
        }
    }

    /**
     * 撤销用户的所有令牌
     */
    public void revokeAllUserTokens(Long userId) {
        try {
            // 将用户ID加入全局黑名单
            String userBlacklistKey = "user:blacklist:" + userId;
            redisTemplate.opsForValue().set(userBlacklistKey, true,
                    securityProps.getJwt().getRefreshTokenExpiration(), TimeUnit.MILLISECONDS);

            log.info("用户所有令牌已撤销: userId={}", userId);

        } catch (Exception e) {
            log.error("撤销用户令牌失败: userId={}", userId, e);
        }
    }

    // =================== 信息提取方法 ===================

    /**
     * 从令牌中提取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseTokenClaims(token);
        return claims != null ? Long.valueOf(claims.getSubject()) : null;
    }

    /**
     * 从令牌中提取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseTokenClaims(token);
        return claims != null ? claims.get("username", String.class) : null;
    }

    /**
     * 从令牌中提取用户邮箱
     */
    public String getEmailFromToken(String token) {
        Claims claims = parseTokenClaims(token);
        return claims != null ? claims.get("email", String.class) : null;
    }

    /**
     * 从令牌中提取角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = parseTokenClaims(token);
        if (claims == null) return List.of();

        List<String> roles = claims.get("roles", List.class);
        return roles != null ? roles : List.of();
    }

    /**
     * 从令牌中提取租户ID列表
     */
    @SuppressWarnings("unchecked")
    public List<Long> getTenantIdsFromToken(String token) {
        Claims claims = parseTokenClaims(token);
        if (claims == null) return List.of();

        List<Object> tenantIds = claims.get("tenantIds", List.class);
        if (tenantIds == null) return List.of();

        return tenantIds.stream()
                .map(id -> Long.valueOf(id.toString()))
                .collect(Collectors.toList());
    }

    /**
     * 获取令牌过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = parseTokenClaims(token);
        return claims != null ? claims.getExpiration() : null;
    }

    /**
     * 获取令牌剩余有效时间（秒）
     */
    public long getTokenRemainingTime(String token) {
        try {
            Date expirationDate = getExpirationDateFromToken(token);
            if (expirationDate == null) return 0;

            long currentTime = Instant.now().toEpochMilli();
            long expirationTime = expirationDate.getTime();
            return Math.max(0, (expirationTime - currentTime) / 1000);
        } catch (Exception e) {
            log.debug("获取令牌剩余时间失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 检查是否为刷新令牌
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = parseTokenClaims(token);
            if (claims == null) return false;

            String tokenType = claims.get("tokenType", String.class);
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 生成访问令牌（内部方法）
     */
    private String generateAccessToken(UserPrincipal userPrincipal,
                                       String deviceFingerprint,
                                       String ipAddress) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();
        return buildAccessToken(userPrincipal, jti, deviceFingerprint, ipAddress, now);
    }

    /**
     * 构建访问令牌
     */
    private String buildAccessToken(UserPrincipal userPrincipal,
                                    String jti,
                                    String deviceFingerprint,
                                    String ipAddress,
                                    Instant now) {
        long expirationMs = securityProps.getJwt().getAccessTokenExpiration();
        Instant expiryDate = now.plus(expirationMs, ChronoUnit.MILLIS);

        // 提取用户角色
        List<String> roles = userPrincipal.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.startsWith("ROLE_") ?
                        authority.substring("ROLE_".length()) : authority)
                .collect(Collectors.toList());

        JwtBuilder builder = Jwts.builder()
                .id(jti)
                .subject(userPrincipal.getId().toString())
                .issuer(securityProps.getJwt().getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .claim("username", userPrincipal.getUsername())
                .claim("email", userPrincipal.getEmail())
                .claim("roles", roles)
                .claim("tenantIds", userPrincipal.getTenantIds())
                .claim("tokenType", "access");

        // 添加设备指纹（如果提供）
        if (deviceFingerprint != null) {
            builder.claim("fingerprint", hashFingerprint(deviceFingerprint));
        }

        // 添加IP地址（如果提供）
        if (ipAddress != null) {
            builder.claim("ip", ipAddress);
        }

        // 根据配置选择加密算法
        if (securityProps.getJwt().isUseRsa()) {
            return builder.signWith(rsaPrivateKey, SignatureAlgorithm.RS256).compact();
        } else {
            return builder.signWith(hmacSecretKey).compact();
        }
    }

    /**
     * 构建刷新令牌
     */
    private String buildRefreshToken(UserPrincipal userPrincipal,
                                     String accessJti,
                                     String deviceFingerprint,
                                     Instant now) {
        String refreshJti = UUID.randomUUID().toString();
        long expirationMs = securityProps.getJwt().getRefreshTokenExpiration();
        Instant expiryDate = now.plus(expirationMs, ChronoUnit.MILLIS);

        JwtBuilder builder = Jwts.builder()
                .id(refreshJti)
                .subject(userPrincipal.getId().toString())
                .issuer(securityProps.getJwt().getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .claim("username", userPrincipal.getUsername())
                .claim("email", userPrincipal.getEmail())
                .claim("roles", userPrincipal.getRoles())
                .claim("tenantIds", userPrincipal.getTenantIds())
                .claim("accessJti", accessJti)
                .claim("tokenType", "refresh");

        if (deviceFingerprint != null) {
            builder.claim("fingerprint", hashFingerprint(deviceFingerprint));
        }

        if (securityProps.getJwt().isUseRsa()) {
            return builder.signWith(rsaPrivateKey, SignatureAlgorithm.RS256).compact();
        } else {
            return builder.signWith(hmacSecretKey).compact();
        }
    }

    /**
     * 解析令牌获取Claims
     */
    @Cacheable(value = "jwt-claims", key = "#token", unless = "#result == null")
    public Claims parseTokenClaims(String token) {
        try {
            JwtParserBuilder parser = Jwts.parser()
                    .requireIssuer(securityProps.getJwt().getIssuer());

            if (securityProps.getJwt().isUseRsa()) {
                parser.setSigningKey(rsaPublicKey);
            } else {
                parser.setSigningKey(hmacSecretKey);
            }

            return parser.build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (Exception e) {
            log.debug("解析令牌失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 检查令牌是否在黑名单中
     */
    private boolean isTokenBlacklisted(String jti) {
        if (jti == null) return false;

        String key = TOKEN_BLACKLIST_PREFIX + jti;
        return redisTemplate.hasKey(key);
    }

    /**
     * 存储令牌指纹
     */
    private void storeTokenFingerprint(String jti, String fingerprint, long expiration) {
        String key = TOKEN_FINGERPRINT_PREFIX + jti;
        redisTemplate.opsForValue().set(key, hashFingerprint(fingerprint),
                expiration, TimeUnit.MILLISECONDS);
    }

    /**
     * 哈希设备指纹
     */
    private String hashFingerprint(String fingerprint) {
        if (fingerprint == null) return null;
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(fingerprint);
    }

    /**
     * 验证设备指纹
     */
    private boolean verifyFingerprint(String provided, String stored) {
        if (provided == null || stored == null) return false;
        return hashFingerprint(provided).equals(stored);
    }

    /**
     * 加载RSA私钥
     */
    private RSAPrivateKey loadRSAPrivateKey(String base64PrivateKey) throws Exception {
        if (base64PrivateKey == null || base64PrivateKey.trim().isEmpty()) {
            throw new IllegalArgumentException("RSA私钥不能为空");
        }

        byte[] keyBytes = Base64.getDecoder().decode(base64PrivateKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(spec);
    }

    /**
     * 加载RSA公钥
     */
    private RSAPublicKey loadRSAPublicKey(String base64PublicKey) throws Exception {
        if (base64PublicKey == null || base64PublicKey.trim().isEmpty()) {
            throw new IllegalArgumentException("RSA公钥不能为空");
        }

        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(spec);
    }

    /**
     * 从Claims创建UserPrincipal（用于令牌刷新）
     */
    private UserPrincipal createUserPrincipalFromClaims(Long userId,
                                                        String username,
                                                        String email,
                                                        List<String> roles,
                                                        List<Long> tenantIds) {
        // 这里创建一个简化的UserPrincipal用于令牌刷新
        // TODO 实际项目中应该通过UserDetailsService重新加载完整信息
        return UserPrincipal.builder()
                .id(userId)
                .username(username)
                .email(email)
                .password("") // 刷新场景不需要密码
                .authorities(roles.stream()
                        .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList()))
                .tenantIds(tenantIds)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
    }

    // =================== 数据传输对象 ===================

    /**
     * 令牌对
     */
    public record TokenPair(String accessToken, String refreshToken, String jti) {

        /**
         * 获取访问令牌类型
         */
        public String getTokenType() {
            return "Bearer";
        }

        /**
         * 检查令牌对是否有效
         */
        public boolean isValid() {
            return accessToken != null && !accessToken.isEmpty() &&
                    refreshToken != null && !refreshToken.isEmpty() &&
                    jti != null && !jti.isEmpty();
        }
    }
}