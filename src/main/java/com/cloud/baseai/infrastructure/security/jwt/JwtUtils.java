package com.cloud.baseai.infrastructure.security.jwt;

import com.cloud.baseai.infrastructure.security.UserPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <h1>JWT工具类</h1>
 *
 * <p>JWT（JSON Web Token）是现代Web应用认证的标准方案。
 * 这个工具类负责制造、验证和解析安全令牌。每个JWT令牌都像一张数字身份证，
 * 包含了用户的身份信息和权限数据。</p>
 *
 * <p><b>JWT的三个组成部分：</b></p>
 * <p>JWT由三部分组成，用点号分隔：header.payload.signature</p>
 * <ul>
 * <li><b>Header（头部）：</b>包含令牌类型和加密算法信息</li>
 * <li><b>Payload（载荷）：</b>包含用户信息和自定义声明</li>
 * <li><b>Signature（签名）：</b>确保令牌未被篡改的数字签名</li>
 * </ul>
 *
 * <p><b>安全设计考虑：</b></p>
 * <p>设计JWT系统时特别注意了以下安全要点：</p>
 * <p>1. <strong>密钥管理：</strong>使用强密钥并定期轮换</p>
 * <p>2. <strong>过期控制：</strong>设置合理的令牌过期时间</p>
 * <p>3. <strong>敏感信息保护：</strong>不在JWT中存储密码等敏感数据</p>
 * <p>4. <strong>异常处理：</strong>妥善处理各种令牌异常情况</p>
 */
