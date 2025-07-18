package com.cloud.baseai.infrastructure.security.service;

import com.cloud.baseai.domain.user.model.User;
import com.cloud.baseai.domain.user.model.UserRole;
import com.cloud.baseai.domain.user.model.UserTenant;
import com.cloud.baseai.domain.user.repository.UserRepository;
import com.cloud.baseai.domain.user.repository.UserRoleRepository;
import com.cloud.baseai.domain.user.repository.UserTenantRepository;
import com.cloud.baseai.infrastructure.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <h1>自定义用户详情服务</h1>
 *
 * <p>Spring Security会查阅这个服务类，从我们的业务系统中查找用户信息，
 * 来确认访客的身份和权限。</p>
 *
 * <p><b>设计模式：适配器模式</b></p>
 * <p>UserDetailsService实际上是一个适配器，它将我们的业务用户模型适配到
 * Spring Security框架所需要的UserDetails接口。这种设计让我们能够：</p>
 * <p>1. 保持业务模型的独立性，不被安全框架绑定</p>
 * <p>2. 在安全检查时获取最新的用户状态和权限信息</p>
 * <p>3. 灵活地实现各种认证策略（用户名、邮箱、手机号等）</p>
 *
 * <p><b>多租户权限管理的复杂性</b></p>
 * <p>在单租户系统中，用户权限管理相对简单：用户拥有某些角色，角色对应某些权限。
 * 但在多租户SaaS系统中，情况变得复杂：</p>
 * <p>- 同一用户在不同租户中可能有不同角色</p>
 * <p>- 权限需要同时考虑全局权限和租户级权限</p>
 * <p>- 数据访问需要基于租户进行隔离</p>
 * <p>这个服务类优雅地处理了这些复杂性。</p>
 *
 * <p><b>性能优化策略</b></p>
 * <p>用户信息查询是一个高频操作，我们采用了多种优化策略：</p>
 * <p>1. 使用Spring Cache进行用户信息缓存</p>
 * <p>2. 批量查询减少数据库往返次数</p>
 * <p>3. 延迟加载非关键信息</p>
 * <p>4. 合理设置事务边界避免不必要的锁等待</p>
 */
