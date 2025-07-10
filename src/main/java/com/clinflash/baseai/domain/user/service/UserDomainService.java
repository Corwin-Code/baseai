package com.clinflash.baseai.domain.user.service;

import com.clinflash.baseai.application.user.dto.UserStatisticsDTO;
import com.clinflash.baseai.domain.user.model.*;
import com.clinflash.baseai.domain.user.repository.*;
import com.clinflash.baseai.infrastructure.utils.UserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * <h2>用户领域服务</h2>
 *
 * <p>领域服务是DDD中的重要概念，它包含了那些不自然属于某个实体或值对象的业务逻辑。
 * 就像现实世界中的专业顾问一样，领域服务提供了跨实体的复杂业务规则和算法。</p>
 *
 * <p><b>设计原则：</b></p>
 * <p>领域服务应该是无状态的，它接收领域对象作为参数，执行业务逻辑，
 * 然后返回结果或新的领域对象。它不应该直接操作基础设施层，
 * 而是通过仓储接口来访问数据。</p>
 *
 * <p><b>核心职责：</b></p>
 * <ul>
 * <li><b>复杂业务规则：</b>如权限检查、邀请管理等跨实体的逻辑</li>
 * <li><b>业务算法：</b>如统计计算、数据分析等算法性逻辑</li>
 * <li><b>领域策略：</b>如激活码生成、邀请令牌管理等策略性逻辑</li>
 * <li><b>不变量维护：</b>确保领域的业务规则和约束得到维护</li>
 * </ul>
 */
@Service
public class UserDomainService {

    private static final Logger log = LoggerFactory.getLogger(UserDomainService.class);

    private final UserRepository userRepo;
    private final TenantRepository tenantRepo;
    private final RoleRepository roleRepo;
    private final UserTenantRepository userTenantRepo;
    private final UserRoleRepository userRoleRepo;

    // 内存缓存用于临时存储激活码和邀请令牌
    private final Map<String, ActivationCodeInfo> activationCodes = new ConcurrentHashMap<>();
    private final Map<String, InvitationTokenInfo> invitationTokens = new ConcurrentHashMap<>();

    public UserDomainService(
            UserRepository userRepo,
            TenantRepository tenantRepo,
            RoleRepository roleRepo,
            UserTenantRepository userTenantRepo,
            UserRoleRepository userRoleRepo) {

        this.userRepo = userRepo;
        this.tenantRepo = tenantRepo;
        this.roleRepo = roleRepo;
        this.userTenantRepo = userTenantRepo;
        this.userRoleRepo = userRoleRepo;
    }

    // =================== 权限验证相关 ===================

    /**
     * 检查用户是否可以邀请成员
     *
     * <p>邀请成员是一个需要特定权限的操作。我们需要检查用户在目标租户中的角色，
     * 确保只有具备相应权限的用户才能发送邀请。这就像公司中只有HR或管理者
     * 才能招聘新员工一样。</p>
     */
    public boolean canInviteMembers(Long userId, Long tenantId) {
        try {
            // 查找用户在租户中的角色
            Optional<UserTenant> userTenant = userTenantRepo.findByUserIdAndTenantId(userId, tenantId);
            if (userTenant.isEmpty()) {
                return false; // 用户不是该租户的成员
            }

            // 检查用户状态是否正常
            if (userTenant.get().status() != TenantMemberStatus.ACTIVE) {
                return false;
            }

            // 获取用户角色
            if (userTenant.get().roleId() == null) {
                return false;
            }

            Optional<Role> role = roleRepo.findById(userTenant.get().roleId());
            // 检查角色是否有邀请权限
            return role.filter(this::hasInvitePermission).isPresent();

        } catch (Exception e) {
            log.error("检查邀请权限失败: userId={}, tenantId={}", userId, tenantId, e);
            return false;
        }
    }