@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    /**
     * JWT签名密钥
     * <p>这个密钥用于签名和验证JWT令牌，必须保密且足够复杂。
     * 在生产环境中，建议使用外部密钥管理服务。</p>
     */
    private final SecretKey jwtSecret;

    /**
     * JWT过期时间（毫秒）
     * <p>访问令牌的有效期，平衡安全性和用户体验。
     * 过短会频繁要求用户重新登录，过长会增加安全风险。</p>
     */
    @Value("${app.jwt.expiration:86400000}")  // 默认24小时
    private long jwtExpirationMs;

    /**
     * 刷新令牌过期时间（毫秒）
     * <p>刷新令牌的有效期，通常比访问令牌长得多。
     * 用于在访问令牌过期后获取新的访问令牌。</p>
     */
    @Value("${app.jwt.refresh-expiration:604800000}")  // 默认7天
    private long refreshTokenExpirationMs;

    /**
     * JWT签发者标识
     * <p>标识令牌的签发方，用于防止令牌被其他系统误用。</p>
     */
    @Value("${app.jwt.issuer:baseai-cloud}")
    private String jwtIssuer;

    /**
     * 构造函数，初始化JWT密钥
     *
     * <p>我们使用配置的密钥字符串生成加密安全的密钥对象。
     * 如果没有配置密钥，会使用默认值（仅适用于开发环境）。</p>
     */
    public JwtUtils(@Value("${app.jwt.secret:super-secret-key-for-jwt-signing}") String jwtSecretKey) {
        // 确保密钥长度足够（HMAC-SHA256至少需要256位）
        if (jwtSecretKey.length() < 32) {
            log.warn("JWT密钥长度不足，建议使用至少32个字符的密钥");
            jwtSecretKey = jwtSecretKey + "padding-to-make-key-longer-for-security";
        }
        this.jwtSecret = Keys.hmacShaKeyFor(jwtSecretKey.getBytes());
    }

    /**
     * 生成访问令牌
     *
     * <p>这是JWT系统的核心方法之一。它接收用户的认证信息，
     * 生成一个包含用户身份和权限的访问令牌。就像为用户颁发
     * 一张有时效的数字通行证。</p>
     *
     * @param authentication Spring Security的认证对象
     * @return 生成的JWT访问令牌字符串
     */
    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateAccessToken(userPrincipal);
    }

    /**
     * 根据用户主体生成访问令牌
     *
     * <p>这个重载方法允许我们直接从UserPrincipal对象生成令牌，
     * 在刷新令牌等场景中很有用。</p>
     */
    public String generateAccessToken(UserPrincipal userPrincipal) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(jwtExpirationMs, ChronoUnit.MILLIS);

        // 提取用户角色，用于后续的权限检查
        List<String> roles = userPrincipal.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(userPrincipal.getId().toString())  // 用户ID作为主题
                .setIssuer(jwtIssuer)                          // 设置签发者
                .setIssuedAt(Date.from(now))                   // 签发时间
                .setExpiration(Date.from(expiryDate))          // 过期时间

                // 自定义声明：包含业务相关的用户信息
                .claim("username", userPrincipal.getUsername())
                .claim("email", userPrincipal.getEmail())
                .claim("roles", roles)
                .claim("tenantIds", userPrincipal.getTenantIds())  // 多租户支持
                .claim("tokenType", "access")                      // 令牌类型标识

                .signWith(jwtSecret)                               // 使用密钥签名
                .compact();                                        // 生成最终的JWT字符串
    }

    /**
     * 生成刷新令牌
     *
     * <p>刷新令牌用于在访问令牌过期后获取新的访问令牌，无需用户重新登录。
     * 这种设计既保证了安全性（访问令牌过期时间短），又提升了用户体验
     * （不需要频繁登录）。</p>
     */
    public String generateRefreshToken(UserPrincipal userPrincipal) {
        Instant now = Instant.now();
        Instant expiryDate = now.plus(refreshTokenExpirationMs, ChronoUnit.MILLIS);

        return Jwts.builder()
                .setSubject(userPrincipal.getId().toString())
                .setIssuer(jwtIssuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiryDate))
                .claim("tokenType", "refresh")      // 明确标识这是刷新令牌
                .claim("username", userPrincipal.getUsername())
                .signWith(jwtSecret)
                .compact();
    }

    /**
     * 从JWT令牌中提取用户ID
     *
     * <p>这个方法解析JWT令牌并提取其中的用户ID。用户ID存储在JWT的
     * subject字段中，是令牌的核心标识信息。</p>
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 从JWT令牌中提取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 从JWT令牌中提取用户邮箱
     */
    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("email", String.class);
    }

    /**
     * 从JWT令牌中提取用户角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("roles", List.class);
    }

    /**
     * 从JWT令牌中提取租户ID列表
     *
     * <p>在多租户系统中，用户可能属于多个租户。这个方法提取用户
     * 有权访问的所有租户ID，用于后续的权限控制。</p>
     */
    @SuppressWarnings("unchecked")
    public List<Long> getTenantIdsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        List<Object> tenantIds = claims.get("tenantIds", List.class);
        if (tenantIds == null) {
            return List.of();
        }
        return tenantIds.stream()
                .map(id -> Long.valueOf(id.toString()))
                .collect(Collectors.toList());
    }

    /**
     * 获取令牌过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * 检查令牌是否为刷新令牌
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String tokenType = claims.get("tokenType", String.class);
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 验证JWT令牌的有效性
     *
     * <p>这是安全系统的关键方法，它验证令牌的完整性和有效性。
     * 就像银行验证钞票的真伪一样，这个方法确保令牌没有被伪造或篡改。</p>
     *
     * <p><b>验证步骤：</b></p>
     * <p>1. 检查令牌格式是否正确</p>
     * <p>2. 验证数字签名是否匹配</p>
     * <p>3. 检查令牌是否过期</p>
     * <p>4. 验证签发者是否正确</p>
     *
     * @param token 需要验证的JWT令牌
     * @return 令牌有效返回true，否则返回false
     */
    public boolean validateToken(String token) {
        try {
            // 解析并验证令牌
            Jws<Claims> claimsJws = Jwts.parser()
                    .setSigningKey(jwtSecret)     // 设置验证密钥
                    .requireIssuer(jwtIssuer)     // 验证签发者
                    .build()
                    .parseClaimsJws(token);       // 解析令牌

            // 检查令牌是否过期
            Claims claims = claimsJws.getBody();
            Date expiration = claims.getExpiration();
            if (expiration.before(new Date())) {
                log.debug("JWT令牌已过期");
                return false;
            }

            log.debug("JWT令牌验证成功");
            return true;

        } catch (ExpiredJwtException e) {
            log.debug("JWT令牌已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("不支持的JWT令牌: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("无效的JWT令牌格式: {}", e.getMessage());
        } catch (SecurityException e) {
            log.error("JWT签名验证失败: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT令牌参数错误: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT令牌验证失败: {}", e.getMessage());
        }

        return false;
    }

    /**
     * 从令牌中提取声明信息
     *
     * <p>这是一个核心的私有方法，用于解析JWT令牌并提取其中的声明信息。
     * 声明（Claims）包含了令牌的所有有效载荷数据。</p>
     *
     * @param token JWT令牌字符串
     * @return 解析出的声明对象
     * @throws JwtException 当令牌无效时抛出异常
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(jwtSecret)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 检查令牌是否即将过期
     *
     * <p>这个方法用于判断令牌是否在指定时间内过期，可以用于
     * 提前刷新令牌，避免用户在使用过程中突然遇到令牌过期的情况。</p>
     *
     * @param token            要检查的令牌
     * @param thresholdMinutes 阈值（分钟）
     * @return 如果令牌将在阈值时间内过期，返回true
     */
    public boolean isTokenExpiringSoon(String token, int thresholdMinutes) {
        try {
            Date expirationDate = getExpirationDateFromToken(token);
            Date thresholdDate = Date.from(Instant.now().plus(thresholdMinutes, ChronoUnit.MINUTES));
            return expirationDate.before(thresholdDate);
        } catch (Exception e) {
            log.debug("检查令牌过期时间失败: {}", e.getMessage());
            return true; // 出错时假设即将过期
        }
    }

    /**
     * 获取令牌剩余有效时间（秒）
     */
    public long getTokenRemainingTime(String token) {
        try {
            Date expirationDate = getExpirationDateFromToken(token);
            long currentTime = Instant.now().toEpochMilli();
            long expirationTime = expirationDate.getTime();
            return Math.max(0, (expirationTime - currentTime) / 1000);
        } catch (Exception e) {
            log.debug("获取令牌剩余时间失败: {}", e.getMessage());
            return 0;
        }
    }
}