# BaseAI Cloud Spring Security 完整配置指南

## 🎯 概述

本文档提供了BaseAI Cloud项目中Spring Security完整实现的配置和使用指南。我们的安全系统采用了现代化的JWT无状态认证机制，结合细粒度的权限控制，为多租户SaaS平台提供了企业级的安全保障。

## 🏗️ 架构设计

### 核心组件架构图

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   HTTP Request  │───▶│  CORS Filter    │───▶│Security Filter  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                              ┌─────────────────────────┘
                              ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│JWT Entry Point  │◀───│JWT Auth Filter  │───▶│Authentication   │
│(认证失败处理)    │    │(令牌验证)       │    │Manager          │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                              ┌─────────────────────────┘
                              ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│UserDetails      │◀───│Permission       │───▶│Business         │
│Service          │    │Evaluator        │    │Controller       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 安全层次结构

1. **网络层安全**: CORS配置，防止跨域攻击
2. **认证层安全**: JWT令牌验证，确保用户身份
3. **授权层安全**: 基于角色和权限的访问控制
4. **业务层安全**: 细粒度的资源级权限检查
5. **数据层安全**: 多租户数据隔离

## 🔧 配置步骤

### 1. 添加Maven依赖

确保在`pom.xml`中包含以下依赖：

```xml

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
<groupId>io.jsonwebtoken</groupId>
<artifactId>jjwt-api</artifactId>
<version>0.12.3</version>
</dependency>
<dependency>
<groupId>io.jsonwebtoken</groupId>
<artifactId>jjwt-impl</artifactId>
<version>0.12.3</version>
<scope>runtime</scope>
</dependency>
<dependency>
<groupId>io.jsonwebtoken</groupId>
<artifactId>jjwt-jackson</artifactId>
<version>0.12.3</version>
<scope>runtime</scope>
</dependency>
```

### 2. 应用配置

在`application.yml`中添加JWT相关配置：

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:your-very-long-secret-key-at-least-256-bits-for-production}
    expiration: 86400000  # 24小时
    refresh-expiration: 604800000  # 7天
    issuer: baseai-cloud

security:
  # 允许的跨域源
  cors:
    allowed-origins: "*"
    allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"
    allowed-headers: "*"
    allow-credentials: true
    max-age: 3600

logging:
  level:
    com.cloud.baseai.infrastructure.security: DEBUG
```

### 3. 数据库表结构

确保数据库中存在以下核心表：

```sql
-- 用户表
CREATE TABLE sys_users
(
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50) UNIQUE  NOT NULL,
    email         VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255)        NOT NULL,
    enabled       BOOLEAN   DEFAULT true,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 角色表
CREATE TABLE sys_roles
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(50) UNIQUE NOT NULL,
    label      VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户角色关联表
CREATE TABLE sys_user_roles
(
    user_id BIGINT REFERENCES sys_user (id),
    role_id BIGINT REFERENCES sys_roles (id),
    PRIMARY KEY (user_id, role_id)
);

-- 租户表
CREATE TABLE sys_tenants
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100)       NOT NULL,
    code       VARCHAR(50) UNIQUE NOT NULL,
    enabled    BOOLEAN   DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户租户关联表
CREATE TABLE sys_user_tenants
(
    user_id   BIGINT REFERENCES sys_user (id),
    tenant_id BIGINT REFERENCES sys_tenants (id),
    status    VARCHAR(20) DEFAULT 'ACTIVE',
    joined_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, tenant_id)
);
```

### 4. 初始化数据

插入基础角色和超级管理员：

```sql
-- 插入系统角色
INSERT INTO sys_roles (name, label)
VALUES ('SUPER_ADMIN', '超级管理员'),
       ('SYSTEM_ADMIN', '系统管理员'),
       ('TENANT_OWNER', '租户所有者'),
       ('TENANT_ADMIN', '租户管理员'),
       ('USER', '普通用户'),
       ('AUDITOR', '审计员'),
       ('TOOL_ADMIN', '工具管理员');

-- 创建超级管理员用户
INSERT INTO sys_users (username, email, password_hash)
VALUES ('admin', 'admin@baseai.com', '$2a$12$your-bcrypt-encoded-password');

-- 分配超级管理员角色
INSERT INTO sys_user_roles (user_id, role_id)
SELECT u.id, r.id
FROM sys_users u,
     sys_roles r
WHERE u.username = 'admin'
  AND r.name = 'SUPER_ADMIN';