    /**
     * 检查用户是否可以管理成员
     */
    public boolean canManageMembers(Long userId, Long tenantId) {
        try {
            Optional<UserTenant> userTenant = userTenantRepo.findByUserIdAndTenantId(userId, tenantId);
            if (userTenant.isEmpty() || userTenant.get().status() != TenantMemberStatus.ACTIVE) {
                return false;
            }

            if (userTenant.get().roleId() == null) {
                return false;
            }

            Optional<Role> role = roleRepo.findById(userTenant.get().roleId());
            return role.isPresent() && hasManagePermission(role.get());

        } catch (Exception e) {
            log.error("检查管理权限失败: userId={}, tenantId={}", userId, tenantId, e);
            return false;
        }
    }

    /**
     * 检查用户是否是租户的最后一个管理员
     *
     * <p>这是一个重要的业务规则：不能移除租户的最后一个管理员，
     * 否则租户就无人管理了。这就像公司必须至少有一个负责人一样。</p>
     */
    public boolean isLastAdmin(Long userId, Long tenantId) {
        try {
            // 查找所有管理员角色
            List<Role> adminRoles = roleRepo.findAdminRoles();
            if (adminRoles.isEmpty()) {
                return false;
            }

            Set<Long> adminRoleIds = adminRoles.stream()
                    .map(Role::id)
                    .collect(Collectors.toSet());

            // 统计该租户的管理员数量
            long adminCount = userTenantRepo.countByTenantIdAndRoleIds(tenantId, adminRoleIds);

            // 如果只有一个管理员，且是当前用户，则返回true
            if (adminCount == 1) {
                Optional<UserTenant> userTenant = userTenantRepo.findByUserIdAndTenantId(userId, tenantId);
                return userTenant.isPresent() &&
                        adminRoleIds.contains(userTenant.get().roleId());
            }

            return false;

        } catch (Exception e) {
            log.error("检查最后管理员失败: userId={}, tenantId={}", userId, tenantId, e);
            return true; // 出错时采用保守策略
        }
    }

    // =================== 激活码管理 ===================

    /**
     * 生成激活码
     *
     * <p>激活码是确保邮箱真实性的重要机制。我们生成一个随机的激活码，
     * 并设置合理的有效期，然后通过邮件发送给用户。</p>
     */
    public String generateActivationCode(String email) {
        String code = UserUtils.generateRandomCode(8);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(24); // 24小时有效期

        ActivationCodeInfo codeInfo = new ActivationCodeInfo(
                email, code, expiresAt, false
        );

        activationCodes.put(email, codeInfo);

        log.debug("生成激活码: email={}, code={}, expiresAt={}", email, code, expiresAt);
        return code;
    }

    /**
     * 验证激活码
     */
    public boolean isValidActivationCode(String email, String code) {
        try {
            ActivationCodeInfo codeInfo = activationCodes.get(email);
            if (codeInfo == null) {
                return false;
            }

            // 检查是否已使用
            if (codeInfo.used) {
                return false;
            }

            // 检查是否过期
            if (codeInfo.expiresAt.isBefore(OffsetDateTime.now())) {
                activationCodes.remove(email); // 清理过期的激活码
                return false;
            }

            // 验证激活码
            if (!codeInfo.code.equals(code)) {
                return false;
            }

            // 标记为已使用
            activationCodes.put(email, new ActivationCodeInfo(
                    email, code, codeInfo.expiresAt, true
            ));

            return true;

        } catch (Exception e) {
            log.error("验证激活码失败: email={}, code={}", email, code, e);
            return false;
        }
    }

    // =================== 邀请令牌管理 ===================

    /**
     * 生成邀请令牌
     *
     * <p>邀请令牌是一个安全的、一次性的链接标识。它包含了邀请的所有必要信息，
     * 并且有合理的有效期。令牌的设计既要保证安全性，又要便于用户使用。</p>
     */
    public String generateInvitationToken(Long tenantId, String email, Long roleId, Long inviterId) {
        String token = UserUtils.generateSecureToken();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7); // 7天有效期

        InvitationTokenInfo tokenInfo = new InvitationTokenInfo(
                token, tenantId, email, roleId, inviterId, expiresAt, "PENDING"
        );

