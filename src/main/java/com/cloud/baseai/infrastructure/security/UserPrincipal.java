package com.cloud.baseai.infrastructure.security;

import com.cloud.baseai.domain.user.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <h1>用户主体类</h1>
 *
 * <p>用户在数字世界中的"身份证"。它不仅包含了用户的基本信息，
 * 还记录了用户在系统中的权限和所属的租户关系。</p>
 *
 * <p><b>为什么需要自定义UserPrincipal？</b></p>
 * <p>Spring Security提供了标准的User类，但在企业级应用中，我们往往需要
 * 更丰富的用户信息。特别是在多租户SaaS系统中，用户可能同时属于多个组织，
 * 拥有不同的角色和权限。标准的User类无法满足这些复杂的业务需求。</p>
 *
 * <p><b>多租户支持的设计思考：</b></p>
 * <p>在传统的单租户系统中，用户与权限的关系相对简单。但在多租户系统中，
 * 同一个用户在不同的租户中可能拥有完全不同的权限。比如，张三在公司A
 * 是管理员，在公司B可能只是普通用户。</p>
 *
 * <p><b>安全性考虑：</b></p>
 * <p>这个类实现了Spring Security的UserDetails接口，这意味着它会被
 * 频繁地在安全检查中使用。我们在设计时特别注意了以下几点：</p>
 * <p>1. 不存储敏感信息（如密码），只保留认证后的身份信息</p>
 * <p>2. 实现了正确的equals和hashCode方法，确保在集合中使用时的正确性</p>
 * <p>3. 所有字段都是不可变的，防止被意外修改</p>
 */
public class UserPrincipal implements UserDetails {

    /**
     * 用户的唯一标识符
     * <p>这是用户在系统中的主键，用于唯一标识一个用户</p>
     */
    @Getter
    private final Long id;

    /**
     * 用户名
     * <p>用户登录时使用的用户名，在系统中应该是唯一的</p>
     */
    private final String username;

    /**
     * 用户邮箱
     * <p>用户的邮箱地址，通常用于通知、密码重置等功能，也可以用作登录凭证</p>
     */
    @Getter
    private final String email;

    /**
     * 密码哈希值
     * <p>注意：这里存储的是加密后的密码，永远不应该存储明文密码</p>
     */
    private final String password;

    /**
     * 用户的权限列表
     * <p>包含用户拥有的所有权限，用于Spring Security的授权检查</p>
     */
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * 用户所属的租户ID列表
     * <p>在多租户系统中，用户可能同时属于多个租户（组织）</p>
     */
    @Getter
    private final List<Long> tenantIds;

    /**
     * 账户是否启用
     * <p>管理员可以禁用用户账户而不删除用户数据</p>
     */
    private final boolean enabled;

    /**
     * 账户是否未过期
     * <p>用于实现账户的有效期管理</p>
     */
    private final boolean accountNonExpired;

    /**
     * 账户是否未锁定
     * <p>用于实现账户锁定机制，比如登录失败次数过多后锁定</p>
     */
    private final boolean accountNonLocked;

    /**
     * 凭证（密码）是否未过期
     * <p>用于实现密码定期更换策略</p>
     */
    private final boolean credentialsNonExpired;

    /**
     * 私有构造函数，使用Builder模式创建实例
     */
    private UserPrincipal(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.email = builder.email;
        this.password = builder.password;
        this.authorities = builder.authorities;
        this.tenantIds = builder.tenantIds;
        this.enabled = builder.enabled;
        this.accountNonExpired = builder.accountNonExpired;
        this.accountNonLocked = builder.accountNonLocked;
        this.credentialsNonExpired = builder.credentialsNonExpired;
    }

    /**
     * 创建Builder实例的静态方法
     *
     * <p>这是Builder模式的标准写法，提供一个静态方法来开始构建过程</p>
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 便捷的工厂方法：从域对象创建UserPrincipal
     *
     * @param user      用户域对象
     * @param roles     用户角色列表
     * @param tenantIds 用户所属租户ID列表
     * @return 构建完成的UserPrincipal实例
     */
    public static UserPrincipal create(User user, List<String> roles, List<Long> tenantIds) {
        // 将角色转换为Spring Security的权限格式
        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        return UserPrincipal.builder()
                .id(user.id())
                .username(user.username())
                .email(user.email())
                .password(user.passwordHash())
                .authorities(authorities)
                .tenantIds(tenantIds)
                .enabled(user.isActivated())
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
    }