```

## 🚀 使用指南

### 1. Controller层权限控制

```java

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    // 基于角色的权限控制
    @PreAuthorize("hasRole('USER')")
    @GetMapping
    public List<DocumentDTO> listDocuments() {
        return documentService.listDocuments();
    }

    // 基于资源的权限控制
    @PreAuthorize("hasPermission(#documentId, 'DOCUMENT', 'READ')")
    @GetMapping("/{documentId}")
    public DocumentDTO getDocument(@PathVariable Long documentId) {
        return documentService.getDocument(documentId);
    }

    // 基于租户的权限控制
    @PreAuthorize("hasPermission(#tenantId, 'TENANT', 'ADMIN')")
    @PostMapping("/batch-import")
    public void batchImport(@RequestParam Long tenantId,
                            @RequestBody List<DocumentDTO> documents) {
        documentService.batchImport(tenantId, documents);
    }

    // 复合权限控制
    @PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and @securityService.isOwner(#documentId, authentication.principal.id))")
    @DeleteMapping("/{documentId}")
    public void deleteDocument(@PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
    }
}
```

### 2. Service层编程式权限检查

```java

@Service
public class DocumentService {

    public List<DocumentDTO> getUserDocuments() {
        // 获取当前用户ID
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("用户未登录"));

        // 获取用户所属租户
        List<Long> tenantIds = SecurityUtils.getCurrentUserTenantIds();

        return documentRepository.findByUserIdAndTenantIds(userId, tenantIds);
    }

    public void updateDocument(Long documentId, DocumentDTO dto) {
        // 检查用户是否有权限修改此文档
        if (!SecurityUtils.hasRole("ADMIN") && !isDocumentOwner(documentId)) {
            throw new AccessDeniedException("无权限修改此文档");
        }

        // 执行更新逻辑
        documentRepository.update(documentId, dto);
    }

    private boolean isDocumentOwner(Long documentId) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (currentUserId == null) {
            return false;
        }

        Document document = documentRepository.findById(documentId);
        return document != null && document.getUserId().equals(currentUserId);
    }
}
```

### 3. 前端集成示例

#### 登录请求

```javascript
// 用户登录
async function login(username, password) {
    try {
        const response = await fetch('/api/v1/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                username: username,
                password: password,
                rememberMe: true,
                deviceInfo: navigator.userAgent
            })
        });

        const result = await response.json();

        if (result.success) {
            // 存储令牌
            localStorage.setItem('accessToken', result.data.accessToken);
            localStorage.setItem('refreshToken', result.data.refreshToken);

            // 存储用户信息
            localStorage.setItem('userInfo', JSON.stringify(result.data.userInfo));

            return result.data;
        } else {
            throw new Error(result.message);
        }
    } catch (error) {
        console.error('登录失败:', error);
        throw error;
    }
}
```

#### 自动令牌刷新

```javascript
// HTTP拦截器，自动处理令牌刷新
axios.interceptors.request.use(
    config => {
        const token = localStorage.getItem('accessToken');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    error => Promise.reject(error)
);

axios.interceptors.response.use(
    response => response,
    async error => {
        const originalRequest = error.config;

        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                const refreshToken = localStorage.getItem('refreshToken');
                if (!refreshToken) {
                    throw new Error('No refresh token');
                }

                const response = await axios.post('/api/v1/auth/refresh', {
                    refreshToken: refreshToken
                });

                const {accessToken, refreshToken: newRefreshToken} = response.data.data;

                localStorage.setItem('accessToken', accessToken);
                localStorage.setItem('refreshToken', newRefreshToken);

                originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                return axios(originalRequest);

            } catch (refreshError) {
                // 刷新失败，跳转到登录页
                localStorage.clear();
                window.location.href = '/login';
                return Promise.reject(refreshError);
            }
        }

        return Promise.reject(error);
    }
);
```

## 🔒 安全最佳实践

### 1. 密码安全

```java
// 在用户注册时验证密码强度
@PostMapping("/register")
public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    // 验证密码强度
    PasswordStrengthResult strengthResult = SecurityUtils.checkPasswordStrength(request.getPassword());
    if (!strengthResult.isStrong()) {
        return ResponseEntity.badRequest()
                .body(ApiResult.error("WEAK_PASSWORD", strengthResult.getMessage()));
    }

    // 加密密码
    String encodedPassword = SecurityUtils.encodePassword(request.getPassword());

    // 创建用户
    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPassword(encodedPassword);

    userService.createUser(user);

    return ResponseEntity.ok(ApiResult.success("注册成功"));
}
```

### 2. 日志脱敏

```java

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    public void logUserAction(String action, Object data) {
        // 脱敏处理
        String maskedData = maskSensitiveData(data);

        log.info("用户操作: userId={}, action={}, data={}",
                SecurityUtils.getCurrentUserId().orElse(null),
                action,
                maskedData);
    }

    private String maskSensitiveData(Object data) {
        if (data instanceof User user) {
            return String.format("User{id=%d, username=%s, email=%s}",
                    user.getId(),
                    user.getUsername(),
                    SecurityUtils.maskEmail(user.getEmail()));
        }
        return data.toString();
    }
}
```

### 3. 安全配置检查

```yaml
# 生产环境安全配置检查清单

app:
  jwt:
    # ✅ 使用强密钥（至少256位）
    secret: ${JWT_SECRET} # 从环境变量读取
    # ✅ 合理的过期时间
    expiration: 3600000   # 1小时（生产环境建议更短）
    # ✅ 指定明确的签发者
    issuer: baseai-cloud-prod

