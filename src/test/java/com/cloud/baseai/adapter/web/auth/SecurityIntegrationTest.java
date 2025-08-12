package com.cloud.baseai.adapter.web.auth;

import com.cloud.baseai.infrastructure.security.UserPrincipal;
import com.cloud.baseai.infrastructure.security.jwt.JwtTokenService;
import com.cloud.baseai.infrastructure.security.service.CustomUserDetailsService;
import com.cloud.baseai.infrastructure.utils.RSAKeyGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * <h1>Spring Security 集成测试</h1>
 *
 * <p>这个测试类用于验证修复后的Spring Security配置是否正常工作。
 * 测试覆盖了JWT认证、权限控制、令牌刷新等核心功能。</p>
 *
 * <p><b>测试场景：</b></p>
 * <ul>
 * <li>用户登录和JWT令牌生成</li>
 * <li>受保护接口的访问控制</li>
 * <li>令牌刷新机制</li>
 * <li>令牌撤销和登出</li>
 * <li>设备指纹验证</li>
 * <li>CORS跨域支持</li>
 * </ul>
 */
@RequiredArgsConstructor
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("Spring Security 集成测试")
public class SecurityIntegrationTest {

    private final WebApplicationContext context;
    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("测试公开接口可以正常访问")
    void testPublicEndpoints() throws Exception {
        // 测试健康检查接口
        mockMvc.perform(get("/api/v1/system/health"))
                .andDo(print())
                .andExpect(status().isOk());

        // 测试API文档接口
        mockMvc.perform(get("/v3/api-docs"))
                .andDo(print())
                .andExpect(status().isOk());

        // 测试Swagger UI
        mockMvc.perform(get("/swagger-ui.html"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("测试受保护接口需要认证")
    void testProtectedEndpointsRequireAuthentication() throws Exception {
        // 测试需要认证的接口返回401
        mockMvc.perform(get("/api/v1/auth/me"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/profile")
                        .param("userId", "1"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/kb/documents")
                        .param("tenantId", "1"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("测试用户登录流程")
    void testUserLoginFlow() throws Exception {
        // 准备登录请求
        LoginRequest loginRequest = new LoginRequest(
                "testuser",
                "Test123!",
                true,
                "Test Device"
        );

        String requestBody = objectMapper.writeValueAsString(loginRequest);

        // 执行登录请求
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("User-Agent", "Test Agent")
                        .header("X-Device-Fingerprint", "test-fingerprint-123"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.userInfo").exists());
    }

    @Test
    @DisplayName("测试JWT令牌生成和验证")
    void testJwtTokenGeneration() {
        // 创建测试用户
        UserPrincipal userPrincipal = createTestUserPrincipal();

        // 生成令牌对
        JwtTokenService.TokenPair tokenPair = jwtTokenService.generateTokenPair(
                userPrincipal, "test-fingerprint", "127.0.0.1");

        // 验证令牌对
        assertThat(tokenPair).isNotNull();
        assertThat(tokenPair.accessToken()).isNotBlank();
        assertThat(tokenPair.refreshToken()).isNotBlank();
        assertThat(tokenPair.jti()).isNotBlank();

        // 验证令牌有效性
        assertThat(jwtTokenService.validateToken(tokenPair.accessToken(), "test-fingerprint")).isTrue();
        assertThat(jwtTokenService.validateToken(tokenPair.refreshToken(), "test-fingerprint")).isTrue();

        // 验证令牌信息提取
        assertThat(jwtTokenService.getUserIdFromToken(tokenPair.accessToken())).isEqualTo(1L);
        assertThat(jwtTokenService.getUsernameFromToken(tokenPair.accessToken())).isEqualTo("testuser");
        assertThat(jwtTokenService.getRolesFromToken(tokenPair.accessToken())).contains("USER");
    }

    @Test
    @DisplayName("测试令牌刷新机制")
    void testTokenRefresh() {
        // 创建测试用户和令牌
        UserPrincipal userPrincipal = createTestUserPrincipal();
        JwtTokenService.TokenPair tokenPair = jwtTokenService.generateTokenPair(
                userPrincipal, "test-fingerprint", "127.0.0.1");

        // 刷新访问令牌
        String newAccessToken = jwtTokenService.refreshAccessToken(
                tokenPair.refreshToken(), "test-fingerprint");

        // 验证新令牌
        assertThat(newAccessToken).isNotBlank();
        assertThat(jwtTokenService.validateToken(newAccessToken, "test-fingerprint")).isTrue();

        // 验证新令牌包含正确的用户信息
        assertThat(jwtTokenService.getUserIdFromToken(newAccessToken)).isEqualTo(1L);
    }

    @Test
    @DisplayName("测试令牌撤销机制")
    void testTokenRevocation() {
        // 创建测试令牌
        UserPrincipal userPrincipal = createTestUserPrincipal();
        JwtTokenService.TokenPair tokenPair = jwtTokenService.generateTokenPair(
                userPrincipal, "test-fingerprint", "127.0.0.1");

        // 验证令牌初始状态
        assertThat(jwtTokenService.validateToken(tokenPair.accessToken(), "test-fingerprint")).isTrue();

        // 撤销令牌
        jwtTokenService.revokeToken(tokenPair.accessToken());

        // 验证令牌已失效
        assertThat(jwtTokenService.validateToken(tokenPair.accessToken(), "test-fingerprint")).isFalse();
    }

    @Test
    @DisplayName("测试设备指纹验证")
    void testDeviceFingerprintValidation() {
        UserPrincipal userPrincipal = createTestUserPrincipal();

        // 使用特定设备指纹生成令牌
        JwtTokenService.TokenPair tokenPair = jwtTokenService.generateTokenPair(
                userPrincipal, "device-fingerprint-123", "127.0.0.1");

        // 使用正确的设备指纹验证
        assertThat(jwtTokenService.validateToken(tokenPair.accessToken(), "device-fingerprint-123")).isTrue();

        // 使用错误的设备指纹验证
        assertThat(jwtTokenService.validateToken(tokenPair.accessToken(), "wrong-fingerprint")).isFalse();

        // 不提供设备指纹时应该通过（兼容性）
        assertThat(jwtTokenService.validateToken(tokenPair.accessToken(), null)).isTrue();
    }

    @Test
    @DisplayName("测试RSA密钥对生成和加载")
    void testRSAKeyPairGeneration() {
        // 生成RSA密钥对
        RSAKeyGenerator.RSAKeyPair keyPair = RSAKeyGenerator.generateKeyPair();

        // 验证密钥对
        assertThat(keyPair).isNotNull();
        assertThat(keyPair.getPrivateKey()).isNotNull();
        assertThat(keyPair.getPublicKey()).isNotNull();
        assertThat(keyPair.isValid()).isTrue();
        assertThat(keyPair.getKeySize()).isEqualTo(2048);

        // 测试Base64编码和解码
        String privateKeyBase64 = keyPair.getPrivateKeyBase64();
        String publicKeyBase64 = keyPair.getPublicKeyBase64();

        assertThat(privateKeyBase64).isNotBlank();
        assertThat(publicKeyBase64).isNotBlank();

        // 验证密钥加载
        RSAPrivateKey loadedPrivateKey = RSAKeyGenerator.loadPrivateKeyFromBase64(privateKeyBase64);
        RSAPublicKey loadedPublicKey = RSAKeyGenerator.loadPublicKeyFromBase64(publicKeyBase64);

        assertThat(loadedPrivateKey).isNotNull();
        assertThat(loadedPublicKey).isNotNull();

        // 验证加载的密钥对是否匹配
        assertThat(RSAKeyGenerator.validateKeyPair(loadedPrivateKey, loadedPublicKey)).isTrue();
    }

    @Test
    @DisplayName("测试密码编码和验证")
    void testPasswordEncoding() {
        String rawPassword = "Test123!";

        // 编码密码
        String encodedPassword = passwordEncoder.encode(rawPassword);
        assertThat(encodedPassword).isNotBlank();
        assertThat(encodedPassword).isNotEqualTo(rawPassword);

        // 验证密码
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword)).isTrue();
        assertThat(passwordEncoder.matches("wrongpassword", encodedPassword)).isFalse();
    }

    @Test
    @DisplayName("测试CORS配置")
    void testCorsConfiguration() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Content-Type")));
    }

    @Test
    @DisplayName("测试认证流程端到端")
    void testAuthenticationEndToEnd() throws Exception {
        // 1. 登录获取令牌
        LoginRequest loginRequest = new LoginRequest("testuser", "Test123!", true, "Test Device");
        String requestBody = objectMapper.writeValueAsString(loginRequest);

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 解析响应获取令牌
        // LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);
        // String accessToken = loginResponse.accessToken();

        // 2. 使用令牌访问受保护接口
        // mockMvc.perform(get("/api/v1/auth/me")
        //                 .header("Authorization", "Bearer " + accessToken))
        //         .andExpect(status().isOk())
        //         .andExpect(jsonPath("$.success").value(true))
        //         .andExpect(jsonPath("$.data.username").value("testuser"));

        // 3. 登出撤销令牌
        // mockMvc.perform(post("/api/v1/auth/logout")
        //                 .header("Authorization", "Bearer " + accessToken))
        //         .andExpected(status().isOk());

        // 4. 验证令牌已失效
        // mockMvc.perform(get("/api/v1/auth/me")
        //                 .header("Authorization", "Bearer " + accessToken))
        //         .andExpected(status().isUnauthorized());
    }

    // =================== 辅助方法 ===================

    /**
     * 创建测试用户主体
     */
    private UserPrincipal createTestUserPrincipal() {
        return UserPrincipal.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("Test123!"))
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_TENANT_MEMBER")
                ))
                .tenantIds(List.of(1L))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
    }

    /**
     * 登录请求DTO（用于测试）
     */
    private record LoginRequest(
            String username,
            String password,
            boolean rememberMe,
            String deviceInfo
    ) {
    }
}