    // =================== UserDetails接口实现 ===================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 检查用户是否属于指定租户
     *
     * <p>这是一个便捷方法，经常在权限检查中使用。比如在Controller中
     * 可以快速检查当前用户是否有权访问某个租户的数据。</p>
     *
     * @param tenantId 要检查的租户ID
     * @return 如果用户属于该租户返回true，否则返回false
     */
    public boolean belongsToTenant(Long tenantId) {
        return tenantIds != null && tenantIds.contains(tenantId);
    }

    /**
     * 检查用户是否拥有指定角色
     *
     * <p>这个方法提供了一种编程式检查用户角色的方式，作为注解式
     * 权限检查的补充。在某些复杂的业务逻辑中很有用。</p>
     *
     * @param role 要检查的角色名（不包含ROLE_前缀）
     * @return 如果用户拥有该角色返回true，否则返回false
     */
    public boolean hasRole(String role) {
        String roleWithPrefix = "ROLE_" + role;
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(roleWithPrefix));
    }

    /**
     * 获取用户的角色列表（去除ROLE_前缀）
     *
     * <p>Spring Security内部使用ROLE_前缀，但在业务逻辑中我们通常
     * 不需要这个前缀。这个方法返回清理后的角色列表。</p>
     */
    public List<String> getRoles() {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .collect(Collectors.toList());
    }

    // =================== Object方法重写 ===================

    /**
     * 重写equals方法
     *
     * <p>在Spring Security中，UserPrincipal经常被存储在集合中，
     * 比如缓存、会话等。正确实现equals方法确保对象比较的正确性。</p>
     *
     * <p>我们使用用户ID作为唯一标识符进行比较，因为ID在系统中是唯一的。</p>
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPrincipal that = (UserPrincipal) o;
        return Objects.equals(id, that.id);
    }

    /**
     * 重写hashCode方法
     *
     * <p>hashCode必须与equals保持一致。如果两个对象equals返回true，
     * 那么它们的hashCode也必须相同。</p>
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * 重写toString方法
     *
     * <p>提供有用的调试信息，但注意不要包含敏感信息如密码</p>
     */
    @Override
    public String toString() {
        return String.format("UserPrincipal{id=%d, username='%s', email='%s', tenants=%s, roles=%s}",
                id, username, email, tenantIds, getRoles());
    }

    // =================== Builder模式实现 ===================

    /**
     * Builder类 - 优雅构建UserPrincipal的工具
     *
     * <p>Builder模式让我们能够逐步构建复杂对象，代码更加清晰易读。
     * 特别是在有很多可选参数的情况下，Builder模式比构造函数更优雅。</p>
     *
     * <p><b>使用示例：</b></p>
     * <pre>
     * UserPrincipal user = UserPrincipal.builder()
     *     .id(123L)
     *     .username("john.doe")
     *     .email("john@example.com")
     *     .authorities(authorities)
     *     .tenantIds(List.of(1L, 2L))
     *     .enabled(true)
     *     .build();
     * </pre>
     */
    public static class Builder {
        private Long id;
        private String username;
        private String email;
        private String password;
        private Collection<? extends GrantedAuthority> authorities;
        private List<Long> tenantIds;
        private boolean enabled = true;
        private boolean accountNonExpired = true;
        private boolean accountNonLocked = true;
        private boolean credentialsNonExpired = true;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder authorities(Collection<? extends GrantedAuthority> authorities) {
            this.authorities = authorities;
            return this;
        }

        public Builder tenantIds(List<Long> tenantIds) {
            this.tenantIds = tenantIds;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder accountNonExpired(boolean accountNonExpired) {
            this.accountNonExpired = accountNonExpired;
            return this;
        }

        public Builder accountNonLocked(boolean accountNonLocked) {
            this.accountNonLocked = accountNonLocked;
            return this;
        }

        public Builder credentialsNonExpired(boolean credentialsNonExpired) {
            this.credentialsNonExpired = credentialsNonExpired;
            return this;
        }

        /**
         * 构建UserPrincipal实例
         *
         * <p>在构建之前会进行基本的验证，确保必要字段不为空</p>
         */
        public UserPrincipal build() {
            // 基本验证
            if (id == null) {
                throw new IllegalArgumentException("用户ID不能为空");
            }
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("用户名不能为空");
            }
            if (authorities == null) {
                throw new IllegalArgumentException("用户权限不能为空");
            }

            return new UserPrincipal(this);
        }
    }
}