security:
  cors:
    # ⚠️ 生产环境不应使用通配符
    allowed-origins: "https://app.baseai.com,https://admin.baseai.com"
    allow-credentials: true

logging:
  level:
    # ⚠️ 生产环境不应开启DEBUG日志
    com.cloud.baseai.infrastructure.security: INFO
    org.springframework.security: WARN
```

## 📊 监控和审计

### 1. 安全事件监控

```java

@Component
public class SecurityEventListener {

    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY");

    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        UserPrincipal user = (UserPrincipal) event.getAuthentication().getPrincipal();

        securityLog.info("LOGIN_SUCCESS: userId={}, username={}, timestamp={}",
                user.getId(), user.getUsername(), Instant.now());
    }

    @EventListener
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = (String) event.getAuthentication().getPrincipal();

        securityLog.warn("LOGIN_FAILURE: username={}, reason={}, timestamp={}",
                username, event.getException().getMessage(), Instant.now());
    }

    @EventListener
    public void handleAccessDenied(AuthorizationDeniedEvent event) {
        Authentication auth = event.getAuthentication().get();

        securityLog.warn("ACCESS_DENIED: userId={}, resource={}, timestamp={}",
                auth.getName(), event.getAuthorizationDecision(), Instant.now());
    }
}
```

### 2. 性能监控

```java

@Component
public class SecurityMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter loginAttempts;
    private final Counter loginSuccesses;
    private final Counter loginFailures;
    private final Timer jwtValidationTime;

    public SecurityMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.loginAttempts = Counter.builder("security.login.attempts").register(meterRegistry);
        this.loginSuccesses = Counter.builder("security.login.success").register(meterRegistry);
        this.loginFailures = Counter.builder("security.login.failure").register(meterRegistry);
        this.jwtValidationTime = Timer.builder("security.jwt.validation.time").register(meterRegistry);
    }

    public void recordLoginAttempt() {
        loginAttempts.increment();
    }

    public void recordLoginSuccess() {
        loginSuccesses.increment();
    }

    public void recordLoginFailure() {
        loginFailures.increment();
    }

    public Timer.Sample startJwtValidation() {
        return Timer.start(meterRegistry);
    }
}
```

## 🔍 故障排除

### 常见问题和解决方案

#### 1. JWT令牌无法验证

**症状**: 用户登录后立即被要求重新登录

**可能原因**:

- JWT密钥配置错误
- 时钟同步问题
- 令牌格式问题

**解决方法**:

```bash
# 检查JWT密钥配置
echo $JWT_SECRET | wc -c  # 应该至少32个字符

# 检查系统时间
date
ntpdate -q pool.ntp.org

# 启用DEBUG日志查看详细错误
logging.level.com.cloud.baseai.infrastructure.security.jwt: DEBUG
```

#### 2. 权限检查失败

**症状**: 有权限的用户无法访问资源

**排查步骤**:

```java
// 添加调试代码
@PreAuthorize("hasPermission(#documentId, 'DOCUMENT', 'READ')")
public DocumentDTO getDocument(@PathVariable Long documentId) {
    // 调试当前用户信息
    log.debug("Current user: {}", SecurityUtils.getCurrentUserPrincipal());
    log.debug("User roles: {}", SecurityUtils.getCurrentUserRoles());
    log.debug("User tenants: {}", SecurityUtils.getCurrentUserTenantIds());

    return documentService.getDocument(documentId);
}
```

#### 3. CORS错误

**症状**: 前端跨域请求被阻止

**解决方法**:

```yaml
security:
  cors:
    allowed-origins: "http://localhost:3000,https://app.baseai.com"
    allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"
    allowed-headers: "*"
    allow-credentials: true
    max-age: 3600
```

## 📈 性能优化

### 1. 缓存策略

```java

@Configuration
@EnableCaching
public class SecurityCacheConfig {

    @Bean
    public CacheManager securityCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(15))
                .recordStats());
        return cacheManager;
    }
}
```

### 2. 数据库优化

```sql
-- 为用户认证查询添加索引
CREATE INDEX idx_user_username ON sys_users (username);
CREATE INDEX idx_user_email ON sys_users (email);
CREATE INDEX idx_user_role_user_id ON sys_user_roles (user_id);
CREATE INDEX idx_user_tenant_user_id ON sys_user_tenants (user_id);

-- 为权限检查添加复合索引
CREATE INDEX idx_user_tenant_status ON sys_user_tenants (user_id, tenant_id, status);
```

## 🎉 总结

本Spring Security实现提供了：

✅ **完整的JWT认证体系**
✅ **细粒度的权限控制**
✅ **多租户数据隔离**
✅ **企业级安全特性**
✅ **丰富的工具类支持**
✅ **完善的错误处理**
✅ **性能优化配置**

通过这套安全系统，BaseAI Cloud平台能够为企业用户提供可靠、安全、高效的认证授权服务。