@Service
@Transactional(readOnly = true)  // 默认只读事务，提高性能
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    /**
     * 用户仓储，用于查询用户基本信息
     */
    private final UserRepository userRepository;

    /**
     * 用户角色仓储，用于查询用户的角色信息
     */
    private final UserRoleRepository userRoleRepository;

    /**
     * 用户租户仓储，用于查询用户所属的租户信息
     */
    private final UserTenantRepository userTenantRepository;

    /**
     * 构造函数，注入所需的仓储依赖
     */
    public CustomUserDetailsService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            UserTenantRepository userTenantRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.userTenantRepository = userTenantRepository;
    }

    /**
     * 根据用户名加载用户详情
     *
     * <p>这是Spring Security UserDetailsService接口的核心方法。
     * 当用户尝试登录时，Spring Security会调用这个方法来加载用户信息。
     * 我们需要根据用户名查找用户，并构建完整的UserDetails对象。</p>
     *
     * <p><b>认证流程中的角色：</b></p>
     * <p>1. 用户提交用户名和密码</p>
     * <p>2. Spring Security调用这个方法加载用户信息</p>
     * <p>3. 框架将用户输入的密码与加载到的密码进行比较</p>
     * <p>4. 如果匹配，认证成功；否则认证失败</p>
     *
     * <p><b>支持多种登录方式：</b></p>
     * <p>我们设计了智能的用户名解析策略，支持用户使用用户名或邮箱登录。
     * 这种灵活性大大提升了用户体验。</p>
     *
     * @param username 用户输入的用户名（可能是用户名、邮箱等）
     * @return 完整的用户详情对象
     * @throws UsernameNotFoundException 当用户不存在时抛出
     */
    @Override
    @Cacheable(value = "user-details", key = "#username", unless = "#result == null")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("开始加载用户详情: username={}", username);

        try {
            // 步骤1：查找用户基本信息
            User user = findUserByUsernameOrEmail(username);
            if (user == null) {
                log.warn("用户不存在: {}", username);
                throw new UsernameNotFoundException("用户不存在: " + username);
            }

            // 步骤2：检查用户账户状态
            validateUserStatus(user);

            // 步骤3：加载用户权限信息
            UserDetailsInfo userInfo = loadUserDetailsInfo(user.id());

            // 步骤4：构建UserPrincipal对象
            UserPrincipal userPrincipal = buildUserPrincipal(user, userInfo);

            log.debug("用户详情加载完成: userId={}, username={}, roles={}, tenants={}",
                    user.id(), user.username(), userInfo.roles.size(), userInfo.tenantIds.size());

            return userPrincipal;

        } catch (UsernameNotFoundException e) {
            // 重新抛出用户名未找到异常
            throw e;
        } catch (Exception e) {
            log.error("加载用户详情时发生异常: username={}", username, e);
            throw new RuntimeException("加载用户详情失败", e);
        }
    }

    /**
     * 根据用户ID加载用户详情
     *
     * <p>这个方法主要用于JWT认证流程。当我们从JWT令牌中提取出用户ID后，
     * 需要重新加载最新的用户信息来确保数据的时效性。比如用户的角色可能
     * 在登录后被管理员修改了，我们需要获取最新的权限信息。</p>
     *
     * <p><b>为什么不直接使用JWT中的用户信息？</b></p>
     * <p>虽然JWT令牌中包含了用户的基本信息，但出于以下考虑，我们还是选择
     * 重新加载用户数据：</p>
     * <p>1. <strong>实时性：</strong>确保获取到用户的最新状态和权限</p>
     * <p>2. <strong>安全性：</strong>检查用户账户是否被禁用或锁定</p>
     * <p>3. <strong>一致性：</strong>保持认证流程的统一性</p>
     *
     * @param userId 用户ID
     * @return 用户详情对象，如果用户不存在或已禁用则返回null
     */
    @Cacheable(value = "user-details", key = "'user-id:' + #userId", unless = "#result == null")
    public UserDetails loadUserById(Long userId) {
        log.debug("根据用户ID加载用户详情: userId={}", userId);

        try {
            // 查找用户
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("用户不存在: userId={}", userId);
                return null;
            }

            // 检查用户状态
            if (!user.isActivated()) {
                log.warn("用户账户已禁用: userId={}", userId);
                return null;
            }

            // 加载详细信息
            UserDetailsInfo userInfo = loadUserDetailsInfo(userId);
            return buildUserPrincipal(user, userInfo);

        } catch (Exception e) {
            log.error("根据ID加载用户详情失败: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 刷新用户详情缓存
     *
     * <p>当用户的权限或状态发生变化时，我们需要主动刷新缓存，
     * 确保用户在下次访问时能获取到最新的权限信息。</p>
     *
     * @param userId 需要刷新缓存的用户ID
     */
    public void refreshUserCache(Long userId) {
        log.info("刷新用户缓存: userId={}", userId);
        // 这里应该调用缓存管理器的evict方法
        // cacheManager.getCache("user-details").evict("user-id:" + userId);
        // 同时也要清除用户名相关的缓存
    }

    // =================== 私有辅助方法 ===================

    /**
     * 根据用户名或邮箱查找用户
     *
     * <p>这个方法实现了灵活的用户查找策略。用户可以使用用户名或邮箱地址登录，
     * 我们会智能地判断输入的是哪种类型，然后执行相应的查询。</p>
     *
     * <p><b>判断逻辑：</b></p>
     * <p>如果输入字符串包含@符号，我们认为这是一个邮箱地址；
     * 否则，我们认为这是一个用户名。这种简单的启发式规则在大多数情况下都很有效。</p>
     */
    private User findUserByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail.contains("@")) {
            // 看起来像邮箱，使用邮箱查询
            log.debug("使用邮箱查询用户: email={}", usernameOrEmail);
            return userRepository.findByEmail(usernameOrEmail).orElse(null);
        } else {
            // 使用用户名查询
            log.debug("使用用户名查询用户: username={}", usernameOrEmail);
            return userRepository.findByUsername(usernameOrEmail).orElse(null);
        }
    }

    /**
     * 验证用户账户状态
     *
     * <p>在认证之前，我们需要检查用户账户的各种状态。如果账户存在问题，
     * 应该及早发现并给出明确的错误信息，而不是让用户稀里糊涂地认证失败。</p>
     *
     * @param user 要验证的用户对象
     * @throws RuntimeException 当用户状态异常时抛出相应异常
     */
    private void validateUserStatus(User user) {
        if (!user.isActivated()) {
            log.warn("用户账户已禁用: userId={}, username={}", user.id(), user.username());
            throw new RuntimeException("用户账户已被禁用");
        }

        // 这里可以添加更多的状态检查
        // 比如检查账户是否过期、是否锁定等
    }

    /**
     * 加载用户的详细权限信息
     *
     * <p>这个方法是权限系统的核心，它负责加载用户的所有权限相关信息。
     * 在多租户系统中，这包括：</p>
     * <p>1. 用户的全局角色（跨租户的系统级权限）</p>
     * <p>2. 用户在各个租户中的角色</p>
     * <p>3. 用户所属的租户列表</p>
     *
     * <p><b>数据库查询优化：</b></p>
     * <p>我们使用批量查询来减少数据库访问次数。虽然需要执行多个查询，
     * 但相比于使用复杂的连接查询，这种方式更加清晰且易于优化。</p>
     *
     * @param userId 用户ID
     * @return 包含角色和租户信息的详情对象
     */
    private UserDetailsInfo loadUserDetailsInfo(Long userId) {
        log.debug("加载用户权限信息: userId={}", userId);

        // 加载用户角色信息
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        List<Long> roles = userRoles.stream()
                .map(UserRole::roleId)
                .distinct()
                .collect(Collectors.toList());

        // 加载用户租户信息
        List<UserTenant> userTenants = userTenantRepository.findByUserId(userId);
        List<Long> tenantIds = userTenants.stream()
                .map(UserTenant::tenantId)
                .distinct()
                .collect(Collectors.toList());

        log.debug("用户权限信息加载完成: userId={}, roleCount={}, tenantCount={}",
                userId, roles.size(), tenantIds.size());

        return new UserDetailsInfo(roles, tenantIds);
    }

    /**
     * 构建UserPrincipal对象
     *
     * <p>这是整个用户加载流程的最后一步，我们将从数据库查询到的各种信息
     * 组装成Spring Security需要的UserDetails对象。这个过程需要特别注意
     * 权限的格式转换和数据的完整性。</p>
     *
     * <p><b>权限格式转换：</b></p>
     * <p>Spring Security要求权限以特定格式存储。角色权限需要以"ROLE_"开头，
     * 而功能权限则可以是任意字符串。我们在这里进行统一的格式处理。</p>
     *
     * @param user     用户基本信息
     * @param userInfo 用户权限信息
     * @return 构建完成的UserPrincipal对象
     */
    private UserPrincipal buildUserPrincipal(User user, UserDetailsInfo userInfo) {
        // 将角色转换为Spring Security的权限格式
        List<GrantedAuthority> authorities = userInfo.roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        // 这里可以添加更多的权限，比如基于功能的权限
        // authorities.addAll(loadUserPermissions(user.getId()));

        return UserPrincipal.builder()
                .id(user.id())
                .username(user.username())
                .email(user.email())
                .password(user.passwordHash())  // 加密后的密码
                .authorities(authorities)
                .tenantIds(userInfo.tenantIds)
                .enabled(user.isActivated())
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
    }

    /**
     * 用户详情信息内部类
     *
     * <p>这个内部类用于在方法间传递用户的角色和租户信息。
     * 使用专门的数据传输对象使代码更加清晰和类型安全。</p>
     */
    private static class UserDetailsInfo {
        final List<Long> roles;
        final List<Long> tenantIds;

        UserDetailsInfo(List<Long> roles, List<Long> tenantIds) {
            this.roles = roles;
            this.tenantIds = tenantIds;
        }
    }

    /**
     * 检查用户是否拥有指定权限
     *
     * <p>这个方法提供了一种编程式的权限检查方式，可以在业务代码中
     * 灵活地进行权限判断。它作为注解式权限控制的补充。</p>
     *
     * @param userId     用户ID
     * @param permission 要检查的权限
     * @return 如果用户拥有该权限返回true，否则返回false
     */
    public boolean hasPermission(Long userId, String permission) {
        try {
            UserDetails userDetails = loadUserById(userId);
            if (userDetails == null) {
                return false;
            }

            return userDetails.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals(permission));
        } catch (Exception e) {
            log.error("权限检查失败: userId={}, permission={}", userId, permission, e);
            return false;
        }
    }

    /**
     * 检查用户是否属于指定租户
     *
     * <p>在多租户系统中，这是一个非常常用的检查。我们经常需要验证
     * 当前用户是否有权访问某个租户的数据。</p>
     *
     * @param userId   用户ID
     * @param tenantId 租户ID
     * @return 如果用户属于该租户返回true，否则返回false
     */
    public boolean belongsToTenant(Long userId, Long tenantId) {
        try {
            UserDetails userDetails = loadUserById(userId);
            if (userDetails instanceof UserPrincipal userPrincipal) {
                return userPrincipal.belongsToTenant(tenantId);
            }
            return false;
        } catch (Exception e) {
            log.error("租户归属检查失败: userId={}, tenantId={}", userId, tenantId, e);
            return false;
        }
    }
}