        invitationTokens.put(token, tokenInfo);

        log.debug("生成邀请令牌: token={}, tenantId={}, email={}", token, tenantId, email);
        return token;
    }

    /**
     * 验证邀请令牌
     */
    public InvitationTokenInfo validateInvitationToken(String token) {
        try {
            InvitationTokenInfo tokenInfo = invitationTokens.get(token);
            if (tokenInfo == null) {
                return null;
            }

            // 检查是否已使用或过期
            if (!"PENDING".equals(tokenInfo.status)) {
                return null;
            }

            if (tokenInfo.expiresAt.isBefore(OffsetDateTime.now())) {
                invitationTokens.remove(token);
                return null;
            }

            return tokenInfo;

        } catch (Exception e) {
            log.error("验证邀请令牌失败: token={}", token, e);
            return null;
        }
    }

    /**
     * 标记邀请为已使用
     */
    public void markInvitationAsUsed(String token) {
        InvitationTokenInfo tokenInfo = invitationTokens.get(token);
        if (tokenInfo != null) {
            invitationTokens.put(token, new InvitationTokenInfo(
                    tokenInfo.token, tokenInfo.tenantId, tokenInfo.email,
                    tokenInfo.roleId, tokenInfo.inviterId, tokenInfo.expiresAt, "USED"
            ));
        }
    }

    /**
     * 标记邀请为已拒绝
     */
    public void markInvitationAsRejected(String token) {
        InvitationTokenInfo tokenInfo = invitationTokens.get(token);
        if (tokenInfo != null) {
            invitationTokens.put(token, new InvitationTokenInfo(
                    tokenInfo.token, tokenInfo.tenantId, tokenInfo.email,
                    tokenInfo.roleId, tokenInfo.inviterId, tokenInfo.expiresAt, "REJECTED"
            ));
        }
    }

    /**
     * 获取待处理邀请
     */
    public List<PendingInvitation> getPendingInvitations(String email) {
        return invitationTokens.values().stream()
                .filter(token -> email.equals(token.email) && "PENDING".equals(token.status))
                .filter(token -> token.expiresAt.isAfter(OffsetDateTime.now()))
                .map(this::toPendingInvitation)
                .collect(Collectors.toList());
    }

    // =================== 统计计算 ===================

    /**
     * 计算用户统计信息
     *
     * <p>这个方法演示了领域服务如何处理复杂的业务算法。
     * 统计计算涉及多个实体和仓储，需要协调各种数据源来产生有意义的业务指标。</p>
     */
    public UserStatisticsDTO calculateUserStatistics(Long tenantId, String timeRange) {
        try {
            OffsetDateTime startDate = calculateStartDate(timeRange);
            OffsetDateTime now = OffsetDateTime.now();

            // 基础统计
            int totalUsers = (int) (tenantId != null ?
                    userTenantRepo.countByTenantId(tenantId) :
                    userRepo.countAll());

            int newUsersThisMonth = (int) userRepo.countByCreatedAtBetween(
                    now.minusMonths(1), now, tenantId);

            int activeUsersThisMonth = (int) userRepo.countByLastLoginAtBetween(
                    now.minusMonths(1), now, tenantId);

            // 计算增长率
            int newUsersLastMonth = (int) userRepo.countByCreatedAtBetween(
                    now.minusMonths(2), now.minusMonths(1), tenantId);

            double userGrowthRate = calculateGrowthRate(newUsersThisMonth, newUsersLastMonth);

            // 按租户统计
            Map<String, Integer> usersByTenant = calculateUsersByTenant();

            // 按角色统计
            Map<String, Integer> usersByRole = calculateUsersByRole(tenantId);

            // 每日统计
            List<UserStatisticsDTO.DailyUserStats> dailyStats = calculateDailyStats(startDate, now, tenantId);

            return new UserStatisticsDTO(
                    totalUsers,
                    newUsersThisMonth,
                    activeUsersThisMonth,
                    userGrowthRate,
                    usersByTenant,
                    usersByRole,
                    dailyStats
            );

        } catch (Exception e) {
            log.error("计算用户统计失败: tenantId={}, timeRange={}", tenantId, timeRange, e);
            return createEmptyStatistics();
        }
    }

    // =================== 私有辅助方法 ===================

    /**
     * 检查角色是否有邀请权限
     */
    private boolean hasInvitePermission(Role role) {
        // 简化实现：管理员和所有者有邀请权限
        return role.name().contains("ADMIN") ||
                role.name().contains("OWNER") ||
                role.name().contains("MANAGER");
    }

    /**
     * 检查角色是否有管理权限
     */
    private boolean hasManagePermission(Role role) {
        return role.name().contains("ADMIN") ||
                role.name().contains("OWNER");
    }

    /**
     * 转换为待处理邀请对象
     */
    private PendingInvitation toPendingInvitation(InvitationTokenInfo tokenInfo) {
        try {
            Optional<Tenant> tenant = tenantRepo.findById(tokenInfo.tenantId);
            Optional<Role> role = roleRepo.findById(tokenInfo.roleId);
            Optional<User> inviter = userRepo.findById(tokenInfo.inviterId);

            return new PendingInvitation(
                    tokenInfo.token,
                    tenant.map(Tenant::orgName).orElse("未知组织"),
                    role.map(Role::label).orElse("未知角色"),
                    inviter.map(User::username).orElse("未知用户"),
                    tokenInfo.expiresAt
            );

        } catch (Exception e) {
            log.error("转换待处理邀请失败: token={}", tokenInfo.token, e);
            return new PendingInvitation(
                    tokenInfo.token, "未知组织", "未知角色", "未知用户", tokenInfo.expiresAt
            );
        }
    }

    /**
     * 计算开始日期
     */
    private OffsetDateTime calculateStartDate(String timeRange) {
        OffsetDateTime now = OffsetDateTime.now();
        return switch (timeRange) {
            case "7d" -> now.minusDays(7);
            case "30d" -> now.minusDays(30);
            case "90d" -> now.minusDays(90);
            case "1y" -> now.minusYears(1);
            default -> now.minusDays(30);
        };
    }

    /**
     * 计算增长率
     */
    private double calculateGrowthRate(int current, int previous) {
        if (previous == 0) {
            return current > 0 ? 1.0 : 0.0;
        }
        return (double) (current - previous) / previous;
    }

    /**
     * 按租户统计用户数
     */
    private Map<String, Integer> calculateUsersByTenant() {
        // 简化实现
        return Map.of();
    }

    /**
     * 按角色统计用户数
     */
    private Map<String, Integer> calculateUsersByRole(Long tenantId) {
        // 简化实现
        return Map.of();
    }

    /**
     * 计算每日统计
     */
    private List<UserStatisticsDTO.DailyUserStats> calculateDailyStats(
            OffsetDateTime startDate, OffsetDateTime endDate, Long tenantId) {
        // 简化实现
        return List.of();
    }

    /**
     * 创建空的统计数据
     */
    private UserStatisticsDTO createEmptyStatistics() {
        return new UserStatisticsDTO(
                0, 0, 0, 0.0,
                Map.of(), Map.of(), List.of()
        );
    }

    // =================== 内部数据结构 ===================

    /**
     * 激活码信息
     */
    private record ActivationCodeInfo(
            String email,
            String code,
            OffsetDateTime expiresAt,
            boolean used
    ) {
    }

    /**
     * 邀请令牌信息
     */
    public record InvitationTokenInfo(
            String token,
            Long tenantId,
            String email,
            Long roleId,
            Long inviterId,
            OffsetDateTime expiresAt,
            String status
    ) {
    }

    /**
     * 待处理邀请
     */
    public record PendingInvitation(
            String token,
            String orgName,
            String roleName,
            String inviterName,
            OffsetDateTime expiresAt
    ) {
    }
}